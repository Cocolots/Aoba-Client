/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.gui.font;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FreeType;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.client.gui.font.FontOption.Filter;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.GlyphStitcher;
import net.minecraft.client.gui.font.glyphs.EffectGlyph;
import net.minecraft.client.gui.font.providers.FreeTypeUtil;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

public class FontManager {
	private static final String BUILTIN_FONT_PATH = "/assets/aoba/fonts/";
	private static final String[] BUILTIN_FONT_IDS = { "lato_regular", "helvetica", "fingerpaint" };
	public static final String DEFAULT_FONT = "lato_regular";

	public static final int WEIGHT_THIN = 100;
	public static final int WEIGHT_EXTRA_LIGHT = 200;
	public static final int WEIGHT_LIGHT = 300;
	public static final int WEIGHT_NORMAL = 400;
	public static final int WEIGHT_MEDIUM = 500;
	public static final int WEIGHT_SEMI_BOLD = 600;
	public static final int WEIGHT_BOLD = 700;
	public static final int WEIGHT_EXTRA_BOLD = 800;
	public static final int WEIGHT_BLACK = 900;

	private static final Map<Integer, Float> WEIGHT_EMBOLDEN = Map.of(
		100, -1.5f,
		200, -1.0f,
		300, -0.5f,
		400, 0.0f,
		500, 0.25f,
		600, 0.5f,
		700, 0.8f,
		800, 1.1f,
		900, 1.5f
	);

	private static Minecraft MC;

	public static final float DEFAULT_FONT_SIZE = 6f;

	public ConcurrentHashMap<String, UIFont> fonts = new ConcurrentHashMap<>();

	public FontManager() {
		MC = Minecraft.getInstance();
	}

	public void Initialize() {
		loadBuiltinFonts();
		loadUserFonts();

		if (fonts.get(DEFAULT_FONT) == null) {
			LogUtils.getLogger().error(
					"[Aoba] Default font '{}' failed to load. Drop {}.ttf into src/main/resources{}",
					DEFAULT_FONT, DEFAULT_FONT, BUILTIN_FONT_PATH);
		}
	}

	private void loadBuiltinFonts() {
		for (String id : BUILTIN_FONT_IDS) {
			String resourcePath = BUILTIN_FONT_PATH + id + ".ttf";
			try (InputStream is = FontManager.class.getResourceAsStream(resourcePath)) {
				if (is == null) {
					LogUtils.getLogger().warn("[Aoba] Built-in font not found: {}", resourcePath);
					continue;
				}
				byte[] bytes = is.readAllBytes();
				fonts.put(id, new UIFont(id, bytes));
				LogUtils.getLogger().info("[Aoba] Loaded built-in font: {}", id);
			} catch (Exception e) {
				LogUtils.getLogger().error("[Aoba] Failed to load built-in font: {}", id, e);
			}
		}
	}

	private void loadUserFonts() {
		File fontDirectory = new File(MC.gameDirectory + File.separator + "aoba" + File.separator + "fonts");
		if (!fontDirectory.exists() || !fontDirectory.isDirectory()) return;

		LogUtils.getLogger().info("Found Font Directory: " + fontDirectory.getAbsolutePath());
		File[] files = fontDirectory.listFiles((_, name) -> name.endsWith(".ttf"));
		if (files == null)
			return;

		for (File file : files) {
			String fontName = file.getName().replace(".ttf", "");
			try {
				byte[] bytes = Files.readAllBytes(file.toPath());
				fonts.put(fontName, new UIFont(fontName, bytes));
				LogUtils.getLogger().info("Loaded font: " + fontName);
			} catch (Exception e) {
				LogUtils.getLogger().error("Failed to load font: " + file.getName(), e);
			}
		}
	}

	public UIFont getFont(String name) {
		UIFont font = fonts.get(name);
		return font != null ? font : getDefaultFont();
	}

	public UIFont getDefaultFont() {
		return fonts.get(DEFAULT_FONT);
	}

	public static float getEmboldenForWeight(int weight) {
		return WEIGHT_EMBOLDEN.getOrDefault(weight, 0f);
	}

	public static Font loadFontFromBytes(byte[] fontBytes, String id, float emboldenPixels) throws IOException {
		float atlasSize = Math.round(DEFAULT_FONT_SIZE * 1.5f);
		return loadFontFromBytes(fontBytes, id, emboldenPixels, atlasSize);
	}

	public static Font loadFontFromBytes(byte[] fontBytes, String id, float emboldenPixels, float atlasSize) throws IOException {
		ByteBuffer byteBuffer = MemoryUtil.memAlloc(fontBytes.length);
		byteBuffer.put(fontBytes).flip();

		try (MemoryStack stack = MemoryStack.stackPush()) {
			PointerBuffer pointerBuffer = stack.mallocPointer(1);
			FreeTypeUtil.assertError(
					FreeType.FT_New_Memory_Face(FreeTypeUtil.getLibrary(), byteBuffer, 0L, pointerBuffer),
					"Initializing font face");
			FT_Face face = FT_Face.create(pointerBuffer.get());

			String format = FreeType.FT_Get_Font_Format(face);
			if (!"TrueType".equals(format)) {
				MemoryUtil.memFree(byteBuffer);
				throw new IOException("Font is not in TTF format, was " + format);
			}
			FreeTypeUtil.assertError(FreeType.FT_Select_Charmap(face, FreeType.FT_ENCODING_UNICODE),
					"Find unicode charmap");

			GlyphProvider provider = new AobaGlyphProvider(byteBuffer, face, atlasSize, emboldenPixels);

			List<GlyphProvider.Conditional> list = new ArrayList<>();
			list.add(new GlyphProvider.Conditional(provider, Filter.ALWAYS_PASS));

			GlyphStitcher stitcher = new GlyphStitcher(MC.getTextureManager(),
					Identifier.parse("aoba:fonts/" + id.toLowerCase()));

			try (FontSet storage = new FontSet(stitcher)) {
				storage.reload(list, Set.of());

				return new Font(new Font.Provider() {
					@Override
					public GlyphSource glyphs(FontDescription desc) {
						return storage.source(false);
					}

					@Override
					public EffectGlyph effect() {
						return storage.whiteGlyph();
					}
				});
			}
		} catch (Exception e) {
			MemoryUtil.memFree(byteBuffer);
			throw e;
		}
	}
}
