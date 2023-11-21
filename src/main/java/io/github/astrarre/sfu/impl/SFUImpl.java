package io.github.astrarre.sfu.impl;

import io.github.astrarre.sfu.SourceFixerUpper;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.tools.JavaFileObject;
import net.fabricmc.mappingio.tree.MappingTreeView;
import sfu_rpkg.com.sun.source.tree.CompilationUnitTree;
import sfu_rpkg.com.sun.source.util.JavacTask;
import sfu_rpkg.com.sun.source.util.Trees;
import sfu_rpkg.com.sun.tools.javac.api.JavacTool;
import sfu_rpkg.com.sun.tools.javac.file.JavacFileManager;
import sfu_rpkg.com.sun.tools.javac.util.Context;

public class SFUImpl implements SourceFixerUpper {

    private Charset charset = Charset.defaultCharset();
    private MappingTreeView tree;
    private int srcNamespace, dstNamespace;
    private Path output;

    private final List<Path> inputs, sourcepath, classpath;

    public SFUImpl() {
        this.inputs = new ArrayList<>();
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
    public void process() throws IOException {
        Context context = new Context();
        JavacFileManager jcFileManager = new JavacFileManager(context, true, charset);
        JavacTool javac = JavacTool.create();

        Path filePath = Files.list(inputs.get(0)).filter(Files::isRegularFile).findAny().get();

        Iterable<? extends JavaFileObject> javaFiles = jcFileManager.getJavaFileObjects(filePath);
        JavacTask jcTask = javac.getTask(null,
                jcFileManager,
                null,
                List.of(),
                null,
                javaFiles);
        Trees trees = Trees.instance(jcTask);

        Iterable<? extends CompilationUnitTree> codeResult = jcTask.parse();
        jcTask.analyze();

        for (CompilationUnitTree codeTree : codeResult) {
            var jsv = new RangeCollectingVisitor(trees);
            codeTree.accept(jsv, null);
            StringBuilder builder = new StringBuilder(Files.readString(filePath));
            Remapper remapper = new Remapper(builder, jsv.members, jsv.types, tree, srcNamespace, dstNamespace);
            remapper.apply();
            System.out.println(builder);
        }
    }
}
