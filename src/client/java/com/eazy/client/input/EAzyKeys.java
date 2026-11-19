package com.eazy.client.input;

import com.eazy.client.EAzyClient;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/**
 * Tüm EAzy tuşları buradan kaydedilir.
 * Tuşlar Minecraft'ın Kontroller ekranında "EAzy" başlığı altında görünür ve değiştirilebilir.
 */
public final class EAzyKeys {
	public static final KeyMapping.Category CATEGORY =
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath(EAzyClient.MOD_ID, "main"));

	private EAzyKeys() {
	}

	/**
	 * Yeni bir klavye tuşu kaydeder.
	 *
	 * @param translationKey dil dosyasındaki ad anahtarı (örn. key.eazy.open_menu)
	 * @param glfwKey        varsayılan tuş (GLFW.GLFW_KEY_*)
	 */
	public static KeyMapping register(String translationKey, int glfwKey) {
		return KeyMappingHelper.registerKeyMapping(
				new KeyMapping(translationKey, InputConstants.Type.KEYSYM, glfwKey, CATEGORY));
	}
}
