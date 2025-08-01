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

import io.github.future0923.debug.tools.base.hutool.core.codec.PercentCodec;

/**
 * <a href="https://www.ietf.org/rfc/rfc3986.html">RFC3986</a> 编码实现<br>
 * 定义见：<a href="https://www.ietf.org/rfc/rfc3986.html#appendix-A">https://www.ietf.org/rfc/rfc3986.html#appendix-A</a>
 *
 * @author looly
 * @since 5.7.16
 */
public class RFC3986 {

	/**
	 * gen-delims = ":" / "/" / "?" / "#" / "[" / "]" / "@"
	 */
	public static final PercentCodec GEN_DELIMS = PercentCodec.of(":/?#[]@");

	/**
	 * sub-delims = "!" / "$" / "{@code &}" / "'" / "(" / ")" / "*" / "+" / "," / ";" / "="
	 */
	public static final PercentCodec SUB_DELIMS = PercentCodec.of("!$&'()*+,;=");

	/**
	 * reserved = gen-delims / sub-delims<br>
	 * see：<a href="https://www.ietf.org/rfc/rfc3986.html#section-2.2">https://www.ietf.org/rfc/rfc3986.html#section-2.2</a>
	 */
	public static final PercentCodec RESERVED = GEN_DELIMS.orNew(SUB_DELIMS);

	/**
	 * unreserved  = ALPHA / DIGIT / "-" / "." / "_" / "~"<br>
	 * see: <a href="https://www.ietf.org/rfc/rfc3986.html#section-2.3">https://www.ietf.org/rfc/rfc3986.html#section-2.3</a>
	 */
	public static final PercentCodec UNRESERVED = PercentCodec.of(unreservedChars());

	/**
	 * pchar = unreserved / pct-encoded / sub-delims / ":" / "@"
	 */
	public static final PercentCodec PCHAR = UNRESERVED.orNew(SUB_DELIMS).or(PercentCodec.of(":@"));

	/**
	 * segment  = pchar<br>
	 * see: <a href="https://www.ietf.org/rfc/rfc3986.html#section-3.3">https://www.ietf.org/rfc/rfc3986.html#section-3.3</a>
	 */
	public static final PercentCodec SEGMENT = PCHAR;
	/**
	 * segment-nz-nc  = SEGMENT ; non-zero-length segment without any colon ":"
	 */
	public static final PercentCodec SEGMENT_NZ_NC = PercentCodec.of(SEGMENT).removeSafe(':');

	/**
	 * path = segment / "/"
	 */
	public static final PercentCodec PATH = SEGMENT.orNew(PercentCodec.of("/"));

	/**
	 * query = pchar / "/" / "?"
	 */
	public static final PercentCodec QUERY = PCHAR.orNew(PercentCodec.of("/?"));

	/**
	 * fragment     = pchar / "/" / "?"
	 */
	public static final PercentCodec FRAGMENT = QUERY;

	/**
	 * query中的value<br>
	 * value不能包含"{@code &}"，可以包含 "="
	 */
	public static final PercentCodec QUERY_PARAM_VALUE = PercentCodec.of(QUERY).removeSafe('&');

	/**
	 * query中的value编码器，严格模式，value中不能包含任何分隔符。
	 *
	 * @since 6.0.0
	 */
	public static final PercentCodec QUERY_PARAM_VALUE_STRICT = UNRESERVED;

	/**
	 * query中的key<br>
	 * key不能包含"{@code &}" 和 "="
	 */
	public static final PercentCodec QUERY_PARAM_NAME = PercentCodec.of(QUERY_PARAM_VALUE).removeSafe('=');

	/**
	 * query中的key编码器，严格模式，key中不能包含任何分隔符。
	 *
	 * @since 6.0.0
	 */
	public static final PercentCodec QUERY_PARAM_NAME_STRICT = UNRESERVED;

	/**
	 * unreserved  = ALPHA / DIGIT / "-" / "." / "_" / "~"
	 *
	 * @return unreserved字符
	 */
	private static StringBuilder unreservedChars() {
		StringBuilder sb = new StringBuilder();

		// ALPHA
		for (char c = 'A'; c <= 'Z'; c++) {
			sb.append(c);
		}
		for (char c = 'a'; c <= 'z'; c++) {
			sb.append(c);
		}

		// DIGIT
		for (char c = '0'; c <= '9'; c++) {
			sb.append(c);
		}

		// "-" / "." / "_" / "~"
		sb.append("_.-~");

		return sb;
	}
}
