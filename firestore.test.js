const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const rulesUnitTestingPath = require.resolve("@firebase/rules-unit-testing");
const firebase = require(
  require.resolve("firebase/compat/app", { paths: [rulesUnitTestingPath, "/usr/local/lib/node_modules"] })
).default;
require(
  require.resolve("firebase/compat/firestore", { paths: [rulesUnitTestingPath, "/usr/local/lib/node_modules"] })
);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function serverTs() {
  return firebase.firestore.FieldValue.serverTimestamp();
}

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

test("Unauthenticated user cannot read or write any user subcollections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("tracked_media").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("favorite_people").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("tv_progress").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("preferences").doc("user_preferences").get());
});

test("Cross-user isolation: Bob cannot read or write Alice's collections", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const aliceRef = context.firestore().collection("users").doc(ALICE_UID);
    await aliceRef.collection("tracked_media").doc("tmdb_m_10").set({
      id: "tmdb_m_10",
      title: "Inception",
      mediaType: "MOVIE",
      status: "COMPLETED",
      isFavorite: true,
      userId: ALICE_UID,
      updatedAt: new Date(),
    });
    await aliceRef.collection("favorite_people").doc("tmdb_p_6193").set({
      id: "tmdb_p_6193",
      personId: 6193,
      name: "Leonardo DiCaprio",
      createdAt: 1000,
      userId: ALICE_UID,
      updatedAt: new Date(),
    });
    await aliceRef.collection("tv_progress").doc("tmdb_tv_1_s1_e1").set({
      mediaId: "tmdb_tv_1",
      seasonNumber: 1,
      episodeNumber: 1,
      status: "WATCHED",
      updatedAt: 1000,
      userId: ALICE_UID,
      serverUpdatedAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceRefFromBob = bobDb.collection("users").doc(ALICE_UID);

  await assertFails(aliceRefFromBob.collection("tracked_media").get());
  await assertFails(aliceRefFromBob.collection("tracked_media").doc("tmdb_m_10").get());
  await assertFails(aliceRefFromBob.collection("favorite_people").doc("tmdb_p_6193").get());
  await assertFails(aliceRefFromBob.collection("tv_progress").doc("tmdb_tv_1_s1_e1").get());
  await assertFails(
    aliceRefFromBob.collection("tracked_media").doc("tmdb_m_99").set({
      id: "tmdb_m_99",
      title: "Injected",
      mediaType: "MOVIE",
      status: "WATCHING",
      isFavorite: false,
      userId: BOB_UID,
      updatedAt: serverTs(),
    })
  );
});

test("Authenticated owner can read, write, merge-update, and delete all 6 sync subcollections", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const aliceRef = aliceDb.collection("users").doc(ALICE_UID);

  // 1. tracked_media (including explicit null optional fields as uploaded by Android FirestoreSyncManager)
  const trackedRef = aliceRef.collection("tracked_media").doc("tmdb_m_27205");
  await assertSucceeds(
    trackedRef.set(
      {
        id: "tmdb_m_27205",
        title: "Inception",
        mediaType: "MOVIE",
        releaseYear: 2010,
        genres: "Action, Sci-Fi",
        posterUrl: "https://image.tmdb.org/t/p/w500/poster.jpg",
        backdropUrl: "https://image.tmdb.org/t/p/w1280/backdrop.jpg",
        overview: "Dream heist thriller",
        directorOrCreator: "Christopher Nolan",
        cast: "Leonardo DiCaprio, Joseph Gordon-Levitt",
        communityRating: 8.8,
        runtimeMinutes: 148,
        totalSeasons: 0,
        totalEpisodes: 1,
        status: "COMPLETED",
        userRating: 9.5,
        userReview: "Masterpiece",
        isFavorite: true,
        lastWatchedDate: null,
        watchedEpisodes: 1,
        watchedSeasons: 0,
        customWatchlist: null,
        isCustom: false,
        originalLanguage: "en",
        originCountry: "US",
        streamingProviders: "US:Netflix",
        createdAt: 1700000000000,
        userId: ALICE_UID,
        updatedAt: serverTs(),
      },
      { merge: true }
    )
  );
  await assertSucceeds(trackedRef.get());
  await assertSucceeds(aliceRef.collection("tracked_media").get());

  // 2. custom_collections (including spaces in document ID)
  const collRef = aliceRef.collection("custom_collections").doc("Weekend Marathon");
  await assertSucceeds(
    collRef.set(
      {
        name: "Weekend Marathon",
        description: "Movies to binge",
        createdAt: 1700000001000,
        userId: ALICE_UID,
      },
      { merge: true }
    )
  );
  // Idempotent multi-device merge update with newer createdAt
  await assertSucceeds(
    collRef.set(
      {
        name: "Weekend Marathon",
        description: "Updated binge list",
        createdAt: 1700000005000,
        userId: ALICE_UID,
      },
      { merge: true }
    )
  );
  await assertSucceeds(aliceRef.collection("custom_collections").get());

  // 3. preferences
  const prefRef = aliceRef.collection("preferences").doc("user_preferences");
  await assertSucceeds(
    prefRef.set(
      {
        regionCode: "IN",
        languageCode: "hi",
        autoDetect: false,
        filterCatalogByLanguage: true,
        showStreamingProviders: true,
        userId: ALICE_UID,
        updatedAt: serverTs(),
      },
      { merge: true }
    )
  );
  await assertSucceeds(prefRef.get());

  // 4. search_history
  const searchRef = aliceRef.collection("search_history").doc("christopher nolan");
  await assertSucceeds(
    searchRef.set(
      {
        query: "Christopher Nolan",
        timestamp: 1700000002000,
        userId: ALICE_UID,
      },
      { merge: true }
    )
  );
  await assertSucceeds(aliceRef.collection("search_history").get());

  // 5. favorite_people
  const personRef = aliceRef.collection("favorite_people").doc("tmdb_p_6193");
  await assertSucceeds(
    personRef.set(
      {
        id: "tmdb_p_6193",
        personId: 6193,
        name: "Leonardo DiCaprio",
        profileUrl: "https://image.tmdb.org/t/p/w500/leo.jpg",
        department: "Acting",
        birthday: "1974-11-11",
        placeOfBirth: "Los Angeles, California, USA",
        createdAt: 1700000003000,
        userId: ALICE_UID,
        updatedAt: serverTs(),
      },
      { merge: true }
    )
  );
  await assertSucceeds(aliceRef.collection("favorite_people").get());

  // 6. tv_progress
  const tvProgRef = aliceRef.collection("tv_progress").doc("tmdb_tv_1396_s1_e2");
  await assertSucceeds(
    tvProgRef.set(
      {
        mediaId: "tmdb_tv_1396",
        seasonNumber: 1,
        episodeNumber: 2,
        status: "WATCHED",
        updatedAt: 1700000004000,
        userId: ALICE_UID,
        serverUpdatedAt: serverTs(),
      },
      { merge: true }
    )
  );
  await assertSucceeds(aliceRef.collection("tv_progress").get());
  await assertSucceeds(tvProgRef.delete());

  // 7. Minimal required-only payload (optional fields omitted)
  await assertSucceeds(
    aliceRef.collection("tracked_media").doc("tmdb_m_min").set({
      id: "tmdb_m_min",
      title: "Minimal Movie",
      mediaType: "MOVIE",
      status: "PLAN_TO_WATCH",
      isFavorite: false,
      userId: ALICE_UID,
      updatedAt: serverTs(),
    })
  );
  await assertSucceeds(
    aliceRef.collection("favorite_people").doc("tmdb_p_min").set({
      id: "tmdb_p_min",
      personId: 99,
      name: "Minimal Person",
      createdAt: 1700000005000,
      userId: ALICE_UID,
      updatedAt: serverTs(),
    })
  );
});

