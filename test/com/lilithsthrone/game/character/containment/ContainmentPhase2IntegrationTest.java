package com.lilithsthrone.game.character.containment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Phase 2 integration tests: swallow height cap, digestion eligibility, banish ordering, stomach vs womb data.
 */
public class ContainmentPhase2IntegrationTest {

	@Test
	public void averageDepthMatchesLegacySixtyPercent() {
		float cap = ContainmentRules.heightCapCm(170, 1f, 3f, 3f);
		assertEquals(170f * 0.6f, cap, 0.01f);
	}

	@Test
	public void deepOrificeAllowsNearOwnHeight() {
		float cap = ContainmentRules.heightCapCm(170, 2f, 3f, 3f);
		assertEquals(204f, cap, 0.01f);
		assertTrue(cap > 168f);
	}

	@Test
	public void tightnessScalesOnTopOfDepth() {
		float loose = ContainmentRules.heightCapCm(170, 1f, 6f, 3f);
		float tight = ContainmentRules.heightCapCm(170, 1f, 3f, 3f);
		assertEquals(tight * 2f, loose, 0.01f);
	}

	@Test
	public void unlimitedDepthIgnoresDepthPercentage() {
		float inf = ContainmentRules.depthFactor(false, 0.5f);
		assertTrue(Float.isInfinite(inf));
		float cap = ContainmentRules.heightCapCm(170, inf, 3f, 3f);
		assertEquals(102f, cap, 0.01f);
	}

	@Test
	public void depthBottleneckVsTightnessBottleneck() {
		float depthCap = ContainmentRules.depthOnlyCapCm(170, 1f);
		float combinedTight = ContainmentRules.heightCapCm(170, 1f, 1.5f, 3f);
		assertTrue(ContainmentRules.isDepthTheBottleneck(160, depthCap, depthCap, false));
		assertFalse(ContainmentRules.isDepthTheBottleneck(90, depthCap, combinedTight, false));
	}

	@Test
	public void digestionRequiresToggleAndDeletableNonUniquePrey() {
		assertFalse(ContainmentRules.canDigestPrey(false, false, true));
		assertFalse(ContainmentRules.canDigestPrey(true, true, true));
		assertFalse(ContainmentRules.canDigestPrey(true, false, false));
		assertTrue(ContainmentRules.canDigestPrey(true, false, true));
	}

	@Test
	public void digestionChainOnlyForStomachWhenEnabled() {
		assertFalse(ContainmentRules.usesDigestionChain(ContainmentType.WOMB, true));
		assertFalse(ContainmentRules.usesDigestionChain(ContainmentType.STOMACH, false));
		assertTrue(ContainmentRules.usesDigestionChain(ContainmentType.STOMACH, true));
	}

	@Test
	public void banishHostMustReleasePreyFirst() {
		assertTrue(ContainmentRules.mustReleasePreyBeforeBanishingHost(true));
		assertFalse(ContainmentRules.mustReleasePreyBeforeBanishingHost(false));
		assertTrue(ContainmentRules.mustUncontainPreyBeforeBanishingPrey(true));
	}

	@Test
	public void stomachAndWombDataStayDistinct() {
		ContainmentData womb = new ContainmentData(ContainmentType.WOMB, 1000L, 1);
		ContainmentData stomach = new ContainmentData(ContainmentType.STOMACH, 2000L, 2);
		assertEquals(ContainmentType.WOMB, womb.getType());
		assertEquals(ContainmentType.STOMACH, stomach.getType());
		assertEquals("子宫", ContainmentType.WOMB.getDisplayName());
		assertEquals("胃", ContainmentType.STOMACH.getDisplayName());
		stomach.setStage(3);
		assertEquals(3, stomach.getStage());
	}

	@Test
	public void invalidInputsYieldZeroCap() {
		assertEquals(0f, ContainmentRules.heightCapCm(0, 1f, 3f, 3f), 0.01f);
		assertEquals(0f, ContainmentRules.heightCapCm(170, 1f, 3f, 0f), 0.01f);
	}

