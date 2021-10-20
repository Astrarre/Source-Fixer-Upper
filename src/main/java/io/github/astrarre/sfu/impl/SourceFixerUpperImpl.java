package io.github.astrarre.sfu.impl;

import io.github.astrarre.sfu.*;
import net.fabricmc.mappingio.tree.MappingTreeView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

public class SourceFixerUpperImpl implements SourceFixerUpper {

    private final List<SourceEntry> inputs = new ArrayList<>();
    private final List<SourceEntry> sourcepath = new ArrayList<>();
    private final List<CompiledSourceEntry> classpath = new ArrayList<>();
    private Executor executor = ForkJoinPool.commonPool();
    private Mappings mappings;
    private Hierarchy hierarchy;
    private Output output;

    @Override
    public SourceFixerUpper withExecutor(Executor executor) {
        this.executor = executor;
        return this;
    }

    @Override
    public SourceFixerUpper withMappings(MappingTreeView tree, String from, String to) {
        this.mappings = new Mappings(tree, from, to);
        return this;
    }

    @Override
    public SourceFixerUpper withHierarchy(Hierarchy hierarchy) {
        this.hierarchy = hierarchy;
        return this;
    }

    @Override
    public SourceFixerUpper withOutput(Output output) {
        this.output = output;
        return this;
    }

    @Override
    public SourceFixerUpper input(SourceEntry sourceEntry) {
        inputs.add(sourceEntry);
        return this;
    }

    @Override
    public SourceFixerUpper sourcepath(SourceEntry sourceEntry) {
        sourcepath.add(sourceEntry);
        return this;
    }

    @Override
    public SourceFixerUpper classpath(CompiledSourceEntry compiledSourceEntry) {
        classpath.add(compiledSourceEntry);
        return this;
    }

    @Override
    public CompletableFuture<?> start() {
        Objects.requireNonNull(executor, "Executor cannot be null");
        Objects.requireNonNull(mappings, "Mappings cannot be null");
        Objects.requireNonNull(hierarchy, "Hierarchy cannot be null");
        Objects.requireNonNull(output, "Output cannot be null");

        return CompletableFuture.supplyAsync(() -> null, executor)
                .thenCompose($ -> {
                    try {
                        return new SFUData(executor, mappings, hierarchy, output, inputs, sourcepath, classpath).process();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
