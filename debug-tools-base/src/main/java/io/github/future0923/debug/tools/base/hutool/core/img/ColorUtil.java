/*
 * Copyright (C) 2024-2025 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.github.future0923.debug.tools.base.hutool.core.img;

import io.github.future0923.debug.tools.base.hutool.core.convert.Convert;
import io.github.future0923.debug.tools.base.hutool.core.lang.Assert;
import io.github.future0923.debug.tools.base.hutool.core.util.ArrayUtil;
import io.github.future0923.debug.tools.base.hutool.core.util.RandomUtil;
import io.github.future0923.debug.tools.base.hutool.core.util.StrUtil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 颜色工具类
 *
 * @since 5.8.7
 */
public class ColorUtil {

	/**
	 * RGB颜色范围上限
	 */
	private static final int RGB_COLOR_BOUND = 256;

	/**
	 * Color对象转16进制表示，例如#fcf6d6
	 *
	 * @param color {@link Color}
	 * @return 16进制的颜色值，例如#fcf6d6
	 * @since 4.1.14
	 */
	public static String toHex(Color color) {
		return toHex(color.getRed(), color.getGreen(), color.getBlue());
	}

	/**
	 * RGB颜色值转换成十六进制颜色码
	 *
	 * @param r 红(R)
	 * @param g 绿(G)
	 * @param b 蓝(B)
	 * @return 返回字符串形式的 十六进制颜色码 如
	 */
	public static String toHex(int r, int g, int b) {
		// rgb 小于 255
		if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) {
			throw new IllegalArgumentException("RGB must be 0~255!");
		}
		return String.format("#%02X%02X%02X", r, g, b);
	}

	/**
	 * 将颜色值转换成具体的颜色类型 汇集了常用的颜色集，支持以下几种形式：
	 *
	 * <pre>
	 * 1. 颜色的英文名（大小写皆可）
	 * 2. 16进制表示，例如：#fcf6d6或者$fcf6d6
	 * 3. RGB形式，例如：13,148,252
	 * </pre>
	 * <p>
	 * 方法来自：com.lnwazg.kit
	 *
	 * @param colorName 颜色的英文名，16进制表示或RGB表示
	 * @return {@link Color}
	 * @since 4.1.14
	 */
	public static Color getColor(String colorName) {
		if (StrUtil.isBlank(colorName)) {
			return null;
		}
		colorName = colorName.toUpperCase();

		if ("BLACK".equals(colorName)) {
			return Color.BLACK;
		} else if ("WHITE".equals(colorName)) {
			return Color.WHITE;
		} else if ("LIGHTGRAY".equals(colorName) || "LIGHT_GRAY".equals(colorName)) {
			return Color.LIGHT_GRAY;
		} else if ("GRAY".equals(colorName)) {
			return Color.GRAY;
		} else if ("DARKGRAY".equals(colorName) || "DARK_GRAY".equals(colorName)) {
			return Color.DARK_GRAY;
		} else if ("RED".equals(colorName)) {
			return Color.RED;
		} else if ("PINK".equals(colorName)) {
			return Color.PINK;
		} else if ("ORANGE".equals(colorName)) {
			return Color.ORANGE;
		} else if ("YELLOW".equals(colorName)) {
			return Color.YELLOW;
		} else if ("GREEN".equals(colorName)) {
			return Color.GREEN;
		} else if ("MAGENTA".equals(colorName)) {
			return Color.MAGENTA;
		} else if ("CYAN".equals(colorName)) {
			return Color.CYAN;
		} else if ("BLUE".equals(colorName)) {
			return Color.BLUE;
		} else if ("DARKGOLD".equals(colorName)) {
			// 暗金色
			return hexToColor("#9e7e67");
		} else if ("LIGHTGOLD".equals(colorName)) {
			// 亮金色
			return hexToColor("#ac9c85");
		} else if (StrUtil.startWith(colorName, '#')) {
			return hexToColor(colorName);
		} else if (StrUtil.startWith(colorName, '$')) {
			// 由于#在URL传输中无法传输，因此用$代替#
			return hexToColor("#" + colorName.substring(1));
		} else {
			// rgb值
			final List<String> rgb = StrUtil.split(colorName, ',');
			if (3 == rgb.size()) {
				final Integer r = Convert.toInt(rgb.get(0));
				final Integer g = Convert.toInt(rgb.get(1));
				final Integer b = Convert.toInt(rgb.get(2));
				if (false == ArrayUtil.hasNull(r, g, b)) {
					return new Color(r, g, b);
				}
			} else {
				return null;
			}
		}
		return null;
	}

	/**
	 * 获取一个RGB值对应的颜色
	 *
	 * @param rgb RGB值
	 * @return {@link Color}
	 * @since 4.1.14
	 */
	public static Color getColor(int rgb) {
		return new Color(rgb);
	}

	/**
	 * 16进制的颜色值转换为Color对象，例如#fcf6d6
	 *
	 * @param hex 16进制的颜色值，例如#fcf6d6
	 * @return {@link Color}
	 * @since 4.1.14
	 */
	public static Color hexToColor(String hex) {
		return getColor(Integer.parseInt(StrUtil.removePrefix(hex, "#"), 16));
	}

	/**
	 * 叠加颜色
	 * @param color1 颜色1
	 * @param color2 颜色2
	 * @return 叠加后的颜色
	 */
	public static Color add(Color color1, Color color2) {
		double r1 = color1.getRed();
		double g1 = color1.getGreen();
		double b1 = color1.getBlue();
		double a1 = color1.getAlpha();
		double r2 = color2.getRed();
		double g2 = color2.getGreen();
		double b2 = color2.getBlue();
		double a2 = color2.getAlpha();
		int r = (int) ((r1 * a1 / 255 + r2 * a2 / 255) / (a1 / 255 + a2 / 255));
		int g = (int) ((g1 * a1 / 255 + g2 * a2 / 255) / (a1 / 255 + a2 / 255));
		int b = (int) ((b1 * a1 / 255 + b2 * a2 / 255) / (a1 / 255 + a2 / 255));
		return new Color(r, g, b);
	}

	/**
	 * 生成随机颜色，与指定颜色有一定的区分度
	 *
	 * @param compareColor 比较颜色
	 * @param minDistance 最小色差，按三维坐标计算的距离值
	 * @return 随机颜色
	 * @since 5.8.30
	 */
	public static Color randomColor(Color compareColor,int minDistance) {
		// 注意minDistance太大会增加循环次数，保证至少1/3的概率生成成功
		Assert.isTrue(minDistance < maxDistance(compareColor) / 3 * 2,
				"minDistance is too large, there are too few remaining colors!");
		Color color = randomColor();
		while (computeColorDistance(compareColor,color) < minDistance) {
			color = randomColor();
		}
		return color;
	}

	/**
	 * 生成随机颜色
	 *
	 * @return 随机颜色
	 * @since 3.1.2
	 */
	public static Color randomColor() {
		return randomColor(null);
	}

	/**
	 * 计算给定点与其他点之间的最大可能距离。
	 *
	 * @param color 指定颜色
	 * @return 其余颜色与color的最大距离
	 * @since 6.0.0-M16
	 */
	public static int maxDistance(final Color color) {
		if (null == color) {
			// (0,0,0)到(256,256,256)的距离约等于442.336
			return 443;
		}
		final int maxX = RGB_COLOR_BOUND - 2 * color.getRed();
		final int maxY = RGB_COLOR_BOUND - 2 * color.getGreen();
		final int maxZ = RGB_COLOR_BOUND - 2 * color.getBlue();
		return (int)Math.sqrt(maxX * maxX + maxY * maxY + maxZ * maxZ);
	}

	/**
	 * 计算两个颜色之间的色差，按三维坐标距离计算
	 *
	 * @param color1 颜色1
	 * @param color2 颜色2
	 * @return 色差，按三维坐标距离值
	 * @since 5.8.30
	 */
	public static int computeColorDistance(Color color1, Color color2) {
		if (null == color1 || null == color2) {
			// (0,0,0)到(256,256,256)的距离约等于442.336
			return 443;
		}
		return (int) Math.sqrt(Math.pow(color1.getRed() - color2.getRed(), 2)
				+ Math.pow(color1.getGreen() - color2.getGreen(), 2)
				+ Math.pow(color1.getBlue() - color2.getBlue(), 2));
	}

	/**
	 * 生成随机颜色
	 *
	 * @param random 随机对象 {@link Random}
	 * @return 随机颜色
	 * @since 3.1.2
	 */
	public static Color randomColor(Random random) {
		if (null == random) {
			random = RandomUtil.getRandom();
		}
		return new Color(random.nextInt(RGB_COLOR_BOUND), random.nextInt(RGB_COLOR_BOUND), random.nextInt(RGB_COLOR_BOUND));
	}

	/**
	 * 获取给定图片的主色调，背景填充用
	 *
	 * @param image      {@link BufferedImage}
	 * @param rgbFilters 过滤多种颜色
	 * @return {@link String} #ffffff
	 * @since 5.6.7
	 */
	public static String getMainColor(BufferedImage image, int[]... rgbFilters) {
		int r, g, b;
		Map<String, Long> countMap = new HashMap<>();
		int width = image.getWidth();
		int height = image.getHeight();
		int minx = image.getMinX();
		int miny = image.getMinY();
		for (int i = minx; i < width; i++) {
			for (int j = miny; j < height; j++) {
				int pixel = image.getRGB(i, j);
				r = (pixel & 0xff0000) >> 16;
				g = (pixel & 0xff00) >> 8;
				b = (pixel & 0xff);
				if (matchFilters(r, g, b, rgbFilters)) {
					continue;
				}
				countMap.merge(r + "-" + g + "-" + b, 1L, Long::sum);
			}
		}
		String maxColor = null;
		long maxCount = 0;
		for (Map.Entry<String, Long> entry : countMap.entrySet()) {
			String key = entry.getKey();
			Long count = entry.getValue();
			if (count > maxCount) {
				maxColor = key;
				maxCount = count;
			}
		}
		final String[] splitRgbStr = StrUtil.splitToArray(maxColor, '-');
		String rHex = Integer.toHexString(Integer.parseInt(splitRgbStr[0]));
		String gHex = Integer.toHexString(Integer.parseInt(splitRgbStr[1]));
		String bHex = Integer.toHexString(Integer.parseInt(splitRgbStr[2]));
		rHex = rHex.length() == 1 ? "0" + rHex : rHex;
		gHex = gHex.length() == 1 ? "0" + gHex : gHex;
		bHex = bHex.length() == 1 ? "0" + bHex : bHex;
		return "#" + rHex + gHex + bHex;
	}

	/**
	 * 给定RGB是否匹配过滤器中任何一个RGB颜色
	 *
	 * @param r          R
	 * @param g          G
	 * @param b          B
	 * @param rgbFilters 颜色过滤器
	 * @return 是否匹配
	 */
	private static boolean matchFilters(int r, int g, int b, int[]... rgbFilters) {
		if (ArrayUtil.isNotEmpty(rgbFilters)) {
			for (int[] rgbFilter : rgbFilters) {
				if (r == rgbFilter[0] && g == rgbFilter[1] && b == rgbFilter[2]) {
					return true;
				}
			}
		}
		return false;
	}
}
