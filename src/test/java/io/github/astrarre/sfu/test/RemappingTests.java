package io.github.astrarre.sfu.test;

import io.github.astrarre.sfu.SourceFixerUpper;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import net.fabricmc.mappingio.format.Tiny2Reader;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

public class RemappingTests {

    @Test
    public void remap() throws IOException {
        Path root = Path.of("src/test/resources");
        Path originals = root.resolve("original");
        Path remapped = root.resolve("remapped");
        Path mappings = root.resolve("mappings");
        Path tempRoot = Files.createTempDirectory("mercury-test");

        for (Path original : Files.list(originals).toList()) {
            Path name = originals.relativize(original);
            Path output = tempRoot.resolve(name);
            Path test = remapped.resolve(name);

            MemoryMappingTree tree = new MemoryMappingTree();

            try (Reader reader = Files.newBufferedReader(mappings.resolve(name + ".tiny"))) {
                Tiny2Reader.read(reader, tree);
            }

            SourceFixerUpper.create()
                    .mappings(tree, tree.getNamespaceId("a"), tree.getNamespaceId("b"))
                    .input(original)
                    .output(output)
                    .process();

            verifyDirsAreEqual(output, test);
            verifyDirsAreEqual(test, output);
        }
    }

    private static void verifyDirsAreEqual(Path one, Path other) throws IOException {
        Files.walkFileTree(one, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                FileVisitResult result = super.visitFile(file, attrs);

                Path relativize = one.relativize(file);
                Path fileInOther = other.resolve(relativize);

                byte[] otherBytes = Files.readAllBytes(fileInOther);
                byte[] theseBytes = Files.readAllBytes(file);

                if (!Arrays.equals(otherBytes, theseBytes)) {
                    throw new AssertionFailedError(file + " is not equal to " + fileInOther);
                }

                return result;
            }
        });
    }
}
