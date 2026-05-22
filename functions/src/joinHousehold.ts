import { HttpsError, onCall } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";

if (admin.apps.length === 0) admin.initializeApp();
const db = admin.firestore();

interface Payload {
  code?: string;
}

export const joinHousehold = onCall<Payload>(
  { region: "europe-west1" },
  async (request) => {
    const uid = request.auth?.uid;
    if (!uid) throw new HttpsError("unauthenticated", "Sign-in required");

    const code = (request.data.code ?? "").toString().toUpperCase().trim();
    if (code.length !== 6)
      throw new HttpsError(
        "invalid-argument",
        "Invite code must be 6 characters"
      );

    const matches = await db
      .collection("households")
      .where("inviteCode", "==", code)
      .limit(1)
      .get();
    if (matches.empty)
      throw new HttpsError("not-found", "No household for that code");

    const householdDoc = matches.docs[0];
    const memberUids: string[] = householdDoc.get("memberUids") ?? [];
    if (memberUids.includes(uid))
      throw new HttpsError("already-exists", "Already a member");

    await db.runTransaction(async (tx) => {
      tx.update(householdDoc.ref, {
        memberUids: admin.firestore.FieldValue.arrayUnion(uid),
      });
      tx.set(
        db.collection("users").doc(uid),
        { households: admin.firestore.FieldValue.arrayUnion(householdDoc.id) },
        { merge: true }
      );
    });

    return { householdId: householdDoc.id };
  }
);
