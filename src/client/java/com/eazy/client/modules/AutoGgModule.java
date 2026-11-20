package com.eazy.client.modules;

import com.eazy.client.EAzyClient;
import com.eazy.client.core.Category;
import com.eazy.client.core.EAzyModule;
import com.eazy.client.core.ModuleManager;
import com.eazy.client.modules.autogg.LearnedPatterns;
import com.mojang.brigadier.Command;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * AutoGG (kendi projenden 26.2'ye taşındı; yazar Zyven, MIT). Bir oyuncuyu öldürünce sohbete otomatik "gg" yazar.
 * Komutlar: /autogg (aç/kapat), /autogg debug (öğrenme modu), /autogg forget (öğrenilenleri sil).
 * Ekranda XP seviyesinin üstünde, son vurduğun oyuncunun elindeki totem sayısı (kırmızı).
 */
public final class AutoGgModule extends EAzyModule {
	private static AutoGgModule instance;

	private static final String[] MESSAGES = {"gg eline sağlık", "eline sağlık", "gg nt"};

	private static final long COOLDOWN_MS = 6000;
	private static final long MIN_DELAY_MS = 700;
	private static final long MAX_EXTRA_DELAY_MS = 900;
	private static final long HIT_WINDOW_MS = 6000;
	private static final long EXPLOSIVE_WINDOW_MS = 8000;
	private static final long LEARN_WINDOW_MS = 20000;
	private static final double MAX_DISTANCE = 20.0;

	private static final Random RANDOM = new Random();

	private static boolean debug = false;

	private static final Map<String, Long> LAST_HIT = new HashMap<>();
	private static final Set<UUID> COUNTED_DEAD = new HashSet<>();
	private static long lastExplosiveMs = 0;

	private static String targetName = null;
	private static boolean targetDead = false;
	private static int targetTotemsLastKnown = 0;

	private static long lastSentMs = 0;
	private static String lastMessage = null;
	private static String pendingMessage = null;
	private static long pendingSendAtMs = 0;

	public AutoGgModule() {
		super("autogg", Category.INTEGRATION, true);
		instance = this;
		LearnedPatterns.load();
		registerHooks();
	}

	private static boolean active() {
		AutoGgModule m = instance;
		return m != null && m.isEnabled();
	}

	private void registerHooks() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				dispatcher.register(ClientCommandManager.literal("autogg")
						.executes(context -> {
							ModuleManager.toggle(this);
							context.getSource().sendFeedback(Component.literal("AutoGG: " + (isEnabled() ? "AÇIK" : "KAPALI")));
							return Command.SINGLE_SUCCESS;
						})
						.then(ClientCommandManager.literal("debug").executes(context -> {
							debug = !debug;
							context.getSource().sendFeedback(Component.literal(
									"AutoGG öğrenme (debug): " + (debug ? "AÇIK" : "KAPALI")));
							return Command.SINGLE_SUCCESS;
						}))
						.then(ClientCommandManager.literal("forget").executes(context -> {
							int removed = LearnedPatterns.clear();
							context.getSource().sendFeedback(Component.literal(
									"AutoGG: öğrenilen mesajlar silindi (" + removed + ")"));
							return Command.SINGLE_SUCCESS;
						}))));

		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			if (!active()) {
				return InteractionResult.PASS;
			}
			Minecraft mc = Minecraft.getInstance();
			if (mc.player == null || player != mc.player) {
				return InteractionResult.PASS;
			}
			long now = System.currentTimeMillis();
			if (entity instanceof Player target) {
				String hitName = target.getName().getString();
				LAST_HIT.put(hitName, now);
				if (!hitName.equals(targetName)) {
					targetName = hitName;
					targetDead = false;
					targetTotemsLastKnown = 0;
				}
			} else if (entity instanceof EndCrystal) {
				lastExplosiveMs = now;
			}
			return InteractionResult.PASS;
		});

		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (!active()) {
				return InteractionResult.PASS;
			}
			Minecraft mc = Minecraft.getInstance();
			if (mc.player == null || player != mc.player) {
				return InteractionResult.PASS;
			}
			long now = System.currentTimeMillis();
			if (player.getItemInHand(hand).is(Items.END_CRYSTAL)) {
				lastExplosiveMs = now;
			} else {
				var state = level.getBlockState(hit.getBlockPos());
				if (state.is(Blocks.RESPAWN_ANCHOR) || state.getBlock() instanceof BedBlock) {
					lastExplosiveMs = now;
				}
			}
			return InteractionResult.PASS;
		});

		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (!overlay && active()) {
				try {
					onGameMessage(message);
				} catch (RuntimeException e) {
					EAzyClient.LOGGER.warn("AutoGG mesaj işleme hatası", e);
				}
			}
		});
	}

	@Override
	protected void onDisable() {
		pendingMessage = null;
	}

	@Override
	protected void onTick(Minecraft client) {
		if (client.player == null || client.level == null) {
			pendingMessage = null;
			COUNTED_DEAD.clear();
			targetName = null;
			targetDead = false;
			targetTotemsLastKnown = 0;
			return;
		}
		long now = System.currentTimeMillis();
		checkDeadPlayers(client, now);

		if (pendingMessage != null && now >= pendingSendAtMs) {
			String toSend = pendingMessage;
			pendingMessage = null;
			lastSentMs = now;
			lastMessage = toSend;
			client.player.connection.sendChat(toSend);
			debugLog(client, "mesaj gönderildi: " + toSend);
		}
	}

	private static void checkDeadPlayers(Minecraft client, long now) {
		for (Player p : client.level.players()) {
			if (p == client.player) {
				continue;
			}
			UUID id = p.getUUID();
			if (!p.isDeadOrDying()) {
				COUNTED_DEAD.remove(id);
				continue;
			}
			if (!COUNTED_DEAD.add(id)) {
				continue;
			}
			String name = p.getName().getString();
			markDeath(name);
			boolean byHit = hitRecently(name, now);
			boolean byExplosive = now - lastExplosiveMs <= EXPLOSIVE_WINDOW_MS
					&& client.player.distanceToSqr(p) <= MAX_DISTANCE * MAX_DISTANCE;
			if (byHit || byExplosive) {
				debugLog(client, "can 0 oldu: " + name + " | vuruş=" + byHit + " patlama=" + byExplosive);
				queueMessage(now);
			}
		}
	}

	private static void onGameMessage(Component message) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) {
			return;
		}
		String plain = message.getString();
		if (plain.startsWith("[AutoGG")) {
			return;
		}
		String me = mc.player.getName().getString();
		long now = System.currentTimeMillis();

		String duelLoser = duelLoserName(plain);
		if (duelLoser != null) {
			markDeath(duelLoser);
		}

		if (isDuelWin(plain, me)) {
			debugLog(mc, "düello kazanıldı, gg yazılıyor");
			queueMessage(now);
			return;
		}

		String learnedVictim = LearnedPatterns.match(plain, me);
		if (learnedVictim != null) {
			markDeath(learnedVictim);
			debugLog(mc, "öğrenilmiş mesaj eşleşti: kurban=" + learnedVictim);
			queueMessage(now);
			return;
		}

		TranslatableContents death = findDeathContent(message, 0);
		if (death == null) {
			if (debug) {
				learnFrom(mc, plain, me, now);
			}
			return;
		}
		Object[] args = death.getArgs();
		if (args.length == 0) {
			return;
		}

		String victim = argToString(args[0]);
		markDeath(victim);
		if (nameMatches(victim, me)) {
			return;
		}

		boolean credit = false;
		for (int i = 1; i < args.length; i++) {
			if (nameMatches(argToString(args[i]), me)) {
				credit = true;
				break;
			}
		}

		if (!credit) {
			boolean otherKillerNamed = args.length >= 2
					&& !death.getKey().startsWith("death.attack.badRespawnPoint");
			if (!otherKillerNamed) {
				if (now - lastExplosiveMs <= EXPLOSIVE_WINDOW_MS && victimIsNear(mc, victim)) {
					credit = true;
				}
				if (!credit && hitRecently(victim, now)) {
					credit = true;
				}
			}
		}

		if (credit) {
			debugLog(mc, "ölüm mesajı: " + death.getKey() + " | kurban=" + victim);
			queueMessage(now);
		}
	}

	private static void learnFrom(Minecraft mc, String plain, String me, long now) {
		if (mc.player.isDeadOrDying()) {
			return;
		}
		Set<String> candidates = new HashSet<>();
		for (Map.Entry<String, Long> entry : LAST_HIT.entrySet()) {
			if (now - entry.getValue() <= LEARN_WINDOW_MS) {
				candidates.add(entry.getKey());
			}
		}
		if (now - lastExplosiveMs <= LEARN_WINDOW_MS) {
			for (Player p : mc.level.players()) {
				if (p != mc.player) {
					candidates.add(p.getName().getString());
				}
			}
		}
		if (candidates.isEmpty()) {
			return;
		}
		String template = LearnedPatterns.learn(plain, me, candidates);
		if (template == null) {
			return;
		}
		mc.player.displayClientMessage(Component.literal("[AutoGG] Yeni ölüm mesajı öğrenildi: " + template), false);
		String victim = LearnedPatterns.match(plain, me);
		if (victim != null) {
			markDeath(victim);
			queueMessage(now);
		}
	}

	private static void queueMessage(long now) {
		if (pendingMessage != null || now - lastSentMs < COOLDOWN_MS) {
			return;
		}
		String chosen;
		do {
			chosen = MESSAGES[RANDOM.nextInt(MESSAGES.length)];
		} while (chosen.equals(lastMessage) && MESSAGES.length > 1);
		pendingMessage = chosen;
		pendingSendAtMs = now + MIN_DELAY_MS + (long) (RANDOM.nextDouble() * MAX_EXTRA_DELAY_MS);
	}

	private static void debugLog(Minecraft mc, String text) {
		if (debug && mc.player != null) {
			mc.player.displayClientMessage(Component.literal("[AutoGG debug] " + text), false);
		}
	}

	private static boolean hitRecently(String victim, long now) {
		for (Map.Entry<String, Long> entry : LAST_HIT.entrySet()) {
			if (nameMatches(victim, entry.getKey()) && now - entry.getValue() <= HIT_WINDOW_MS) {
				return true;
			}
		}
		return false;
	}

	private static boolean victimIsNear(Minecraft mc, String victim) {
		for (Player p : mc.level.players()) {
			if (p != mc.player && nameMatches(victim, p.getName().getString())) {
				return mc.player.distanceToSqr(p) <= MAX_DISTANCE * MAX_DISTANCE;
			}
		}
		return true;
	}

	private static TranslatableContents findDeathContent(Component text, int depth) {
		if (depth > 4) {
			return null;
		}
		ComponentContents content = text.getContents();
		if (content instanceof TranslatableContents translatable && translatable.getKey().startsWith("death.")) {
			return translatable;
		}
		for (Component sibling : text.getSiblings()) {
			TranslatableContents found = findDeathContent(sibling, depth + 1);
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	private static void markDeath(String victimText) {
		if (targetName != null && nameMatches(victimText, targetName)) {
			targetDead = true;
			targetTotemsLastKnown = 0;
		}
	}

	private static String duelLoserName(String plain) {
		String marker = " isimli oyuncuyu mağlup etti";
		int index = plain.indexOf(marker);
		if (index <= 0) {
			return null;
		}
		String before = plain.substring(0, index).trim();
		int space = before.lastIndexOf(' ');
		return space >= 0 ? before.substring(space + 1) : before;
	}

	private static int countHeldTotems(Player player) {
		int total = 0;
		if (player.getMainHandItem().is(Items.TOTEM_OF_UNDYING)) {
			total += player.getMainHandItem().getCount();
		}
		if (player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
			total += player.getOffhandItem().getCount();
		}
		return total;
	}

	/** HudMixin tarafından her karede çağrılır: XP seviyesinin üstünde kırmızı totem sayacı. */
	public static void renderTargetTotems(GuiGraphics graphics) {
		if (!active() || targetName == null) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.level == null) {
			return;
		}
		if (!targetDead) {
			for (Player p : client.level.players()) {
				if (p != client.player && p.getName().getString().equals(targetName)) {
					if (p.isDeadOrDying()) {
						targetDead = true;
						targetTotemsLastKnown = 0;
					} else {
						targetTotemsLastKnown = countHeldTotems(p);
					}
					break;
				}
			}
		}
		int count = targetDead ? 0 : targetTotemsLastKnown;
		graphics.drawCenteredString(client.font, Component.literal(Integer.toString(count)),
				graphics.guiWidth() / 2, graphics.guiHeight() - 57, 0xFFFF3B3B);
	}

	private static boolean isDuelWin(String plain, String me) {
		int comma = plain.indexOf(',');
		if (comma <= 0 || !plain.contains("isimli oyuncuyu mağlup etti")) {
			return false;
		}
		return nameMatches(plain.substring(0, comma).trim(), me);
	}

	private static String argToString(Object arg) {
		if (arg instanceof Component text) {
			return text.getString();
		}
		return String.valueOf(arg);
	}

	private static boolean nameMatches(String text, String name) {
		if (text == null || name == null || name.isEmpty()) {
			return false;
		}
		return text.equals(name)
				|| text.startsWith(name + " ")
				|| text.endsWith(" " + name)
				|| text.contains(" " + name + " ");
	}
}
