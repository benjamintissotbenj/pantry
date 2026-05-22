// Env vars (GCLOUD_PROJECT, FIRESTORE_EMULATOR_HOST) are set by test/setup.ts
// which is loaded by Jest before any module code runs, so firebase-admin picks
// up the emulator host on its first initializeApp() call inside joinHousehold.ts.
import * as admin from "firebase-admin";
import { joinHousehold } from "../src/joinHousehold";

// joinHousehold.ts calls initializeApp() at module load — reuse that app instance.
const db = admin.firestore();

function callable(impl: any) {
  // Helper to invoke the v2 callable handler directly via its .run() method.
  return async (data: any, auth?: { uid: string }) => {
    return impl.run({ data, auth });
  };
}

describe("joinHousehold", () => {
  const callJoin = callable(joinHousehold);

  beforeEach(async () => {
    // Wipe Firestore between tests so each test starts from a clean state.
    const cols = await db.listCollections();
    await Promise.all(cols.map((c) => deleteCollection(c)));
  });

  test("rejects unauthenticated callers", async () => {
    await expect(callJoin({ code: "ABCDEF" })).rejects.toMatchObject({
      code: "unauthenticated",
    });
  });

  test("rejects invalid code length", async () => {
    await expect(
      callJoin({ code: "ABC" }, { uid: "u-1" })
    ).rejects.toMatchObject({ code: "invalid-argument" });
  });

  test("returns not-found when code does not match", async () => {
    await expect(
      callJoin({ code: "AAAAAA" }, { uid: "u-1" })
    ).rejects.toMatchObject({ code: "not-found" });
  });

  test("adds the user to the household on first join", async () => {
    const ref = await db
      .collection("households")
      .add({ name: "Casa", inviteCode: "ABCDEF", memberUids: ["u-1"] });
    const result = await callJoin({ code: "ABCDEF" }, { uid: "u-2" });
    expect(result).toEqual({ householdId: ref.id });
    const fresh = await ref.get();
    expect(fresh.get("memberUids")).toContain("u-2");
    const user = await db.collection("users").doc("u-2").get();
    expect(user.get("households")).toContain(ref.id);
  });

  test("returns already-exists if already a member", async () => {
    await db
      .collection("households")
      .add({ name: "Casa", inviteCode: "ABCDEF", memberUids: ["u-1"] });
    await expect(
      callJoin({ code: "ABCDEF" }, { uid: "u-1" })
    ).rejects.toMatchObject({ code: "already-exists" });
  });
});

async function deleteCollection(
  col: FirebaseFirestore.CollectionReference
): Promise<void> {
  const docs = await col.listDocuments();
  await Promise.all(docs.map((d) => d.delete()));
}
