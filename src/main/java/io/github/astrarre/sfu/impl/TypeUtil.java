package io.github.astrarre.sfu.impl;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.Tree;

public class TypeUtil {
	public static String getInternalName(Tree tree) {
		if(tree instanceof TypeElement e) {
			return resolveInternalName(e);
		} else if(tree instanceof PrimitiveTypeTree t) {
			return getDesc_(t.getPrimitiveTypeKind(), null);
		} else {
			throw new UnsupportedOperationException(tree.getClass() + " " + tree);
		}
	}

	public static Tree getMethodName(ExpressionTree element) {
		if(element instanceof IdentifierTree idTree) {
			return idTree;
		} else if(element instanceof NewClassTree t) {
			return t.getIdentifier();
		} else {
			return getMethodName(((MemberSelectTree)element).getExpression());
		}
	}

	public static String resolveInternalName(TypeElement element) {
		return element.getQualifiedName().toString().replace('.', '/'); // todo resolution n shit
	}

	public static String getDesc(ExecutableElement method) {
		StringBuilder builder = new StringBuilder("(");
		for(VariableElement param : method.getParameters()) {
			builder.append(getDesc(param.asType()));
		}
		builder.append(')');
		builder.append(getDesc(method.getReturnType()));
		return builder.toString();
	}

	public static String getDesc(TypeMirror mirror) {
		if(mirror instanceof TypeVariable variable) {
			return getDesc(variable.getUpperBound());
		} else if(mirror instanceof IntersectionType intersection) {
			return getDesc(intersection.getBounds().get(0));
		}

		return getDesc_(mirror.getKind(), mirror);
	}

	private static String getDesc_(TypeKind kind, TypeMirror mirror) {
		return switch(kind) {
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
			case DECLARED -> {
				String str = mirror.toString();
				if(str.isBlank()) {
					throw new IllegalArgumentException(mirror + " unable to be resolved");
				}
				yield 'L' + str.replaceAll("<.*", "") + ';';
			}
			default -> throw new IllegalArgumentException("wat " + mirror.getKind());
		};
	}

	private static final String ANON_HEADER = "<anonymous ";
	public static String getClassName(Element element) {
		Element e = element.getEnclosingElement();
		String name = element.toString();
		if(e instanceof TypeElement) {
			int index = name.lastIndexOf('.');
			return getClassName(e) + '$' + name.substring(index + 1);
		}
		if(name.startsWith(ANON_HEADER)) {
			return name.substring(ANON_HEADER.length(), name.length() - 1);
		}
		return name;
	}
}
