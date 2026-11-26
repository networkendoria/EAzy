package com.lopi.client.modules;

import com.lopi.client.core.Category;
import com.lopi.client.core.LopiModule;
import com.lopi.client.core.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;

/** Sol üstte küçük bilgi satırı: FPS, ping ve (isteğe bağlı) koordinat. Sadece okur, oyunu değiştirmez. */
public final class InfoHudModule extends LopiModule {
	private static InfoHudModule instance;

	private final Setting<Boolean> fps = setting("fps", Boolean.class, true);
	private final Setting<Boolean> ping = setting("ping", Boolean.class, true);
	private final Setting<Boolean> coords = setting("coords", Boolean.class, false);

	public InfoHudModule() {
		super("info_hud", Category.INTERFACE, true);
		instance = this;
	}

	/** HudMixin tarafından çağrılır. */
	public static void render(GuiGraphicsExtractor graphics) {
		InfoHudModule m = instance;
		if (m == null || !m.isEnabled()) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null) {
			return;
		}
		int y = 4;
		if (m.fps.get()) {
			graphics.text(mc.font, "FPS: " + mc.getFps(), 4, y, 0xFFE9E1FF, true);
			y += 10;
		}
		if (m.ping.get() && mc.getConnection() != null) {
			PlayerInfo info = mc.getConnection().getPlayerInfo(player.getUUID());
			if (info != null) {
				graphics.text(mc.font, "Ping: " + info.getLatency() + " ms", 4, y, 0xFFE9E1FF, true);
				y += 10;
			}
		}
		if (m.coords.get()) {
			graphics.text(mc.font, String.format("XYZ: %d %d %d", (int) Math.floor(player.getX()),
					(int) Math.floor(player.getY()), (int) Math.floor(player.getZ())), 4, y, 0xFFE9E1FF, true);
		}
	}
}
