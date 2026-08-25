package com.lilithsthrone.game.character.body.valueEnums;

import com.lilithsthrone.game.character.gender.Gender;
import com.lilithsthrone.game.settings.ContentPreferenceValue;
import com.lilithsthrone.main.Main;
import com.lilithsthrone.utils.Util;
import com.lilithsthrone.utils.colours.Colour;
import com.lilithsthrone.utils.colours.PresetColour;

/**
 * @since 0.2.11
 * @version 0.2.11
 * @author Innoxia
 */
public enum AgeCategory {
	
	// Always at least 18, as returned by valueOf()
	TEENS_LATE("十八九岁", 18, 20, PresetColour.AGE_TEENS, ContentPreferenceValue.FOUR_HIGH),
	
	TWENTIES_EARLY("二十出头", 20, 23, PresetColour.AGE_TWENTIES, ContentPreferenceValue.FIVE_ABUNDANT),
	
	TWENTIES_MIDDLE("二十五六", 23, 27, PresetColour.AGE_TWENTIES, ContentPreferenceValue.FIVE_ABUNDANT),
	
	TWENTIES_LATE("不到三十", 27, 30, PresetColour.AGE_TWENTIES, ContentPreferenceValue.FOUR_HIGH),
	
	THIRTIES_EARLY("三十出头", 30, 33, PresetColour.AGE_THIRTIES, ContentPreferenceValue.THREE_AVERAGE),
	
	THIRTIES_MIDDLE("三十五六", 33, 37, PresetColour.AGE_THIRTIES, ContentPreferenceValue.THREE_AVERAGE),
	
	THIRTIES_LATE("不到四十", 37, 40, PresetColour.AGE_THIRTIES, ContentPreferenceValue.TWO_LOW),
	
	FORTIES_EARLY("四十出头", 40, 43, PresetColour.AGE_FORTIES, ContentPreferenceValue.TWO_LOW),
	
	FORTIES_MIDDLE("四十五六", 43, 47, PresetColour.AGE_FORTIES, ContentPreferenceValue.ONE_MINIMAL),
	
	FORTIES_LATE("不到五十", 47, 50, PresetColour.AGE_FORTIES, ContentPreferenceValue.ONE_MINIMAL),
	
	FIFTIES_EARLY("五十出头", 50, 53, PresetColour.AGE_FIFTIES, ContentPreferenceValue.ONE_MINIMAL),
	
	FIFTIES_MIDDLE("五十五六", 53, 57, PresetColour.AGE_FIFTIES, ContentPreferenceValue.ZERO_NONE),
	
	FIFTIES_LATE("不到六十", 57, 60, PresetColour.AGE_FIFTIES, ContentPreferenceValue.ZERO_NONE),
	
	SIXTIES_EARLY("六十出头", 60, 63, PresetColour.AGE_SIXTIES, ContentPreferenceValue.ZERO_NONE),
	
	SIXTIES_MIDDLE("六十五六", 63, 67, PresetColour.AGE_SIXTIES, ContentPreferenceValue.ZERO_NONE),
	
	SIXTIES_LATE("不到七十", 67, 70, PresetColour.AGE_SIXTIES, ContentPreferenceValue.ZERO_NONE),
	
	SIXTIES_PLUS("七十岁以上", 70, 100, PresetColour.AGE_SIXTIES, ContentPreferenceValue.ZERO_NONE);

	private String name;
	private int minimumAge;
	private int maximumAge;
	private Colour colour;
	private ContentPreferenceValue agePreferenceDefault;

	private AgeCategory(String name, int minimumAge, int maximumAge, Colour colour, ContentPreferenceValue agePreferenceDefault) {
		this.name = name;
		this.minimumAge = minimumAge;
		this.maximumAge = maximumAge;
		this.colour = colour;
		this.agePreferenceDefault = agePreferenceDefault;
	}

	public int getMinimumValue() {
		return minimumAge;
	}

	public int getMaximumValue() {
		return maximumAge;
	}
	
	public int getMedianValue() {
		return minimumAge + (maximumAge - minimumAge) / 2;
	}

	public static AgeCategory valueOf(int age) {
		if(age<TEENS_LATE.getMinimumValue()) {
			return TEENS_LATE;
		}
		for(AgeCategory f : AgeCategory.values()) {
			if(age>=f.getMinimumValue() && age<f.getMaximumValue()) {
				return f;
			}
		}
		return SIXTIES_PLUS;
	}
	
	public String getName() {
		return name;
	}
	
	public Colour getColour() {
		return colour;
	}

	public ContentPreferenceValue getAgePreferenceDefault() {
		return agePreferenceDefault;
	}
	
	public static int getAgeFromPreferences(Gender gender) {
		AgeCategory category;
		try {
			category = Util.getRandomObjectFromWeightedMap(Main.getProperties().agePreferencesMap.get(gender.getType()));
		} catch(Exception ex) {
			category = AgeCategory.TWENTIES_MIDDLE;
		}
		if(category==null) {
			category = AgeCategory.TWENTIES_MIDDLE;
		}
		
		int lowerBound = category.getMinimumValue();
		int upperBound = category.getMaximumValue();
		return lowerBound + Util.random.nextInt(upperBound-lowerBound);
	}
}
