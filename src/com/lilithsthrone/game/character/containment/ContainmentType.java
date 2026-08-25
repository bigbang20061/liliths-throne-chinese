package com.lilithsthrone.game.character.containment;

/**
 * 体内收容的类型（入口/所在器官）。
 * 
 * @since 0.4.11.3
 * @version 0.4.11.3
 * @author LSW
 */
public enum ContainmentType {
	
	/** Unbirth：经阴道进入子宫。 */
	WOMB("子宫"),
	
	/** Vore：经口进入胃。 */
	STOMACH("胃");
	
	private final String displayName;
	
	private ContainmentType(String displayName) {
		this.displayName = displayName;
	}
	
	public String getDisplayName() {
		return displayName;
	}
}
