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
package io.github.future0923.debug.tools.base.hutool.core.convert.impl;

import io.github.future0923.debug.tools.base.hutool.core.convert.AbstractConverter;
import io.github.future0923.debug.tools.base.hutool.core.convert.Convert;
import io.github.future0923.debug.tools.base.hutool.core.convert.ConvertException;
import io.github.future0923.debug.tools.base.hutool.core.date.DateTime;
import io.github.future0923.debug.tools.base.hutool.core.date.DateUtil;
import io.github.future0923.debug.tools.base.hutool.core.date.format.GlobalCustomFormat;
import io.github.future0923.debug.tools.base.hutool.core.util.ObjectUtil;
import io.github.future0923.debug.tools.base.hutool.core.util.StrUtil;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.chrono.Era;
import java.time.chrono.IsoEra;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

/**
 * JDK8中新加入的java.time包对象解析转换器<br>
 * 支持的对象包括：
 *
 * <pre>
 * java.time.Instant
 * java.time.LocalDateTime
 * java.time.LocalDate
 * java.time.LocalTime
 * java.time.ZonedDateTime
 * java.time.OffsetDateTime
 * java.time.OffsetTime
 * </pre>
 *
 * @author looly
 * @since 5.0.0
 */
public class TemporalAccessorConverter extends AbstractConverter<TemporalAccessor> {
	private static final long serialVersionUID = 1L;

	private final Class<?> targetType;
	/**
	 * 日期格式化
	 */
	private String format;

	/**
	 * 构造
	 *
	 * @param targetType 目标类型
	 */
	public TemporalAccessorConverter(Class<?> targetType) {
		this(targetType, null);
	}

	/**
	 * 构造
	 *
	 * @param targetType 目标类型
	 * @param format     日期格式
	 */
	public TemporalAccessorConverter(Class<?> targetType, String format) {
		this.targetType = targetType;
		this.format = format;
	}

	/**
	 * 获取日期格式
	 *
	 * @return 设置日期格式
	 */
	public String getFormat() {
		return format;
	}

