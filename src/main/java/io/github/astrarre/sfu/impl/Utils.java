package io.github.astrarre.sfu.impl;

import javax.lang.model.element.*;
import javax.lang.model.type.*;

public class Utils {
    public static String getDesc(ExecutableElement method) {
        StringBuilder builder = new StringBuilder("(");
        for (VariableElement param : method.getParameters()) {
            builder.append(getDesc(param.asType()));
        }
        builder.append(')');
        builder.append(getDesc(method.getReturnType()));
        return builder.toString();
    }

    public static String getDesc(TypeMirror mirror) {
        if (mirror instanceof TypeVariable variable) {
            return getDesc(variable.getUpperBound());
        } else if (mirror instanceof IntersectionType intersection) {
            return getDesc(intersection.getBounds().get(0));
        }

        return switch (mirror.getKind()) {
            case ARRAY -> '[' + getDesc(((ArrayType) mirror).getComponentType());
            case INT -> "I";
            case BYTE -> "B";
            case CHAR -> "C";
            case LONG -> "J";
            case VOID -> "V";
            case FLOAT -> "F";
            case SHORT -> "S";
            case NULL -> "Lnull;";
            case DOUBLE -> "D";
            case BOOLEAN -> "Z";
            // we do a little trolling
            case DECLARED -> 'L' + mirror.toString().replace('.', '/').replaceAll("<.*", "") + ';';
            default -> throw new IllegalArgumentException("wat " + mirror.getKind());
        };
    }


    public static AnnotationMirror getAnnotationMirror(Element typeElement, TypeMirror clazz) {
        String clazzName = clazz.toString();
        for (AnnotationMirror m : typeElement.getAnnotationMirrors()) {
            if (m.getAnnotationType().toString().equals(clazzName)) {
                return m;
            }
        }
        return null;
    }

    public static String getClassName(Element element) {
        if (element.getEnclosingElement() instanceof TypeElement) {
            String name = element.toString();
            int index = name.lastIndexOf('.');
            return name.substring(0, index) + '$' + name.substring(index + 1);
        }
        return element.toString();
    }
}
