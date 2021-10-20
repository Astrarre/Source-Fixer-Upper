package io.github.astrarre.sfu.impl;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.comp.Check;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.util.Context;

import net.fabricmc.mappingio.format.Tiny1Reader;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

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
				List.of("-s", Path.of("test").toAbsolutePath().toString()),
				null,
				javaFiles);
		Trees trees = Trees.instance(jcTask);

		try {
			/* Iterate the java compiler parse out task. */
			Iterable<? extends CompilationUnitTree> codeResult = jcTask.parse();
			jcTask.analyze();

			for(CompilationUnitTree codeTree : codeResult) {
				/* Parse out one java file source code.*/
				var jsv = new RangeCollectingVisitor(trees);
				codeTree.accept(jsv, null);
				MemoryMappingTree tree = new MemoryMappingTree();
				Tiny1Reader.read(Files.newBufferedReader(Path.of("test.tiny")), tree);
				Remapper remapper = new Remapper(builder, jsv.members, jsv.types, tree, "from", "to");
				remapper.apply();
			}

			/* Get the parsed out method list. */
			// retMethodList = jsv.getMethodList();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}
