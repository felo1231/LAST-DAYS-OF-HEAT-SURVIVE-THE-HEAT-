const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_chief_001";
const BOB_UID = "bob_chief_002";
const CHARLIE_UID = "charlie_chief_003";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validSavePayload(uid, overrides = {}) {
  const nowPast = new Date(Date.now() - 2000);
  return {
    userId: uid,
    language: "AR",
    soundEnabled: true,
    totalPower: 2850,
    furnaceLevel: 2,
    furnaceOverdrive: false,
    chapter: 1,
    explorationStage: 3,
    dayNumber: 2,
    dayTimeMinutes: 540,
    lastSavedEpochMs: Date.now(),
    avatarHeroId: "h_sergey",
    frameStyle: "ROYAL_GOLD",
    allianceId: "",
    allianceTag: "",
    dailyGiftLastEpochMs: 0,
    shopPurchasesCount: 0,
    resources: {
      meat: 320.0,
      wood: 420.0,
      coal: 280.0,
      iron: 160.0,
      gems: 500,
      meals: 45.0,
      speedupsMin: 25,
      silverKeys: 4,
      goldKeys: 2,
      heroXp: 650,
    },
    buildings: [{ id: "b_coal", type: "COAL_MINE", level: 2 }],
    survivors: [{ id: "s1", nameEn: "Mina", nameAr: "مينا" }],
    chiefOrders: [{ type: "URGENT_MOBILIZATION", activeRemainingSec: 0, cooldownRemainingSec: 0 }],
    techLevels: [{ id: "EFFICIENT_MINING", level: 1 }],
    troops: { infantry: 35, lancers: 25, marksmen: 25 },
    heroes: [{ id: "h_sergey", level: 5, stars: 2 }],
    formationHeroIds: ["h_sergey", "h_bahiti", "h_molly"],
    quests: { chapter: 1, claimedChapterMissions: [] },
    blizzard: { isActive: false, countdownSec: 90, activeRemainingSec: 0, stormCount: 0 },
    worldNodes: [{ id: "wn_1", type: "POLAR_BEAR", level: 1 }],
    marches: [],
    createdAt: nowPast,
    updatedAt: nowPast,
    ...overrides,
  };
}

function validLeaderboardPayload(uid, overrides = {}) {
  const nowPast = new Date(Date.now() - 2000);
  return {
    userId: uid,
    chiefName: "رمسيس الثاني",
    avatarHeroId: "h_sergey",
    allianceTag: "KMT",
    totalPower: 2850,
    developmentScore: 1450,
    furnaceLevel: 2,
    chapter: 1,
    explorationStage: 3,
    language: "AR",
    createdAt: nowPast,
    updatedAt: nowPast,
    ...overrides,
  };
}

function validWorldCitadelPayload(uid, overrides = {}) {
  const nowPast = new Date(Date.now() - 2000);
  return {
    userId: uid,
    chiefName: "رمسيس الثاني",
    avatarHeroId: "h_sergey",
    frameStyle: "ROYAL_GOLD",
    allianceId: "al_kmt",
    allianceTag: "KMT",
    furnaceLevel: 3,
    totalPower: 3900,
    mapX: 0.34,
    mapY: 0.44,
    shieldActive: false,
    createdAt: nowPast,
    updatedAt: nowPast,
    ...overrides,
  };
}

function validAlliancePayload(allianceId, leaderUid, overrides = {}) {
  const nowPast = new Date(Date.now() - 2000);
  return {
    allianceId,
    name: "حماة الشمس",
    tag: "SUN",
    description: "تحالف ملوك النيل",
    bannerColorHex: 0xFFF6C453,
    leaderUid,
    leaderName: "رمسيس الثاني",
    memberUids: [leaderUid],
    memberCount: 1,
    totalPower: 3900,
    createdAt: nowPast,
    updatedAt: nowPast,
    ...overrides,
  };
}

function validWorldChatPayload(senderUid, overrides = {}) {
  const nowPast = new Date(Date.now() - 2000);
  return {
    senderUid,
    senderName: "رمسيس الثاني",
    avatarHeroId: "h_sergey",
    allianceTag: "SUN",
    text: "تحيا مملكة الشمس!",
    sentEpochMs: Date.now() - 1000,
    createdAt: nowPast,
    ...overrides,
  };
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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("saves").doc("main").get());
  await assertFails(unauthDb.collection("leaderboard").where("totalPower", ">=", 0).get());
  await assertFails(unauthDb.collection("world_citadels").where("totalPower", ">=", 0).get());
  await assertFails(unauthDb.collection("alliances").where("totalPower", ">=", 0).get());
  await assertFails(unauthDb.collection("world_chat").where("sentEpochMs", ">=", 0).get());
  await assertFails(unauthDb.collection("world_citadels").doc(ALICE_UID).set(validWorldCitadelPayload(ALICE_UID)));
});

