package io.github.astrarre.sfu.impl;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import sfu_rpkg.com.sun.source.tree.AnnotatedTypeTree;
import sfu_rpkg.com.sun.source.tree.AnnotationTree;
import sfu_rpkg.com.sun.source.tree.ArrayTypeTree;
import sfu_rpkg.com.sun.source.tree.ClassTree;
import sfu_rpkg.com.sun.source.tree.CompilationUnitTree;
import sfu_rpkg.com.sun.source.tree.ExpressionTree;
import sfu_rpkg.com.sun.source.tree.IdentifierTree;
import sfu_rpkg.com.sun.source.tree.ImportTree;
import sfu_rpkg.com.sun.source.tree.InstanceOfTree;
import sfu_rpkg.com.sun.source.tree.MemberSelectTree;
import sfu_rpkg.com.sun.source.tree.MethodInvocationTree;
import sfu_rpkg.com.sun.source.tree.MethodTree;
import sfu_rpkg.com.sun.source.tree.NewArrayTree;
import sfu_rpkg.com.sun.source.tree.NewClassTree;
import sfu_rpkg.com.sun.source.tree.Tree;
import sfu_rpkg.com.sun.source.tree.TypeCastTree;
import sfu_rpkg.com.sun.source.tree.TypeParameterTree;
import sfu_rpkg.com.sun.source.tree.VariableTree;
import sfu_rpkg.com.sun.source.tree.WildcardTree;
import sfu_rpkg.com.sun.source.util.TreePath;
import sfu_rpkg.com.sun.source.util.TreeScanner;
import sfu_rpkg.com.sun.source.util.Trees;
import sfu_rpkg.com.sun.tools.javac.tree.JCTree;
import sfu_rpkg.com.sun.tools.javac.tree.TreeInfo;

public class RangeCollectingVisitor extends TreeScanner<Void, Void> {
	public final List<MemberRange> members = new ArrayList<>();
	public final List<TypeRange> types = new ArrayList<>();
	public final List<ImportRange> imports = new ArrayList<>();
	final Trees trees;
	final TypeUtil util;
	boolean methodLock = true;
	private JCTree.JCCompilationUnit root;
	private String currentName;

	public RangeCollectingVisitor(Trees trees) {
		this.trees = trees;
		this.util = new TypeUtil(trees);
	}

	@Override
	public Void visitImport(ImportTree node, Void unused) {
		Tree tree = node.getQualifiedIdentifier();
		JCTree internalTree = (JCTree) tree;
		ImportRange range = new ImportRange(
				internalTree.getStartPosition(),
				internalTree.getEndPosition(this.root.endPositions),
				tree.toString(),
				node.isStatic());
		this.imports.add(range);
		return super.visitImport(node, unused);
	}

	@Override
	public Void visitCompilationUnit(CompilationUnitTree node, Void unused) {
		this.root = (JCTree.JCCompilationUnit) node;
		this.util.unitTree = node;
		return super.visitCompilationUnit(node, unused);
	}

	@Override
	public Void visitClass(ClassTree node, Void unused) {
		TreePath path = this.trees.getPath(this.root, node);
		String currentName = TypeUtil.getInternalName(this.trees.getElement(path));

		for(Tree member : node.getMembers()) {
			if(member instanceof VariableTree v) {
				JCTree jc = (JCTree) v.getType(); // wait no this wont work, fuck, only really works for single variable declarations
				MemberRange e = new MemberRange(jc.getEndPosition(this.root.endPositions),
						-1,
						currentName,
						v.getName().toString(), this.util.getDesc(v.getType()),
						false);
				this.members.add(e);
			}
		}

		String oldName = this.currentName;
		this.currentName = currentName;
		super.visitClass(node, unused);
		this.currentName = oldName;
		return null;
	}

    // todo this is a horrifying way of resolving classes
    @Override
    public Void visitIdentifier(IdentifierTree node, Void unused) {
        TreePath path = this.trees.getPath(this.root, node);
        TypeMirror mirror;
        try {
            mirror = this.trees.getTypeMirror(path);
        } catch(IllegalArgumentException | NullPointerException e) {
            mirror = null;
        }
        if(mirror != null) {
            JCTree tree = (JCTree) node;
            this.types.add(new TypeRange(tree.getStartPosition(), tree.getEndPosition(this.root.endPositions), DescVisitor.getDesc(mirror), false));
        }

        return super.visitIdentifier(node, unused);
    }