	@Test
	public void uniquePlayerIsNotBlockedFromBeingPrey() {
		assertTrue(ContainmentRules.uniquePreyBlocked(true, false));
		assertFalse(ContainmentRules.uniquePreyBlocked(true, true));
		assertFalse(ContainmentRules.uniquePreyBlocked(false, false));
	}

	@Test
	public void shrinkStopsAtMinimumHeight() {
		assertEquals(75, ContainmentRules.shrinkTargetHeight(100, 50));
		assertEquals(50, ContainmentRules.shrinkTargetHeight(50, 50));
		assertFalse(ContainmentRules.canShrinkHeight(50, 50));
		assertTrue(ContainmentRules.canShrinkHeight(100, 50));
	}

	@Test
	public void rebirthOnlyForNonUniqueWombPrey() {
		assertTrue(ContainmentRules.canRebirthPrey(true, true, false, false, true));
		assertFalse(ContainmentRules.canRebirthPrey(false, true, false, false, true));
		assertFalse(ContainmentRules.canRebirthPrey(true, false, false, false, true));
		assertFalse(ContainmentRules.canRebirthPrey(true, true, true, false, true));
		assertFalse(ContainmentRules.canRebirthPrey(true, true, false, true, true));
		assertFalse(ContainmentRules.canRebirthPrey(true, true, false, false, false));
	}


	@Test
	public void mixedPreyClearsOnlyEmptyOrificeChain() {
		assertFalse(ContainmentRules.shouldClearWombEffects(true));
		assertTrue(ContainmentRules.shouldClearWombEffects(false));
		assertFalse(ContainmentRules.shouldClearStomachEffects(true));
		assertTrue(ContainmentRules.shouldClearStomachEffects(false));
	}

	@Test
	public void nonNpcPreyCannotBeDigested() {
		assertTrue(ContainmentRules.playerPreyMustBeReleasedNotDigested(false));
		assertFalse(ContainmentRules.playerPreyMustBeReleasedNotDigested(true));
	}

	@Test
	public void struggleEscapeUsesZeroRoll() {
		assertTrue(ContainmentRules.struggleEscapes(0, 4));
		assertFalse(ContainmentRules.struggleEscapes(1, 4));
		assertFalse(ContainmentRules.struggleEscapes(0, 0));
	}

	@Test
	public void laterStomachPreyIsNotSettledWithEarlierPrey() {

		long t0 = 1_000_000L;
		long ready = t0 + ContainmentRules.MIN_STOMACH_DIGEST_SECONDS;
		assertTrue(ContainmentRules.stomachPreyReadyToSettle(ready, t0, 0L));
		assertFalse(ContainmentRules.stomachPreyReadyToSettle(ready - 1, t0, 0L));
		assertFalse(ContainmentRules.stomachPreyReadyToSettle(ready, t0 + 60, 0L));
		assertTrue(ContainmentRules.shouldRenewFinalCarryingEffect(true));
		assertFalse(ContainmentRules.shouldRenewFinalCarryingEffect(false));
	}

	@Test
	public void combatSwallowFallsBackToStomachWhenWombBlocked() {
		assertEquals("STOMACH", ContainmentRules.pickAvailableSwallowType(false, true));
		assertEquals("WOMB", ContainmentRules.pickAvailableSwallowType(true, true));
		assertEquals("WOMB", ContainmentRules.pickAvailableSwallowType(true, false));
		assertEquals(null, ContainmentRules.pickAvailableSwallowType(false, false));
	}

	@Test
	public void releaseRestoresSnapshotHealthNotZero() {
		assertEquals(80f, ContainmentRules.healthToRestoreOnRelease(80f, 0f), 0.01f);
		assertEquals(10f, ContainmentRules.healthToRestoreOnRelease(-1f, 10f), 0.01f);
		assertEquals(1f, ContainmentRules.healthToRestoreOnRelease(-1f, 0f), 0.01f);
	}

	@Test
	public void manualDigestRequiresFullStomachChain() {
		long t0 = 5_000L;
		assertFalse(ContainmentRules.stomachPreyReadyToSettle(t0 + 3600L, t0, 0L));
		assertTrue(ContainmentRules.stomachPreyReadyToSettle(t0 + ContainmentRules.MIN_STOMACH_DIGEST_SECONDS, t0, 0L));
	}

