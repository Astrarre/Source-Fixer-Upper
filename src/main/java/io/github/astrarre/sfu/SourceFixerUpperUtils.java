package io.github.astrarre.sfu;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class SourceFixerUpperUtils {

    /**
     * Walks the given path as a standard jar, ignoring source files
     *
     * @param sfu  Instance to configure
     * @param path Jar to add
     * @throws IOException If a file could not be read, a directory iterated, or similar error occurs
     */
    public static void walkStandardJar(SourceFixerUpper sfu, Path path) throws IOException {
        try (FileSystem fileSystem = FileSystems.newFileSystem(path)) {
            walk(sfu, fileSystem.getPath("/"), null, true, 0);
        }
    }

    /**
     * Walks the given path as a standard jar, ignoring source files
     *
     * @param sfu     Instance to configure
     * @param path    Root directory to add
     * @param charset Charset to use for sources
     * @throws IOException If a file could not be read, a directory iterated, or similar error occurs
     */
    public static void walkStandardSources(SourceFixerUpper sfu, Path path, Charset charset) throws IOException {
        Files.walkFileTree(path, new InnerVisitor(path, sfu, false, false, charset, 0));
    }

    /**
     * Walks the given path and automatically add entries
     *
     * @param sfu          Instance to configure
     * @param path         Path to add
     * @param charset      Charset to use for sources. Use {@code null} to ignore source files
     * @param classes      Add *.class files to the classpath
     * @param nestingLevel How many times to go deeper in a search by entering *.jar files. 0 to ignore. -1 for infinite
     *                     depth
     * @throws IOException If a file could not be read, a directory iterated, or similar error occurs
     */
    public static void walk(SourceFixerUpper sfu, Path path, Charset charset, boolean classes, int nestingLevel) throws IOException {
        // TODO: Handle multi-release jars
        //  Idk how to implement it in a performant way, but then again this isn't exactly performant, is it?
        Files.walkFileTree(path, new InnerVisitor(path, sfu, true, classes, charset, nestingLevel));
    }

    private static class InnerVisitor extends SimpleFileVisitor<Path> {

        private final Path root;
        private final SourceFixerUpper sfu;
        private final boolean sourcepath;
        private final boolean classes;
        private final Charset charset;
        private final int nestingLevel;

        public InnerVisitor(Path root, SourceFixerUpper sfu, boolean sourcepath, boolean classes, Charset charset, int nestingLevel) {
            this.root = root;
            this.sfu = sfu;
            this.sourcepath = sourcepath;
            this.classes = classes;
            this.charset = charset;
            this.nestingLevel = nestingLevel;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            String relative = root.relativize(file).toString();

            if (charset != null && relative.endsWith(".java")) {
                SourceEntry entry = new SourceEntry(relative, ByteBuffer.wrap(Files.readAllBytes(file)), charset);

                if (sourcepath) {
                    sfu.sourcepath(entry);
                } else {
                    sfu.input(entry);
                }
            } else if (classes && relative.endsWith(".class")) {
                sfu.classpath(new CompiledSourceEntry(relative, ByteBuffer.wrap(Files.readAllBytes(file))));
            } else if (nestingLevel != 0) {
                FileSystem inner = null;

                try {
                    inner = FileSystems.newFileSystem(file);
                } catch (IOException ignored) {
                }

                if (inner != null) {
                    try (FileSystem $ = inner) {
                        for (Path root : $.getRootDirectories()) {
                            Files.walkFileTree(root, new InnerVisitor(root, sfu, sourcepath, classes, charset, nestingLevel - 1));
                        }
                    }
                }
            }

            return super.visitFile(file, attrs);
        }
    }
}
