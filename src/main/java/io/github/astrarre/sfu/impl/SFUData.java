package io.github.astrarre.sfu.impl;

import com.sun.source.tree.sfu_rpkg.CompilationUnitTree;
import com.sun.source.util.sfu_rpkg.JavacTask;
import com.sun.tools.javac.api.sfu_rpkg.JavacTool;
import com.sun.tools.javac.file.sfu_rpkg.JavacFileManager;
import com.sun.tools.javac.util.sfu_rpkg.Context;
import io.github.astrarre.sfu.CompiledSourceEntry;
import io.github.astrarre.sfu.Output;
import io.github.astrarre.sfu.SourceEntry;

import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

record SFUData(Executor executor, Mappings mappings, Output output, List<SourceEntry> inputs,
               List<SourceEntry> sourcepath, List<CompiledSourceEntry> classpath) {

    CompletableFuture<?> process() throws IOException {
        Context context = new Context();

        // hahayes only 1 charset (cry about it)
        JavacFileManager manager = new JavacFileManager(context, true, Charset.defaultCharset());
        JavacTool javac = JavacTool.create();

        List<JavaFileObject> sources = new ArrayList<>();
        List<JavaFileObject> sourcepath = new ArrayList<>();
        List<JavaFileObject> classpath = new ArrayList<>();

        for (SourceEntry input : inputs) {
            sources.add(new VirtualFileObject(input.pathName(), input.content(), input.charset(), JavaFileObject.Kind.SOURCE));
        }

        for (SourceEntry input : this.sourcepath) {
            sourcepath.add(new VirtualFileObject(input.pathName(), input.content(), input.charset(), JavaFileObject.Kind.SOURCE));
        }

        for (CompiledSourceEntry input : this.classpath) {
            classpath.add(new VirtualFileObject(input.pathName(), input.content(), null, JavaFileObject.Kind.CLASS));
        }

        JavacTask task = javac.getTask(null, manager, null, null, null, sources);

        Iterable<? extends CompilationUnitTree> parse = task.parse();
        task.analyze();

        List<CompletableFuture<?>> futures = new ArrayList<>();

        for (CompilationUnitTree tree : parse) {
            futures.add(process(tree));
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private CompletableFuture<?> process(CompilationUnitTree tree) {
        return CompletableFuture.completedFuture(null);
    }

    private static class VirtualFileObject extends SimpleJavaFileObject {

        private static int counter = 0;
        private final String name;
        private final ByteBuffer content;
        private final Charset charset;

        protected VirtualFileObject(String name, ByteBuffer content, Charset charset, Kind kind) {
            super(URI.create("virtual://" + counter++ + kind.extension), kind);
            this.name = name;
            this.content = content;
            this.charset = charset;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isNameCompatible(String simpleName, Kind kind) {
            String baseName = simpleName + kind.extension;
            return kind.equals(getKind()) && (baseName.equals(getName()) || getName().endsWith("/" + baseName));
        }

        @Override
        public InputStream openInputStream() {
            return new InputStream() {
                int position, mark;

                @Override
                public int read() {
                    return content.get(position++);
                }

                @Override
                public int read(byte[] b, int off, int len) {
                    int min = Math.min(content.remaining(), len);
                    content.get(b, off, min);
                    position += min;
                    return min;
                }

                @Override
                public synchronized void mark(int readlimit) {
                    mark = position;
                }

                @Override
                public synchronized void reset() {
                    position = mark;
                }

                @Override
                public boolean markSupported() {
                    return true;
                }
            };
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return charset.decode(content).toString();
        }
    }
}
