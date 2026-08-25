package com.lilithsthrone.game.character.containment;

/**
 * Pure rules for unbirth/vore containment. No {@code Main.game} dependency so it can be integration-tested.
 */
public final class ContainmentRules {

	private ContainmentRules() {
	}

	/** Average orifice depth maps onto this fraction of host height. */
	public static final float BASE_HEIGHT_FRACTION = 0.6f;

	public static float depthFactor(boolean orificesLimitedDepth, float depthPercentage) {
		if(!orificesLimitedDepth) {
			return Float.POSITIVE_INFINITY;
		}
		return depthPercentage;
	}

	/**
	 * Maximum prey height in cm.
	 * {@code hostHeight * 0.6 * depthFactor * (comfort/baseline)}.
	 */
	public static float heightCapCm(int hostHeight, float depthFactor, float comfort, float baseline) {
		if(hostHeight<=0 || baseline<=0 || comfort<=0) {
			return 0f;
		}
		float comfortRatio = comfort / baseline;
		if(Float.isInfinite(depthFactor)) {
			return hostHeight * BASE_HEIGHT_FRACTION * comfortRatio;
		}
		return hostHeight * BASE_HEIGHT_FRACTION * depthFactor * comfortRatio;
	}

	public static float depthOnlyCapCm(int hostHeight, float depthFactor) {
		if(hostHeight<=0) {
			return 0f;
		}
		if(Float.isInfinite(depthFactor)) {
			return Float.MAX_VALUE;
		}
		return hostHeight * BASE_HEIGHT_FRACTION * depthFactor;
	}

	/** True when the combined cap failed because depth (not tightness) is the bottleneck. */
	public static boolean isDepthTheBottleneck(float preyHeight, float depthCap, float combinedCap, boolean unlimitedDepth) {
		return preyHeight >= combinedCap && !unlimitedDepth && preyHeight >= depthCap;
	}

	/**
	 * Lethal digestion is allowed only when the content toggle is on, the prey is not unique,
	 * and {@code NPC.isReadyToBeDeleted()} would return true.
	 */
	public static boolean canDigestPrey(boolean digestionEnabled, boolean unique, boolean readyToBeDeleted) {
		return digestionEnabled && !unique && readyToBeDeleted;
	}

	public static boolean mustReleasePreyBeforeBanishingHost(boolean hostHasPrey) {
		return hostHasPrey;
	}

	public static boolean mustUncontainPreyBeforeBanishingPrey(boolean preyIsContained) {
		return preyIsContained;
	}

	/** STOMACH + digestion toggle → digesting chain; STOMACH otherwise carrying; WOMB always carrying. */
	public static boolean usesDigestionChain(ContainmentType type, boolean digestionEnabled) {
		return type==ContainmentType.STOMACH && digestionEnabled;
	}

	/** Unique NPCs cannot be swallowed; the player is unique but is an allowed exception. */
	public static boolean uniquePreyBlocked(boolean unique, boolean isPlayer) {
		return unique && !isPlayer;
	}

	public static int shrinkTargetHeight(int current, int minimum) {
		if(current<=0) {
			return current;
		}
		return Math.max(minimum, Math.round(current * 0.75f));
	}

	public static boolean canShrinkHeight(int current, int minimum) {
		return shrinkTargetHeight(current, minimum) < current;
	}

	public static boolean canRebirthPrey(boolean unbirthEnabled, boolean wombType, boolean unique, boolean player, boolean readyToBeDeleted) {
		return unbirthEnabled && wombType && !unique && !player && readyToBeDeleted;
	}


	public static boolean shouldClearWombEffects(boolean remainingWomb) {
		return !remainingWomb;
	}

	public static boolean shouldClearStomachEffects(boolean remainingStomach) {
		return !remainingStomach;
	}

