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
package io.github.future0923.debug.tools.base.hutool.core.annotation;

/**
 * 表示一个被指定了强制别名的注解属性。
 * 当调用{@link #getValue()}时，总是返回{@link #linked}的值
 *
 * @author huangchengxing
 * @see AliasAnnotationPostProcessor
 * @see AliasLinkAnnotationPostProcessor
 * @see RelationType#ALIAS_FOR
 * @see RelationType#FORCE_ALIAS_FOR
 */
public class ForceAliasedAnnotationAttribute extends AbstractWrappedAnnotationAttribute {

	protected ForceAliasedAnnotationAttribute(AnnotationAttribute origin, AnnotationAttribute linked) {
		super(origin, linked);
	}

	/**
	 * 总是返回{@link #linked}的{@link AnnotationAttribute#getValue()}的返回值
	 *
	 * @return {@link #linked}的{@link AnnotationAttribute#getValue()}的返回值
	 */
	@Override
	public Object getValue() {
		return linked.getValue();
	}

	/**
	 * 总是返回{@link #linked}的{@link AnnotationAttribute#isValueEquivalentToDefaultValue()}的返回值
	 *
	 * @return {@link #linked}的{@link AnnotationAttribute#isValueEquivalentToDefaultValue()}的返回值
	 */
	@Override
	public boolean isValueEquivalentToDefaultValue() {
		return linked.isValueEquivalentToDefaultValue();
	}

	/**
	 * 总是返回{@link #linked}的{@link AnnotationAttribute#getAttributeType()}的返回值
	 *
	 * @return {@link #linked}的{@link AnnotationAttribute#getAttributeType()}的返回值
	 */
	@Override
	public Class<?> getAttributeType() {
		return linked.getAttributeType();
	}

}
