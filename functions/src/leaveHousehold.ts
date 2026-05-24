import { HttpsError, onCall } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { getFirestore } from "firebase-admin/firestore";

if (admin.apps.length === 0) admin.initializeApp();
const db = admin.firestore();

interface Payload { hid?: string }

export const leaveHousehold = onCall<Payload>(
  { region: "europe-west1" },
  async (request) => {
    const uid = request.auth?.uid;
    if (!uid) throw new HttpsError("unauthenticated", "Sign-in required");

    const hid = (request.data.hid ?? "").toString();
    if (!hid) throw new HttpsError("invalid-argument", "hid required");

    const hhRef = db.collection("households").doc(hid);
    const snap = await hhRef.get();
    if (!snap.exists) throw new HttpsError("not-found", "Household not found");
    const memberUids = (snap.get("memberUids") as string[] | undefined) ?? [];
    if (!memberUids.includes(uid))
      throw new HttpsError("permission-denied", "Not a member");

    if (memberUids.length === 1 && memberUids[0] === uid) {
      // Last member — recursively delete the household and subcollections.
      await getFirestore().recursiveDelete(hhRef);
      await db.collection("users").doc(uid).update({
        households: admin.firestore.FieldValue.arrayRemove(hid),
      });
      return { deleted: true };
    }

    await db.runTransaction(async (tx) => {
      tx.update(hhRef, {
        memberUids: admin.firestore.FieldValue.arrayRemove(uid),
        [`members.${uid}`]: admin.firestore.FieldValue.delete(),
      });
      tx.update(db.collection("users").doc(uid), {
        households: admin.firestore.FieldValue.arrayRemove(hid),
      });
    });
    return { deleted: false };
  }
);