	/** Player is not an NPC; lethal digestion cannot run, so the prey must be released. */
	public static boolean playerPreyMustBeReleasedNotDigested(boolean preyIsNpc) {
		return !preyIsNpc;
	}

	/** Struggle escape: roll in [0, bound). 0 succeeds. */
	public static boolean struggleEscapes(int roll, int bound) {
		return bound>0 && roll==0;
	}

	/** Average DIGESTING_1+2+3: 24h + 12h + 6h. Attribute scaling clamps around this. */
	public static final long MIN_STOMACH_DIGEST_SECONDS = (24L + 12L + 6L) * 3600L;
	public static final long MAX_STOMACH_DIGEST_SECONDS = (36L + 24L + 12L) * 3600L;
	public static final long ATTRIBUTE_DIGEST_FLOOR_SECONDS = 18L * 3600L;
	public static final long ATTRIBUTE_DIGEST_CEILING_SECONDS = 96L * 3600L;

	public static long digestDurationSeconds(int extra1Hours, int extra2Hours, int extra3Hours) {
		int e1 = Math.max(0, Math.min(12, extra1Hours));
		int e2 = Math.max(0, Math.min(12, extra2Hours));
		int e3 = Math.max(0, Math.min(6, extra3Hours));
		return 3600L * ((24L + e1) + (12L + e2) + (6L + e3));
	}

	public static float physiqueDigestFactor(float hostPhysique, float preyPhysique) {
		float delta = hostPhysique - preyPhysique;
		float factor = 1f - (delta / 80f);
		if(factor<0.45f) {
			return 0.45f;
		}
		if(factor>1.8f) {
			return 1.8f;
		}
		return factor;
	}

	public static float heightDigestFactor(int hostHeightCm, int preyHeightCm) {
		if(hostHeightCm<=0) {
			return 1f;
		}
		float ratio = preyHeightCm / (float) hostHeightCm;
		float factor = ratio / 0.6f;
		if(factor<0.5f) {
			return 0.5f;
		}
		if(factor>1.8f) {
			return 1.8f;
		}
		return factor;
	}

	public static float materialDigestFactor(String hostMaterial, String preyMaterial) {
		return hostMaterialFactor(hostMaterial) * preyMaterialFactor(preyMaterial);
	}

	public static float hostMaterialFactor(String material) {
		if(material==null) {
			return 1f;
		}
		switch(material) {
			case "FIRE":
				return 0.65f;
			case "SLIME":
				return 0.8f;
			case "STONE":
			case "ICE":
				return 1.15f;
			default:
				return 1f;
		}
	}

	public static float preyMaterialFactor(String material) {
		if(material==null) {
			return 1f;
		}
		switch(material) {
			case "SLIME":
			case "WATER":
			case "AIR":
				return 0.55f;
			case "FIRE":
				return 0.7f;
			case "ICE":
			case "RUBBER":
				return 1.35f;
			case "STONE":
				return 1.7f;
			case "ARCANE":
				return 1.2f;
			default:
				return 1f;
		}
	}

	public static long digestDurationSecondsFromAttributes(
			float hostPhysique,
			float preyPhysique,
			int hostHeightCm,
			int preyHeightCm,
			String hostMaterial,
			String preyMaterial) {
		float factor = physiqueDigestFactor(hostPhysique, preyPhysique)
				* heightDigestFactor(hostHeightCm, preyHeightCm)
				* materialDigestFactor(hostMaterial, preyMaterial);
		long seconds = Math.round(MIN_STOMACH_DIGEST_SECONDS * factor);
		if(seconds<ATTRIBUTE_DIGEST_FLOOR_SECONDS) {
			return ATTRIBUTE_DIGEST_FLOOR_SECONDS;
		}
		if(seconds>ATTRIBUTE_DIGEST_CEILING_SECONDS) {
			return ATTRIBUTE_DIGEST_CEILING_SECONDS;
		}
		return seconds;
	}

