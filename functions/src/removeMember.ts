import { HttpsError, onCall } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";

if (admin.apps.length === 0) admin.initializeApp();
const db = admin.firestore();

interface Payload { hid?: string; uid?: string }

export const removeMember = onCall<Payload>(
  { region: "europe-west1", invoker: "public" },
  async (request) => {
    const callerUid = request.auth?.uid;
    if (!callerUid) throw new HttpsError("unauthenticated", "Sign-in required");

    const hid = (request.data.hid ?? "").toString();
    const targetUid = (request.data.uid ?? "").toString();
    if (!hid || !targetUid)
      throw new HttpsError("invalid-argument", "hid and uid required");

    const hhRef = db.collection("households").doc(hid);
    await db.runTransaction(async (tx) => {
      const snap = await tx.get(hhRef);
      if (!snap.exists) throw new HttpsError("not-found", "Household not found");
      const createdBy = snap.get("createdBy") as string | undefined;
      if (createdBy !== callerUid)
        throw new HttpsError("permission-denied", "Only the creator can remove members");
      if (targetUid === createdBy)
        throw new HttpsError("invalid-argument", "Creator cannot remove themselves; use leave instead");
      const memberUids = (snap.get("memberUids") as string[] | undefined) ?? [];
      if (!memberUids.includes(targetUid))
        throw new HttpsError("not-found", "Target not a member");

      tx.update(hhRef, {
        memberUids: admin.firestore.FieldValue.arrayRemove(targetUid),
        [`members.${targetUid}`]: admin.firestore.FieldValue.delete(),
      });
      tx.update(db.collection("users").doc(targetUid), {
        households: admin.firestore.FieldValue.arrayRemove(hid),
      });
    });
    return { ok: true };
  }
);
