package io.github.astrarre.sfu.test;

import io.github.astrarre.sfu.SourceFixerUpper;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import net.fabricmc.mappingio.format.Tiny2Reader;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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

            SourceFixerUpper sfu = SourceFixerUpper.create()
                    .mappings(tree, tree.getNamespaceId("a"), tree.getNamespaceId("b"));

            Files.walk(original).forEach(path -> {
                if (Files.isRegularFile(path)) {
                    sfu.input(path, output.resolve(original.relativize(path)));
                }
            });

            sfu.process();

            verifyDirsAreEqual(output, test, false);
            verifyDirsAreEqual(test, output, true);
        }
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
