package com.lopi.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.HashMap;
import java.util.Map;

/**
 * Küçük 5x7 piksel yazı tipi. Yükleme ekranında oyunun yazı tipi henüz hazır olmayabileceği için
 * yüzde ve "YÜKLENİYOR" yazısı bununla, sadece dolu kareler çizilerek yapılır.
 */
final class PixelFont {
	private static final Map<Character, String[]> GLYPHS = new HashMap<>();

	static {
		put('L', "X....", "X....", "X....", "X....", "X....", "X....", "XXXXX");
		put('O', ".XXX.", "X...X", "X...X", "X...X", "X...X", "X...X", ".XXX.");
		put('P', "XXXX.", "X...X", "X...X", "XXXX.", "X....", "X....", "X....");
		put('I', "XXXXX", "..X..", "..X..", "..X..", "..X..", "..X..", "XXXXX");
		put('Y', "X...X", "X...X", ".X.X.", "..X..", "..X..", "..X..", "..X..");
		put('U', "X...X", "X...X", "X...X", "X...X", "X...X", "X...X", ".XXX.");
		put('K', "X...X", "X..X.", "X.X..", "XX...", "X.X..", "X..X.", "X...X");
		put('E', "XXXXX", "X....", "X....", "XXXX.", "X....", "X....", "XXXXX");
		put('N', "X...X", "XX..X", "X.X.X", "X..XX", "X...X", "X...X", "X...X");
		put('R', "XXXX.", "X...X", "X...X", "XXXX.", "X.X..", "X..X.", "X...X");
		put('C', ".XXXX", "X....", "X....", "X....", "X....", "X....", ".XXXX");
		put('T', "XXXXX", "..X..", "..X..", "..X..", "..X..", "..X..", "..X..");
		put('0', ".XXX.", "X...X", "X..XX", "X.X.X", "XX..X", "X...X", ".XXX.");
		put('1', "..X..", ".XX..", "..X..", "..X..", "..X..", "..X..", ".XXX.");
		put('2', ".XXX.", "X...X", "....X", "...X.", "..X..", ".X...", "XXXXX");
		put('3', "XXXX.", "....X", "....X", ".XXX.", "....X", "....X", "XXXX.");
		put('4', "...X.", "..XX.", ".X.X.", "X..X.", "XXXXX", "...X.", "...X.");
		put('5', "XXXXX", "X....", "XXXX.", "....X", "....X", "X...X", ".XXX.");
		put('6', ".XXX.", "X....", "X....", "XXXX.", "X...X", "X...X", ".XXX.");
		put('7', "XXXXX", "....X", "...X.", "..X..", ".X...", ".X...", ".X...");
		put('8', ".XXX.", "X...X", "X...X", ".XXX.", "X...X", "X...X", ".XXX.");
		put('9', ".XXX.", "X...X", "X...X", ".XXXX", "....X", "....X", ".XXX.");
		put('%', "XX..X", "XX..X", "...X.", "..X..", ".X...", "X..XX", "X..XX");
		put('.', ".....", ".....", ".....", ".....", ".....", ".XX..", ".XX..");
		put(' ', ".....", ".....", ".....", ".....", ".....", ".....", ".....");
	}

	private PixelFont() {
	}

	private static void put(char c, String... rows) {
		GLYPHS.put(c, rows);
	}

	/** Yazının piksel genişliği (cell = bir karenin kenarı). */
	static int width(String text, int cell) {
		return Math.max(0, text.length() * 6 - 1) * cell;
	}

	/** Yazıyı (x, y) sol üst köşeden başlayarak çizer. Ü ve İ için noktalar glifin üstüne eklenir. */
	static void draw(GuiGraphicsExtractor g, String text, int x, int y, int cell, int color) {
		int cx = x;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			boolean dots = false;
			if (c == 'Ü') {
				c = 'U';
				dots = true;
			} else if (c == 'İ') {
				c = 'I';
				dots = true;
			}
			String[] rows = GLYPHS.get(c);
			if (rows != null) {
				for (int r = 0; r < 7; r++) {
					for (int k = 0; k < 5; k++) {
						if (rows[r].charAt(k) == 'X') {
							g.fill(cx + k * cell, y + r * cell, cx + (k + 1) * cell, y + (r + 1) * cell, color);
						}
					}
				}
				if (dots) {
					if (text.charAt(i) == 'Ü') {
						g.fill(cx + cell, y - 2 * cell, cx + 2 * cell, y - cell, color);
						g.fill(cx + 3 * cell, y - 2 * cell, cx + 4 * cell, y - cell, color);
					} else {
						g.fill(cx + 2 * cell, y - 2 * cell, cx + 3 * cell, y - cell, color);
					}
				}
			}
			cx += 6 * cell;
		}
	}
}
