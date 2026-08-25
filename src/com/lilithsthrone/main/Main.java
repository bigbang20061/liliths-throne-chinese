package com.lilithsthrone.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactory;

import com.lilithsthrone.controller.MainController;
import com.lilithsthrone.controller.TooltipUpdateThread;
import com.lilithsthrone.game.Game;
import com.lilithsthrone.game.Properties;
import com.lilithsthrone.game.PropertyValue;
import com.lilithsthrone.game.character.CharacterImportSetting;
import com.lilithsthrone.game.character.PlayerCharacter;
import com.lilithsthrone.game.character.body.valueEnums.Femininity;
import com.lilithsthrone.game.character.gender.Gender;
import com.lilithsthrone.game.character.persona.NameTriplet;
import com.lilithsthrone.game.character.quests.QuestLine;
import com.lilithsthrone.game.character.race.RaceStage;
import com.lilithsthrone.game.character.race.Subspecies;
import com.lilithsthrone.game.combat.Combat;
import com.lilithsthrone.game.dialogue.DialogueNode;
import com.lilithsthrone.game.dialogue.DialogueNodeType;
import com.lilithsthrone.game.dialogue.responses.Response;
import com.lilithsthrone.game.dialogue.story.CharacterCreation;
import com.lilithsthrone.game.dialogue.utils.MapTravelType;
import com.lilithsthrone.game.dialogue.utils.OptionsDialogue;
import com.lilithsthrone.game.sex.Sex;
import com.lilithsthrone.utils.CreditsSlot;
import com.lilithsthrone.utils.Util;
import com.lilithsthrone.utils.colours.PresetColour;
import com.lilithsthrone.world.Generation;
import com.lilithsthrone.world.WorldType;
import com.lilithsthrone.world.places.PlaceType;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/**
 * @since 0.1.0
 * @version 0.4.11
 * @author Innoxia
 */
public class Main extends Application {

	public static Game game;
	public static Sex sex;
	public static Combat combat;

	public static TransformerFactory transformerFactory = TransformerFactory.newInstance();
	private static DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
	private static DocumentBuilder docBuilder;

	public static MainController mainController;

	public static Scene mainScene;

	public static Stage primaryStage;
	
	public static final String AUTHOR = "Innoxia";
	public static final String GAME_NAME = "Lilith's Throne";
	public static final String VERSION_NUMBER = "0.4.11.4"; // Remember to do the stuff below!
	/*
	 * BEFORE BUILDING:
	 * update pom.xml!
	 * use smaller images folder
	 * 
	 * launch4j include JVM options:
	 * -Dbuild.type=exe64 or -Dbuild.type=exe32 as appropriate
	 */
	public static final String VERSION_DESCRIPTION = "Alpha";

	public static boolean quickSaved = false;
	
	private static boolean displayingTurnTimer = false;
	
	/**
	 * To turn it on, just add -Ddebug=true to java's VM options. (You should be able to do this in Eclipse through Run::Run Configurations...::Arguments tab::VM Arguments).
	 * Help page: https://help.eclipse.org/mars/index.jsp?topic=%2Forg.eclipse.pde.doc.user%2Fguide%2Ftools%2Flaunchers%2Farguments.htm
	 *  Or, from the command line java -Ddebug=true -jar LilithsThrone.jar
	 */
	public final static boolean DEBUG = Boolean.valueOf(System.getProperty("debug", "false"));

	public static Image WINDOW_IMAGE;
	
	private static Properties properties;
	
	private static String patchNotes = "";
	
	public static String disclaimer = "<h6 style='text-align: center; color:"+PresetColour.GENERIC_ARCANE.toWebHexString()+";'>你必须阅读并同意以下内容才能进行游戏！</h6>"

			+ "<p>本游戏是一款<b>虚构的</b>色情文字RPG。游戏中包含的所有内容都是虚构宇宙的一部分，与现实生活中的地点、人物或事件无关。<br/><br/>"

			+ "本故事中出现的所有人物均为虚构的实体，他们同意在本故事中出现和表演。"
			+ "如果你对故事中的人物产生担忧，请放心，他们都是按照剧本念台词的成年人。"
			+ "在这个游戏的构建和执行过程中，游戏内描绘的所有角色都没有受到或受到过任何形式的伤害。"
			+ "游戏中的每个角色年龄都至少为18岁(又或者魔法般地至少是你所在国家内允许出现在色情文学中的角色最低年龄)。"
			+  "游戏中的所有角色都和其他角色没有任何形式的血缘关系；在此在此重申，他们只是在按照剧本念台词。<br/><br/>"

			+ "同意此免责声明并游玩本游戏即表明你同意接触到作为本游戏虚构宇宙的一部分出现的性描写和成人内容。"
			+ "此类内容包括但不限于：对性行为的图像描绘、跨种族(与幻想生物的)性行为、性别改变、"
			+ "强奸幻想/对未经同意的暗示、轻度肢体暴力、性暴力和使用药物。<br/>"
			+ "以血腥/极端暴力、排泄物和未成年/不明年龄为例的极端性癖内容<i>不</i>存在于本游戏中。<br/><br/>"

			+ "同意此免责声明并游玩本游戏亦说明你已<b>年满18岁</b>，"
			+ "如果你所在国家的法定年龄超过18岁，则你至少达到了可以购买和观看色情内容的法定年龄。<br/><br/>"

			+ "最后要说明的是，本游戏的创作者希望强调，游戏中的内容完全是虚构的，并不反映他们的任何人的观点或意见。"
			+ "本游戏是本着创作一部艺术互动文学作品的精神制作的，你必须将现实与本游戏中描述的虚构事件明确区分开来。</p>";
	
	public static List<CreditsSlot> credits = new ArrayList<>();

