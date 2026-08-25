package com.lilithsthrone.game.character.containment;

/**
 * 体内收容数据：记录一个角色被收容在宿主体内（子宫/胃）时的状态。
 * 仿 {@link com.lilithsthrone.game.character.pregnancy.Litter} 的风格，作为纯数据载体。
 * 
 * @since 0.4.11.3
 * @version 0.4.11.3
 * @author LSW
 */
public class ContainmentData {
	
	private ContainmentType type;
	private long entryTime;
	private int stage;

	private float healthAtEntry;
	private long settleAt;
	private long releaseAt;

	public ContainmentData(ContainmentType type, long entryTime) {
		this(type, entryTime, 1, -1f, 0L);
	}

	public ContainmentData(ContainmentType type, long entryTime, int stage) {
		this(type, entryTime, stage, -1f, 0L);
	}

	public ContainmentData(ContainmentType type, long entryTime, int stage, float healthAtEntry) {
		this(type, entryTime, stage, healthAtEntry, 0L);
	}

	public ContainmentData(ContainmentType type, long entryTime, int stage, float healthAtEntry, long settleAt) {
		this(type, entryTime, stage, healthAtEntry, settleAt, 0L);
	}

	public ContainmentData(ContainmentType type, long entryTime, int stage, float healthAtEntry, long settleAt, long releaseAt) {
		this.type = type;
		this.entryTime = entryTime;
		this.stage = stage;
		this.healthAtEntry = healthAtEntry;
		this.settleAt = settleAt;
		this.releaseAt = releaseAt;
	}


	
	public ContainmentType getType() {
		return type;
	}
	
	public void setType(ContainmentType type) {
		this.type = type;
	}
	
	public long getEntryTime() {
		return entryTime;
	}
	
	public void setEntryTime(long entryTime) {
		this.entryTime = entryTime;
	}
	
	public int getStage() {
		return stage;
	}
	
	public void setStage(int stage) {
		this.stage = stage;
	}

	public float getHealthAtEntry() {
		return healthAtEntry;
	}

	public void setHealthAtEntry(float healthAtEntry) {
		this.healthAtEntry = healthAtEntry;
	}

	public long getSettleAt() {
		return settleAt;
	}

	public void setSettleAt(long settleAt) {
		this.settleAt = settleAt;
	}

	public long getReleaseAt() {
		return releaseAt;
	}

	public void setReleaseAt(long releaseAt) {
		this.releaseAt = releaseAt;
	}



	
}
