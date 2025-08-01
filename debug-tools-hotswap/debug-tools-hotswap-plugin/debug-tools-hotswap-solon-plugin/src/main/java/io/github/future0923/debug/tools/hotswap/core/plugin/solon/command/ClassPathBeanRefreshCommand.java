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
package io.github.future0923.debug.tools.hotswap.core.plugin.solon.command;

import io.github.future0923.debug.tools.base.logging.Logger;
import io.github.future0923.debug.tools.hotswap.core.annotation.FileEvent;
import io.github.future0923.debug.tools.hotswap.core.command.EventMergeableCommand;
import io.github.future0923.debug.tools.hotswap.core.plugin.solon.agent.AppContextAgent;
import io.github.future0923.debug.tools.hotswap.core.plugin.solon.transformer.SolonBeanClassFileTransformer;
import io.github.future0923.debug.tools.hotswap.core.plugin.solon.transformer.SolonBeanWatchEventListener;
import io.github.future0923.debug.tools.hotswap.core.watch.WatchFileEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 类路径下的bean刷新命令
 *
 * @author future0923
 */
public class ClassPathBeanRefreshCommand extends EventMergeableCommand<ClassPathBeanRefreshCommand> {

    private static final Logger logger = Logger.getLogger(ClassPathBeanRefreshCommand.class);

    private final ClassLoader appClassLoader;

    private final String basePackage;

    private final String className;

    private WatchFileEvent event;

    private byte[] classDefinition;

    /**
     * 修改的类通过{@link SolonBeanClassFileTransformer#transform}创建ClassPathBeanRefreshCommand命令
     */
    public ClassPathBeanRefreshCommand(ClassLoader appClassLoader, String basePackage, String className, byte[] classDefinition) {
        this.appClassLoader = appClassLoader;
        this.basePackage = basePackage;
        this.className = className;
        this.classDefinition = classDefinition;
    }

    /**
     * 新增的类通过{@link SolonBeanWatchEventListener#onEvent(WatchFileEvent)}来解析{@link FileEvent#CREATE}创建ClassPathBeanRefreshCommand命令
     */
    public ClassPathBeanRefreshCommand(ClassLoader appClassLoader, String basePackage, String className, WatchFileEvent event) {
        this.appClassLoader = appClassLoader;
        this.basePackage = basePackage;
        this.event = event;
        this.className = className;
    }

    @Override
    protected WatchFileEvent event() {
        return event;
    }

    /**
     * 反射调用{@link AppContextAgent#refreshClass(String, Class, String)}刷新Solon bean class
     */
    @Override
    public void executeCommand() {
        if (isDeleteEvent()) {
            logger.trace("Skip Solon reload for delete event on class '{}'", className);
            return;
        }
        try {
            Class<?> refreshClass = appClassLoader.loadClass(className);
            logger.debug("Executing AppContextAgent.refreshClass('{}')", className);
            Class<?> clazz = Class.forName("io.github.future0923.debug.tools.hotswap.core.plugin.solon.agent.AppContextAgent", true, appClassLoader);
            Method method = clazz.getDeclaredMethod("refreshClass", String.class, Class.class, String.class);
            String path = null;
            if (event != null) {
                path = event.getURI().getPath();
            }
            method.invoke(null, basePackage, refreshClass, path);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Plugin error, method not found", e);
        } catch (InvocationTargetException e) {
            logger.error("Error refreshing class {} in classLoader {}", e, className, appClassLoader);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Plugin error, illegal access", e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Plugin error, Solon class not found in application classloader", e);
        }

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClassPathBeanRefreshCommand that = (ClassPathBeanRefreshCommand) o;

        if (!appClassLoader.equals(that.appClassLoader)) return false;
        if (!className.equals(that.className)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = appClassLoader.hashCode();
        result = 31 * result + className.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ClassPathBeanRefreshCommand{" +
                "appClassLoader=" + appClassLoader +
                ", basePackage='" + basePackage + '\'' +
                ", className='" + className + '\'' +
                '}';
    }
}
