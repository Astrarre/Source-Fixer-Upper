package io.github.astrarre.sfu.impl;

import io.github.astrarre.sfu.*;
import net.fabricmc.mappingio.tree.MappingTreeView;

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
    private Context context;
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
    public SourceFixerUpper withContext(Context context) {
        this.context = context;
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
        Objects.requireNonNull(context, "Context cannot be null");
        Objects.requireNonNull(output, "Output cannot be null");

        return null;
    }
}
