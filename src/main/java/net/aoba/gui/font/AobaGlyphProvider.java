package net.aoba.gui.font;

import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.UnbakedGlyph;
import com.mojang.blaze3d.font.GlyphBitmap;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.GpuTexture;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.client.gui.font.CodepointMap;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EmptyGlyph;
import net.minecraft.client.gui.font.providers.FreeTypeUtil;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Locale;

public class AobaGlyphProvider implements GlyphProvider {
	@Nullable
	private ByteBuffer fontMemory;
	@Nullable
	private FT_Face face;
	private final float oversample;
	private final long emboldenStrength;
	private final CodepointMap<GlyphEntry> glyphs = new CodepointMap<>(GlyphEntry[]::new, GlyphEntry[][]::new);

	private static final int OVERSAMPLE = 2;

	public AobaGlyphProvider(ByteBuffer fontMemory, FT_Face face, float atlasSize, float emboldenPixels) {
		this.fontMemory = fontMemory;
		this.face = face;
		int pixelSize = Math.round(atlasSize * OVERSAMPLE);
		this.oversample = OVERSAMPLE;
		this.emboldenStrength = (long) (emboldenPixels * 64);

		FreeType.FT_Set_Pixel_Sizes(face, pixelSize, pixelSize);

		try (MemoryStack stack = MemoryStack.stackPush()) {
			IntBuffer glyphIndex = stack.mallocInt(1);
			int charcode = (int) FreeType.FT_Get_First_Char(face, glyphIndex);

			while (true) {
				int idx = glyphIndex.get(0);
				if (idx == 0) return;
				glyphs.put(charcode, new GlyphEntry(idx));
				charcode = (int) FreeType.FT_Get_Next_Char(face, charcode, glyphIndex);
			}
		}
	}

	@Nullable
	@Override
	public UnbakedGlyph getGlyph(int codepoint) {
		GlyphEntry entry = glyphs.get(codepoint);
		return entry != null ? getOrLoadGlyph(codepoint, entry) : null;
	}

	private UnbakedGlyph getOrLoadGlyph(int codepoint, GlyphEntry entry) {
		UnbakedGlyph glyph = entry.glyph;
		if (glyph == null) {
			FT_Face f = validateOpen();
			synchronized (f) {
				glyph = entry.glyph;
				if (glyph == null) {
					glyph = loadGlyph(codepoint, f, entry.index);
					entry.glyph = glyph;
				}
			}
		}
		return glyph;
	}

	private void loadAndEmbolden(FT_Face f, int glyphIndex) {
		FreeType.FT_Load_Glyph(f, glyphIndex, FreeType.FT_LOAD_DEFAULT);
		FT_GlyphSlot slot = f.glyph();
		if (slot != null && emboldenStrength > 0 && slot.format() == FreeType.FT_GLYPH_FORMAT_OUTLINE) {
			FT_Outline outline = slot.outline();
			if (outline != null) {
				FreeType.FT_Outline_Embolden(outline, emboldenStrength);
			}
		}
		FreeType.FT_Render_Glyph(slot, FreeType.FT_RENDER_MODE_NORMAL);
	}

	private UnbakedGlyph loadGlyph(int codepoint, FT_Face f, int glyphIndex) {
		loadAndEmbolden(f, glyphIndex);

		FT_GlyphSlot slot = f.glyph();
		if (slot == null) {
			throw new NullPointerException(String.format(Locale.ROOT, "Glyph U+%06X not initialized", codepoint));
		}

		float advance = FreeTypeUtil.x(slot.advance());
		FT_Bitmap bitmap = slot.bitmap();
		int left = slot.bitmap_left();
		int top = slot.bitmap_top();
		int width = bitmap.width();
		int height = bitmap.rows();

		if (width > 0 && height > 0) {
			return new AobaGlyph(left, top, width, height, advance, glyphIndex);
		}
		return new EmptyGlyph(advance / oversample);
	}

	private FT_Face validateOpen() {
		if (fontMemory != null && face != null) return face;
		throw new IllegalStateException("Provider already closed");
	}

	@Override
	public void close() {
		if (face != null) {
			synchronized (FreeTypeUtil.LIBRARY_LOCK) {
				FreeTypeUtil.checkError(FreeType.FT_Done_Face(face), "Deleting face");
			}
			face = null;
		}
		MemoryUtil.memFree(fontMemory);
		fontMemory = null;
	}

	@Override
	public IntSet getSupportedGlyphs() {
		return glyphs.keySet();
	}

	private class AobaGlyph implements UnbakedGlyph {
		final int width, height;
		final float bearingX, bearingY;
		private final GlyphInfo info;
		final int index;

		AobaGlyph(float left, float top, int width, int height, float advance, int index) {
			this.width = width;
			this.height = height;
			this.info = GlyphInfo.simple(advance / oversample);
			this.bearingX = left / oversample;
			this.bearingY = top / oversample;
			this.index = index;
		}

		@Override
		public GlyphInfo info() { return info; }

		@Override
		public BakedGlyph bake(UnbakedGlyph.Stitcher stitcher) {
			return stitcher.stitch(info, new GlyphBitmap() {
				@Override public int getPixelWidth() { return width; }
				@Override public int getPixelHeight() { return height; }
				@Override public float getOversample() { return oversample; }
				@Override public float getBearingLeft() { return bearingX; }
				@Override public float getBearingTop() { return bearingY; }
				@Override public boolean isColored() { return false; }

				@Override
				public void upload(int x, int y, GpuTexture texture) {
					FT_Face f = validateOpen();
					synchronized (f) {
						loadAndEmbolden(f, index);
						FT_GlyphSlot slot = f.glyph();
						if (slot != null) {
							FT_Bitmap bitmap = slot.bitmap();
							if (bitmap.pixel_mode() == 2 && bitmap.width() == width && bitmap.rows() == height) {
								int size = bitmap.width() * bitmap.rows();
								ByteBuffer buf = bitmap.buffer(size);
								if (buf != null) {
									RenderSystem.getDevice().createCommandEncoder().writeToTexture(
											texture, buf,
											0, 0, x, y, width, height);
								}
							}
						}
					}
				}
			});
		}
	}

	private static class GlyphEntry {
		final int index;
		@Nullable volatile UnbakedGlyph glyph;
		GlyphEntry(int index) { this.index = index; }
	}
}
