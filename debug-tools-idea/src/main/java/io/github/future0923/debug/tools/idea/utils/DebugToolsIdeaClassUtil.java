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
package io.github.future0923.debug.tools.idea.utils;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiReferenceList;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DebugToolsIdeaClassUtil {

    private static final Pattern pattern = Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;", Pattern.MULTILINE);

    public static String getPackageName(String content) {
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    public static String getMethodQualifiedName(PsiMethod psiMethod) {
        // 获取方法所在的Psi类， 在代码分析、重构和导航时非常有用，因为它允许你获取方法所属的类，从而可以执行各种操作，比如检查类的属性、调用其他方法等。
        PsiClass containingClass = psiMethod.getContainingClass();
        if (containingClass != null) {
            StringBuilder fullQualifiedName = new StringBuilder(containingClass.getQualifiedName() + "#" + psiMethod.getName());
            PsiParameter[] parameters = psiMethod.getParameterList().getParameters();
            if (parameters.length > 0) {
                fullQualifiedName.append("(");
                for (int i = 0; i < parameters.length; i++) {
                    fullQualifiedName.append(parameters[i].getType().getCanonicalText());
                    if (i < parameters.length - 1) {
                        fullQualifiedName.append(",");
                    }
                }
                fullQualifiedName.append(")");
            }
            return fullQualifiedName.toString();
        } else {
            return psiMethod.getName();
        }
    }

    public static String getSimpleMethodName(String qualifiedMethodName) {
        String methodName = qualifiedMethodName.substring(qualifiedMethodName.lastIndexOf("#") + 1);
        if (methodName.contains("(")) {
            return methodName.substring(0, methodName.indexOf("("));
        }
        return methodName;

    }

    /**
     * 在给定的项目中查找指定名称的 Java 类
     *
     * @param project            搜索项目
     * @param qualifiedClassName 类标识符
     * @return PsiClass信息
     */
    public static PsiClass findClass(Project project, String qualifiedClassName) {
        return JavaPsiFacade.getInstance(project).findClass(qualifiedClassName, GlobalSearchScope.allScope(project));
    }

    /**
     * 在给定的项目中查找指定名称的 Method 类
     *
     * @return PsiMethod信息
     */
    public static PsiMethod findMethod(PsiClass psiClass, String methodName, String methodSignature) {
        PsiMethod[] methods = psiClass.findMethodsByName(methodName, false);
        for (PsiMethod method : methods) {
            if (Objects.equals(genMethodSignature(method), methodSignature)) {
                return method;
            }
        }
        return null;
    }

    /**
     * 生成方法签名
     */
    public static String genMethodSignature(PsiMethod method) {
        StringBuilder sb = new StringBuilder();
        // 方法名
        sb.append(method.getName());
        // 参数列表
        sb.append("(");
        PsiParameterList parameterList = method.getParameterList();
        PsiParameter[] parameters = parameterList.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            PsiType type = parameters[i].getType();
            sb.append(type.getPresentableText()); // 不含包名，比如 String、List<Integer>
            if (i < parameters.length - 1) sb.append(", ");
        }
        sb.append(")");
        // 返回值
        PsiType returnType = method.getReturnType();
        if (returnType != null) {
            sb.append(": ").append(returnType.getPresentableText());
        }
        // 异常列表
        PsiReferenceList throwsList = method.getThrowsList();
        PsiClassType[] exceptionTypes = throwsList.getReferencedTypes();
        if (exceptionTypes.length > 0) {
            sb.append(" throws ");
            for (int i = 0; i < exceptionTypes.length; i++) {
                sb.append(exceptionTypes[i].getPresentableText());
                if (i < exceptionTypes.length - 1) sb.append(", ");
            }
        }
        return sb.toString();
    }


    /**
     * 获取类名，处理多级内部类。io.github.Test.User.Name -> io.github.Test$User$Name
     */
    public static String tryInnerClassName(PsiClass psiClass) {
        PsiClass originalClass = psiClass;
        StringBuilder classNameBuilder = new StringBuilder();
        // 构建类的层级结构（包括嵌套类）
        while (psiClass != null) {
            if (!classNameBuilder.isEmpty()) {
                classNameBuilder.insert(0, "$");  // 如果已经有内容，表示嵌套类，插入$
            }
            classNameBuilder.insert(0, psiClass.getName());  // 插入当前类的名称
            // 查找父类，如果当前类的父类是PsiClass，则跳出循环
            PsiElement parent = psiClass.getParent();
            if (parent instanceof PsiClass) {
                psiClass = (PsiClass) parent;
            } else {
                break;
            }
        }
        if (originalClass != null) {
            // 获取包名并插入类名之前
            PsiFile psiFile = originalClass.getContainingFile();
            if (psiFile instanceof PsiJavaFile) {
                String packageName = ((PsiJavaFile) psiFile).getPackageName();
                if (!packageName.isEmpty()) {
                    classNameBuilder.insert(0, ".");  // 插入包名的分隔符
                    classNameBuilder.insert(0, packageName);  // 插入包名
                }
            }
        }
        return classNameBuilder.toString();
    }

    /**
     * 获取当前光标所在方法
     */
    public static PsiMethod getCaretPsiMethod(@NotNull AnActionEvent e) {
        // 当前编辑器
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        // 当前文件
        PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
        // 获取光标所在方法
        return PsiTreeUtil.getParentOfType(getElement(editor, file), PsiMethod.class);
    }

    public static PsiElement getElement(Editor editor, PsiFile file) {
        if (editor == null || file == null) {
            return null;
        }
        // 获取光标模型 CaretModel 对象。
        CaretModel caretModel = editor.getCaretModel();
        // 获取光标当前的偏移量（即光标在文件中的位置）
        int position = caretModel.getOffset();
        // 根据光标的位置在文件中查找对应的 PsiElement 对象
        return file.findElementAt(position);
    }

    /**
     * 生成 JVM 规范的方法描述符
     * 示例：(Ljava/lang/String;I)V
     */
    public static @NotNull String getMethodDescriptor(@NotNull PsiMethod method) {
        StringBuilder descriptor = new StringBuilder("(");

        for (PsiParameter parameter : method.getParameterList().getParameters()) {
            descriptor.append(toDescriptor(parameter.getType()));
        }

        descriptor.append(")");

        PsiType returnType = method.getReturnType();
        descriptor.append(returnType != null ? toDescriptor(returnType) : "V");

        return descriptor.toString();
    }

    /**
     * 将 PsiType 转换为 JVM 类型描述符
     */
    private static @NotNull String toDescriptor(@NotNull PsiType type) {
        type = eraseGeneric(type);

        if (type instanceof PsiPrimitiveType) {
            return switch (type.getCanonicalText()) {
                case "boolean" -> "Z";
                case "byte" -> "B";
                case "char" -> "C";
                case "short" -> "S";
                case "int" -> "I";
                case "long" -> "J";
                case "float" -> "F";
                case "double" -> "D";
                case "void" -> "V";
                default -> throw new IllegalArgumentException("Unknown primitive type: " + type.getCanonicalText());
            };
        } else if (type instanceof PsiArrayType) {
            return "[" + toDescriptor(((PsiArrayType) type).getComponentType());
        } else if (type instanceof PsiClassType classType) {
            String className = classType.rawType().getCanonicalText();
            return "L" + className.replace('.', '/') + ";";
        } else {
            // fallback for unknown types
            return "L" + type.getCanonicalText().replace('.', '/') + ";";
        }
    }

    /**
     * 擦除泛型类型，避免返回像 List<T> 这样的类型
     */
    private static @NotNull PsiType eraseGeneric(@NotNull PsiType type) {
        if (type instanceof PsiClassType classType) {
            PsiClass resolvedClass = classType.resolve();
            if (resolvedClass != null && resolvedClass.getTypeParameters().length > 0) {
                PsiElementFactory factory = JavaPsiFacade.getElementFactory(resolvedClass.getProject());
                return factory.createType(resolvedClass);
            }
        }
        return type;
    }
}
