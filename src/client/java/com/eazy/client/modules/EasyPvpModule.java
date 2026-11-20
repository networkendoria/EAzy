package com.eazy.client.modules;

import com.eazy.client.EAzyClient;
import com.eazy.client.core.Category;
import com.eazy.client.core.EAzyModule;
import com.eazy.client.core.ModuleManager;
import com.eazy.client.core.Setting;
import com.eazy.client.gui.EasyPvpScreen;
import com.eazy.client.mixin.ArmorStandInvoker;
import com.mojang.brigadier.Command;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
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
 * kozmetik kapalıyken kendi adın etiketi ve /eaZZ komutları. Parçacık anahtarı EAzy'nin "Parçacıklar kapalı" modülünü yönetir.
 */
public final class EasyPvpModule extends EAzyModule {
	private static EasyPvpModule instance;

	/** Kozmetik seçenekleri: id -> eşya (sıra, menüdeki döngü sırasıdır). */
	public static final String[] COSMETIC_IDS = {"off", "totem", "cherry", "amethyst", "crystal", "gap"};

	private static final int ITEM_ENTITY_ID = -742001;
	private static final int NAME_ENTITY_ID = -742002;

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

	/** "Parçacıklar kapalı" modülü (EAzy'nin kendi modülü); /eaZZ particle ve menü bunu yönetir. */
	public static EAzyModule particlesModule() {
		for (EAzyModule m : ModuleManager.all()) {
			if (m.id().equals("no_particles")) {
				return m;
			}
		}
		return null;
	}

	public static void toggleParticles() {
		EAzyModule m = particlesModule();
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
			var cos = ClientCommandManager.literal("cos");
			for (String id : COSMETIC_IDS) {
				cos = cos.then(ClientCommandManager.literal(id).executes(ctx -> {
					setCosmetic(id);
					Item item = itemFor(id);
					ctx.getSource().sendFeedback(Component.literal("Kozmetik: "
							+ (item == null ? "KAPALI" : new ItemStack(item).getHoverName().getString())));
					return Command.SINGLE_SUCCESS;
				}));
			}
			dispatcher.register(ClientCommandManager.literal("eaZZ")
					.then(ClientCommandManager.literal("totem").executes(ctx -> {
						totemCounter.toggle();
						ModuleManager.saveAsync();
						ctx.getSource().sendFeedback(Component.literal(
								"Totem sayacı: " + (totemCounter.get() ? "AÇIK" : "KAPALI")));
						return Command.SINGLE_SUCCESS;
					}))
					.then(ClientCommandManager.literal("particle").executes(ctx -> {
						toggleParticles();
						EAzyModule m = particlesModule();
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
		Item item = itemFor(cosmetic());
		if (item != null) {
			removeNameTag(level);
			updateItem(level, player, item);
		} else {
			removeItem(level);
			updateNameTag(level, player);
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
		double y = player.getY() + player.getBbHeight() + 0.15;
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
			((ArmorStandInvoker) entity).eazy$setMarker(true);
			entity.setCustomName(Component.literal("› " + player.getName().getString() + " ‹"));
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
	public static void renderTotemCounter(GuiGraphics graphics) {
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
		graphics.drawCenteredString(client.font, Component.literal(Integer.toString(total)),
				graphics.guiWidth() / 2, graphics.guiHeight() - 46, 0xFFFFFFFF);
	}
}
