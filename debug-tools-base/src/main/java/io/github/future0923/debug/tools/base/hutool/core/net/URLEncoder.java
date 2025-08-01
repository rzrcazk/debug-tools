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
package io.github.future0923.debug.tools.base.hutool.core.net;

import io.github.future0923.debug.tools.base.hutool.core.util.CharUtil;
import io.github.future0923.debug.tools.base.hutool.core.util.HexUtil;
import io.github.future0923.debug.tools.base.hutool.core.util.StrUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.BitSet;

/**
 * URL编码，数据内容的类型是 application/x-www-form-urlencoded。
 * TODO 6.x移除此类，使用PercentCodec代替（无法很好区分URL编码和www-form编码）
 *
 * <pre>
 * 1.字符"a"-"z"，"A"-"Z"，"0"-"9"，"."，"-"，"*"，和"_" 都不会被编码;
 * 2.将空格转换为%20 ;
 * 3.将非文本内容转换成"%xy"的形式,xy是两位16进制的数值;
 * </pre>
 *
 * @author looly
 * @see cn.hutool.core.codec.PercentCodec
 * @deprecated 此类中的方法并不规范，请使用 {@link RFC3986}
 */
@Deprecated
public class URLEncoder implements Serializable {
	private static final long serialVersionUID = 1L;

	// --------------------------------------------------------------------------------------------- Static method start
	/**
	 * 默认URLEncoder<br>
	 * 默认的编码器针对URI路径编码，定义如下：
	 *
	 * <pre>
	 * default = pchar / "/"
	 * pchar = unreserved（不处理） / pct-encoded / sub-delims（子分隔符） / ":" / "@"
	 * unreserved = ALPHA / DIGIT / "-" / "." / "_" / "~"
	 * sub-delims = "!" / "$" / "&amp;" / "'" / "(" / ")" / "*" / "+" / "," / ";" / "="
	 * </pre>
	 */
	public static final URLEncoder DEFAULT = createDefault();

	/**
	 * URL的Path的每一个Segment URLEncoder<br>
	 * 默认的编码器针对URI路径编码，定义如下：
	 *
	 * <pre>
	 * pchar = unreserved / pct-encoded / sub-delims / ":"（非空segment不包含:） / "@"
	 * unreserved = ALPHA / DIGIT / "-" / "." / "_" / "~"
	 * sub-delims = "!" / "$" / "&amp;" / "'" / "(" / ")" / "*" / "+" / "," / ";" / "="
	 * </pre>
	 *
	 * 定义见：https://www.rfc-editor.org/rfc/rfc3986.html#section-3.3
	 */
	public static final URLEncoder PATH_SEGMENT = createPathSegment();

	/**
	 * URL的Fragment URLEncoder<br>
	 * 默认的编码器针对Fragment，定义如下：
	 *
	 * <pre>
	 * fragment    = *( pchar / "/" / "?" )
	 * pchar       = unreserved / pct-encoded / sub-delims / ":" / "@"
	 * unreserved  = ALPHA / DIGIT / "-" / "." / "_" / "~"
	 * sub-delims  = "!" / "$" / "&amp;" / "'" / "(" / ")" / "*" / "+" / "," / ";" / "="
	 * </pre>
	 *
	 * 具体见：https://datatracker.ietf.org/doc/html/rfc3986#section-3.5
	 * @since 5.7.13
	 */
	public static final URLEncoder FRAGMENT = createFragment();

	/**
	 * 用于查询语句的URLEncoder<br>
	 * 编码器针对URI路径编码，定义如下：
	 *
	 * <pre>
	 * 0x20 ' ' =》 '+'
	 * 0x2A, 0x2D, 0x2E, 0x30 to 0x39, 0x41 to 0x5A, 0x5F, 0x61 to 0x7A as-is
	 * '*', '-', '.', '0' to '9', 'A' to 'Z', '_', 'a' to 'z' Also '=' and '&amp;' 不编码
	 * 其它编码为 %nn 形式
	 * </pre>
	 * <p>
	 * 详细见：https://www.w3.org/TR/html5/forms.html#application/x-www-form-urlencoded-encoding-algorithm
	 */
	public static final URLEncoder QUERY = createQuery();

	/**
	 * 全编码的URLEncoder<br>
	 * <pre>
	 *  0x2A, 0x2D, 0x2E, 0x30 to 0x39, 0x41 to 0x5A, 0x5F, 0x61 to 0x7A as-is
	 *  '*', '-', '.', '0' to '9', 'A' to 'Z', '_', 'a' to 'z' 不编码
	 *  其它编码为 %nn 形式
	 * </pre>
	 */
	public static final URLEncoder ALL = createAll();