	/**
	 * 设置日期格式
	 *
	 * @param format 日期格式
	 */
	public void setFormat(String format) {
		this.format = format;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Class<TemporalAccessor> getTargetType() {
		return (Class<TemporalAccessor>) this.targetType;
	}

	@Override
	protected TemporalAccessor convertInternal(Object value) {
		if (value instanceof Number) {
			return parseFromLong(((Number) value).longValue());
		} else if (value instanceof TemporalAccessor) {
			return parseFromTemporalAccessor((TemporalAccessor) value);
		} else if (value instanceof Date) {
			final DateTime dateTime = DateUtil.date((Date) value);
			return parseFromInstant(dateTime.toInstant(), dateTime.getZoneId());
		} else if (value instanceof Calendar) {
			final Calendar calendar = (Calendar) value;
			return parseFromInstant(calendar.toInstant(), calendar.getTimeZone().toZoneId());
		} else if (value instanceof Map) {
			final Map<?, ?> map = (Map<?, ?>) value;
			if (LocalDate.class.equals(this.targetType)) {
				return LocalDate.of(Convert.toInt(map.get("year")), Convert.toInt(map.get("month")), Convert.toInt(map.get("day")));
			} else if (LocalDateTime.class.equals(this.targetType)) {
				return LocalDateTime.of(Convert.toInt(map.get("year")), Convert.toInt(map.get("month")), Convert.toInt(map.get("day")),
					Convert.toInt(map.get("hour")), Convert.toInt(map.get("minute")), Convert.toInt(map.get("second")), Convert.toInt(map.get("second")));
			} else if (LocalTime.class.equals(this.targetType)) {
				return LocalTime.of(Convert.toInt(map.get("hour")), Convert.toInt(map.get("minute")), Convert.toInt(map.get("second")), Convert.toInt(map.get("nano")));
			}
			throw new ConvertException("Unsupported type: [{}] from map: [{}]", this.targetType, map);
		} else {
			return parseFromCharSequence(convertToStr(value));
		}
	}

	/**
	 * 通过反射从字符串转java.time中的对象
	 *
	 * @param value 字符串值
	 * @return 日期对象
	 */
	private TemporalAccessor parseFromCharSequence(CharSequence value) {
		if (StrUtil.isBlank(value)) {
			return null;
		}

		if (DayOfWeek.class.equals(this.targetType)) {
			return DayOfWeek.valueOf(StrUtil.toString(value));
		} else if (Month.class.equals(this.targetType)) {
			return Month.valueOf(StrUtil.toString(value));
		} else if (Era.class.equals(this.targetType)) {
			return IsoEra.valueOf(StrUtil.toString(value));
		} else if (MonthDay.class.equals(this.targetType)) {
			return MonthDay.parse(value);
		}

		final Instant instant;
		ZoneId zoneId;
		if (null != this.format) {
			final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(this.format);

			// issue#I9HQQE
			final TemporalAccessor temporalAccessor = parseWithFormat(this.targetType, value, formatter);
			if (null != temporalAccessor) {
				return temporalAccessor;
			}

			instant = formatter.parse(value, Instant::from);
			zoneId = formatter.getZone();
		} else {
			final DateTime dateTime = DateUtil.parse(value);
			instant = Objects.requireNonNull(dateTime).toInstant();
			zoneId = dateTime.getZoneId();
		}
		return parseFromInstant(instant, zoneId);
	}

	/**
	 * 对于自定义格式的字符串，单独解析为{@link TemporalAccessor}
	 *
	 * @param targetClass 目标类型
	 * @param value       日期字符串
	 * @param formatter   格式
	 * @return {@link TemporalAccessor}
	 */
	private TemporalAccessor parseWithFormat(final Class<?> targetClass, final CharSequence value, final DateTimeFormatter formatter) {
		// issue#I9HQQE
		if (LocalDate.class == targetClass) {
			return LocalDate.parse(value, formatter);
		} else if (LocalDateTime.class == targetClass) {
			return LocalDateTime.parse(value, formatter);
		} else if (LocalTime.class == targetClass) {
			return LocalTime.parse(value, formatter);
		}
		return null;
	}

	/**
	 * 将Long型时间戳转换为java.time中的对象
	 *
	 * @param time 时间戳
	 * @return java.time中的对象
	 */
	private TemporalAccessor parseFromLong(Long time) {
		if (DayOfWeek.class.equals(this.targetType)) {
			return DayOfWeek.of(Math.toIntExact(time));
		} else if (Month.class.equals(this.targetType)) {
			return Month.of(Math.toIntExact(time));
		} else if (Era.class.equals(this.targetType)) {
			return IsoEra.of(Math.toIntExact(time));
		}

		final Instant instant;
		if (GlobalCustomFormat.FORMAT_SECONDS.equals(this.format)) {
			// https://gitee.com/chinabugotech/hutool/issues/I6IS5B
			// Unix时间戳
			instant = Instant.ofEpochSecond(time);
		} else {
			instant = Instant.ofEpochMilli(time);
		}
		return parseFromInstant(instant, null);
	}

	/**
	 * 将TemporalAccessor型时间戳转换为java.time中的对象
	 *
	 * @param temporalAccessor TemporalAccessor对象
	 * @return java.time中的对象
	 */
	private TemporalAccessor parseFromTemporalAccessor(TemporalAccessor temporalAccessor) {
		if (DayOfWeek.class.equals(this.targetType)) {
			return DayOfWeek.from(temporalAccessor);
		} else if (Month.class.equals(this.targetType)) {
			return Month.from(temporalAccessor);
		} else if (MonthDay.class.equals(this.targetType)) {
			return MonthDay.from(temporalAccessor);
		}

		TemporalAccessor result = null;
		if (temporalAccessor instanceof LocalDateTime) {
			result = parseFromLocalDateTime((LocalDateTime) temporalAccessor);
		} else if (temporalAccessor instanceof ZonedDateTime) {
			result = parseFromZonedDateTime((ZonedDateTime) temporalAccessor);
		}

		if (null == result) {
			result = parseFromInstant(DateUtil.toInstant(temporalAccessor), null);
		}

		return result;
	}

	/**
	 * 将TemporalAccessor型时间戳转换为java.time中的对象
	 *
	 * @param localDateTime {@link LocalDateTime}对象
	 * @return java.time中的对象
	 */
	private TemporalAccessor parseFromLocalDateTime(LocalDateTime localDateTime) {
		if (Instant.class.equals(this.targetType)) {
			return DateUtil.toInstant(localDateTime);
		}
		if (LocalDate.class.equals(this.targetType)) {
			return localDateTime.toLocalDate();
		}
		if (LocalTime.class.equals(this.targetType)) {
			return localDateTime.toLocalTime();
		}
		if (ZonedDateTime.class.equals(this.targetType)) {
			return localDateTime.atZone(ZoneId.systemDefault());
		}
		if (OffsetDateTime.class.equals(this.targetType)) {
			return localDateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
		}
		if (OffsetTime.class.equals(this.targetType)) {
			return localDateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime().toOffsetTime();
		}

		return null;
	}

	/**
	 * 将TemporalAccessor型时间戳转换为java.time中的对象
	 *
	 * @param zonedDateTime {@link ZonedDateTime}对象
	 * @return java.time中的对象
	 */
	private TemporalAccessor parseFromZonedDateTime(ZonedDateTime zonedDateTime) {
		if (Instant.class.equals(this.targetType)) {
			return DateUtil.toInstant(zonedDateTime);
		}
		if (LocalDateTime.class.equals(this.targetType)) {
			return zonedDateTime.toLocalDateTime();
		}
		if (LocalDate.class.equals(this.targetType)) {
			return zonedDateTime.toLocalDate();
		}
		if (LocalTime.class.equals(this.targetType)) {
			return zonedDateTime.toLocalTime();
		}
		if (OffsetDateTime.class.equals(this.targetType)) {
			return zonedDateTime.toOffsetDateTime();
		}
		if (OffsetTime.class.equals(this.targetType)) {
			return zonedDateTime.toOffsetDateTime().toOffsetTime();
		}

		return null;
	}

	/**
	 * 将TemporalAccessor型时间戳转换为java.time中的对象
	 *
	 * @param instant {@link Instant}对象
	 * @param zoneId  时区ID，null表示当前系统默认的时区
	 * @return java.time中的对象
	 */
	private TemporalAccessor parseFromInstant(Instant instant, ZoneId zoneId) {
		if (Instant.class.equals(this.targetType)) {
			return instant;
		}

		zoneId = ObjectUtil.defaultIfNull(zoneId, ZoneId::systemDefault);

		TemporalAccessor result = null;
		if (LocalDateTime.class.equals(this.targetType)) {
			result = LocalDateTime.ofInstant(instant, zoneId);
		} else if (LocalDate.class.equals(this.targetType)) {
			result = instant.atZone(zoneId).toLocalDate();
		} else if (LocalTime.class.equals(this.targetType)) {
			result = instant.atZone(zoneId).toLocalTime();
		} else if (ZonedDateTime.class.equals(this.targetType)) {
			result = instant.atZone(zoneId);
		} else if (OffsetDateTime.class.equals(this.targetType)) {
			result = OffsetDateTime.ofInstant(instant, zoneId);
		} else if (OffsetTime.class.equals(this.targetType)) {
			result = OffsetTime.ofInstant(instant, zoneId);
		}
		return result;
	}
}
