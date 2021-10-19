package io.github.astrarre.sfu.impl;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.util.TreeScanner;

import java.util.ArrayList;
import java.util.List;

public class RangeCollectingVisitor extends TreeScanner<Void, Void> {
    public record MemberRange(int from, int to, String owner, String name, String desc) {}
    public record TypeRange(int from, int to, String owner) {}

    public final List<MemberRange> members = new ArrayList<>();
    public final List<TypeRange> types = new ArrayList<>();

    @Override
    public Void visitClass(ClassTree node, Void unused) {
        return super.visitClass(node, unused);
    }

    @Override
    public Void visitNewClass(NewClassTree node, Void unused) {
        return super.visitNewClass(node, unused);
    }

    @Override
    public Void visitMethod(MethodTree node, Void unused) {

        return super.visitMethod(node, unused);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
        return super.visitMethodInvocation(node, unused);
    }
}
