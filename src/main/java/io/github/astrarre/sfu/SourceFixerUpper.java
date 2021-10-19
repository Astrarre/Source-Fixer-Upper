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
     * @return A new {@link SourceFixerUpper}
     */
    static SourceFixerUpper create() {
        return new SourceFixerUpperImpl();
    }

    /**
     * Reconfigures this instance to use the given executor
     *
     * @param executor The executor to use for multithreading
     * @return This
     */
    SourceFixerUpper withExecutor(Executor executor);

    /**
     * Reconfigures this instance to use the given mappings
     *
     * @param tree The mapping tree to use
     * @param from The source namespace
     * @param to   The destination namespace
     * @return This
     */
    SourceFixerUpper withMappings(MappingTreeView tree, String from, String to);

    /**
     * Reconfigures this instance to use the given context
     *
     * @param context The context to use for member resolving
     * @return This
     */
    SourceFixerUpper withContext(Context context);

    /**
     * Reconfigures this instance to use the given output
     *
     * @param output The sink to write processed files
     * @return This
     */
    SourceFixerUpper withOutput(Output output);

    /**
     * Adds a source input
     *
     * @param sourceEntry Source entry to add
     * @return This
     */
    SourceFixerUpper input(SourceEntry sourceEntry);

    /**
     * Adds a source path entry
     *
     * @param sourceEntry Source path entry to add
     * @return This
     */
    SourceFixerUpper sourcepath(SourceEntry sourceEntry);

    /**
     * Adds a compiled classpath entry
     *
     * @param compiledSourceEntry A raw class file to add
     * @return This
     */
    SourceFixerUpper classpath(CompiledSourceEntry compiledSourceEntry);

    /**
     * Creates a future which will start processing
     *
     * @return A future. Should be {@link CompletableFuture#join() joined}
     */
    CompletableFuture<?> start();
}
