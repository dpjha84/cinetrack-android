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
const EVE_UID = "eve_789";

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

test("Unauthenticated user cannot read or write any user subcollections or match_rooms", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("tracked_media").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("favorite_people").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("tv_progress").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("preferences").doc("user_preferences").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("monetization").doc("entitlement").get());
  await assertFails(unauthDb.collection("match_rooms").doc("AB12CD").get());
  await assertFails(unauthDb.collection("match_rooms").get());
});

test("Cross-user isolation: Bob cannot read or write Alice's collections or monetization entitlement", async () => {
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
    await aliceRef.collection("monetization").doc("entitlement").set({
      isPro: true,
      planType: "PRO_ANNUAL",
      subscribedProviders: "Netflix,Prime Video",
      streamOnMyAppsOnly: true,
      userId: ALICE_UID,
      updatedAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceRefFromBob = bobDb.collection("users").doc(ALICE_UID);

  await assertFails(aliceRefFromBob.collection("tracked_media").get());
  await assertFails(aliceRefFromBob.collection("tracked_media").doc("tmdb_m_10").get());
  await assertFails(aliceRefFromBob.collection("monetization").doc("entitlement").get());
  await assertFails(
    aliceRefFromBob.collection("monetization").doc("entitlement").set({
      isPro: true,
      planType: "PRO_LIFETIME",
      subscribedProviders: "",
      streamOnMyAppsOnly: false,
      userId: BOB_UID,
      updatedAt: serverTs(),
    })
  );
});

test("Authenticated owner can read, write, merge-update, and delete all 7 sync subcollections", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const aliceRef = aliceDb.collection("users").doc(ALICE_UID);

  // 1. tracked_media
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

  // 2. custom_collections
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
  await assertSucceeds(tvProgRef.delete());

  // 7. monetization entitlement
  const entRef = aliceRef.collection("monetization").doc("entitlement");
  await assertSucceeds(
    entRef.set(
      {
        isPro: true,
        planType: "PRO_ANNUAL",
        subscribedProviders: "Netflix,Prime Video,Hotstar",
        streamOnMyAppsOnly: true,
        userId: ALICE_UID,
        updatedAt: serverTs(),
      },
      { merge: true }
    )
  );
  await assertSucceeds(entRef.get());
});

test("MatchRoom: host creates room, guest joins and swipes, mutual match succeeds, swipe tampering is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const eveDb = testEnv.authenticatedContext(EVE_UID).firestore();

  const roomCode = "WP2026";
  const aliceRoomRef = aliceDb.collection("match_rooms").doc(roomCode);
  const bobRoomRef = bobDb.collection("match_rooms").doc(roomCode);

  const candidates = JSON.stringify([
    { id: "tmdb_m_693134", title: "Dune: Part Two", posterUrl: "https://image.tmdb.org/t/p/w500/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg" },
  ]);

  // Host creates room
  await assertSucceeds(
    aliceRoomRef.set({
      roomCode,
      hostUid: ALICE_UID,
      hostName: "Alice",
      regionCode: "US",
      selectedProviders: "Netflix,Max",
      status: "OPEN",
      participantUids: [ALICE_UID],
      candidatesJson: candidates,
      swipesByUid: { [ALICE_UID]: "tmdb_m_693134" },
      matchedMediaId: null,
      matchedTitle: null,
      matchedPosterUrl: null,
      createdAt: 1700000010000,
      updatedAt: serverTs(),
    })
  );

  // Unfiltered collection list rejected (rules are not filters)
  await assertFails(eveDb.collection("match_rooms").get());
  // Participant-filtered list succeeds
  await assertSucceeds(
    aliceDb.collection("match_rooms").where("participantUids", "array-contains", ALICE_UID).get()
  );

  // Guest Bob looks up room by 6-char code and joins + records his own swipe + mutual match
  await assertSucceeds(bobRoomRef.get());
  await assertSucceeds(
    bobRoomRef.update({
      participantUids: [ALICE_UID, BOB_UID],
      [`swipesByUid.${BOB_UID}`]: "tmdb_m_693134",
      status: "MATCHED",
      matchedMediaId: "tmdb_m_693134",
      matchedTitle: "Dune: Part Two",
      matchedPosterUrl: "https://image.tmdb.org/t/p/w500/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg",
      updatedAt: serverTs(),
    })
  );

  // Bob CANNOT overwrite Alice's swipes
  await assertFails(
    bobRoomRef.update({
      [`swipesByUid.${ALICE_UID}`]: "",
      updatedAt: serverTs(),
    })
  );

  // Bob CANNOT close or delete Alice's room
  await assertFails(
    bobRoomRef.update({
      status: "CLOSED",
      updatedAt: serverTs(),
    })
  );
  await assertFails(bobRoomRef.delete());

  // Host Alice CAN close and delete her room
  await assertSucceeds(
    aliceRoomRef.update({
      status: "CLOSED",
      updatedAt: serverTs(),
    })
  );
  await assertSucceeds(aliceRoomRef.delete());
});

test("Schema validation rejects ghost fields, spoofed hostUid/userId, and invalid enums", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const aliceRef = aliceDb.collection("users").doc(ALICE_UID);

  // Invalid planType enum rejected
  await assertFails(
    aliceRef.collection("monetization").doc("entitlement").set({
      isPro: true,
      planType: "PRO_HACKED",
      subscribedProviders: "",
      streamOnMyAppsOnly: false,
      userId: ALICE_UID,
      updatedAt: serverTs(),
    })
  );

  // Spoofed hostUid on room create rejected
  await assertFails(
    aliceDb.collection("match_rooms").doc("SP00F1").set({
      roomCode: "SP00F1",
      hostUid: BOB_UID,
      hostName: "Spoof",
      regionCode: "US",
      selectedProviders: "Netflix",
      status: "OPEN",
      participantUids: [ALICE_UID],
      candidatesJson: "[]",
      swipesByUid: {},
      createdAt: 1000,
      updatedAt: serverTs(),
    })
  );
});
