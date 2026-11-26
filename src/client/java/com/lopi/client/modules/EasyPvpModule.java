package com.lopi.client.modules;

import com.lopi.client.LopiClient;
import com.lopi.client.core.Category;
import com.lopi.client.core.LopiModule;
import com.lopi.client.core.ModuleManager;
import com.lopi.client.core.Setting;
import com.lopi.client.gui.EasyPvpScreen;
import com.lopi.client.mixin.ArmorStandInvoker;
import com.mojang.brigadier.Command;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * EasyPVP (kendi projenden 26.2'ye taşındı; yazar Zyven, MIT).
 * Totem sayacı (XP seviyesinin üstünde), kozmetik (kafanın üstünde dönen eşya, sadece sende görünür),
 * kozmetik kapalıyken kendi adın etiketi ve /eaZZ komutları. Parçacık anahtarı Lopi'nin "Parçacıklar kapalı" modülünü yönetir.
 */
public final class EasyPvpModule extends LopiModule {
	private static EasyPvpModule instance;

	/** Kozmetik seçenekleri: id -> eşya (sıra, menüdeki döngü sırasıdır). */
	public static final String[] COSMETIC_IDS = {"off", "totem", "cherry", "amethyst", "crystal", "gap"};

	private static final int ITEM_ENTITY_ID = -742001;
	private static final int NAME_ENTITY_ID = -742002;

	/** Lopi logosu: assets/lopi/font/logo.json içindeki tek karakterlik yazı tipi (U+E000). */
	private static final Style LOGO_STYLE = Style.EMPTY.withFont(
			new FontDescription.Resource(Identifier.fromNamespaceAndPath("lopi", "logo")));

	/** Kendi isim etiketi: [Lopi logosu] oyuncu adı. */
	private static Component nameTagText(String playerName) {
		return Component.empty()
				.append(Component.literal("\uE000").withStyle(LOGO_STYLE))
				.append(Component.literal(" " + playerName));
	}

	private final Setting<Boolean> totemCounter = setting("totem_counter", Boolean.class, true);
	private final Setting<String> cosmetic = setting("cosmetic", String.class, "off").hidden();

	private ClientLevel trackedLevel;
	private ItemEntity itemEntity;
	private ArmorStand nameEntity;

	public EasyPvpModule() {
		super("easypvp", Category.INTEGRATION, true);
		instance = this;
		registerCommand();
	}

	public static Item itemFor(String id) {
		return switch (id) {
			case "totem" -> Items.TOTEM_OF_UNDYING;
			case "cherry" -> Items.CHERRY_SAPLING;
			case "amethyst" -> Items.AMETHYST_SHARD;
			case "crystal" -> Items.END_CRYSTAL;
			case "gap" -> Items.GOLDEN_APPLE;
			default -> null;
		};
	}

	/** Parçacıkları bu modül mü kapattı (EAzz kapanınca geri açılsın diye). */
	private boolean forcedParticlesOff;

	/**
	 * EAzz anahtarı: açılınca parçacıkları kapatır, totem sayacını açar, kozmetik + isim etiketini gösterir;
	 * kapanınca kendi kapattığı parçacıkları geri açar.
	 */
	public static void setEazz(boolean on) {
		EasyPvpModule m = instance;
		if (m == null) {
			return;
		}
		ModuleManager.setEnabled(m, on);
		LopiModule particles = particlesModule();
		if (on) {
			m.totemCounter.set(true);
			if (particles != null && !particles.isEnabled()) {
				ModuleManager.setEnabled(particles, true);
				m.forcedParticlesOff = true;
				Minecraft.getInstance().particleEngine.clearParticles();
			}
		} else if (m.forcedParticlesOff && particles != null) {
			ModuleManager.setEnabled(particles, false);
			m.forcedParticlesOff = false;
		}
		ModuleManager.saveAsync();
	}

	public Setting<Boolean> totemCounterSetting() {
		return totemCounter;
	}

	public String cosmetic() {
		String id = cosmetic.get();
		return itemFor(id) == null ? "off" : id;
	}

	public void setCosmetic(String id) {
		cosmetic.set(id);
		ModuleManager.saveAsync();
	}

	/** "Parçacıklar kapalı" modülü (Lopi'nin kendi modülü); /eaZZ particle ve menü bunu yönetir. */
	public static LopiModule particlesModule() {
		for (LopiModule m : ModuleManager.all()) {
			if (m.id().equals("no_particles")) {
				return m;
			}
		}
		return null;
	}

	public static void toggleParticles() {
		LopiModule m = particlesModule();
		if (m != null) {
			ModuleManager.toggle(m);
			if (m.isEnabled()) {
				Minecraft.getInstance().particleEngine.clearParticles();
			}
		}
	}

	@Override
	public Screen createSettingsScreen(Screen parent) {
		return new EasyPvpScreen(parent, this);
	}

	private void registerCommand() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			var cos = ClientCommands.literal("cos");
			for (String id : COSMETIC_IDS) {
				cos = cos.then(ClientCommands.literal(id).executes(ctx -> {
					setCosmetic(id);
					Item item = itemFor(id);
					ctx.getSource().sendFeedback(Component.literal("Kozmetik: "
							+ (item == null ? "KAPALI" : new ItemStack(item).getHoverName().getString())));
					return Command.SINGLE_SUCCESS;
				}));
			}
			dispatcher.register(ClientCommands.literal("eaZZ")
					.then(ClientCommands.literal("totem").executes(ctx -> {
						totemCounter.toggle();
						ModuleManager.saveAsync();
						ctx.getSource().sendFeedback(Component.literal(
								"Totem sayacı: " + (totemCounter.get() ? "AÇIK" : "KAPALI")));
						return Command.SINGLE_SUCCESS;
					}))
					.then(ClientCommands.literal("particle").executes(ctx -> {
						toggleParticles();
						LopiModule m = particlesModule();
						// Parçacıklar "kapalı modülü" açıkken parçacık yoktur.
						ctx.getSource().sendFeedback(Component.literal("Partiküller: "
								+ (m != null && m.isEnabled() ? "KAPALI" : "AÇIK")));
						return Command.SINGLE_SUCCESS;
					}))
					.then(cos));
		});
	}

	@Override
	protected void onTick(Minecraft client) {
		LocalPlayer player = client.player;
		ClientLevel level = client.level;
		if (player == null || level == null) {
			forget();
			return;
		}
		if (level != trackedLevel) {
			forget();
			trackedLevel = level;
		}
		// İsim etiketi her zaman görünür; kozmetik açıksa etiketin üstünde yer alır.
		updateNameTag(level, player);
		Item item = itemFor(cosmetic());
		if (item != null) {
			updateItem(level, player, item);
		} else {
			removeItem(level);
		}
	}

	@Override
	protected void onDisable() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null && mc.level == trackedLevel) {
			removeItem(mc.level);
			removeNameTag(mc.level);
		}
		forget();
	}

	private void updateItem(ClientLevel level, LocalPlayer player, Item cosmetic) {
		double x = player.getX();
		double y = player.getY() + player.getBbHeight() + 0.85;
		double z = player.getZ();
		if (itemEntity == null || itemEntity.isRemoved() || !itemEntity.getItem().is(cosmetic)) {
			removeItem(level);
			ItemEntity entity = new ItemEntity(level, x, y, z, new ItemStack(cosmetic));
			entity.setId(ITEM_ENTITY_ID);
			entity.setNoGravity(true);
			entity.setInvulnerable(true);
			entity.setPickUpDelay(32767);
			entity.setUnlimitedLifetime();
			level.addEntity(entity);
			itemEntity = entity;
		}
		itemEntity.setPos(x, y, z);
		itemEntity.setDeltaMovement(Vec3.ZERO);
	}

	private void updateNameTag(ClientLevel level, LocalPlayer player) {
		double x = player.getX();
		double y = player.getY() + player.getBbHeight();
		double z = player.getZ();
		if (nameEntity == null || nameEntity.isRemoved()) {
			removeNameTag(level);
			ArmorStand entity = new ArmorStand(level, x, y, z);
			entity.setId(NAME_ENTITY_ID);
			entity.setInvisible(true);
			entity.setNoGravity(true);
			entity.setInvulnerable(true);
			((ArmorStandInvoker) entity).lopi$setMarker(true);
			entity.setCustomName(nameTagText(player.getName().getString()));
			entity.setCustomNameVisible(true);
			level.addEntity(entity);
			nameEntity = entity;
		}
		nameEntity.setPos(x, y, z);
		nameEntity.setDeltaMovement(Vec3.ZERO);
	}

	private void removeItem(ClientLevel level) {
		if (itemEntity != null) {
			level.removeEntity(ITEM_ENTITY_ID, Entity.RemovalReason.DISCARDED);
			itemEntity = null;
		}
	}

	private void removeNameTag(ClientLevel level) {
		if (nameEntity != null) {
			level.removeEntity(NAME_ENTITY_ID, Entity.RemovalReason.DISCARDED);
			nameEntity = null;
		}
	}

	private void forget() {
		trackedLevel = null;
		itemEntity = null;
		nameEntity = null;
	}

	/** HudMixin tarafından çağrılır: XP seviyesinin üstünde, envanterdeki toplam totem sayısı (beyaz). */
	public static void renderTotemCounter(GuiGraphicsExtractor graphics) {
		EasyPvpModule m = instance;
		if (m == null || !m.isEnabled() || !m.totemCounter.get()) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (player == null) {
			return;
		}
		int total = 0;
		var inv = player.getInventory();
		for (int slot = 0; slot < inv.getContainerSize(); slot++) {
			ItemStack stack = inv.getItem(slot);
			if (stack.is(Items.TOTEM_OF_UNDYING)) {
				total += stack.getCount();
			}
		}
		graphics.centeredText(client.font, Component.literal(Integer.toString(total)),
				graphics.guiWidth() / 2, graphics.guiHeight() - 46, 0xFFFFFFFF);
	}
}