	/**
	 * 创建默认URLEncoder<br>
	 * 默认的编码器针对URI路径编码，定义如下：
	 *
	 * <pre>
	 * default = pchar / "/"
	 * pchar = unreserved（不处理） / pct-encoded / sub-delims（子分隔符） / ":" / "@"
	 * unreserved = ALPHA / DIGIT / "-" / "." / "_" / "~"
	 * sub-delims = "!" / "$" / "&amp;" / "'" / "(" / ")" / "*" / "+" / "," / ";" / "="
	 * </pre>
	 *
	 * @return URLEncoder
	 */
	public static URLEncoder createDefault() {
		final URLEncoder encoder = new URLEncoder();
		encoder.addSafeCharacter('-');
		encoder.addSafeCharacter('.');
		encoder.addSafeCharacter('_');
		encoder.addSafeCharacter('~');

		// Add the sub-delims
		addSubDelims(encoder);

		// Add the remaining literals
		encoder.addSafeCharacter(':');
		encoder.addSafeCharacter('@');

		// Add '/' so it isn't encoded when we encode a path
		encoder.addSafeCharacter('/');

		return encoder;
	}

	/**
	 * URL的Path的每一个Segment URLEncoder<br>
	 * 默认的编码器针对URI路径的每一段编码，定义如下：
	 *
	 * <pre>
	 * pchar = unreserved / pct-encoded / sub-delims / ":"（非空segment不包含:） / "@"
	 * unreserved = ALPHA / DIGIT / "-" / "." / "_" / "~"
	 * sub-delims = "!" / "$" / "&amp;" / "'" / "(" / ")" / "*" / "+" / "," / ";" / "="
	 * </pre>
	 *
	 * 定义见：https://www.rfc-editor.org/rfc/rfc3986.html#section-3.3
	 *
	 * @return URLEncoder
	 */
	public static URLEncoder createPathSegment() {
		final URLEncoder encoder = new URLEncoder();

		// unreserved
		encoder.addSafeCharacter('-');
		encoder.addSafeCharacter('.');
		encoder.addSafeCharacter('_');
		encoder.addSafeCharacter('~');

		// Add the sub-delims
		addSubDelims(encoder);

		// Add the remaining literals
		//non-zero-length segment without any colon ":"
		//encoder.addSafeCharacter(':');
		encoder.addSafeCharacter('@');

		return encoder;
	}

	/**
	 * URL的Fragment URLEncoder<br>
	 * 默认的编码器针对Fragment，定义如下：
	 *
	 * <pre>
	 * fragment    = *( pchar / "/" / "?" )
	 * pchar       = unreserved / pct-encoded / sub-delims / ":" / "@"
	 * unreserved  = ALPHA / DIGIT / "-" / "." / "_" / "~"
	 * sub-delims  = "!" / "$" / "&amp;" / "'" / "(" / ")" / "*" / "+" / "," / ";" / "="
	 * </pre>
	 *
	 * 具体见：https://datatracker.ietf.org/doc/html/rfc3986#section-3.5
	 *
	 * @return URLEncoder
	 * @since 5.7.13
	 */
	public static URLEncoder createFragment() {
		final URLEncoder encoder = new URLEncoder();
		encoder.addSafeCharacter('-');
		encoder.addSafeCharacter('.');
		encoder.addSafeCharacter('_');
		encoder.addSafeCharacter('~');

		// Add the sub-delims
		addSubDelims(encoder);

		// Add the remaining literals
		encoder.addSafeCharacter(':');
		encoder.addSafeCharacter('@');

		encoder.addSafeCharacter('/');
		encoder.addSafeCharacter('?');

		return encoder;
	}

	/**
	 * 创建用于查询语句的URLEncoder<br>
	 * 编码器针对URI路径编码，定义如下：
	 *
	 * <pre>
	 * 0x20 ' ' =》 '+'
	 * 0x2A, 0x2D, 0x2E, 0x30 to 0x39, 0x41 to 0x5A, 0x5F, 0x61 to 0x7A as-is
	 * '*', '-', '.', '0' to '9', 'A' to 'Z', '_', 'a' to 'z' Also '=' and '&amp;' 不编码
	 * 其它编码为 %nn 形式
	 * </pre>
	 * <p>
	 * 详细见：https://www.w3.org/TR/html5/forms.html#application/x-www-form-urlencoded-encoding-algorithm
	 *
	 * @return URLEncoder
	 */
	public static URLEncoder createQuery() {
		final URLEncoder encoder = new URLEncoder();
		// Special encoding for space
		encoder.setEncodeSpaceAsPlus(true);
		// Alpha and digit are safe by default
		// Add the other permitted characters
		encoder.addSafeCharacter('*');
		encoder.addSafeCharacter('-');
		encoder.addSafeCharacter('.');
		encoder.addSafeCharacter('_');

		encoder.addSafeCharacter('=');
		encoder.addSafeCharacter('&');

		return encoder;
	}