    @Override
	public Void visitMethod(MethodTree node, Void unused) {
		Tree endTree = node.getReturnType();
		if(endTree == null) {
			endTree = node.getModifiers();
		}

		JCTree jc = (JCTree) endTree;
		MemberRange e = new MemberRange(jc.getEndPosition(this.root.endPositions),
				-1,
				this.currentName,
				node.getName().toString(), this.util.getDesc(node),
				true);
		this.members.add(e);
		return super.visitMethod(node, unused);
	}

	@Override
	public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
		ExpressionTree select = node.getMethodSelect(); //
		TypeUtil.Renamable renamable = TypeUtil.getName(select);
		if(renamable != null) {
			ExecutableElement method = (ExecutableElement) TreeInfo.symbol((JCTree) select);
			TypeElement invokedClass = (TypeElement) method.getEnclosingElement();
			String owner = TypeUtil.getInternalName(invokedClass);
			MemberRange e = new MemberRange(renamable.getFrom(this.root),
					renamable.getTo(this.root),
					owner,
					renamable.name(),
					DescVisitor.getDesc(method.asType()),
					true);
			this.members.add(e);
		}
		this.scan(node.getTypeArguments(), null);
		this.methodLock = true;
		this.scan(node.getMethodSelect(), null); // already processed
		this.methodLock = false;
		this.scan(node.getArguments(), null);
		return null;
	}


	@Override
	public Void visitMemberSelect(MemberSelectTree node, Void unused) {
		if(this.methodLock) {
            // Class.staticMethod & Outer.this.instanceMethod can be calculated here, maybe store if method is static as heuristic?
			return null;
		}
        // todo handle Class.staticField and Outer.this.instanceField

		TreePath path = this.trees.getPath(this.root, node);
		Element element = this.trees.getElement(path);
		if(element instanceof VariableElement v) {
			JCTree jc = (JCTree) node;
			MemberRange e = new MemberRange(-1,
					jc.getEndPosition(this.root.endPositions),
					this.currentName,
					node.getIdentifier().toString(),
					DescVisitor.getDesc(v.asType()),
					false);
			this.members.add(e);
		}
		return super.visitMemberSelect(node, unused);
	}

    @Override
    public Void visitAnnotatedType(AnnotatedTypeTree node, Void unused) {
        this.addTypeRange(node.getUnderlyingType());
        return super.visitAnnotatedType(node, unused);
    }

    @Override
    public Void visitAnnotation(AnnotationTree node, Void unused) {
        this.addTypeRange(node.getAnnotationType());
        return super.visitAnnotation(node, unused);
    }

    @Override
    public Void visitInstanceOf(InstanceOfTree node, Void unused) {
        this.addTypeRange(node.getType());
        return super.visitInstanceOf(node, unused);
    }

    @Override
    public Void visitNewArray(NewArrayTree node, Void unused) {
        this.addTypeRange(node.getType());
        return super.visitNewArray(node, unused);
    }

    @Override
    public Void visitNewClass(NewClassTree node, Void unused) {
        this.addTypeRange(node.getIdentifier());
        return super.visitNewClass(node, unused);
    }

    @Override
    public Void visitTypeCast(TypeCastTree node, Void unused) {
        this.addTypeRange(node.getType());
        return super.visitTypeCast(node, unused);
    }

    @Override
    public Void visitVariable(VariableTree node, Void unused) {
        this.addTypeRange(node.getType());
        return super.visitVariable(node, unused);
    }

    @Override
    public Void visitTypeParameter(TypeParameterTree node, Void unused) {
        for(Tree bound : node.getBounds()) {
            this.addTypeRange(bound);
        }
        return super.visitTypeParameter(node, unused);
    }

    @Override
    public Void visitWildcard(WildcardTree node, Void unused) {
        this.addTypeRange(node.getBound());
        return super.visitWildcard(node, unused);
    }

    void addTypeRange(Tree tree) { // todo find "roots" (eg. array element types, each of the parameter types, so on so forth), maybe just skip them and visit?
        if(tree == null) return;
        JCTree internal = (JCTree) tree;
        // todo test for full qualification
        var e = new TypeRange(internal.getStartPosition(), internal.getEndPosition(this.root.endPositions), this.util.getDesc(tree), false);
        this.types.add(e);
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

	public record TypeRange(int from, int to, String owner, boolean isFullyQualified) {}
	public record ImportRange(int from, int to, String importString, boolean isStatic) {}
}
