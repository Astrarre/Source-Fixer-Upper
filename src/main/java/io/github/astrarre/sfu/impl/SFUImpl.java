package io.github.astrarre.sfu.impl;

import io.github.astrarre.sfu.SourceFixerUpper;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.mappingio.tree.MappingTreeView;

public class SFUImpl implements SourceFixerUpper {

    private MappingTreeView tree;
    private int srcNamespace, dstNamespace;
    private Path output;

    private final List<Path> inputs, sourcepath, classpath;

    public SFUImpl() {
        inputs = new ArrayList<>();
        sourcepath = new ArrayList<>();
        classpath = new ArrayList<>();
    }

    @Override
    public SourceFixerUpper mappings(MappingTreeView tree, int srcNamespace, int dstNamespace) {
        this.tree = tree;
        this.srcNamespace = srcNamespace;
        this.dstNamespace = dstNamespace;
        return this;
    }

    @Override
    public SourceFixerUpper input(Path root) {
        this.inputs.add(root);
        return this;
    }

    @Override
    public SourceFixerUpper sourcepath(Path root) {
        this.sourcepath.add(root);
        return this;
    }

    @Override
    public SourceFixerUpper classpath(Path root) {
        this.classpath.add(root);
        return this;
    }

    @Override
    public SourceFixerUpper output(Path output) {
        this.output = output;
        return this;
    }

    @Override
    public void process() {

    }
}
