package io.github.astrarre.sfu.impl;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;

public class RangeCollectingVisitor extends TreeScanner<Void, Void> {
	public final List<MemberRange> members = new ArrayList<>();
	public final List<TypeRange> types = new ArrayList<>();
	final Trees trees;
	private JCTree.JCCompilationUnit root;
	private String currentName;

	public RangeCollectingVisitor(Trees trees) {
		this.trees = trees;
	}

	@Override
	public Void visitCompilationUnit(CompilationUnitTree node, Void unused) {
		this.root = (JCTree.JCCompilationUnit) node;

		return super.visitCompilationUnit(node, unused);
	}

	@Override
	public Void visitClass(ClassTree node, Void unused) {
		TreePath path = this.trees.getPath(this.root, node);
		this.currentName = TypeUtil.getClassName(this.trees.getElement(path));

		for(Tree member : node.getMembers()) {
			if(member instanceof VariableTree v) {
				JCTree jc = (JCTree) v.getType();
				MemberRange e = new MemberRange(jc.getEndPosition(this.root.endPositions),
						-1,
						this.currentName,
						v.getName().toString(),
						TypeUtil.getInternalName(v.getType()),
						false);
				this.members.add(e);
			}
		}

		return super.visitClass(node, unused);
	}


	@Override
	public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
		ExecutableElement method = (ExecutableElement) TreeInfo.symbol((JCTree) node.getMethodSelect());
		TypeElement invokedClass = (TypeElement) method.getEnclosingElement();
		String owner = TypeUtil.resolveInternalName(invokedClass), name = method.getSimpleName().toString();
		var tree = TypeUtil.getMethodName(node.getMethodSelect());
		JCTree jc = (JCTree) tree;
		MemberRange e = new MemberRange(jc.getStartPosition(),
				jc.getEndPosition(this.root.endPositions),
				owner,
				name,
				TypeUtil.getDesc(method),
				true);
		this.members.add(e);
		return super.visitMethodInvocation(node, unused);
	}

	@Override
	public Void visitMemberSelect(MemberSelectTree node, Void unused) {
		TreePath path = this.trees.getPath(this.root, node);
		Element element = this.trees.getElement(path);
		if(element instanceof VariableElement v) {
			JCTree jc = (JCTree) node;
			MemberRange e = new MemberRange(-1,
					jc.getEndPosition(this.root.endPositions),
					this.currentName,
					node.getIdentifier().toString(),
					TypeUtil.getDesc(v.asType()),
					false);
			this.members.add(e);
		}
		return super.visitMemberSelect(node, unused);
	}

	public record MemberRange(int from, int to, String owner, String name, String desc, boolean isMethod) {
		public MemberRange(int from, int to, String owner, String name, String desc, boolean isMethod) {
			this.from = from;
			this.to = to;
			this.owner = owner.replace('.', '/');
			this.name = name.replace('.', '/');
			this.desc = desc.replace('.', '/');
			this.isMethod = isMethod;
		}
	}

	public record TypeRange(int from, int to, String owner) {}
}