	// World generation:
	public static Generation gen;
	@Override
	public void start(Stage primaryStage) throws Exception {

		CheckForDataDirectory();
		CheckForResFolder();
		
		credits.add(new CreditsSlot("Anonymous", "", 99, 99, 99, 99));
		
		
		credits.add(new CreditsSlot("Kyle S P", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("Paradoxiso", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("luka_fateburn", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("CinnamonSuccubus", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("Luka_Fateburn", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("Phlarx", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("Moro", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("Neo", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("Abaddon_TMZ", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("ForgottenOne", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("Velvet", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("GentleTark", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("QW", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("Master Isami", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("Valeiya", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("Bubbleeey", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("RatKing", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("H3adShotB33otch", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("BerzerkerSteel", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("Dave Ziegler", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("Kaas", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("Dark Miros", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("DethEagle666", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("Mystic Exarch", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("Lucifer", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("A(woo)CE", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("BL4Z3ST0RM", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("~Chai~", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("Scarecrowlust", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("Doomtrack", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("Ookurikara", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("Inferniken", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("Ace Morris", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("Zyrodil", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("Vorst", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("MegaJank", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("Fox-Sama", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("Riaten", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("John Guardian", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("LemonMuffin", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("AuRah", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("shotgunlo", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("Polyfield", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("Homero L", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("UtmostPlatypus", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("Raven Claudius", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("Drakar Bragi", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("The Colonel", "", 0, 0, 0, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("Melone", "", 0, 0, 0, 0, Subspecies.DEMON));
		
		
		credits.add(new CreditsSlot("Adhana Konker", "", 0, 0, 3, 0));
		credits.add(new CreditsSlot("Akira", "", 0, 0, 0, 2));
		credits.add(new CreditsSlot("Aleskah", "", 0, 0, 0, 1));
		credits.add(new CreditsSlot("Lexi <3", "", 0, 0, 0, 1));
		credits.add(new CreditsSlot("Alvinsimon", "", 0, 0, 3, 0));
		credits.add(new CreditsSlot("dragonouv2019", "", 0, 0, 4, 0));
		credits.add(new CreditsSlot("Aklev", "", 0, 0, 3, 0));
		credits.add(new CreditsSlot("AndroidSam", "", 0, 0, 2, 0));
		credits.add(new CreditsSlot("48days", "", 0, 0, 2, 17));
		credits.add(new CreditsSlot("Spaghetti Code", "", 0, 0, 2, 3));
		credits.add(new CreditsSlot("Anonymous_Platypus", "", 0, 0, 4, 0));
		credits.add(new CreditsSlot("Apthydragon", "", 0, 0, 9, 0));
		credits.add(new CreditsSlot("Archan9el S117", "", 0, 0, 0, 12));
		credits.add(new CreditsSlot("SchALLieS", "", 0, 0, 4, 12));
		credits.add(new CreditsSlot("Argmoe", "", 0, 0, 14, 0));
		credits.add(new CreditsSlot("HoneyNutQueerios", "", 0, 0, 16, 0));
		credits.add(new CreditsSlot("Arkhan", "", 0, 0, 6, 0));
		credits.add(new CreditsSlot("Ash", "", 0, 1, 0, 10));
		credits.add(new CreditsSlot("Jack Cloudie", "", 0, 1, 10, 0));
		credits.add(new CreditsSlot("b00marrows", "", 0, 1, 5, 0));
		credits.add(new CreditsSlot("Deimios", "", 0, 0, 3, 7));
		credits.add(new CreditsSlot("Baz GoldenClaw", "", 0, 0, 17, 0));
		credits.add(new CreditsSlot("Mhaak", "", 0, 0, 0, 9));
		credits.add(new CreditsSlot("FidelPinochetov", "", 0, 0, 0, 13));
		credits.add(new CreditsSlot("Tieria", "", 0, 0, 1, 0));
		credits.add(new CreditsSlot("Runehood66", "", 0, 0, 0, 5));
		credits.add(new CreditsSlot("Krissy2017", "", 0, 0, 2, 6));
		credits.add(new CreditsSlot("Blackcanine", "", 0, 0, 17, 0));
		credits.add(new CreditsSlot("Blackheart", "", 0, 0, 1, 3));
		credits.add(new CreditsSlot("Blacktouch", "", 0, 0, 2, 17));
		credits.add(new CreditsSlot("BlakLite", "", 0, 0, 9, 0));
		credits.add(new CreditsSlot("Blue999", "", 0, 0, 0, 19));
		credits.add(new CreditsSlot("BlueWolf", "", 0, 0, 5, 0));
		credits.add(new CreditsSlot("Brandon Stach", "", 0, 0, 15, 0));
		credits.add(new CreditsSlot("BreakerB", "", 0, 0, 6, 0));
		credits.add(new CreditsSlot("BRobort", "", 0, 0, 9, 0));
		credits.add(new CreditsSlot("BloodsailXXII", "", 0, 0, 0, 19));
		credits.add(new CreditsSlot("Burt", "", 0, 0, 6, 0));
		credits.add(new CreditsSlot("Atroykus", "", 0, 0, 0, 6));
		credits.add(new CreditsSlot("Calrak", "", 0, 0, 0, 18, Subspecies.DEMON));
		credits.add(new CreditsSlot("CancerMage", "", 0, 0, 12, 0));
		credits.add(new CreditsSlot("Captain_Sigmus", "", 0, 0, 7, 0));
		credits.add(new CreditsSlot("Casper &quot;Cdaser&quot; D.", "", 0, 0, 10, 0));
		credits.add(new CreditsSlot("CelestialNightmare", "", 0, 0, 0, 15));
		credits.add(new CreditsSlot("Sxythe", "", 0, 0, 0, 2));
		credits.add(new CreditsSlot("Lexi the slut", "", 0, 0, 0, 19));
		credits.add(new CreditsSlot("Chattyneko", "", 0, 0, 10, 0));
		credits.add(new CreditsSlot("Vmpireassassin (Chloe)", "", 0, 0, 0, 5));
		credits.add(new CreditsSlot("cinless", "", 0, 0, 0, 13));
		credits.add(new CreditsSlot("crashtestdummy", "", 0, 0, 9, 5));
		credits.add(new CreditsSlot("Crimson", "", 0, 0, 0, 17));
		credits.add(new CreditsSlot("CrowCorvus", "", 0, 0, 9, 0));
		credits.add(new CreditsSlot("Cryostorm", "", 0, 0, 15, 0));
		credits.add(new CreditsSlot("Cursed Rena", "", 0, 0, 1, 12));
		credits.add(new CreditsSlot("Cynical-Cy", "", 0, 0, 11, 0));
		credits.add(new CreditsSlot("Dace617", "", 0, 0, 0, 4));
		credits.add(new CreditsSlot("Saladofstones", "", 0, 0, 7, 0));
		credits.add(new CreditsSlot("Dan", "", 0, 1, 0, 15));
		credits.add(new CreditsSlot("Hikaru Lightbringer", "", 0, 0, 5, 0));
		credits.add(new CreditsSlot("Griff", "", 0, 0, 3, 0));
		credits.add(new CreditsSlot("Daniel D Magnan", "", 0, 0, 10, 0));
		credits.add(new CreditsSlot("DarKaz", "", 0, 0, 0, 2));
		credits.add(new CreditsSlot("Darthsawyer", "", 0, 0, 6, 0));
		credits.add(new CreditsSlot("Yllarius", "", 0, 0, 2, 0));
		credits.add(new CreditsSlot("DeadEyesSee", "", 0, 0, 11, 0));
		credits.add(new CreditsSlot("DeadMasterZero", "", 0, 0, 8, 6));
		credits.add(new CreditsSlot("CruellerTwo24 ", "", 0, 0, 0, 5));
		credits.add(new CreditsSlot("Demonicgamer666", "", 0, 0, 0, 8));
		credits.add(new CreditsSlot("John Scarlet", "", 0, 0, 0, 5));
		credits.add(new CreditsSlot("Desgax", "", 0, 0, 7, 0));
		credits.add(new CreditsSlot("Destont", "", 0, 0, 16, 0));
		credits.add(new CreditsSlot("Nordo", "", 0, 0, 0, 1));
		credits.add(new CreditsSlot("Di", "", 0, 0, 0, 2));
		credits.add(new CreditsSlot("DLI", "", 0, 0, 0, 14));
		credits.add(new CreditsSlot("suka", "", 0, 0, 19, 0));
		credits.add(new CreditsSlot("rinoskin", "", 0, 0, 0, 6));
		credits.add(new CreditsSlot("Alatar", "", 0, 0, 0, 2));
		credits.add(new CreditsSlot("Elmsdor", "", 0, 0, 0, 19));
		credits.add(new CreditsSlot("EnigmaticYoshi", "", 0, 0, 19, 0));
		credits.add(new CreditsSlot("Endless", "", 0, 0, 6, 2));
		credits.add(new CreditsSlot("Gr33n B3ans", "", 0, 0, 0, 2));
		credits.add(new CreditsSlot("Erin Kyan", "", 0, 0, 13, 0));
		credits.add(new CreditsSlot("Avandemine", "", 0, 0, 1, 11));
		credits.add(new CreditsSlot("Evit", "", 0, 0, 5, 0));
		credits.add(new CreditsSlot("F. Rowan", "", 0, 0, 7, 0));
		credits.add(new CreditsSlot("Farseeker", "", 0, 0, 8, 0));
		credits.add(new CreditsSlot("pupslut felix", "", 0, 0, 0, 12));
		credits.add(new CreditsSlot("Fenrakk101", "", 0, 0, 11, 0));
		credits.add(new CreditsSlot("Fiona", "", 0, 0, 0, 19));
		credits.add(new CreditsSlot("ForeverFree2MeTaMax", "", 0, 0, 14, 0));
		credits.add(new CreditsSlot("FossorTumulus", "", 0, 0, 11, 0));
		credits.add(new CreditsSlot("William E", "", 0, 0, 0, 15));
		credits.add(new CreditsSlot("Freekingamer", "", 0, 0, 0, 10));
		credits.add(new CreditsSlot("fun_bot", "", 0, 0, 0, 3));
		credits.add(new CreditsSlot("Niki Parks", "", 0, 0, 19, 0));
		credits.add(new CreditsSlot("Garkylal", "", 0, 0, 10, 0));
		credits.add(new CreditsSlot("Georgio154", "", 0, 0, 1, 6));
		credits.add(new CreditsSlot("glocknar", "", 0, 0, 11, 0));
		credits.add(new CreditsSlot("Goldmember", "", 0, 0, 0, 3));
		credits.add(new CreditsSlot("Grakcnar", "", 0, 0, 9, 0));
		credits.add(new CreditsSlot("Grave Indignation", "", 0, 0, 4, 0));
		credits.add(new CreditsSlot("GravyRainbow", "", 0, 0, 0, 6));
		credits.add(new CreditsSlot("WodashGSJ", "", 0, 0, 15, 0));
		credits.add(new CreditsSlot("Aceofspades", "", 0, 0, 2, 0));
		credits.add(new CreditsSlot("Assiyalos", "", 0, 0, 9, 0));
		credits.add(new CreditsSlot("Hedgehog", "", 0, 0, 0, 10));
		credits.add(new CreditsSlot("Helyriel", "", 0, 0, 15, 0));
		credits.add(new CreditsSlot("Jatch", "", 0, 0, 11, 0));
		credits.add(new CreditsSlot("JaminGold", "", 0, 0, 4, 0));
		credits.add(new CreditsSlot("Jason Paterson", "", 0, 0, 0, 9));
		credits.add(new CreditsSlot("no1skill", "", 0, 0, 0, 5));
		credits.add(new CreditsSlot("Bocaj91", "", 0, 0, 0, 13));
		credits.add(new CreditsSlot("Krejil", "", 0, 0, 13, 0));
		credits.add(new CreditsSlot("Joeybear", "", 0, 0, 0, 5));
		credits.add(new CreditsSlot("Eushully", "", 0, 0, 0, 12));
		credits.add(new CreditsSlot("Joshua Walter", "", 0, 0, 0, 1));
		credits.add(new CreditsSlot("Garth614", "", 0, 0, 0, 15));
		credits.add(new CreditsSlot("Justicia Anthony", "", 0, 0, 0, 5));
		credits.add(new CreditsSlot("HerrKommissar11", "", 0, 0, 1, 4));
		credits.add(new CreditsSlot("Kaerea", "", 0, 0, 7, 0));
		credits.add(new CreditsSlot("Kaleb the Wise", "", 0, 0, 0, 5));
		credits.add(new CreditsSlot("Karlimero", "", 0, 0, 0, 8));
		credits.add(new CreditsSlot("Tappi", "", 0, 0, 7, 0));
		credits.add(new CreditsSlot("KazukiNero", "", 0, 0, 12, 0));
		credits.add(new CreditsSlot("Kelly999", "", 0, 1, 12, 0));
		credits.add(new CreditsSlot("kenshin5491", "", 0, 0, 18, 0));
		credits.add(new CreditsSlot("Kestrel", "", 0, 0, 19, 0));
		credits.add(new CreditsSlot("BlueVulcan", "", 0, 0, 6, 0));
		credits.add(new CreditsSlot("KingofKings", "", 0, 0, 0, 19));
		credits.add(new CreditsSlot("Kiroberos", "", 0, 0, 0, 17));
		credits.add(new CreditsSlot("Kernog", "", 0, 0, 1, 0));
		credits.add(new CreditsSlot("Knight-Lord Xander", "", 0, 0, 0, 19));
		credits.add(new CreditsSlot("Krozoz", "", 0, 0, 2, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("Krulin", "", 0, 0, 1, 0));
		credits.add(new CreditsSlot("Kyralon", "", 0, 0, 0, 5));
		credits.add(new CreditsSlot("Chris Turpin", "", 0, 0, 17, 0));
		credits.add(new CreditsSlot("Lee Thompson", "", 0, 0, 14, 0));
		credits.add(new CreditsSlot("Leob", "", 0, 0, 10, 4));
		credits.add(new CreditsSlot("Pallid", "", 0, 0, 0, 5));
		credits.add(new CreditsSlot("ilderon", "", 0, 0, 5, 0));
		credits.add(new CreditsSlot("Littlemankitten", "", 0, 0, 0, 12));
		credits.add(new CreditsSlot("LadyofFoxes", "", 0, 0, 2, 0));
		credits.add(new CreditsSlot("Mr L", "", 0, 0, 4, 1));
		credits.add(new CreditsSlot("loveless", "", 0, 0, 0, 19, Subspecies.DEMON));
		credits.add(new CreditsSlot("Vaddex", "", 0, 0, 0, 5));
		credits.add(new CreditsSlot("Kitsune Lyn", "", 0, 0, 0, 10));
		credits.add(new CreditsSlot("Manwhore", "", 0, 0, 0, 3));
		credits.add(new CreditsSlot("Beldamon", "", 0, 0, 0, 10));
		credits.add(new CreditsSlot("matchsticks", "", 0, 0, 10, 0));
		credits.add(new CreditsSlot("masterpuppet", "", 0, 0, 15, 0));
		credits.add(new CreditsSlot("Nightmare", "", 0, 0, 0, 5));
		credits.add(new CreditsSlot("AlphaOneBravo", "", 0, 0, 0, 19));
		credits.add(new CreditsSlot("Max Nobody", "", 0, 0, 0, 19));
		credits.add(new CreditsSlot("Mega", "", 0, 0, 11, 0));
		credits.add(new CreditsSlot("Mia Montane", "", 0, 0, 0, 3, Subspecies.DEMON));
		credits.add(new CreditsSlot("Mora", "", 0, 0, 4, 0));
		credits.add(new CreditsSlot("Muhaku", "", 0, 0, 9, 0));
		credits.add(new CreditsSlot("Neximus", "", 0, 0, 10, 0));
		credits.add(new CreditsSlot("Mylerra", "", 0, 0, 10, 0));
		credits.add(new CreditsSlot("Kobu", "", 0, 0, 0, 14));
		credits.add(new CreditsSlot("Natemare", "", 0, 0, 8, 0));
		credits.add(new CreditsSlot("IreCobra", "", 0, 0, 10, 0));
		credits.add(new CreditsSlot("NeonRaven94", "", 0, 0, 0, 9));
		credits.add(new CreditsSlot("Neon Swaglord Chen", "", 0, 0, 4, 0));
		credits.add(new CreditsSlot("Nexusman", "", 0, 0, 11, 0));
		credits.add(new CreditsSlot("SisterFister420", "", 0, 0, 0, 10));
		credits.add(new CreditsSlot("Nick LaBlue", "", 0, 0, 16, 0));
		credits.add(new CreditsSlot("Kvernik", "", 0, 0, 6, 0));
		credits.add(new CreditsSlot("Niko", "", 0, 0, 16, 0));
		credits.add(new CreditsSlot("Nnxx", "", 0, 1, 3, 2));
		credits.add(new CreditsSlot("NorwegianMonster", "", 0, 0, 0, 6));
		credits.add(new CreditsSlot("Seo Leifthrasir", "", 0, 0, 0, 6));
		credits.add(new CreditsSlot("Odd8Ball", "", 0, 0, 0, 18));
		credits.add(new CreditsSlot("Party Commissar", "", 0, 0, 4, 13));
		credits.add(new CreditsSlot("Patrik Gr&#246;nlund", "", 0, 0, 2, 0));
		credits.add(new CreditsSlot("Totes Amazeballs", "", 0, 0, 0, 3));
		credits.add(new CreditsSlot("Rohsie", "", 0, 0, 0, 10));
		credits.add(new CreditsSlot("P.", "", 0, 0, 0, 4));
		credits.add(new CreditsSlot("BLKCandy", "", 0, 0, 12, 0));
		credits.add(new CreditsSlot("Pierre Mura", "", 0, 0, 0, 11));
		credits.add(new CreditsSlot("Pokys", "", 0, 0, 9, 0));
		credits.add(new CreditsSlot("PoyntFury", "", 0, 0, 1, 4, Subspecies.DEMON));
		credits.add(new CreditsSlot("QQQ", "", 0, 0, 0, 19, Subspecies.DEMON));
		credits.add(new CreditsSlot("awrfyu_", "", 0, 0, 0, 7));
		credits.add(new CreditsSlot("Rakesh", "", 0, 0, 8, 0));
		credits.add(new CreditsSlot("R.W", "", 0, 3, 11, 0, Subspecies.DEMON));
		credits.add(new CreditsSlot("Raruke", "", 0, 0, 3, 0));
		credits.add(new CreditsSlot("The Void Prince", "", 0, 0, 13, 0));
		credits.add(new CreditsSlot("Master's dumb bitch", "", 0, 0, 0, 19));
		credits.add(new CreditsSlot("Arpie", "", 0, 0, 0, 1));
		credits.add(new CreditsSlot("Reila Oda", "", 0, 0, 0, 9));
		credits.add(new CreditsSlot("Roarik", "", 0, 0, 0, 10));
		credits.add(new CreditsSlot("Dark_Lord", "", 0, 0, 2, 6));
		credits.add(new CreditsSlot("redwulfen", "", 0, 0, 0, 18));
		credits.add(new CreditsSlot("Roger Reyne", "", 0, 0, 0, 3));
		credits.add(new CreditsSlot("RogueRandom", "", 0, 0, 16, 0));
		credits.add(new CreditsSlot("Horagen81", "", 0, 0, 0, 13, Subspecies.DEMON));
		credits.add(new CreditsSlot("RyubosJ", "", 0, 0, 6, 0));
		credits.add(new CreditsSlot("Saladine the Legendary", "", 0, 0, 0, 15));
		credits.add(new CreditsSlot("Sand9k", "", 0, 0, 0, 11));
		credits.add(new CreditsSlot("Schande", "", 0, 0, 0, 11));
		credits.add(new CreditsSlot("Scith", "", 0, 0, 5, 0));
		credits.add(new CreditsSlot("Blue Kobold", "", 0, 0, 12, 0));
		credits.add(new CreditsSlot("sebasjac", "", 0, 0, 0, 2));
		credits.add(new CreditsSlot("S", "", 0, 0, 1, 18));
		credits.add(new CreditsSlot("Shas'O Dal'yth Kauyon Kais Taku", "", 0, 0, 19, 0));
		credits.add(new CreditsSlot("Crow Invictus", "", 0, 0, 18, 0));
		credits.add(new CreditsSlot("Sheltem", "", 0, 0, 16, 0));
		credits.add(new CreditsSlot("Shilou", "", 0, 0, 0, 1, Subspecies.DEMON));
		credits.add(new CreditsSlot("shrikes", "", 0, 0, 8, 2));
		credits.add(new CreditsSlot("Sig", "", 0, 0, 4, 0));
		credits.add(new CreditsSlot("Silentark", "", 0, 0, 16, 0));
		credits.add(new CreditsSlot("Sir beans", "", 0, 0, 10, 0));
		credits.add(new CreditsSlot("Sorter", "", 0, 0, 0, 9));
		credits.add(new CreditsSlot("Spectacular", "", 0, 0, 11, 0));
		credits.add(new CreditsSlot("Spencer", "", 0, 0, 0, 7));
		credits.add(new CreditsSlot("Spookermen", "", 0, 0, 0, 19));
		credits.add(new CreditsSlot("Starchiller", "", 0, 0, 0, 8));
		credits.add(new CreditsSlot("Steph", "", 0, 0, 9, 0));
		credits.add(new CreditsSlot("Strigon888", "", 0, 0, 0, 10));
		credits.add(new CreditsSlot("Suvarestin", "", 0, 0, 2, 0));
		credits.add(new CreditsSlot("Swift Shot", "", 0, 0, 14, 0));
		credits.add(new CreditsSlot("TalonMort", "", 0, 0, 17, 0));
		credits.add(new CreditsSlot("Tanall", "", 0, 1, 17, 0));
		credits.add(new CreditsSlot("Tanner D.", "", 0, 0, 0, 6));
		credits.add(new CreditsSlot("Terrance", "", 0, 0, 3, 0));
		credits.add(new CreditsSlot("Testostetyrone", "", 0, 0, 15, 0));
		credits.add(new CreditsSlot("The Brocenary", "", 0, 0, 0, 4));
		credits.add(new CreditsSlot("Jordan Aitken", "", 0, 0, 15, 0));
		credits.add(new CreditsSlot("T. Garou", "", 0, 0, 0, 12));
		credits.add(new CreditsSlot("xerton", "", 0, 0, 3, 0));
		credits.add(new CreditsSlot("Timmybond24", "", 0, 0, 0, 4));
		credits.add(new CreditsSlot("TKaempfer", "", 0, 0, 8, 0));
		credits.add(new CreditsSlot("Tom Clancy's Pro Skater", "", 0, 0, 6, 0));
		credits.add(new CreditsSlot("FreakyHydra", "", 0, 0, 0, 4));
		credits.add(new CreditsSlot("Kahvi_Toope", "", 0, 0, 0, 6));
		credits.add(new CreditsSlot("Torinir", "", 0, 0, 18, 0));
		credits.add(new CreditsSlot("Torsten015", "", 0, 0, 0, 18));
		credits.add(new CreditsSlot("TreenVall", "", 0, 0, 3, 0));
		credits.add(new CreditsSlot("triangleman", "", 0, 0, 16, 0));
		credits.add(new CreditsSlot("Antriad", "", 0, 0, 1, 13));
		credits.add(new CreditsSlot("Jess", "", 0, 0, 3, 0));
		credits.add(new CreditsSlot("Isidoros", "", 0, 0, 7, 0));
		credits.add(new CreditsSlot("SolarEidolon", "", 0, 0, 8, 0));
		credits.add(new CreditsSlot("Vaelin", "", 0, 0, 4, 14));
		credits.add(new CreditsSlot("vasadariu", "", 0, 0, 15, 0));
		credits.add(new CreditsSlot("waaaghkus", "", 0, 0, 19, 0));
		credits.add(new CreditsSlot("Venomy", "", 0, 0, 0, 5));
		credits.add(new CreditsSlot("iloveyouMiaoNiNi", "", 0, 0, 0, 14));
		credits.add(new CreditsSlot("Weegschaal", "", 0, 0, 0, 3));
		credits.add(new CreditsSlot("Whatever", "", 0, 0, 17, 0));
		credits.add(new CreditsSlot("Will Landrum", "", 0, 0, 0, 7));
		credits.add(new CreditsSlot("William Brown", "", 0, 0, 5, 2));
		credits.add(new CreditsSlot("Marys", "", 0, 0, 0, 10));
		credits.add(new CreditsSlot("CMPirate9867", "", 0, 0, 8, 0));
		credits.add(new CreditsSlot("Wolfrave", "", 0, 0, 6, 0));
		credits.add(new CreditsSlot("Wolfregis", "", 0, 0, 0, 19));
		credits.add(new CreditsSlot("Yuki_Sukafu", "", 0, 0, 5, 0));
		credits.add(new CreditsSlot("Nelson Adams", "", 0, 0, 12, 0));
		credits.add(new CreditsSlot("Zakarin", "", 0, 0, 0, 14, Subspecies.DEMON));
		credits.add(new CreditsSlot("Zaya", "", 0, 0, 5, 0));
		credits.add(new CreditsSlot("Zero_One", "", 0, 0, 4, 0));
		
		
		
		credits.sort(Comparator.comparing((CreditsSlot a) -> a.getName().toLowerCase()));
		
		
		Main.primaryStage = primaryStage;
		
		Main.primaryStage.focusedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
				if(t) {
					TooltipUpdateThread.cancelThreads = true;
				}
			}
		});

		WINDOW_IMAGE = new Image("/com/lilithsthrone/res/images/windowIcon32.png");

		Main.primaryStage.getIcons().add(WINDOW_IMAGE);

		refreshTitle();

		loadFonts();
		
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lilithsthrone/res/fxml/main.fxml"));

		Pane pane = loader.load();

		mainScene = new Scene(pane);

		if (properties.hasValue(PropertyValue.lightTheme)) {
			mainScene.getStylesheets().add("/com/lilithsthrone/res/css/stylesheet_light.css");
		} else {
			mainScene.getStylesheets().add("/com/lilithsthrone/res/css/stylesheet.css");
		}

		mainController = loader.getController();
		Main.primaryStage.setScene(mainScene);
		Main.primaryStage.show();
		Main.game = new Game();
		Main.sex = new Sex();
		Main.combat = new Combat();
		
		loader = new FXMLLoader(getClass().getResource("/com/lilithsthrone/res/fxml/main.fxml"));
		try {
			if (Main.mainScene == null) {
				pane = loader.load();
				Main.mainController = loader.getController();

				Main.mainScene = new Scene(pane);
				if (Main.getProperties().hasValue(PropertyValue.lightTheme))
					Main.mainScene.getStylesheets().add("/com/lilithsthrone/res/css/stylesheet_light.css");
				else
					Main.mainScene.getStylesheets().add("/com/lilithsthrone/res/css/stylesheet.css");
			}

			Main.primaryStage.setScene(Main.mainScene);

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Main.game.setContent(new Response("", "", OptionsDialogue.MENU));
		
	}
	
	public static boolean CheckNotUnpacked() {
		File dir = new File("");
		String currentDir = dir.getAbsolutePath();
		String tempDir = System.getProperty("java.io.tmpdir");
//		System.err.println(currentDir);
//		System.err.println(tempDir);
		return currentDir.contains(tempDir);
	}
	
	protected static void CheckForDataDirectory() {
		File dir = new File("data/");
		if(!dir.exists()) {
			Alert a = new Alert(AlertType.ERROR,
					"找不到“data”文件夹("+dir.getAbsolutePath()+")。错误日志存储功能已被禁用。"
							+ "\n请确保游戏已被完整解压，且用户具有写入权限。"
							+ "\n(详情请参阅 README.txt 中的“MISSING FOLDERS(文件丢失)”部分。)"
							+ "\n是否继续？",
					ButtonType.YES, ButtonType.NO);
			System.err.println("Unable to find the 'data' folder ("+dir.getAbsolutePath()+").");
			a.showAndWait().ifPresent(response -> {
			     if (response == ButtonType.NO) {
			         System.exit(1);
			     }
			 });
		}
	}
	
	protected static void CheckForResFolder() {
		File dir = new File("res/");
		if(!dir.exists()) {
			Alert a = new Alert(AlertType.WARNING,
					"找不到“res”文件夹("+dir.getAbsolutePath()+")。该文件丢失将导致游戏缺少文本且导致错误。"
							+ "\n请确保游戏已被完整解压，且用户具有写入权限。"
							+ "\n如果您正在使用命令行或批处理/脚本启动游戏，请尝试在游戏文件夹中启动以防止出现此错误。"
							+ "\n(详情请参阅 README.txt 中的“MISSING FOLDERS(文件丢失)”部分。)"
							+ "\n是否继续？",
					ButtonType.YES, ButtonType.NO);
			System.err.println("Unable to find the 'res' folder ("+dir.getAbsolutePath()+").");
			a.showAndWait().ifPresent(response -> {
				if(response == ButtonType.NO) {
					System.exit(1);
				}
			});
		}
	}

	/**
	 * Attempts to load fallback fonts to make sure that they are available later. The size doesn't actually matter as
	 * the WebEngine will reload other sizes as required. The files referenced must persist until application shutdown.
	 *
	 * Do not call Font.getFamilies() prior to this as additional fonts must be loaded before the list is cached.
	 */
	protected void loadFonts() {
		if (Font.loadFont(toUri("res/fonts/Source Han/SourceHanSansCN-Regular.otf"), 12) != null) {
			Font.loadFont(toUri("res/fonts/Source Han/SourceHanSansCN-Bold.otf"), 12);
		} else {
			System.err.println("Source Han Sans font could not be loaded.");
		}

		// Load fallback for Calibri
		if (Font.loadFont(toUri("res/fonts/Carlito/Carlito-Regular.ttf"), 11) != null) {
			// Load variants
			Font.loadFont(toUri("res/fonts/Carlito/Carlito-Bold.ttf"), 11);
			Font.loadFont(toUri("res/fonts/Carlito/Carlito-BoldItalic.ttf"), 11);
			Font.loadFont(toUri("res/fonts/Carlito/Carlito-Italic.ttf"), 11);
		} else {
			System.err.println("Carlito font could not be loaded.");
		}

		// Load fallback for Verdana
		if (Font.loadFont(toUri("res/fonts/DejaVu Sans/DejaVuSans.ttf"), 12) != null) {
			// Load variants
			Font.loadFont(toUri("res/fonts/DejaVu Sans/DejaVuSans-Bold.ttf"), 12);
			Font.loadFont(toUri("res/fonts/DejaVu Sans/DejaVuSans-BoldOblique.ttf"), 12);
			Font.loadFont(toUri("res/fonts/DejaVu Sans/DejaVuSans-ExtraLight.ttf"), 12);
			Font.loadFont(toUri("res/fonts/DejaVu Sans/DejaVuSans-Oblique.ttf"), 12);
		} else {
			System.err.println("DejaVu Sans font could not be loaded.");
		}
	}

	/**
	 * Creates a URI string with spaces. The given path can be absolute or relative to the current working directory.
	 * @param path The path to convert
	 * @return A string containing a URI
	 */
	public static String toUri(String path) {
		return Paths.get(path).toUri().toString().replaceAll("%20", " ");
	}

	public static DocumentBuilder getDocBuilder() {
		if (docBuilder == null) {
			try {
				docBuilder = docFactory.newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			}
		}
		return docBuilder;
	}

	public static String getPatchNotes() {
		if (!patchNotes.isEmpty()) {
			return patchNotes;
		}

		String prefaceText = "";
		StringBuilder patchNotesText = new StringBuilder();

		File patchNotesFolder = new File("res/patchNotes");
		if (!patchNotesFolder.exists() || !patchNotesFolder.isDirectory()) {
			System.err.println("Folder 'res/patchNotes' could not be loaded!");
			return "";
		}

		File[] fileList = patchNotesFolder.listFiles((dir, name) -> name.endsWith(".html"));
		Arrays.sort(fileList, Collections.reverseOrder());

		for (File file : fileList) {
			Matcher matcher = Pattern.compile("^\\d{8}-(preface|patchnotes)-", Pattern.CASE_INSENSITIVE).matcher(file.getName());
			if (!matcher.find()) {
				continue;
			}

			try (FileInputStream fis = new FileInputStream(file)) {
				String contentHTML = Util.inputStreamToString(fis);
				switch (matcher.group(1).toLowerCase()) {
					case "preface":
						if (prefaceText.isEmpty()) {
							prefaceText = contentHTML;
						}
						break;

					case "patchnotes":
						patchNotesText.append("<br />\n<div class=\"list\">\n").append(contentHTML).append("</div>\n");
						break;
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		patchNotes = prefaceText + patchNotesText;

		return patchNotes;
	}

	public static void main(String[] args) {
		
		// Create folders:
		File dir = new File("data/");
		dir.mkdir();
		dir = new File("data/saves");
		dir.mkdir();
		dir = new File("data/characters");
		dir.mkdir();
		
		
		// Open error log
		if(!DEBUG) {
			System.out.println("Printing to error.log");
			try {
				PrintStream stream = new PrintStream("data/error.log");
				System.setErr(stream);
				System.err.println("Game Version: "+VERSION_NUMBER+" ("+System.getProperty("build.type", "jar")+")");
				System.err.println("Java: "+System.getProperty("java.version")+" ("+System.getProperty("java.vendor")+")");
				System.err.println("OS: "+System.getProperty("os.name")+" ("+System.getProperty("os.arch")+")");
				if (new File("res/mods").exists()) {
					System.err.print("Mod folders present: ");
					int i=0;
					for(File f : new File("res/mods").listFiles()) {
						if(f.isDirectory()) {
							if(i>0) {
								System.err.print(", ");
							}
							System.err.print(f.getName());
						}
						i++;
					}
					System.err.println();
				}
				
				
//				System.err.println("OS: "+System.getProperty("os.name"));
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		// Load properties:
		if (new File("data/properties.xml").exists()) {
			try {
				properties = new Properties();
				properties.loadPropertiesFromXML();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		} else {
			properties = new Properties();
			properties.savePropertiesAsXML();
		}

		launch(args);
	}
	
	/**
	 * Starts a completely new game. Runs a new World Generation.
	 */
	public static void startNewGame(DialogueNode startingDialogueNode) {
		
		Main.game = new Game();
		
		// Generate world:
		if (!(gen == null)) {
			if (gen.isRunning()) {
				gen.cancel();
			}
		}
		
		gen = new Generation();

		gen.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent t) {
				FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lilithsthrone/res/fxml/main.fxml"));
				Pane pane;
				try {
					if (Main.mainScene == null) {
						pane = loader.load();
						Main.mainController = loader.getController();

						Main.mainScene = new Scene(pane);
						if (Main.getProperties().hasValue(PropertyValue.lightTheme))
							Main.mainScene.getStylesheets().add("/com/lilithsthrone/res/css/stylesheet_light.css");
						else
							Main.mainScene.getStylesheets().add("/com/lilithsthrone/res/css/stylesheet.css");
					}

					Main.primaryStage.setScene(Main.mainScene);

				} catch (Exception e) {
					e.printStackTrace();
				}
				
				Main.game.setPlayer(new PlayerCharacter(new NameTriplet("Player"), 1, null, Gender.M_P_MALE, Subspecies.HUMAN, RaceStage.HUMAN, WorldType.MUSEUM, PlaceType.MUSEUM_ENTRANCE));

				Main.game.initNewGame(startingDialogueNode);

				Main.game.endTurn(0);
				
				OptionsDialogue.startingNewGame = false;
				//Main.mainController.processNewDialogue();
			}
		});
		new Thread(gen).start();
	}
	
	public static boolean isVersionOlderThan(String versionToCheck, String versionToCheckAgainst) {
		String[] v1 = versionToCheck.split("\\.");
		String[] v2 = versionToCheckAgainst.split("\\.");
		
		try {
			int maxLength = (v1.length > v2.length) ? v1.length : v2.length;
			for (int i = 0; i < maxLength; i++) {
				int v1i;
				int v2i;
				
				if(v1[1].charAt(0)=='1') { // Versions prior to 0.2.x used an old system of the format: 0.1.10.1 being a lower version than 0.1.9.1:
					v1i = (i < v1.length) ? Integer.valueOf((v1[i]+"00").substring(0, 3)) : 0;
					v2i = (i < v2.length) ? Integer.valueOf((v2[i]+"00").substring(0, 3)) : 0;
					
				} else { // Versions of 0.2.x and higher use a new system of the format: 0.2.10.1 being a higher version than 0.2.9.1:
					v1i = (i < v1.length) ? Integer.valueOf(v1[i]) : 0;
					v2i = (i < v2.length) ? Integer.valueOf(v2[i]) : 0;
				}
				
				if (v1i < v2i) {
					return true;
				} else if (v1i > v2i) {
					return false;
				} 
			}
			
		} catch(Exception ex) {
			return true;
		}
		
		return false;
	}
	
	public static void refreshTitle() {
		Main.primaryStage.setTitle(getTitle());
	}
	
	public static String getTitle() {
		displayingTurnTimer = Main.game!=null && Main.game.isDebugMode() && Main.game.isStarted();
		
		return GAME_NAME
				+ " " + VERSION_NUMBER
				+ " " + VERSION_DESCRIPTION
				+ (DEBUG?" (Debug模式)":"")
				+ (displayingTurnTimer
					?" "+Math.round((Main.game.endTurnTimeTaken/1000000000d)*1000)/1000f+"s"
					:"");
	}
	
	public static boolean isDisplayingTurnTimer() {
		return displayingTurnTimer;
	}
	
	public static int getFontSize() {
		return properties.fontSize;
	}

	public static void setFontSize(int size) {
		properties.fontSize = size;
		properties.savePropertiesAsXML();
	}
	
	public static boolean isQuickSaveAvailable() {
		return Main.game.isStarted()
				&& !Main.game.isInCombat()
				&& !Main.game.isInSex()
				&& Main.game.getCurrentDialogueNode().getDialogueNodeType()==DialogueNodeType.NORMAL
				&& Main.game.isInNeutralDialogue();
	}
	
	public static String getQuickSaveUnavailabilityDescription() {
		if (!Main.game.isInNewWorld()) {
			return "你无法在创建角色期间和序章内保存游戏！";
			
		} else if (Main.game.isInCombat()) {
			return "你无法在战斗中保存游戏！";
			
		} else if (Main.game.isInSex()) {
			return "你无法在性行为场景中保存游戏！";
			
		} else if (Main.game.getCurrentDialogueNode().getDialogueNodeType()!=DialogueNodeType.NORMAL) {
			return "你无法在非中立场景中保存游戏！";
			
		} else if (!Main.game.isStarted() || !Main.game.isInNeutralDialogue()) {
			return "你无法在非中立场景中保存游戏！";
		}
		
		return "";
	}
	
	public static String getQuickSaveName() {
		String name;
		if(!Main.game.isStarted()) {
			name = "快速保存_序章";
		} else {
			name = "快速保存_"+Main.game.getPlayer().getName(false);
		}
		return Main.checkFileName(name);
	}
	
	public static void quickSaveGame() {
		if(isQuickSaveAvailable()){
			Main.getProperties().lastQuickSaveName = getQuickSaveName();
			saveGame(getQuickSaveName(), true, false);
			quickSaved = true;
		} else {
			Main.game.flashMessage(PresetColour.GENERIC_BAD, getQuickSaveUnavailabilityDescription());
		}
	}

	public static void quickLoadGame() {
		String name = "";
		if(quickSaved) {
			name = Main.checkFileName(Main.properties.lastQuickSaveName);
		} else {
			name = Main.checkFileName(getQuickSaveName());
		}

		if(name.isEmpty()) {
			return;
		}
		loadGame(name);
	}

	public static boolean isSaveGameAvailable() {
		return Main.game.isStarted()
				&& ((!Main.game.getSavedDialogueNode().isTravelDisabled() && MapTravelType.WALK_SAFE.isAvailable(Main.game.getPlayerCell(), Main.game.getPlayer()))
						|| Main.game.getSavedDialogueNode().equals(Main.game.getDefaultDialogue(false)));
	}
	
	public static void saveGame(String name, boolean allowOverwrite, boolean isAutoSave) {
		name = Main.checkFileName(name);
		if(name.isEmpty()) {
			return;
		}
		
		Game.exportGame(name, allowOverwrite, isAutoSave);

		try {
			properties.lastSaveLocation = name;//"data/saves/"+name+".lts";
			properties.nameColour = Femininity.valueOf(game.getPlayer().getFemininityValue()).getColour().toWebHexString();
			properties.name = game.getPlayer().getName(false);
			properties.level = game.getPlayer().getLevel();
			properties.money = game.getPlayer().getMoney();
			properties.arcaneEssences = game.getPlayer().getEssenceCount();
			properties.raceColour = game.getPlayer().getSubspecies().getColour(Main.game.getPlayer()).toWebHexString();
			if (game.getPlayer().isFeminine()) {
				properties.race = game.getPlayer().getSubspecies().getSingularFemaleName(game.getPlayer().getBody());
			} else {
				properties.race = game.getPlayer().getSubspecies().getSingularMaleName(game.getPlayer().getBody());
			}
			properties.quest = game.getPlayer().getQuest(QuestLine.MAIN).getName();

			properties.savePropertiesAsXML();

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static String checkFileName(String name) {
		name = name.replace(" ", "_").replaceAll("[/\\\\:\\*\\?\"<>\\|]+", "");
		if (name.length()==0) {
			Main.game.flashMessage(PresetColour.GENERIC_BAD, "名称过短！");
			return "";
		}
		if (name.length() > 64) {
			Main.game.flashMessage(PresetColour.GENERIC_BAD, "名称过长！");
			return "";
		}
		return name;
	}

	public static boolean isLoadGameAvailable(String name) {
		File file = new File("data/saves/"+name+".xml");

		return file.exists();
	}
	
	public static void loadGame(String name) {
		if (isLoadGameAvailable(name)) {
			Game.importGame(name);
			MainController.updateUIButtons();
		}
	}

	public static void loadGame(File f) {
		Game.importGame(f);
		MainController.updateUIButtons();
	}
	
	public static void deleteGame(String name) {
		File file = new File("data/saves/"+name+".xml");

		if (file.exists()) {
			try {
				file.delete();
				Main.game.setContent(new Response("", "", Main.game.getCurrentDialogueNode()));
			} catch (Exception ex) {
				ex.printStackTrace();
			}

		} else {
			Main.game.flashMessage(PresetColour.GENERIC_BAD, "未找到文件……");
		}
	}

	public static void deleteExportedGame(String name) {
		File file = new File("data/saves/"+name+".xml");

		if (file.exists()) {
			try {
				file.delete();
				Main.game.setContent(new Response("", "", Main.game.getCurrentDialogueNode()));
			} catch (Exception ex) {
				ex.printStackTrace();
			}

		} else {
			Main.game.flashMessage(PresetColour.GENERIC_BAD, "未找到文件……");
		}
	}

	public static void deleteExportedCharacter(String name) {
		File file = new File("data/characters/"+name+".xml");

		if (file.exists()) {
			try {
				file.delete();
				Main.game.setContent(new Response("", "", Main.game.getCurrentDialogueNode()));
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			
		} else {
			Main.game.flashMessage(PresetColour.GENERIC_BAD, "未找到文件……");
		}
	}
	
	/**
	 * @param sortAlphabetically Pass in true if you want the files sorted alphabetically, false if you want them sorted by date modified.
	 * @return A list of xml files from the 'data/saves' folder.
	 */
	public static List<File> getSavedGames(boolean sortAlphabetically) {
		List<File> filesList = new ArrayList<>();
		
		File dir = new File("data/saves");
		if (dir.isDirectory()) {
			File[] directoryListing = dir.listFiles((path, name) -> name.endsWith(".xml"));
			if (directoryListing != null) {
				filesList.addAll(Arrays.asList(directoryListing));
			}
		}
		
		if(sortAlphabetically) {
			filesList.sort(Comparator.comparing(File::getName));
		} else {
			filesList.sort(Comparator.comparingLong(File::lastModified).reversed());
		}
		
		return filesList;
	}
	
	public static List<File> getCharactersForImport() {
		List<File> filesList = new ArrayList<>();
		
		File dir = new File("data/characters");
		if (dir.isDirectory()) {
			File[] directoryListing = dir.listFiles((path, name) -> name.endsWith(".xml"));
			if (directoryListing != null) {
				filesList.addAll(Arrays.asList(directoryListing));
			}
		}

		filesList.sort(Comparator.comparingLong(File::lastModified).reversed());
		
		return filesList;
	}
	
	public static List<File> getSlavesForImport() {
		List<File> filesList = new ArrayList<>();
		
		File dir = new File("data/characters");
		if (dir.isDirectory()) {
			File[] directoryListing = dir.listFiles((path, name) -> name.endsWith(".xml"));
			if (directoryListing != null) {
				filesList.addAll(Arrays.asList(directoryListing));
			}
		}
		
		filesList.sort(Comparator.comparingLong(File::lastModified).reversed());
		
		return filesList;
	}
	
	public static List<File> getGamesForImport() {
		List<File> filesList = new ArrayList<>();
		
		File dir = new File("data/saves");
		if (dir.isDirectory()) {
			File[] directoryListing = dir.listFiles((path, name) -> name.endsWith(".xml"));
			if (directoryListing != null) {
				filesList.addAll(Arrays.asList(directoryListing));
			}
		}

		filesList.sort(Comparator.comparingLong(File::lastModified).reversed());
		
		return filesList;
	}
	
	public static void importCharacter(File file) {
		if (file != null) {
			try {
				Main.game.setPlayer(Main.game.getCharacterUtils().startLoadingCharacterFromXML());
				Main.game.setPlayer(Main.game.getCharacterUtils().loadCharacterFromXML(file, Main.game.getPlayer(),
						CharacterImportSetting.NEW_GAME_IMPORT,
						CharacterImportSetting.NO_PREGNANCY,
						CharacterImportSetting.NO_COMPANIONS,
						CharacterImportSetting.NO_ELEMENTAL,
						CharacterImportSetting.CLEAR_SLAVERY,
						CharacterImportSetting.CLEAR_KEY_ITEMS,
						CharacterImportSetting.CLEAR_COMBAT_HISTORY,
						CharacterImportSetting.CLEAR_SEX_HISTORY,
						CharacterImportSetting.REMOVE_RACE_CONCEALED,
						CharacterImportSetting.CLEAR_FAMILY_ID));
				
				Main.game.getPlayer().getSlavesOwned().clear();
				Main.game.getPlayer().endPregnancy(false);
				
				Main.game.setRenderAttributesSection(true);
				Main.game.clearTextStartStringBuilder();
				Main.game.clearTextEndStringBuilder();
				Main.getProperties().setValue(PropertyValue.newWeaponDiscovered, false);
				Main.getProperties().setValue(PropertyValue.newClothingDiscovered, false);
				Main.getProperties().setValue(PropertyValue.newItemDiscovered, false);
				Main.game.getPlayer().calculateStatusEffects(0);

				Main.game.initNewGame(CharacterCreation.START_GAME_WITH_IMPORT);
				
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public static Properties getProperties() {
		return properties;
	}

	public static void saveProperties() {
		properties.savePropertiesAsXML();
	}
}
