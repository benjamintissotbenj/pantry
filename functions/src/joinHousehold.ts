import { HttpsError, onCall } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";

if (admin.apps.length === 0) admin.initializeApp();
const db = admin.firestore();

interface Payload {
  code?: string;
}

export const joinHousehold = onCall<Payload>(
  { region: "europe-west1", invoker: "public" },
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
      const userDoc = await tx.get(db.collection("users").doc(uid));
      // Prefer stored profile values; fall back to the Firebase Auth token fields which
      // are always present and authoritative (covers email+password accounts that have
      // no displayName/email written to their Firestore user doc yet).
      const tokenName  = (request.auth!.token as Record<string, unknown>).name  as string | undefined;
      const tokenEmail = (request.auth!.token as Record<string, unknown>).email as string | undefined;
      const displayName = ((userDoc.get("displayName") as string | undefined) || tokenName  || "").trim();
      const email       = ((userDoc.get("email")       as string | undefined) || tokenEmail || "").trim();

      const memberPath = `members.${uid}`;
      tx.update(householdDoc.ref, {
        memberUids: admin.firestore.FieldValue.arrayUnion(uid),
        [memberPath]: { displayName, email },
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
