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
package io.github.future0923.debug.tools.base.hutool.core.compiler;


import io.github.future0923.debug.tools.base.hutool.core.util.CharUtil;
import io.github.future0923.debug.tools.base.hutool.core.util.URLUtil;

import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Java 字节码文件对象，用于在内存中暂存class字节码，从而可以在ClassLoader中动态加载。
 *
 * @author lzpeng
 * @since 5.5.2
 */
class JavaClassFileObject extends SimpleJavaFileObject {

	/**
	 * 字节码输出流
	 */
	private final ByteArrayOutputStream byteArrayOutputStream;

	/**
	 * 构造
	 *
	 * @param className 编译后的class文件的类名
	 * @see JavaClassFileManager#getJavaFileForOutput(javax.tools.JavaFileManager.Location, String, Kind, javax.tools.FileObject)
	 */
	protected JavaClassFileObject(String className) {
		super(URLUtil.getStringURI(className.replace(CharUtil.DOT, CharUtil.SLASH) + Kind.CLASS.extension), Kind.CLASS);
		this.byteArrayOutputStream = new ByteArrayOutputStream();
	}

	/**
	 * 获得字节码输入流
	 * 编译器编辑源码后，我们将通过此输出流获得编译后的字节码，以便运行时加载类
	 *
	 * @return 字节码输入流
	 * @see JavaClassFileManager#getClassLoader(javax.tools.JavaFileManager.Location)
	 */
	@Override
	public InputStream openInputStream() {
		return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
	}

	/**
	 * 获得字节码输出流
	 * 编译器编辑源码时，会将编译结果输出到本输出流中
	 *
	 * @return 字节码输出流
	 */
	@Override
	public OutputStream openOutputStream() {
		return this.byteArrayOutputStream;
	}

}