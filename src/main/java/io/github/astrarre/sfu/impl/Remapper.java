package io.github.astrarre.sfu.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.mappingio.tree.MappingTreeView;

public class Remapper {
    public final StringBuilder builder;
    public final List<RangeCollectingVisitor.MemberRange> members;
    public final List<RangeCollectingVisitor.TypeRange> types;
    public final MappingTreeView view;
    public final int srcNamespace, dstNamespace;

    public Remapper(StringBuilder builder,
            List<RangeCollectingVisitor.MemberRange> members,
            List<RangeCollectingVisitor.TypeRange> types,
            MappingTreeView view,
            int srcNamespace,
            int dstNamespace) {
        this.srcNamespace = srcNamespace;
        this.dstNamespace = dstNamespace;
        this.builder = builder;
        this.members = new ArrayList<>(members);
        this.types = new ArrayList<>(types);
        this.view = view;
        // todo merge into one list before sorting
        // todo resolve from index for proper sorting
        this.members.sort(Comparator.comparingInt(RangeCollectingVisitor.MemberRange::from).reversed());
        this.types.sort(Comparator.comparingInt(RangeCollectingVisitor.TypeRange::from).reversed());
    }

    public void apply() {
        for (RangeCollectingVisitor.MemberRange member : this.members) {
            String name = member.name();
            var mapping = member.isMethod()
                    ? this.view.getMethod(member.owner(), name, member.desc(), this.srcNamespace)
                    : this.view.getField(member.owner(), name, member.desc(), this.srcNamespace);

            if (mapping != null) {
                String dst = mapping.getDstName(this.dstNamespace);
                int from = member.from(), to = member.to();
                if (member.to() == -1) {
                    from = this.findStart(from, name, false);
                    to = name.length() + from;
                }
                if (member.from() == -1) {
                    to = this.findEnd(to, name, false);
                    from = to - name.length();
                }

                this.builder.replace(from, to, dst);
            }
        }
    }

    // starts at ??? -> <first id>
    public int findEnd(int end, String name, boolean type) {
        for (int i = end - 1; i >= 0; i--) {
            char at = this.builder.charAt(i);
            if (Character.isWhitespace(at) || (!type && at == '.')) {
            } else if (at == '/' && this.builder.charAt(i - 1) == '*') {
                i = reqPos(this.builder.lastIndexOf("/*", i), name) - 1;
            } else {
                return i;
            }
        }
        throw new IllegalStateException("Unable to find start of token " + name);
    }

    static int reqPos(int i, String name) {
        if (i == -1) {
            throw new IllegalStateException("Unable to find range of token " + name);
        }
        return i;
    }

    // starts at '0' -> <first id>
    public int findStart(int start, String name, boolean type) {
        for (int i = start; i < this.builder.length(); i++) {
            char at = this.builder.charAt(i);
            if (Character.isWhitespace(at)) {
            } else if (at == '/' && this.builder.charAt(i + 1) == '*') {
                i = reqPos(this.builder.indexOf("*/", i), name) + 2;
            } else {
                return i;
            }
        }
        throw new IllegalStateException("Unable to find end of token " + name);
    }
}
