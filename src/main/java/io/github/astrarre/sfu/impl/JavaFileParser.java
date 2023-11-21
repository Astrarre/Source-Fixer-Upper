package io.github.astrarre.sfu.impl;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import net.fabricmc.mappingio.format.Tiny1Reader;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import sfu_rpkg.com.sun.source.tree.CompilationUnitTree;
import sfu_rpkg.com.sun.source.util.JavacTask;
import sfu_rpkg.com.sun.source.util.Trees;
import sfu_rpkg.com.sun.tools.javac.api.JavacTool;
import sfu_rpkg.com.sun.tools.javac.file.JavacFileManager;
import sfu_rpkg.com.sun.tools.javac.util.Context;

public class JavaFileParser {

    private final JavacFileManager jcFileManager;
    private final JavaCompiler javac;

    public JavaFileParser() {
        Context context = new Context();
        jcFileManager = new JavacFileManager(context, true, Charset.defaultCharset());
        javac = JavacTool.create();
    }

    public static void main(String[] args) throws IOException {
        JavaFileParser jfp = new JavaFileParser();
        jfp.parseJavaSourceFile(Path.of("Test.java"));
    }

    public void parseJavaSourceFile(Path filePath) throws IOException {
        StringBuilder builder = new StringBuilder(Files.readString(filePath));

        Iterable<? extends JavaFileObject> javaFiles = jcFileManager.getJavaFileObjects(filePath);
        JavacTask jcTask = (JavacTask) javac.getTask(null,
                jcFileManager,
                null,
                List.of(),
                null,
                javaFiles);
        Trees trees = Trees.instance(jcTask);

        try {
            /* Iterate the java compiler parse out task. */
            Iterable<? extends CompilationUnitTree> codeResult = jcTask.parse();
            jcTask.analyze();

            for (CompilationUnitTree codeTree : codeResult) {
                /* Parse out one java file source code. */
                var jsv = new RangeCollectingVisitor(trees);
                codeTree.accept(jsv, null);
                MemoryMappingTree tree = new MemoryMappingTree();
                Tiny1Reader.read(Files.newBufferedReader(Path.of("test.tiny")), tree);
                Remapper remapper = new Remapper(builder, jsv.members, jsv.types, tree, "from", "to");
                remapper.apply();
            }

            /* Get the parsed out method list. */
            // retMethodList = jsv.getMethodList();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
