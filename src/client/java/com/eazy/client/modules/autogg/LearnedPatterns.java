package com.eazy.client.modules.autogg;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * (AutoGG projesinden alındı: yazar Zyven, MIT lisansı.)
 * Ogrenme modunda ogrenilen "olum / galibiyet duyurusu" sablonlari.
 *
 * Ornek mesaj:
 *   "Zyven, Duelloda Ahmet isimli oyuncuyu maglup etti. (Dereceli)"
 * Sablon olarak saklanir:
 *   "{killer}, Duelloda {victim} isimli oyuncuyu maglup etti. (Dereceli)"
 *
 * Sablonlar config/autogg-learned.txt dosyasinda durur, oyun kapansa da silinmez.
 * {killer} her zaman SENIN oyuncu adindir. {num} sayilar icin joker (ELO, can vb.).
 */
public final class LearnedPatterns {

	private static final String KILLER = "{killer}";
	private static final String VICTIM = "{victim}";
	private static final String NUMBER = "{num}";
	private static final String NAME_REGEX = "[A-Za-z0-9_]{1,16}";
	private static final Pattern PLACEHOLDER = Pattern.compile("\\{(killer|victim|num)\\}");
	private static final Pattern DIGITS = Pattern.compile("\\d+(?:[.,]\\d+)?");

	private static final List<String> TEMPLATES = new ArrayList<>();
	private static final Map<String, Pattern> COMPILED = new HashMap<>();
	private static String compiledFor = null;

	private LearnedPatterns() {
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("autogg-learned.txt");
	}

	public static void load() {
		TEMPLATES.clear();
		COMPILED.clear();
		Path path = file();
		if (!Files.exists(path)) {
			return;
		}
		try {
			for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
				String template = line.trim();
				if (template.contains(KILLER) && template.contains(VICTIM) && !TEMPLATES.contains(template)) {
					TEMPLATES.add(template);
				}
			}
		} catch (IOException ignored) {
			// Dosya okunamazsa sablon yokmus gibi devam et
		}
	}

	private static void save() {
		try {
			Path path = file();
			Files.createDirectories(path.getParent());
			Files.write(path, TEMPLATES, StandardCharsets.UTF_8);
		} catch (IOException ignored) {
			// Kaydedilemezse bu oturumda yine calisir
		}
	}

	/** Ogrenilen tum sablonlari siler. Kac tane silindigini dondurur. */
	public static int clear() {
		int removed = TEMPLATES.size();
		TEMPLATES.clear();
		COMPILED.clear();
		save();
		return removed;
	}

	/**
	 * Mesaj ogrenilmis bir sablonla eslesiyorsa (ve oldurulen BEN isem) kurbanin adini dondurur,
	 * eslesmezse null.
	 */
	public static String match(String plain, String me) {
		if (TEMPLATES.isEmpty() || plain == null || me == null || me.isEmpty()) {
			return null;
		}
		if (!me.equals(compiledFor)) {
			COMPILED.clear();
			compiledFor = me;
		}
		String text = plain.trim();
		for (String template : TEMPLATES) {
			Pattern pattern = COMPILED.computeIfAbsent(template, t -> build(t, me));
			Matcher matcher = pattern.matcher(text);
			if (matcher.find()) {
				return matcher.group(1);
			}
		}
		return null;
	}

	/**
	 * Mesajda benim adim VE (yakin zamanda vurdugum / patlattigim) tek bir aday oyuncunun adi
	 * geciyorsa bunu yeni sablon olarak ogrenir. Yeni sablon eklendiyse onu dondurur, yoksa null.
	 */
	public static String learn(String plain, String me, Collection<String> victimCandidates) {
		if (plain == null || me == null || me.isEmpty()) {
			return null;
		}
		String text = plain.trim();
		if (text.isEmpty() || text.length() > 300 || !containsToken(text, me)) {
			return null;
		}

		String victim = null;
		for (String candidate : victimCandidates) {
			if (candidate == null || candidate.isEmpty() || candidate.equals(me)) {
				continue;
			}
			if (containsToken(text, candidate)) {
				if (victim != null && !victim.equals(candidate)) {
					return null; // Birden fazla aday var, kimin oldugu belirsiz
				}
				victim = candidate;
			}
		}
		if (victim == null) {
			return null;
		}

		String template = replaceToken(text, me, KILLER);
		template = replaceToken(template, victim, VICTIM);
		template = DIGITS.matcher(template).replaceAll(Matcher.quoteReplacement(NUMBER));

		if (TEMPLATES.contains(template)) {
			return null;
		}
		TEMPLATES.add(template);
		COMPILED.clear();
		save();
		return template;
	}

	private static Pattern build(String template, String me) {
		StringBuilder regex = new StringBuilder();
		Matcher matcher = PLACEHOLDER.matcher(template);
		int last = 0;
		while (matcher.find()) {
			regex.append(Pattern.quote(template.substring(last, matcher.start())));
			switch (matcher.group(1)) {
				case "killer" -> regex.append("(?<![A-Za-z0-9_])")
					.append(Pattern.quote(me))
					.append("(?![A-Za-z0-9_])");
				case "victim" -> regex.append("(").append(NAME_REGEX).append(")");
				default -> regex.append("\\d+(?:[.,]\\d+)?");
			}
			last = matcher.end();
		}
		regex.append(Pattern.quote(template.substring(last)));
		return Pattern.compile(regex.toString());
	}

	private static Pattern tokenPattern(String name) {
		return Pattern.compile("(?<![A-Za-z0-9_])" + Pattern.quote(name) + "(?![A-Za-z0-9_])");
	}

	private static boolean containsToken(String text, String name) {
		return tokenPattern(name).matcher(text).find();
	}

	private static String replaceToken(String text, String name, String placeholder) {
		return tokenPattern(name).matcher(text).replaceAll(Matcher.quoteReplacement(placeholder));
	}
}
