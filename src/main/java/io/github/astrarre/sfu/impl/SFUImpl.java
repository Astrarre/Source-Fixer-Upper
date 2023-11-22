package io.github.astrarre.sfu.impl;

import io.github.astrarre.sfu.SourceFixerUpper;
import java.io.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.StreamSupport;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.*;
import net.fabricmc.mappingio.tree.MappingTreeView;
import sfu_rpkg.com.sun.source.tree.CompilationUnitTree;
import sfu_rpkg.com.sun.source.util.Trees;
import sfu_rpkg.com.sun.tools.javac.api.ClientCodeWrapper;
import sfu_rpkg.com.sun.tools.javac.api.JavacTaskImpl;
import sfu_rpkg.com.sun.tools.javac.api.JavacTool;

public class SFUImpl implements SourceFixerUpper {

    private static final MethodHandle UNWRAP;

    static {
        try {
            UNWRAP = MethodHandles.privateLookupIn(ClientCodeWrapper.class, MethodHandles.lookup())
                    .findVirtual(ClientCodeWrapper.class, "unwrap",
                            MethodType.methodType(JavaFileObject.class, JavaFileObject.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private Charset charset = Charset.defaultCharset();
    private MappingTreeView tree;
    private int srcNamespace, dstNamespace;

    private final Map<Path, Path> inputs;
    private final List<Path> sourcepath;
    private final List<Path> classpath;

    public SFUImpl() {
        this.inputs = new LinkedHashMap<>();
        this.sourcepath = new ArrayList<>();
        this.classpath = new ArrayList<>();
    }

    @Override
    public SourceFixerUpper charset(Charset charset) {
        this.charset = charset;
        return this;
    }

    @Override
    public SourceFixerUpper mappings(MappingTreeView tree, int srcNamespace, int dstNamespace) {
        this.tree = tree;
        this.srcNamespace = srcNamespace;
        this.dstNamespace = dstNamespace;
        return this;
    }

    @Override
    public SourceFixerUpper input(Path input, Path output) {
        this.inputs.put(input, output);
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
    public void process() throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, charset);
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        JavacTool javac = JavacTool.create();

        JavacTaskImpl task = (JavacTaskImpl) javac.getTask(null,
                fileManager,
                diagnostics,
                List.of("-proc:none"),
                null,
                inputs.entrySet().stream()
                        .flatMap(path -> StreamSupport
                                .stream(fileManager.getJavaFileObjectsFromPaths(List.of(path.getKey())).spliterator(),
                                        false)
                                .map(it -> new WrappedJavaFileObject(it, path.getValue())))
                        .toList());
        ClientCodeWrapper wrapper = ClientCodeWrapper.instance(task.getContext());
        Trees trees = Trees.instance(task);

        Iterable<? extends CompilationUnitTree> units = task.parse();
        task.analyze();

        for (CompilationUnitTree codeTree : units) {
            RangeCollectingVisitor collector = new RangeCollectingVisitor(trees);
            codeTree.accept(collector, null);

            WrappedJavaFileObject sourceFile;

            try {
                sourceFile = (WrappedJavaFileObject) (JavaFileObject) UNWRAP.invokeExact(wrapper,
                        codeTree.getSourceFile());
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }

            StringBuilder builder = new StringBuilder(sourceFile.getCharContent(false));
            Remapper remapper = new Remapper(builder, collector.members, collector.types, tree, srcNamespace,
                    dstNamespace);
            remapper.apply();
            Files.createDirectories(sourceFile.output.getParent());
            Files.writeString(sourceFile.output, builder.toString(), charset);
        }
    }

    record WrappedJavaFileObject(JavaFileObject object, Path output) implements JavaFileObject {
        @Override
        public Kind getKind() {
            return object.getKind();
        }

        @Override
        public boolean isNameCompatible(String simpleName, Kind kind) {
            return object.isNameCompatible(simpleName, kind);
        }

        @Override
        public NestingKind getNestingKind() {
            return object.getNestingKind();
        }

        @Override
        public Modifier getAccessLevel() {
            return object.getAccessLevel();
        }

        @Override
        public URI toUri() {
            return object.toUri();
        }

        @Override
        public String getName() {
            return object.getName();
        }

        @Override
        public InputStream openInputStream() throws IOException {
            return object.openInputStream();
        }

        @Override
        public OutputStream openOutputStream() throws IOException {
            return object.openOutputStream();
        }

        @Override
        public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
            return object.openReader(ignoreEncodingErrors);
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return object.getCharContent(ignoreEncodingErrors);
        }

        @Override
        public Writer openWriter() throws IOException {
            return object.openWriter();
        }

        @Override
        public long getLastModified() {
            return object.getLastModified();
        }

        @Override
        public boolean delete() {
            return object.delete();
        }
    }
}
