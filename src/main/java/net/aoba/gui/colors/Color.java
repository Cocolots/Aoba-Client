/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.gui.colors;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.joml.Vector3f;

public final class Color {
	private final int r;
	private final int g;
	private final int b;
	private final int alpha;

	private final float hue;
	private final float saturation;
	private final float luminance;

	public Color(int r, int g, int b) {
		this(r, g, b, 255);
	}

	public Color(int r, int g, int b, int alpha) {
		this.r = r;
		this.g = g;
		this.b = b;
		this.alpha = alpha;

		float[] hsv = hsvFromRGB(r, g, b);
		this.hue = hsv[0];
		this.saturation = hsv[1];
		this.luminance = hsv[2];
	}

	public Color(float r, float g, float b, float alpha) {
		this((int) (r * 255f), (int) (g * 255f), (int) (b * 255f), (int) (alpha * 255f));
	}

	public Color(Color other) {
		this(other.r, other.g, other.b, other.alpha);
	}

	public Color(float hue, float saturation, float luminance) {
		this(hue, saturation, luminance, 255);
	}

	public Color(float hue, float saturation, float luminance, int alpha) {
		this.hue = hue;
		this.saturation = saturation;
		this.luminance = luminance;
		this.alpha = alpha;

		int[] rgb = hsv2rgbInt(hue, saturation, luminance);
		this.r = rgb[0];
		this.g = rgb[1];
		this.b = rgb[2];
	}

	public Color withRGB(int r, int g, int b) {
		return new Color(r, g, b, alpha);
	}

	public Color withRGBA(int r, int g, int b, int alpha) {
		return new Color(r, g, b, alpha);
	}

	public Color withAlpha(int alpha) {
		return new Color(r, g, b, alpha);
	}

	public Color withHue(float hue) {
		return new Color(hue, saturation, luminance, alpha);
	}

	public Color withSaturation(float saturation) {
		return new Color(hue, saturation, luminance, alpha);
	}

	public Color withLuminance(float luminance) {
		return new Color(hue, saturation, luminance, alpha);
	}

	public Color withHSV(float hue, float saturation, float luminance) {
		return new Color(hue, saturation, luminance, alpha);
	}

	public Color getAsSolid() {
		return new Color(r, g, b, 255);
	}

	public static Color interpolate(Color color1, Color color2, float factor) {
		int r = (int) (color1.r + (color2.r - color1.r) * factor);
		int g = (int) (color1.g + (color2.g - color1.g) * factor);
		int b = (int) (color1.b + (color2.b - color1.b) * factor);
		int alpha = (int) (color1.alpha + (color2.alpha - color1.alpha) * factor);
		return new Color(r, g, b, alpha);
	}

	private static float[] hsvFromRGB(int r, int g, int b) {
		float rPrime = r / 255.0f;
		float gPrime = g / 255.0f;
		float bPrime = b / 255.0f;

		float cMax = Math.max(rPrime, Math.max(gPrime, bPrime));
		float cMin = Math.min(rPrime, Math.min(gPrime, bPrime));
		float delta = cMax - cMin;

		float hue;
		if (delta == 0.0f) {
			hue = 0.0f;
		} else if (cMax == rPrime) {
			hue = 60.0f * (((gPrime - bPrime) / delta) % 6);
		} else if (cMax == gPrime) {
			hue = 60.0f * (((bPrime - rPrime) / delta) + 2);
		} else {
			hue = 60.0f * (((rPrime - gPrime) / delta) + 4);
		}
		if (hue < 0.0f) hue += 360.0f;

		float saturation = cMax == 0.0f ? 0.0f : delta / cMax;
		float luminance = cMax;
		return new float[] { hue, saturation, luminance };
	}

	public float getHue() {
		return hue;
	}

	public float getSaturation() {
		return saturation;
	}

	public float getLuminance() {
		return luminance;
	}

	public String getColorAsString() {
		return Integer.toString(r) + Integer.toString(g) + Integer.toString(b);
	}

	public int getColorAsInt() {
		int A = (alpha << 24) & 0xFF000000;
		int R = (r << 16) & 0x00FF0000;
		int G = (g << 8) & 0x0000FF00;
		int B = b & 0x000000FF;
		return A | R | G | B;
	}

