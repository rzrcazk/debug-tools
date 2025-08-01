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
package io.github.future0923.debug.tools.base.hutool.core.builder;

import io.github.future0923.debug.tools.base.hutool.core.lang.Pair;
import io.github.future0923.debug.tools.base.hutool.core.util.ArrayUtil;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>{@link Object#equals(Object)} 方法的构建器</p>
 *
 * <p>两个对象equals必须保证hashCode值相等，hashCode值相等不能保证一定equals</p>
 *
 * <p>使用方法如下：</p>
 * <pre>
 * public boolean equals(Object obj) {
 *   if (obj == null) { return false; }
 *   if (obj == this) { return true; }
 *   if (obj.getClass() != getClass()) {
 *     return false;
 *   }
 *   MyClass rhs = (MyClass) obj;
 *   return new EqualsBuilder()
 *                 .appendSuper(super.equals(obj))
 *                 .append(field1, rhs.field1)
 *                 .append(field2, rhs.field2)
 *                 .append(field3, rhs.field3)
 *                 .isEquals();
 *  }
 * </pre>
 *
 * <p> 我们也可以通过反射判断所有字段是否equals：</p>
 * <pre>
 * public boolean equals(Object obj) {
 *   return EqualsBuilder.reflectionEquals(this, obj);
 * }
 * </pre>
 * <p>
 * 来自Apache Commons Lang改造
 */
public class EqualsBuilder implements Builder<Boolean> {
	private static final long serialVersionUID = 1L;

	/**
	 * <p>
	 * A registry of objects used by reflection methods to detect cyclical object references and avoid infinite loops.
	 * </p>
	 */
	private static final ThreadLocal<Set<Pair<IDKey, IDKey>>> REGISTRY = new ThreadLocal<>();

	/**
	 * <p>
	 * Returns the registry of object pairs being traversed by the reflection
	 * methods in the current thread.
	 * </p>
	 *
	 * @return Set the registry of objects being traversed
	 * @since 3.0
	 */
	static Set<Pair<IDKey, IDKey>> getRegistry() {
		return REGISTRY.get();
	}

	/**
	 * <p>
	 * Converters value pair into a register pair.
	 * </p>
	 *
	 * @param lhs {@code this} object
	 * @param rhs the other object
	 * @return the pair
	 */
	static Pair<IDKey, IDKey> getRegisterPair(final Object lhs, final Object rhs) {
		final IDKey left = new IDKey(lhs);
		final IDKey right = new IDKey(rhs);
		return new Pair<>(left, right);
	}

	/**
	 * <p>
	 * Returns {@code true} if the registry contains the given object pair.
	 * Used by the reflection methods to avoid infinite loops.
	 * Objects might be swapped therefore a check is needed if the object pair
	 * is registered in given or swapped order.
	 * </p>
	 *
	 * @param lhs {@code this} object to lookup in registry
	 * @param rhs the other object to lookup on registry
	 * @return boolean {@code true} if the registry contains the given object.
	 * @since 3.0
	 */
	static boolean isRegistered(final Object lhs, final Object rhs) {
		final Set<Pair<IDKey, IDKey>> registry = getRegistry();
		final Pair<IDKey, IDKey> pair = getRegisterPair(lhs, rhs);
		final Pair<IDKey, IDKey> swappedPair = new Pair<>(pair.getKey(), pair.getValue());

		return registry != null
				&& (registry.contains(pair) || registry.contains(swappedPair));
	}

	/**
	 * <p>
	 * Registers the given object pair.
	 * Used by the reflection methods to avoid infinite loops.
	 * </p>
	 *
	 * @param lhs {@code this} object to register
	 * @param rhs the other object to register
	 */
	static void register(final Object lhs, final Object rhs) {
		synchronized (EqualsBuilder.class) {
			if (getRegistry() == null) {
				REGISTRY.set(new HashSet<>());
			}
		}

		final Set<Pair<IDKey, IDKey>> registry = getRegistry();
		final Pair<IDKey, IDKey> pair = getRegisterPair(lhs, rhs);
		registry.add(pair);
	}

	/**
	 * <p>
	 * Unregisters the given object pair.
	 * </p>
	 *
	 * <p>
	 * Used by the reflection methods to avoid infinite loops.
	 *
	 * @param lhs {@code this} object to unregister
	 * @param rhs the other object to unregister
	 * @since 3.0
	 */
	static void unregister(final Object lhs, final Object rhs) {
		Set<Pair<IDKey, IDKey>> registry = getRegistry();
		if (registry != null) {
			final Pair<IDKey, IDKey> pair = getRegisterPair(lhs, rhs);
			registry.remove(pair);
			synchronized (EqualsBuilder.class) {
				//read again
				registry = getRegistry();
				if (registry != null && registry.isEmpty()) {
					REGISTRY.remove();
				}
			}
		}
	}

	/**
	 * 是否equals，此值随着构建会变更，默认true
	 */
	private boolean isEquals = true;

	/**
	 * 构造，初始状态值为true
	 */
	public EqualsBuilder() {
		// do nothing for now.
	}

	//-------------------------------------------------------------------------

	/**
	 * <p>反射检查两个对象是否equals，此方法检查对象及其父对象的属性（包括私有属性）是否equals</p>
	 *
	 * @param lhs           此对象
	 * @param rhs           另一个对象
	 * @param excludeFields 排除的字段集合，如果有不参与计算equals的字段加入此集合即可
	 * @return 两个对象是否equals，是返回{@code true}
	 */
	public static boolean reflectionEquals(final Object lhs, final Object rhs, final Collection<String> excludeFields) {
		return reflectionEquals(lhs, rhs, ArrayUtil.toArray(excludeFields, String.class));
	}

	/**
	 * <p>反射检查两个对象是否equals，此方法检查对象及其父对象的属性（包括私有属性）是否equals</p>
	 *
	 * @param lhs           此对象
	 * @param rhs           另一个对象
	 * @param excludeFields 排除的字段集合，如果有不参与计算equals的字段加入此集合即可
	 * @return 两个对象是否equals，是返回{@code true}
	 */
	public static boolean reflectionEquals(final Object lhs, final Object rhs, final String... excludeFields) {
		return reflectionEquals(lhs, rhs, false, null, excludeFields);
	}

	/**
	 * <p>This method uses reflection to determine if the two {@code Object}s
	 * are equal.</p>
	 *
	 * <p>It uses {@code AccessibleObject.setAccessible} to gain access to private
	 * fields. This means that it will throw a security exception if run under
	 * a security manager, if the permissions are not set up correctly. It is also
	 * not as efficient as testing explicitly. Non-primitive fields are compared using
	 * {@code equals()}.</p>
	 *
	 * <p>If the TestTransients parameter is set to {@code true}, transient
	 * members will be tested, otherwise they are ignored, as they are likely
	 * derived fields, and not part of the value of the {@code Object}.</p>
	 *
	 * <p>Static fields will not be tested. Superclass fields will be included.</p>
	 *
	 * @param lhs            {@code this} object
	 * @param rhs            the other object
	 * @param testTransients whether to include transient fields
	 * @return {@code true} if the two Objects have tested equals.
	 */
	public static boolean reflectionEquals(final Object lhs, final Object rhs, final boolean testTransients) {
		return reflectionEquals(lhs, rhs, testTransients, null);
	}

	/**
	 * <p>This method uses reflection to determine if the two {@code Object}s
	 * are equal.</p>
	 *
	 * <p>It uses {@code AccessibleObject.setAccessible} to gain access to private
	 * fields. This means that it will throw a security exception if run under
	 * a security manager, if the permissions are not set up correctly. It is also
	 * not as efficient as testing explicitly. Non-primitive fields are compared using
	 * {@code equals()}.</p>
	 *
	 * <p>If the testTransients parameter is set to {@code true}, transient
	 * members will be tested, otherwise they are ignored, as they are likely
	 * derived fields, and not part of the value of the {@code Object}.</p>
	 *
	 * <p>Static fields will not be included. Superclass fields will be appended
	 * up to and including the specified superclass. A null superclass is treated
	 * as java.lang.Object.</p>
	 *
	 * @param lhs              {@code this} object
	 * @param rhs              the other object
	 * @param testTransients   whether to include transient fields
	 * @param reflectUpToClass the superclass to reflect up to (inclusive),
	 *                         may be {@code null}
	 * @param excludeFields    array of field names to exclude from testing
	 * @return {@code true} if the two Objects have tested equals.
	 * @since 2.0
	 */
	public static boolean reflectionEquals(final Object lhs, final Object rhs, final boolean testTransients, final Class<?> reflectUpToClass,
										   final String... excludeFields) {
		if (lhs == rhs) {
			return true;
		}
		if (lhs == null || rhs == null) {
			return false;
		}
		// Find the leaf class since there may be transients in the leaf
		// class or in classes between the leaf and root.
		// If we are not testing transients or a subclass has no ivars,
		// then a subclass can test equals to a superclass.
		final Class<?> lhsClass = lhs.getClass();
		final Class<?> rhsClass = rhs.getClass();
		Class<?> testClass;
		if (lhsClass.isInstance(rhs)) {
			testClass = lhsClass;
			if (!rhsClass.isInstance(lhs)) {
				// rhsClass is a subclass of lhsClass
				testClass = rhsClass;
			}
		} else if (rhsClass.isInstance(lhs)) {
			testClass = rhsClass;
			if (!lhsClass.isInstance(rhs)) {
				// lhsClass is a subclass of rhsClass
				testClass = lhsClass;
			}
		} else {
			// The two classes are not related.
			return false;
		}
		final EqualsBuilder equalsBuilder = new EqualsBuilder();
		try {
			if (testClass.isArray()) {
				equalsBuilder.append(lhs, rhs);
			} else {
				reflectionAppend(lhs, rhs, testClass, equalsBuilder, testTransients, excludeFields);
				while (testClass.getSuperclass() != null && testClass != reflectUpToClass) {
					testClass = testClass.getSuperclass();
					reflectionAppend(lhs, rhs, testClass, equalsBuilder, testTransients, excludeFields);
				}
			}
		} catch (final IllegalArgumentException e) {
			// In this case, we tried to test a subclass vs. a superclass and
			// the subclass has ivars or the ivars are transient and
			// we are testing transients.
			// If a subclass has ivars that we are trying to test them, we get an
			// exception and we know that the objects are not equal.
			return false;
		}
		return equalsBuilder.isEquals();
	}

	/**
	 * <p>Appends the fields and values defined by the given object of the
	 * given Class.</p>
	 *
	 * @param lhs           the left hand object
	 * @param rhs           the right hand object
	 * @param clazz         the class to append details of
	 * @param builder       the builder to append to
	 * @param useTransients whether to test transient fields
	 * @param excludeFields array of field names to exclude from testing
	 */
	private static void reflectionAppend(
			final Object lhs,
			final Object rhs,
			final Class<?> clazz,
			final EqualsBuilder builder,
			final boolean useTransients,
			final String[] excludeFields) {

		if (isRegistered(lhs, rhs)) {
			return;
		}

		try {
			register(lhs, rhs);
			final Field[] fields = clazz.getDeclaredFields();
			AccessibleObject.setAccessible(fields, true);
			for (int i = 0; i < fields.length && builder.isEquals; i++) {
				final Field f = fields[i];
				if (false == ArrayUtil.contains(excludeFields, f.getName())
						&& (f.getName().indexOf('$') == -1)
						&& (useTransients || !Modifier.isTransient(f.getModifiers()))
						&& (!Modifier.isStatic(f.getModifiers()))) {
					try {
						builder.append(f.get(lhs), f.get(rhs));
					} catch (final IllegalAccessException e) {
						//this can't happen. Would get a Security exception instead
						//throw a runtime exception in case the impossible happens.
						throw new InternalError("Unexpected IllegalAccessException");
					}
				}
			}
		} finally {
			unregister(lhs, rhs);
		}
	}

	//-------------------------------------------------------------------------

	/**
	 * <p>Adds the result of {@code super.equals()} to this builder.</p>
	 *
	 * @param superEquals the result of calling {@code super.equals()}
	 * @return EqualsBuilder - used to chain calls.
	 * @since 2.0
	 */
	public EqualsBuilder appendSuper(final boolean superEquals) {
		if (isEquals == false) {
			return this;
		}
		isEquals = superEquals;
		return this;
	}

	//-------------------------------------------------------------------------

	/**
	 * <p>Test if two {@code Object}s are equal using their
	 * {@code equals} method.</p>
	 *
	 * @param lhs the left hand object
	 * @param rhs the right hand object
	 * @return EqualsBuilder - used to chain calls.
	 */
	public EqualsBuilder append(final Object lhs, final Object rhs) {
		if (isEquals == false) {
			return this;
		}
		if (lhs == rhs) {
			return this;
		}
		if (lhs == null || rhs == null) {
			return setEquals(false);
		}
		if (ArrayUtil.isArray(lhs)) {
			// 判断数组的equals
			return setEquals(ArrayUtil.equals(lhs, rhs));
		}

		// The simple case, not an array, just test the element
		return setEquals(lhs.equals(rhs));
	}

	/**
	 * <p>
	 * Test if two {@code long} s are equal.
	 * </p>
	 *
	 * @param lhs the left hand {@code long}
	 * @param rhs the right hand {@code long}
	 * @return EqualsBuilder - used to chain calls.
	 */
	public EqualsBuilder append(final long lhs, final long rhs) {
		if (isEquals == false) {
			return this;
		}
		isEquals = (lhs == rhs);
		return this;
	}

	/**
	 * <p>Test if two {@code int}s are equal.</p>
	 *
	 * @param lhs the left hand {@code int}
	 * @param rhs the right hand {@code int}
	 * @return EqualsBuilder - used to chain calls.
	 */
	public EqualsBuilder append(final int lhs, final int rhs) {
		if (isEquals == false) {
			return this;
		}
		isEquals = (lhs == rhs);
		return this;
	}

	/**
	 * <p>Test if two {@code short}s are equal.</p>
	 *
	 * @param lhs the left hand {@code short}
	 * @param rhs the right hand {@code short}
	 * @return EqualsBuilder - used to chain calls.
	 */
	public EqualsBuilder append(final short lhs, final short rhs) {
		if (isEquals == false) {
			return this;
		}
		isEquals = (lhs == rhs);
		return this;
	}

	/**
	 * <p>Test if two {@code char}s are equal.</p>
	 *
	 * @param lhs the left hand {@code char}
	 * @param rhs the right hand {@code char}
	 * @return EqualsBuilder - used to chain calls.
	 */
	public EqualsBuilder append(final char lhs, final char rhs) {
		if (isEquals == false) {
			return this;
		}
		isEquals = (lhs == rhs);
		return this;
	}

	/**
	 * <p>Test if two {@code byte}s are equal.</p>
	 *
	 * @param lhs the left hand {@code byte}
	 * @param rhs the right hand {@code byte}
	 * @return EqualsBuilder - used to chain calls.
	 */
	public EqualsBuilder append(final byte lhs, final byte rhs) {
		if (isEquals == false) {
			return this;
		}
		isEquals = (lhs == rhs);
		return this;
	}

	/**
	 * <p>Test if two {@code double}s are equal by testing that the
	 * pattern of bits returned by {@code doubleToLong} are equal.</p>
	 *
	 * <p>This handles NaNs, Infinities, and {@code -0.0}.</p>
	 *
	 * <p>It is compatible with the hash code generated by
	 * {@code HashCodeBuilder}.</p>
	 *
	 * @param lhs the left hand {@code double}
	 * @param rhs the right hand {@code double}
	 * @return EqualsBuilder - used to chain calls.
	 */
	public EqualsBuilder append(final double lhs, final double rhs) {
		if (isEquals == false) {
			return this;
		}
		return append(Double.doubleToLongBits(lhs), Double.doubleToLongBits(rhs));
	}

	/**
	 * <p>Test if two {@code float}s are equal byt testing that the
	 * pattern of bits returned by doubleToLong are equal.</p>
	 *
	 * <p>This handles NaNs, Infinities, and {@code -0.0}.</p>
	 *
	 * <p>It is compatible with the hash code generated by
	 * {@code HashCodeBuilder}.</p>
	 *
	 * @param lhs the left hand {@code float}
	 * @param rhs the right hand {@code float}
	 * @return EqualsBuilder - used to chain calls.
	 */
	public EqualsBuilder append(final float lhs, final float rhs) {
		if (isEquals == false) {
			return this;
		}
		return append(Float.floatToIntBits(lhs), Float.floatToIntBits(rhs));
	}

	/**
	 * <p>Test if two {@code booleans}s are equal.</p>
	 *
	 * @param lhs the left hand {@code boolean}
	 * @param rhs the right hand {@code boolean}
	 * @return EqualsBuilder - used to chain calls.
	 */
	public EqualsBuilder append(final boolean lhs, final boolean rhs) {
		if (isEquals == false) {
			return this;
		}
		isEquals = (lhs == rhs);
		return this;
	}

	/**
	 * <p>Returns {@code true} if the fields that have been checked
	 * are all equal.</p>
	 *
	 * @return boolean
	 */
	public boolean isEquals() {
		return this.isEquals;
	}

	/**
	 * <p>Returns {@code true} if the fields that have been checked
	 * are all equal.</p>
	 *
	 * @return {@code true} if all of the fields that have been checked
	 * are equal, {@code false} otherwise.
	 * @since 3.0
	 */
	@Override
	public Boolean build() {
		return isEquals();
	}

	/**
	 * Sets the {@code isEquals} value.
	 *
	 * @param isEquals The value to set.
	 * @return this
	 */
	protected EqualsBuilder setEquals(boolean isEquals) {
		this.isEquals = isEquals;
		return this;
	}

	/**
	 * Reset the EqualsBuilder so you can use the same object again
	 *
	 * @since 2.5
	 */
	public void reset() {
		this.isEquals = true;
	}
}
