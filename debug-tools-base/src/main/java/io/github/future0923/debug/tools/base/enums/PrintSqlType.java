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
package io.github.future0923.debug.tools.base.enums;

import io.github.future0923.debug.tools.base.hutool.core.util.StrUtil;
import lombok.Getter;

/**
 * @author future0923
 */
@Getter
public enum PrintSqlType {

    /**
     * 格式化打印
     */
    PRETTY("Pretty"),

    /**
     * 压缩打印
     */
    COMPRESS("Compress"),

    /**
     * 不打印
     */
    NO("No"),

    /**
     * 兼容之前版本
     */
    YES("true"),
    ;

    private final String type;

    PrintSqlType(String type) {
        this.type = type;
    }

    /**
     * 是否开启打印
     */
    public static boolean isPrint(String type) {
        if (StrUtil.isBlank(type)) {
            return false;
        }
        return PRETTY.getType().equalsIgnoreCase(type)
                || COMPRESS.getType().equalsIgnoreCase(type)
                || YES.getType().equalsIgnoreCase(type);
    }

    public static PrintSqlType of(String type) {
        if (StrUtil.isBlank(type)) {
            return NO;
        }
        if (PRETTY.getType().equalsIgnoreCase(type) || YES.getType().equalsIgnoreCase(type)) {
            return PRETTY;
        }
        if (COMPRESS.getType().equalsIgnoreCase(type)) {
            return COMPRESS;
        }
        return NO;
    }
}
