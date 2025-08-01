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
package io.github.future0923.debug.tools.base.hutool.core.comparator;

import io.github.future0923.debug.tools.base.hutool.core.util.ObjectUtil;
import io.github.future0923.debug.tools.base.hutool.core.util.ReflectUtil;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Comparator;

/**
 * Bean字段排序器<br>
 * 参阅feilong-core中的PropertyComparator
 *
 * @param <T> 被比较的Bean
 * @author jiangzeyin
 * @deprecated 此类不再需要，使用FuncComparator代替更加灵活
 */
@Deprecated
public abstract class BaseFieldComparator<T> implements Comparator<T>, Serializable {
	private static final long serialVersionUID = -3482464782340308755L;

	/**
	 * 比较两个对象的同一个字段值
	 *
	 * @param o1    对象1
	 * @param o2    对象2
	 * @param field 字段
	 * @return 比较结果
	 */
	protected int compareItem(T o1, T o2, Field field) {
		if (o1 == o2) {
			return 0;
		} else if (null == o1) {// null 排在后面
			return 1;
		} else if (null == o2) {
			return -1;
		}

		Comparable<?> v1;
		Comparable<?> v2;
		try {
			v1 = (Comparable<?>) ReflectUtil.getFieldValue(o1, field);
			v2 = (Comparable<?>) ReflectUtil.getFieldValue(o2, field);
		} catch (Exception e) {
			throw new ComparatorException(e);
		}

		return compare(o1, o2, v1, v2);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private int compare(T o1, T o2, Comparable fieldValue1, Comparable fieldValue2) {
		int result = ObjectUtil.compare(fieldValue1, fieldValue2);
		if (0 == result) {
			//避免TreeSet / TreeMap 过滤掉排序字段相同但是对象不相同的情况
			result = CompareUtil.compare(o1, o2, true);
		}
		return result;
	}
}