	public String getColorAsHex() {
		return String.format("#%08X", getColorAsInt());
	}

	public float getRed() {
		return r / 255.0f;
	}

	public float getGreen() {
		return g / 255.0f;
	}

	public float getBlue() {
		return b / 255.0f;
	}

	public float getAlpha() {
		return alpha / 255.0f;
	}

	public Color add(Color color) {
		return new Color(r + color.r, g + color.g, b + color.b);
	}

	public Color add(float r, float g, float b) {
		return new Color((int) Math.min(255, this.r + r), (int) Math.min(255, this.g + g),
				(int) Math.min(255, this.b + b));
	}

	public Color add(float r, float g, float b, float a) {
		return new Color((int) Math.min(255, this.r + r), (int) Math.min(255, this.g + g),
				(int) Math.min(255, this.b + b), (int) Math.min(255, this.alpha + a));
	}

	public static String rgbToString(int r, int g, int b) {
		return Integer.toString(r) + Integer.toString(g) + Integer.toString(b);
	}

	public static int rgbToInt(int r, int g, int b) {
		return Integer.parseInt(Integer.toString(r) + Integer.toString(g) + Integer.toString(b));
	}

	public static int convertRGBToHex(int r, int g, int b) {
		String strr = StringUtils.leftPad(Integer.toHexString(r), 2, '0');
		String strg = StringUtils.leftPad(Integer.toHexString(g), 2, '0');
		String strb = StringUtils.leftPad(Integer.toHexString(b), 2, '0');
		return Integer.parseInt(strr + strg + strb, 16);
	}

	public static Color convertHextoRGB(String hexColor) {
		hexColor = hexColor.replace("#", "");
		if (hexColor.length() == 6) {
			int r = Integer.valueOf(hexColor.substring(0, 2), 16);
			int g = Integer.valueOf(hexColor.substring(2, 4), 16);
			int b = Integer.valueOf(hexColor.substring(4, 6), 16);
			return new Color(r, g, b);
		} else if (hexColor.length() == 8) {
			int alpha = Integer.valueOf(hexColor.substring(0, 2), 16);
			int r = Integer.valueOf(hexColor.substring(2, 4), 16);
			int g = Integer.valueOf(hexColor.substring(4, 6), 16);
			int b = Integer.valueOf(hexColor.substring(6, 8), 16);
			return new Color(r, g, b, alpha);
		} else {
			throw new IllegalArgumentException("Invalid hex color format. Expected 6 or 8 characters.");
		}
	}

	public static Color hsv2rgb(float hue, float saturation, float luminance) {
		int[] rgb = hsv2rgbInt(hue, saturation, luminance);
		return new Color(rgb[0], rgb[1], rgb[2]);
	}

	private static int[] hsv2rgbInt(float hue, float saturation, float luminance) {
		float h = hue / 60f;
		float chroma = luminance * saturation;
		float x = chroma * (1 - Math.abs((h % 2) - 1));

		Vector3f rgbVec;
		if (h >= 0 && h <= 1)
			rgbVec = new Vector3f(chroma, x, 0);
		else if (h <= 2)
			rgbVec = new Vector3f(x, chroma, 0);
		else if (h <= 3)
			rgbVec = new Vector3f(0, chroma, x);
		else if (h <= 4)
			rgbVec = new Vector3f(0, x, chroma);
		else if (h <= 5)
			rgbVec = new Vector3f(x, 0, chroma);
		else if (h <= 6)
			rgbVec = new Vector3f(chroma, 0, x);
		else
			return new int[] { 0, 0, 0 };

		float m = luminance - chroma;
		return new int[] {
				(int) (255.0f * (rgbVec.x + m)),
				(int) (255.0f * (rgbVec.y + m)),
				(int) (255.0f * (rgbVec.z + m))
		};
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Color other)) return false;
		return r == other.r && g == other.g && b == other.b && alpha == other.alpha
				&& Float.compare(hue, other.hue) == 0
				&& Float.compare(saturation, other.saturation) == 0
				&& Float.compare(luminance, other.luminance) == 0;
	}

	@Override
	public int hashCode() {
		return Objects.hash(r, g, b, alpha, hue, saturation, luminance);
	}
}
