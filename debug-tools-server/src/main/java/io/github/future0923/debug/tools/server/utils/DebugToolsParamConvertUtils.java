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
package io.github.future0923.debug.tools.server.utils;

import io.github.future0923.debug.tools.base.hutool.core.convert.Convert;
import io.github.future0923.debug.tools.base.logging.Logger;
import io.github.future0923.debug.tools.common.dto.RunContentDTO;
import io.github.future0923.debug.tools.common.enums.RunContentType;
import io.github.future0923.debug.tools.base.utils.DebugToolsClassUtils;
import io.github.future0923.debug.tools.common.utils.DebugToolsJsonUtils;
import io.github.future0923.debug.tools.common.utils.DebugToolsLambdaUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;

/**
 * @author future0923
 */
public class DebugToolsParamConvertUtils {

    private static final Logger log = Logger.getLogger(DebugToolsParamConvertUtils.class);

    public static Object[] getArgs(Method bridgedMethod, Map<String, RunContentDTO> targetMethodContent) {
        Object[] targetMethodArgs = new Object[bridgedMethod.getParameterCount()];
        for (int i = 0; i < bridgedMethod.getParameterCount(); i++) {
            Parameter parameter = bridgedMethod.getParameters()[i];
            try {
                targetMethodArgs[i] = getArg(targetMethodContent, parameter, i);
            } catch (Exception e) {
                log.error("转换第{}个参数失败", e, i);
                targetMethodArgs[i] = null;
            }
        }
        return targetMethodArgs;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object getArg(Map<String, RunContentDTO> contentMap, Parameter parameter, Integer parameterIndex) {
        if (contentMap == null || contentMap.isEmpty()) {
            return null;
        }
        RunContentDTO runContentDTO = getRunContentDTO(contentMap, parameterIndex);
        if (runContentDTO == null) {
            return null;
        }
        if (RunContentType.BEAN.getType().equals(runContentDTO.getType())) {
            return BeanInstanceUtils.getInstance(parameter.getType());
        } else if (RunContentType.LAMBDA.getType().equals(runContentDTO.getType())) {
            if (runContentDTO.getContent() != null && parameter.getType().isInterface() && (runContentDTO.getContent().toString().contains("->") || runContentDTO.getContent().toString().contains("::"))) {
                return DebugToolsLambdaUtils.createLambda(runContentDTO.getContent().toString(), parameter.getParameterizedType());
            }
        } else if (RunContentType.JSON_ENTITY.getType().equals(runContentDTO.getType())) {
            return DebugToolsJsonUtils.toBean(DebugToolsJsonUtils.toJsonStr(runContentDTO.getContent()), parameter.getParameterizedType(), true);
        } else if (RunContentType.SIMPLE.getType().equals(runContentDTO.getType())) {
            if (DebugToolsClassUtils.isSimpleValueType(parameter.getType())) {
                try {
                    if (parameter.getType().isAssignableFrom(LocalDateTime.class)) {
                        return LocalDateTime.parse(runContentDTO.getContent().toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    }
                    if (parameter.getType().isAssignableFrom(LocalDate.class)) {
                        return LocalDate.parse(runContentDTO.getContent().toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    }
                    if (parameter.getType().isAssignableFrom(Date.class)) {
                        return DateUtils.parseDate(runContentDTO.getContent().toString(), "yyyy-MM-dd HH:mm:ss");
                    }
                    // spring简单类型转换
                    return Convert.convert(parameter.getType(), runContentDTO.getContent());
                } catch (Exception e) {
                    log.error("转换失败", e);
                    return null;
                }
            }
        } else if (RunContentType.ENUM.getType().equals(runContentDTO.getType())) {
            return Enum.valueOf((Class<? extends Enum>) parameter.getType(), runContentDTO.getContent().toString());
        } else if (RunContentType.REQUEST.getType().equals(runContentDTO.getType())) {
            return null;
        } else if (RunContentType.RESPONSE.getType().equals(runContentDTO.getType())) {
            return null;
        } else if (RunContentType.FILE.getType().equals(runContentDTO.getType())) {
            return new File(runContentDTO.getContent().toString());
        } else if (RunContentType.CLASS.getType().equals(runContentDTO.getType())) {
            try {
                return Class.forName(runContentDTO.getContent().toString());
            } catch (Exception ignored) {

            }
        }
        return null;
    }

    /**
     * 获取参数传递的对应数据
     *
     * @param contentMap     入参信息
     * @param parameterIndex 参数下标（Parameter没有参数名字信息，只能通过顺序）
     * @return RunContentDTO
     */
    private static RunContentDTO getRunContentDTO(Map<String, RunContentDTO> contentMap, Integer parameterIndex) {
        int index = 0;
        for (RunContentDTO value : contentMap.values()) {
            if (parameterIndex == index++) {
                return value;
            }
        }
        return null;
    }

}