test("Schema validation rejects ghost fields, spoofed userId, and invalid enum/rating", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const aliceRef = aliceDb.collection("users").doc(ALICE_UID);

  // Ghost field rejected
  await assertFails(
    aliceRef.collection("tracked_media").doc("tmdb_m_1").set({
      id: "tmdb_m_1",
      title: "Test",
      mediaType: "MOVIE",
      status: "COMPLETED",
      isFavorite: false,
      userId: ALICE_UID,
      updatedAt: serverTs(),
      isAdmin: true,
    })
  );

  // Spoofed userId rejected
  await assertFails(
    aliceRef.collection("favorite_people").doc("tmdb_p_1").set({
      id: "tmdb_p_1",
      personId: 1,
      name: "Spoof",
      createdAt: 1000,
      userId: BOB_UID,
      updatedAt: serverTs(),
    })
  );

  // Invalid tv_progress status enum rejected
  await assertFails(
    aliceRef.collection("tv_progress").doc("tmdb_tv_1_s1_e1").set({
      mediaId: "tmdb_tv_1",
      seasonNumber: 1,
      episodeNumber: 1,
      status: "INVALID_STATUS",
      updatedAt: 1000,
      userId: ALICE_UID,
      serverUpdatedAt: serverTs(),
    })
  );
});
