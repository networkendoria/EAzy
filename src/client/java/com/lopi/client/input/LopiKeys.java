package com.lopi.client.input;

import com.lopi.client.LopiClient;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/**
 * Tüm Lopi tuşları buradan kaydedilir.
 * Tuşlar Minecraft'ın Kontroller ekranında "Lopi" başlığı altında görünür ve değiştirilebilir.
 */
public final class LopiKeys {
	public static final KeyMapping.Category CATEGORY =
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath(LopiClient.MOD_ID, "main"));

	private LopiKeys() {
	}

	/** Verilen GLFW tuşu şu anda basılı mı (KeyMapping kaydı gerektirmez). */
	public static boolean isDown(int glfwKey) {
		if (glfwKey <= 0) {
			return false;
		}
		return InputConstants.isKeyDown(net.minecraft.client.Minecraft.getInstance().getWindow(), glfwKey);
	}

	/** Sağ Shift basılı mı (menü kısayolu ve zoom bastırma için). */
	public static boolean rightShiftDown() {
		return isDown(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT);
	}

	/**
	 * Yeni bir klavye tuşu kaydeder.
	 *
	 * @param translationKey dil dosyasındaki ad anahtarı (örn. key.lopi.open_menu)
	 * @param glfwKey        varsayılan tuş (GLFW.GLFW_KEY_*)
	 */
	public static KeyMapping register(String translationKey, int glfwKey) {
		return KeyMappingHelper.registerKeyMapping(
				new KeyMapping(translationKey, InputConstants.Type.KEYSYM, glfwKey, CATEGORY));
	}
}
