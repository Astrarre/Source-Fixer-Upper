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
	public final String from, to;
	public final int fromIndex, toIndex;

	public Remapper(StringBuilder builder,
			List<RangeCollectingVisitor.MemberRange> members,
			List<RangeCollectingVisitor.TypeRange> types,
			MappingTreeView view,
			String from,
			String to) {
		this.fromIndex = view.getNamespaceId(from);
		this.toIndex = view.getNamespaceId(to);
		this.builder = builder;
		this.members = new ArrayList<>(members);
		this.types = new ArrayList<>(types);
		this.view = view;
		this.from = from;
		this.to = to;
		this.members.sort(Comparator.comparingInt(RangeCollectingVisitor.MemberRange::from).reversed());
		this.types.sort(Comparator.comparingInt(RangeCollectingVisitor.TypeRange::from).reversed());
	}

	public void apply() {
		for(RangeCollectingVisitor.MemberRange member : this.members) {
			var mapping = this.view.getMethod(member.owner(), member.name(), member.desc(), this.fromIndex);

			if(mapping != null) {
				String dst = mapping.getDstName(this.toIndex);
				int from = member.from(), to = member.to();
				if(member.to() == -1) {
					// requires advanced checking
				}
				if(member.from() == -1) {
					from = this.builder.lastIndexOf(member.name(), to);
				}

				this.builder.replace(from, to, dst);
			}
		}
	}
}
