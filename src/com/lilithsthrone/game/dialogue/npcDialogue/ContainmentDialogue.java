package com.lilithsthrone.game.dialogue.npcDialogue;

import com.lilithsthrone.game.character.GameCharacter;
import com.lilithsthrone.game.character.containment.ContainmentType;
import com.lilithsthrone.game.dialogue.DialogueNode;
import com.lilithsthrone.game.dialogue.DialogueNodeType;
import com.lilithsthrone.game.dialogue.responses.Response;
import com.lilithsthrone.game.dialogue.utils.UtilText;
import com.lilithsthrone.main.Main;

import com.lilithsthrone.utils.Util;

import com.lilithsthrone.world.WorldType;
import com.lilithsthrone.world.places.PlaceType;

/**
 * First-person dialogue when the player is contained inside another character.
 * Assigned to {@link PlaceType#GENERIC_CONTAINMENT_CELL} so save/load does not NPE.
 */
public class ContainmentDialogue {

	private static GameCharacter getHost() {
		if(Main.game.getPlayer()==null || !Main.game.getPlayer().isContained()) {
			return null;
		}
		String hostId = Main.game.getPlayer().getContainmentHostId();
		if(hostId==null || hostId.isEmpty()) {
			return null;
		}
		try {
			return Main.game.getNPCById(hostId);
		} catch(Exception e) {
			return null;
		}
	}

	private static ContainmentType getPlayerContainmentType() {
		GameCharacter host = getHost();
		if(host==null) {
			return null;
		}
		com.lilithsthrone.game.character.containment.ContainmentData data = host.getContainedCharacters().get(Main.game.getPlayer().getId());
		return data==null?null:data.getType();
	}

	public static final DialogueNode INSIDE_HOST = new DialogueNode("体内", "你正被容纳在另一个人的身体里。", true) {
		@Override
		public DialogueNodeType getDialogueNodeType() {
			return DialogueNodeType.NORMAL;
		}
		@Override
		public boolean isTravelDisabled() {
			return Main.game.getPlayer()!=null && Main.game.getPlayer().isContained();
		}


		@Override
		public String getContent() {
			GameCharacter host = getHost();
			if(host==null) {
				return "<p>环绕着你的肉壁忽然松开。你被释放到了体外。</p>";
			}
			if(host instanceof com.lilithsthrone.game.character.npc.NPC) {
				Main.game.setParserTarget("host", (com.lilithsthrone.game.character.npc.NPC)host);
			}

			ContainmentType type = getPlayerContainmentType();
			boolean stomach = type==ContainmentType.STOMACH;
			StringBuilder sb = new StringBuilder();
			sb.append("<p>");
			if(stomach) {
				sb.append(UtilText.parse(host,
						"你蜷缩在[npc.name]湿热的胃里。肉壁一阵阵收缩，把你裹得严严实实。"
						+ "隔着肚皮，隐约能听见外面的世界。"));
			} else {
				sb.append(UtilText.parse(host,
						"你整个人被纳入[npc.name]的子宫。湿热的肉壁紧紧包裹着你，每一次呼吸都让腹壁随之起伏。"));
			}
			sb.append("</p><p>");
			sb.append(UtilText.parse(host, "现在你无法自己离开。只能等待[npc.name]把你释放出来——或者在这里继续待下去。"));
			sb.append("</p>");
			return sb.toString();
		}
		@Override
		public Response getResponse(int responseTab, int index) {
			GameCharacter host = getHost();
			if(host==null) {
				if(index==1) {
					return new Response("出来", "你自由了。", Main.game.getDefaultDialogue(false)) {
						@Override
						public void effects() {
							if(Main.game.getPlayer().isContained()) {
								Main.game.releaseContainedCharacter(Main.game.getPlayer().getId());
							}
							if(Main.game.getPlayer().getWorldLocation()==WorldType.EMPTY) {
								Main.game.getPlayer().setLocation(WorldType.DOMINION, PlaceType.DOMINION_AUNTS_HOME, false);
							}
						}
					};
				}
				return null;
			}
			if(index==1) {
				return new Response("挣扎",
						UtilText.parse(host, "在[npc.name]体内用力挣扎。每次都会过去一段时间，有机会挤出去。"),
						INSIDE_HOST) {
					private boolean escaped;
					@Override
					public int getSecondsPassed() {
						return 15*60;
					}
					@Override
					public void effects() {
						if(com.lilithsthrone.game.character.containment.ContainmentRules.struggleEscapes(Util.random.nextInt(4), 4)) {
							escaped = true;
							Main.game.releaseContainedCharacter(Main.game.getPlayer().getId());
							Main.game.appendToTextStartStringBuilder(UtilText.parse(host,
									"<p>你拼命撑开肉壁，终于从[npc.name]体内挤了出来。</p>"));
						} else {
							Main.game.appendToTextStartStringBuilder(Main.game.getContainmentStruggleEvent());
						}
					}
					@Override
					public DialogueNode getNextDialogue() {
						if(escaped) {
							return Main.game.getDefaultDialogue(false);
						}
						return super.getNextDialogue();
					}
				};
			}
			if(index==2) {
				return new Response("等待",
						UtilText.parse(host, "安静地待在[npc.name]体内，让时间过去。最终阶段结束时会被放出。"),
						INSIDE_HOST) {
					@Override
					public int getSecondsPassed() {
						return 15*60;
					}
					@Override
					public DialogueNode getNextDialogue() {
						if(Main.game.getPlayer()==null || !Main.game.getPlayer().isContained()) {
							return Main.game.getDefaultDialogue(false);
						}
						return super.getNextDialogue();
					}
				};
			}
			if(index==3) {
				return new Response("恳求放出",
						UtilText.parse(host, "让[npc.name]把你放出来。"),
						INSIDE_HOST) {
					@Override
					public void effects() {
						Main.game.releaseContainedCharacter(Main.game.getPlayer().getId());
					}
					@Override
					public DialogueNode getNextDialogue() {
						return Main.game.getDefaultDialogue(false);
					}
				};
			}
			if(index==0) {
				return new Response("无法自行离开",
						"你没法自己走出去，但可以挣扎逃脱或恳求对方放出。",
						null);
			}

			return null;
		}
	};
}
