package io.github.astrarre.sfu;

import io.github.astrarre.sfu.impl.SourceFixerUpperImpl;
import net.fabricmc.mappingio.tree.MappingTreeView;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

public interface SourceFixerUpper {

    /**
     * Creates a {@link SourceFixerUpper} only configured with the {@link ForkJoinPool#commonPool() common pool}
     *
     * @return a new {@link SourceFixerUpper}
     */
    static SourceFixerUpper create() {
        return new SourceFixerUpperImpl();
    }

    /**
     * Reconfigures this instance to use the given executor
     *
     * @param executor the executor to use for multithreading
     * @return this
     */
    SourceFixerUpper withExecutor(Executor executor);

    /**
     * Reconfigures this instance to use the given mappings
     *
     * @param tree The mapping tree to use
     * @param from The source namespace
     * @param to   The destination namespace
     * @return this
     */
    SourceFixerUpper withMappings(MappingTreeView tree, String from, String to);

    /**
     * Reconfigures this instance to use the given context
     *
     * @param context The context to use for member resolving
     * @return this
     */
    SourceFixerUpper withContext(Context context);

    /**
     * Reconfigures this instance to use the given output
     *
     * @param output The sink to write processed files
     * @return this
     */
    SourceFixerUpper withOutput(Output output);

    /**
     * Adds a source input
     *
     * @param sourceEntry Source entry to add
     * @return this
     */
    SourceFixerUpper input(SourceEntry sourceEntry);

    /**
     * Creates a future which will start processing
     *
     * @return A future. Should be {@link CompletableFuture#join() joined}
     */
    CompletableFuture<?> start();
}