test("Authenticated owner can create, read, and update cloud save with profile and shop fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("saves").doc("main");

  await assertSucceeds(docRef.set(validSavePayload(ALICE_UID)));
  await assertSucceeds(docRef.get());

  await assertSucceeds(
    docRef.update({
      avatarHeroId: "h_molly",
      frameStyle: "NILE_TURQUOISE",
      shopPurchasesCount: 3,
      dailyGiftLastEpochMs: Date.now(),
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Cross-user access to private cloud saves is denied", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(BOB_UID).collection("saves").doc("main").set(
      validSavePayload(BOB_UID)
    );
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("saves").doc("main").get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("saves").doc("main").update({
      totalPower: 99999,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Leaderboard enforces owner writes, category fields, and filtered queries", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("leaderboard").doc(ALICE_UID).set(validLeaderboardPayload(ALICE_UID)));
  await assertSucceeds(bobDb.collection("leaderboard").doc(BOB_UID).set(validLeaderboardPayload(BOB_UID, { totalPower: 4200 })));

  await assertSucceeds(
    aliceDb.collection("leaderboard").where("totalPower", ">=", 0).orderBy("totalPower", "desc").limit(50).get()
  );
  await assertFails(aliceDb.collection("leaderboard").get());
  await assertFails(
    aliceDb.collection("leaderboard").doc(BOB_UID).update({ totalPower: 1, updatedAt: new Date(Date.now() - 500) })
  );
});

test("World citadels allow owner writes, bounded coordinates, and signed-in listing", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("world_citadels").doc(ALICE_UID).set(validWorldCitadelPayload(ALICE_UID)));
  await assertSucceeds(
    bobDb.collection("world_citadels").where("totalPower", ">=", 0).orderBy("totalPower", "desc").limit(40).get()
  );

  // Out-of-bounds mapX rejected
  await assertFails(
    bobDb.collection("world_citadels").doc(BOB_UID).set(validWorldCitadelPayload(BOB_UID, { mapX: 1.5 }))
  );

  // Cross-user overwrite rejected
  await assertFails(
    bobDb.collection("world_citadels").doc(ALICE_UID).update({ totalPower: 10, updatedAt: new Date(Date.now() - 500) })
  );
});

test("Alliances support leader create, non-leader join/leave, and block leader/tag hijacking", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const allianceRefAlice = aliceDb.collection("alliances").doc("al_sun");
  const allianceRefBob = bobDb.collection("alliances").doc("al_sun");

  await assertSucceeds(allianceRefAlice.set(validAlliancePayload("al_sun", ALICE_UID)));
  await assertSucceeds(aliceDb.collection("alliances").where("totalPower", ">=", 0).orderBy("totalPower", "desc").limit(30).get());

  // Bob joins alliance
  await assertSucceeds(
    allianceRefBob.update({
      memberUids: [ALICE_UID, BOB_UID],
      memberCount: 2,
      totalPower: 6500,
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // Bob cannot hijack alliance name or leaderUid
  await assertFails(
    allianceRefBob.update({
      name: "Hijacked",
      updatedAt: new Date(Date.now() - 300),
    })
  );

  // Bob leaves alliance
  await assertSucceeds(
    allianceRefBob.update({
      memberUids: [ALICE_UID],
      memberCount: 1,
      totalPower: 3900,
      updatedAt: new Date(Date.now() - 200),
    })
  );
});

test("World chat and Alliance chat enforce senderUid and alliance membership", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  // World chat create & query
  await assertSucceeds(aliceDb.collection("world_chat").doc("msg_1").set(validWorldChatPayload(ALICE_UID)));
  await assertFails(aliceDb.collection("world_chat").doc("msg_spoof").set(validWorldChatPayload(BOB_UID)));
  await assertSucceeds(
    bobDb.collection("world_chat").where("sentEpochMs", ">=", 0).orderBy("sentEpochMs", "desc").limit(40).get()
  );

  // Alliance with Alice & Bob (Charlie is NOT a member)
  await aliceDb.collection("alliances").doc("al_sun").set(
    validAlliancePayload("al_sun", ALICE_UID, { memberUids: [ALICE_UID, BOB_UID], memberCount: 2 })
  );

  const msgPayload = {
    ...validWorldChatPayload(ALICE_UID),
    memberUids: [ALICE_UID, BOB_UID],
  };

  await assertSucceeds(aliceDb.collection("alliances").doc("al_sun").collection("messages").doc("am_1").set(msgPayload));

  // Member Bob can query alliance messages
  await assertSucceeds(
    bobDb.collection("alliances").doc("al_sun").collection("messages").where("memberUids", "array-contains", BOB_UID).get()
  );

  // Non-member Charlie cannot post or read alliance messages
  await assertFails(
    charlieDb.collection("alliances").doc("al_sun").collection("messages").doc("am_bad").set({
      ...validWorldChatPayload(CHARLIE_UID),
      memberUids: [ALICE_UID, BOB_UID, CHARLIE_UID],
    })
  );
  await assertFails(
    charlieDb.collection("alliances").doc("al_sun").collection("messages").doc("am_1").get()
  );
});
