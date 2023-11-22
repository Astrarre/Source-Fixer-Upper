package io.github.astrarre.sfu.test;

import io.github.astrarre.sfu.SourceFixerUpper;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class RemappingTests {

    Path tempRoot = Files.createTempDirectory("mercury-test");
    Path root = Path.of("src/test/resources");
    Path originals = root.resolve("original");
    Path remapped = root.resolve("remapped");
    Path mappings = root.resolve("mappings");
    Path classpaths = root.resolve("classpath");
    Path sourcepaths = root.resolve("sourcepath");

    public RemappingTests() throws IOException {
    }

    @ParameterizedTest(name = ParameterizedTest.ARGUMENTS_PLACEHOLDER)
    @ValueSource(strings = {
            "fields",
            "verbatim",
            "move_external_class_simple",
    })
    public void remap(String testCase) throws IOException {
        Path original = originals.resolve(testCase);
        Path output = tempRoot.resolve(testCase);
        Path test = remapped.resolve(testCase);
        Path classpath = classpaths.resolve(testCase + ".jar");
        Path sourcepath = sourcepaths.resolve(testCase + ".jar");

        MemoryMappingTree tree = new MemoryMappingTree();

        try (Reader reader = Files.newBufferedReader(mappings.resolve(testCase + ".tiny"))) {
            MappingReader.read(reader, MappingFormat.TINY_2_FILE, tree);
        }

        SourceFixerUpper sfu = SourceFixerUpper.create()
                .mappings(tree, tree.getNamespaceId("a"), tree.getNamespaceId("b"));

        Files.walk(original).forEach(path -> {
            if (Files.isRegularFile(path)) {
                sfu.input(path, output.resolve(original.relativize(path)));
            }
        });

        if (Files.exists(classpath)) {
            sfu.classpath(classpath);
        }

        if (Files.exists(sourcepath)) {
            sfu.sourcepath(sourcepath);
        }

        sfu.process();

        verifyDirsAreEqual(output, test, false);
        verifyDirsAreEqual(test, output, true);
    }

    private static void verifyDirsAreEqual(Path one, Path other, boolean flip) throws IOException {
        Files.walkFileTree(one, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                FileVisitResult result = super.visitFile(file, attrs);
                Path fileInOther = other.resolve(one.relativize(file));

                if (flip) {
                    Assertions.assertEquals(Files.readString(file), Files.readString(fileInOther));
                } else {
                    Assertions.assertEquals(Files.readString(fileInOther), Files.readString(file));
                }

                return result;
            }
        });
    }
}