	@Test
	public void sampledDigestDurationBlocksEarlyManualDigest() {
		assertEquals(ContainmentRules.MIN_STOMACH_DIGEST_SECONDS, ContainmentRules.digestDurationSeconds(0, 0, 0));
		assertEquals(ContainmentRules.MAX_STOMACH_DIGEST_SECONDS, ContainmentRules.digestDurationSeconds(12, 12, 6));
		long t0 = 8_000L;
		long settleAt = t0 + ContainmentRules.digestDurationSeconds(12, 12, 6);
		assertFalse(ContainmentRules.stomachPreyReadyToSettle(t0 + ContainmentRules.MIN_STOMACH_DIGEST_SECONDS, t0, settleAt));
		assertTrue(ContainmentRules.stomachPreyReadyToSettle(settleAt, t0, settleAt));
	}

	@Test
	public void strongerHostDigestsFasterThanWeakerHost() {
		long strongHost = ContainmentRules.digestDurationSecondsFromAttributes(80f, 10f, 180, 90, "FLESH", "FLESH");
		long weakHost = ContainmentRules.digestDurationSecondsFromAttributes(10f, 80f, 180, 90, "FLESH", "FLESH");
		assertTrue(strongHost < weakHost);
		long slimePrey = ContainmentRules.digestDurationSecondsFromAttributes(40f, 40f, 180, 90, "FLESH", "SLIME");
		long stonePrey = ContainmentRules.digestDurationSecondsFromAttributes(40f, 40f, 180, 90, "FLESH", "STONE");
		assertTrue(slimePrey < stonePrey);
		long tinyPrey = ContainmentRules.digestDurationSecondsFromAttributes(40f, 40f, 180, 60, "FLESH", "FLESH");
		long largePrey = ContainmentRules.digestDurationSecondsFromAttributes(40f, 40f, 180, 150, "FLESH", "FLESH");
		assertTrue(tinyPrey < largePrey);
		assertTrue(strongHost >= ContainmentRules.ATTRIBUTE_DIGEST_FLOOR_SECONDS);
		assertTrue(stonePrey <= ContainmentRules.ATTRIBUTE_DIGEST_CEILING_SECONDS);
	}



	@Test
	public void digestStagesSplitRemainingSettleAt() {
		assertEquals(100, ContainmentRules.digestStageDurationSeconds(0, 300, 3));
		assertEquals(150, ContainmentRules.digestStageDurationSeconds(0, 300, 2));
		assertEquals(300, ContainmentRules.digestStageDurationSeconds(0, 300, 1));
		assertEquals(1, ContainmentRules.digestStageDurationSeconds(400, 300, 3));
		assertEquals(24*3600, ContainmentRules.digestStageDurationSeconds(0, 0, 3));
	}

	@Test
	public void laterPlayerPreyIsNotReleasedWithEarlierCarryingTimer() {
		long t0 = 2_000L;
		long later = t0 + 60L;
		long ready = t0 + ContainmentRules.MAX_CARRYING_TO_RELEASE_SECONDS;
		assertTrue(ContainmentRules.preyReadyForFinalRelease(ready, t0));
		assertFalse(ContainmentRules.preyReadyForFinalRelease(ready, later));
	}

	@Test
	public void recordedReleaseAtBeatsMinimumCarryingDuration() {
		long t0 = 2_000L;
		int extra1 = 12;
		int extra2 = 12;
		long releaseAt = t0 + ContainmentRules.carryingDurationSeconds(extra1, extra2);
		assertEquals(ContainmentRules.MAX_CARRYING_TO_RELEASE_SECONDS, releaseAt - t0);
		assertFalse(ContainmentRules.preyReadyForFinalRelease(t0 + ContainmentRules.MIN_CARRYING_TO_RELEASE_SECONDS, t0, releaseAt));
		assertTrue(ContainmentRules.preyReadyForFinalRelease(releaseAt, t0, releaseAt));
		assertEquals(36 * 3600, ContainmentRules.carryingStageDurationSeconds(t0 + 36 * 3600, releaseAt, 2));
		assertEquals(7 * 24 * 3600, ContainmentRules.carryingStageDurationSeconds(t0 + 72 * 3600, releaseAt, 1));
	}





}

