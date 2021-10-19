package io.github.astrarre.sfu.impl;

import com.sun.source.tree.*;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.util.Context;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;

public class JavaFileParser {

    private final JavacFileManager jcFileManager;
    private final JavaCompiler javac;
    private Elements elements;

    public JavaFileParser() {
        Context context = new Context();
        jcFileManager = new JavacFileManager(context, true, Charset.defaultCharset());
        javac = JavacTool.create();
    }

    public static void main(String[] args) {
        JavaFileParser jfp = new JavaFileParser();
        jfp.parseJavaSourceFile(Path.of("Test.java"));
    }

    public void parseJavaSourceFile(Path filePath) {
        TreeVisitor<Void, Void> jsv = new TreeScanner<>() {
            private JCTree.JCCompilationUnit root;

            @Override
            public Void visitCompilationUnit(CompilationUnitTree node, Void o) {
                this.root = (JCTree.JCCompilationUnit) node;
                return super.visitCompilationUnit(node, o);
            }

            @Override
            public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
                ExecutableElement method = (ExecutableElement) TreeInfo.symbol((JCTree) node.getMethodSelect());
                TypeElement invokedClass = (TypeElement) method.getEnclosingElement();
                System.out.println(invokedClass.getQualifiedName() + "#" + method.getSimpleName() + "#" + Utils.getDesc(method));
                return super.visitMethodInvocation(node, unused);
            }

            @Override
            public Void visitClass(ClassTree node, Void unused) {
                System.out.println(elements.getDocComment(TreeInfo.symbol((JCTree) node)));
                return super.visitClass(node, unused);
            }

            @Override
            public Void scan(Tree tree, Void unused) {
                if (tree != null && root != null) {
                    debug(tree.getKind().name(), tree);
                }

                return super.scan(tree, unused);
            }

            private void debug(String name, Tree node) {
                JCTree tree = (JCTree) node;
                System.out.println(name + ": " + tree.toString().replace("\n", "\\n") + " " + tree.getStartPosition() + " " + tree.getEndPosition(root.endPositions));
            }
        };


        Iterable<? extends JavaFileObject> javaFiles = jcFileManager.getJavaFileObjects(filePath);
        JavacTask jcTask = (JavacTask) javac.getTask(null, jcFileManager, null, List.of("-s", Path.of("test").toAbsolutePath().toString()), null, javaFiles);
        this.elements = jcTask.getElements();
        try {
            /* Iterate the java compiler parse out task. */
            Iterable<? extends CompilationUnitTree> codeResult = jcTask.parse();
            jcTask.analyze();

            for (CompilationUnitTree codeTree : codeResult) {
                /* Parse out one java file source code.*/
                codeTree.accept(jsv, null);
            }

            /* Get the parsed out method list. */
            // retMethodList = jsv.getMethodList();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
