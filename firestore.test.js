const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read user profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.doc(`users/${ALICE_UID}`).get());
});

test("Alice can create and read her own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}`).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      displayName: "Alice",
      avatarUrl: "https://example.com/avatar.jpg"
    })
  );
  await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}`).get());
});

test("Bob cannot read or overwrite Alice's profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}`).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      displayName: "Alice"
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.doc(`users/${ALICE_UID}`).get());
  await assertFails(
    bobDb.doc(`users/${ALICE_UID}`).set({
      userId: BOB_UID,
      email: "bob@example.com",
      displayName: "Malicious Bob"
    })
  );
});

test("Alice can create and list her playlists", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/playlists/p1`).set({
      id: "p1",
      userId: ALICE_UID,
      title: "My Chill Playlist",
      description: "Lofi and calm beats",
      coverUrl: "https://example.com/cover.jpg",
      songCount: 10,
      songsJson: "[]",
      isSpotifyImport: false
    })
  );
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/playlists`).get());
});

test("Bob cannot access Alice's playlists", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.doc(`users/${ALICE_UID}/playlists/p1`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/playlists`).get());
});

test("Alice can save and read her settings", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/settings/preferences`).set({
      userId: ALICE_UID,
      audioQuality: "VERY_HIGH",
      stopOnTaskRemoved: true,
      customThemePrompt: "neon emerald synthwave",
      primaryColorHex: "#00FF88",
      accentColorHex: "#FF007F"
    })
  );
  await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}/settings/preferences`).get());
});
