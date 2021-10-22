package io.github.astrarre.sfu.impl;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import com.sun.source.tree.sfu_rpkg.AnnotatedTypeTree;
import com.sun.source.tree.sfu_rpkg.ArrayTypeTree;
import com.sun.source.tree.sfu_rpkg.CompilationUnitTree;
import com.sun.source.tree.sfu_rpkg.ExpressionTree;
import com.sun.source.tree.sfu_rpkg.IdentifierTree;
import com.sun.source.tree.sfu_rpkg.IntersectionTypeTree;
import com.sun.source.tree.sfu_rpkg.MemberSelectTree;
import com.sun.source.tree.sfu_rpkg.MethodInvocationTree;
import com.sun.source.tree.sfu_rpkg.MethodTree;
import com.sun.source.tree.sfu_rpkg.NewClassTree;
import com.sun.source.tree.sfu_rpkg.ParameterizedTypeTree;
import com.sun.source.tree.sfu_rpkg.PrimitiveTypeTree;
import com.sun.source.tree.sfu_rpkg.Tree;
import com.sun.source.tree.sfu_rpkg.TypeParameterTree;
import com.sun.source.tree.sfu_rpkg.UnionTypeTree;
import com.sun.source.tree.sfu_rpkg.VariableTree;
import com.sun.source.util.sfu_rpkg.TreePath;
import com.sun.source.util.sfu_rpkg.Trees;
import com.sun.tools.javac.tree.sfu_rpkg.JCTree;

public class TypeUtil {
	private static final String ANON_HEADER = "<anonymous ";
	final Trees trees;
	CompilationUnitTree unitTree;

	public TypeUtil(Trees trees) {
		this.trees = trees;
	}

	public String getDesc(Tree tree) {
		if(tree instanceof AnnotatedTypeTree e) {
			return this.getDesc(e.getUnderlyingType());
		} else if(tree instanceof PrimitiveTypeTree t) {
			return DescVisitor.getPrimitiveDesc(t.getPrimitiveTypeKind());
		} else if(tree instanceof IdentifierTree t) {
			TreePath path = this.trees.getPath(this.unitTree, t);
			return DescVisitor.getDesc(this.trees.getTypeMirror(path));
		} else if(tree instanceof ParameterizedTypeTree t) {
			return this.getDesc(t.getType());
		} else if(tree instanceof MethodTree t) {
			StringBuilder builder = new StringBuilder();
			builder.append('(');
			for(VariableTree parameter : t.getParameters()) {
				builder.append(this.getDesc(parameter.getType()));
			}
			builder.append(')');
			Tree returnType = t.getReturnType();
			if(returnType != null) {
				builder.append(this.getDesc(returnType));
			} else {
				builder.append('V');
			}
			return builder.toString();
		} else if(tree instanceof ArrayTypeTree t) {
			return "[" + this.getDesc(t.getType());
		} else if(tree instanceof IntersectionTypeTree t) {
			return this.getDesc(t.getBounds().get(0));
		} else if(tree instanceof TypeParameterTree t) {
			return this.getDesc(t.getBounds().get(0));
		} else if(tree instanceof UnionTypeTree t) {
			return this.getDesc(t.getTypeAlternatives().get(0));
		} else {
			throw new UnsupportedOperationException(tree.getClass() + " " + tree);
		}
	}

	public static String getInternalName(Element element) {
		Element e = element.getEnclosingElement();
		String name = element.toString();
		if(e instanceof TypeElement) {
			int index = name.lastIndexOf('.');
			return getInternalName(e) + '$' + name.substring(index + 1);
		}
		if(name.startsWith(ANON_HEADER)) {
			name = name.substring(ANON_HEADER.length(), name.length() - 1);
		}
		return name.replace('.', '/');
	}

	public static Renamable getName(ExpressionTree tree) {
		if(tree instanceof IdentifierTree t) {
			String name = t.getName().toString();
			if(name.equals("super")) { // super constructor call, can't be remapped cus it's a keyword obviously
				return null;
			}
			return new Renamable(tree, name, true);
		} else if(tree instanceof MemberSelectTree t) {
			String name = t.getIdentifier().toString();
			return new Renamable(t.getExpression(), name, false);
		} else {
			throw new UnsupportedOperationException(tree + "");
		}
	}

	record Renamable(Tree tree, String name, boolean full) {
		int getFrom(JCTree.JCCompilationUnit t) {
			JCTree internal = (JCTree) tree;
			return full ? internal.getStartPosition() : internal.getEndPosition(t.endPositions);
		}
		int getTo(JCTree.JCCompilationUnit t) {
			JCTree internal = (JCTree) tree;
			return full ? internal.getEndPosition(t.endPositions) : -1;
		}
	}
}