	public static int digestStageDurationSeconds(long nowSeconds, long earliestSettleAt, int stagesLeft) {
		int stages = stagesLeft<1?1:stagesLeft;
		if(earliestSettleAt<=0) {
			if(stages>=3) {
				return 24 * 3600;
			}
			if(stages==2) {
				return 12 * 3600;
			}
			return 6 * 3600;
		}
		long remaining = earliestSettleAt - nowSeconds;
		if(remaining<=0) {
			return 1;
		}
		return (int)Math.max(1L, remaining / stages);
	}


	public static boolean stomachPreyReadyToSettle(long nowSeconds, long entryTime, long settleAt) {
		if(settleAt>0) {
			return nowSeconds>=settleAt;
		}
		return nowSeconds>=entryTime && (nowSeconds - entryTime)>=MIN_STOMACH_DIGEST_SECONDS;
	}



	public static boolean shouldRenewFinalCarryingEffect(boolean remainingOfThatType) {
		return remainingOfThatType;
	}


	/** Prefer womb when it is actually available; otherwise stomach. */
	public static String pickAvailableSwallowType(boolean wombOk, boolean stomachOk) {
		if(wombOk) {
			return "WOMB";
		}
		if(stomachOk) {
			return "STOMACH";
		}
		return null;
	}

	/** Prefer snapshot from swallow; if missing and current HP is already 0, give 1. */
	public static float healthToRestoreOnRelease(float healthAtEntry, float currentHealth) {
		if(healthAtEntry>0) {
			return healthAtEntry;
		}
		if(currentHealth>0) {
			return currentHealth;
		}
		return 1f;
	}

	/** CARRYING_1+2 minima (24h+24h) plus CARRYING_3 (7d). */
	public static final long MIN_CARRYING_TO_RELEASE_SECONDS = (24L + 24L + 7L * 24L) * 3600L;
	/** CARRYING_1+2 maxima (36h+36h) plus CARRYING_3 (7d). Old saves without releaseAt use this so they never release early. */
	public static final long MAX_CARRYING_TO_RELEASE_SECONDS = (36L + 36L + 7L * 24L) * 3600L;
	public static final int CARRYING_STAGE_EXTRA_HOURS_BOUND = 13;
	public static final long CARRYING_STAGE_3_SECONDS = 7L * 24L * 3600L;

	public static long carryingDurationSeconds(int extra1Hours, int extra2Hours) {
		int e1 = Math.max(0, Math.min(12, extra1Hours));
		int e2 = Math.max(0, Math.min(12, extra2Hours));
		return 3600L * ((24L + e1) + (24L + e2) + 7L * 24L);
	}

	public static int carryingStageDurationSeconds(long nowSeconds, long releaseAt, int stagesLeft) {
		int stages = stagesLeft<1?1:stagesLeft;
		if(releaseAt<=0) {
			if(stages>=2) {
				return 24 * 3600;
			}
			return (int)CARRYING_STAGE_3_SECONDS;
		}
		long remaining = releaseAt - nowSeconds;
		if(remaining<=0) {
			return 1;
		}
		if(stages>=3) {
			return (int)Math.max(1L, remaining - (24L * 3600L) - CARRYING_STAGE_3_SECONDS);
		}
		if(stages>=2) {
			return (int)Math.max(1L, remaining - CARRYING_STAGE_3_SECONDS);
		}
		return (int)Math.max(1L, remaining);
	}

	public static boolean preyReadyForFinalRelease(long nowSeconds, long entryTime) {
		return preyReadyForFinalRelease(nowSeconds, entryTime, 0L);
	}

	public static boolean preyReadyForFinalRelease(long nowSeconds, long entryTime, long releaseAt) {
		if(releaseAt>0) {
			return nowSeconds>=releaseAt;
		}
		return nowSeconds>=entryTime && (nowSeconds - entryTime)>=MAX_CARRYING_TO_RELEASE_SECONDS;
	}








}