	/**
	 * 创建URLEncoder<br>
	 * 编码器针对URI路径编码，定义如下：
	 *
	 * <pre>
	 * 0x2A, 0x2D, 0x2E, 0x30 to 0x39, 0x41 to 0x5A, 0x5F, 0x61 to 0x7A as-is
	 * '*', '-', '.', '0' to '9', 'A' to 'Z', '_', 'a' to 'z' 不编码
	 * 其它编码为 %nn 形式
	 * </pre>
	 * <p>
	 * 详细见：https://www.w3.org/TR/html5/forms.html#application/x-www-form-urlencoded-encoding-algorithm
	 *
	 * @return URLEncoder
	 */
	public static URLEncoder createAll() {
		final URLEncoder encoder = new URLEncoder();
		encoder.addSafeCharacter('*');
		encoder.addSafeCharacter('-');
		encoder.addSafeCharacter('.');
		encoder.addSafeCharacter('_');

		return encoder;
	}
	// --------------------------------------------------------------------------------------------- Static method end

	/**
	 * 存放安全编码
	 */
	private final BitSet safeCharacters;
	/**
	 * 是否编码空格为+
	 */
	private boolean encodeSpaceAsPlus = false;

	/**
	 * 构造<br>
	 * [a-zA-Z0-9]默认不被编码
	 */
	public URLEncoder() {
		this(new BitSet(256));

		// unreserved
		addAlpha();
		addDigit();
	}

	/**
	 * 构造
	 *
	 * @param safeCharacters 安全字符，安全字符不被编码
	 */
	private URLEncoder(BitSet safeCharacters) {
		this.safeCharacters = safeCharacters;
	}

	/**
	 * 增加安全字符<br>
	 * 安全字符不被编码
	 *
	 * @param c 字符
	 */
	public void addSafeCharacter(char c) {
		safeCharacters.set(c);
	}

	/**
	 * 移除安全字符<br>
	 * 安全字符不被编码
	 *
	 * @param c 字符
	 */
	public void removeSafeCharacter(char c) {
		safeCharacters.clear(c);
	}

	/**
	 * 是否将空格编码为+
	 *
	 * @param encodeSpaceAsPlus 是否将空格编码为+
	 */
	public void setEncodeSpaceAsPlus(boolean encodeSpaceAsPlus) {
		this.encodeSpaceAsPlus = encodeSpaceAsPlus;
	}

	/**
	 * 将URL中的字符串编码为%形式
	 *
	 * @param path    需要编码的字符串
	 * @param charset 编码, {@code null}返回原字符串，表示不编码
	 * @return 编码后的字符串
	 */
	public String encode(String path, Charset charset) {
		if (null == charset || StrUtil.isEmpty(path)) {
			return path;
		}

		final StringBuilder rewrittenPath = new StringBuilder(path.length());
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		OutputStreamWriter writer = new OutputStreamWriter(buf, charset);

		int c;
		for (int i = 0; i < path.length(); i++) {
			c = path.charAt(i);
			if (safeCharacters.get(c)) {
				rewrittenPath.append((char) c);
			} else if (encodeSpaceAsPlus && c == CharUtil.SPACE) {
				// 对于空格单独处理
				rewrittenPath.append('+');
			} else {
				// convert to external encoding before hex conversion
				try {
					writer.write((char) c);
					writer.flush();
				} catch (IOException e) {
					buf.reset();
					continue;
				}

				byte[] ba = buf.toByteArray();
				for (byte toEncode : ba) {
					// Converting each byte in the buffer
					rewrittenPath.append('%');
					HexUtil.appendHex(rewrittenPath, toEncode, false);
				}
				buf.reset();
			}
		}
		return rewrittenPath.toString();
	}

	/**
	 * 增加安全字符[a-z][A-Z]
	 */
	private void addAlpha() {
		for (char i = 'a'; i <= 'z'; i++) {
			addSafeCharacter(i);
		}
		for (char i = 'A'; i <= 'Z'; i++) {
			addSafeCharacter(i);
		}
	}

	/**
	 * 增加数字1-9
	 */
	private void addDigit() {
		for (char i = '0'; i <= '9'; i++) {
			addSafeCharacter(i);
		}
	}


	/**
	 * 增加sub-delims<br>
	 * sub-delims  = "!" / "$" / "&" / "'" / "(" / ") / "*" / "+" / "," / ";" / "="
	 * 定义见：https://datatracker.ietf.org/doc/html/rfc3986#section-2.2
	 */
	private static void addSubDelims(URLEncoder encoder){
		// Add the sub-delims
		encoder.addSafeCharacter('!');
		encoder.addSafeCharacter('$');
		encoder.addSafeCharacter('&');
		encoder.addSafeCharacter('\'');
		encoder.addSafeCharacter('(');
		encoder.addSafeCharacter(')');
		encoder.addSafeCharacter('*');
		encoder.addSafeCharacter('+');
		encoder.addSafeCharacter(',');
		encoder.addSafeCharacter(';');
		encoder.addSafeCharacter('=');
	}
}
