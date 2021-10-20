package io.github.astrarre.sfu.test;

import io.github.astrarre.sfu.SourceFixerUpper;
import io.github.astrarre.sfu.SourceFixerUpperUtils;
import net.fabricmc.mappingio.format.Tiny2Reader;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class RemappingTests {

    @Test
    public void remap() throws IOException {
        Path root = Path.of("src/test/resources");
        Path originals = root.resolve("original");
        Path remapped = root.resolve("remapped");
        Path mappings = root.resolve("mappings");

        for (Path original : Files.list(originals).toList()) {
            Path name = originals.relativize(original);
            Path test = remapped.resolve(name);

            MemoryMappingTree tree = new MemoryMappingTree();

            try (Reader reader = Files.newBufferedReader(mappings.resolve(name + ".tiny"))) {
                Tiny2Reader.read(reader, tree);
            }

            List<AssertingWriter> writers = new ArrayList<>();

            SourceFixerUpper sfu = SourceFixerUpper.create()
                    .withMappings(tree, "a", "b")
                    .withHierarchy(new NullHierarchy())
                    .withOutput(fileName -> {
                        AssertingWriter writer = new AssertingWriter(test.resolve(fileName));
                        writers.add(writer);
                        return writer;
                    });

            SourceFixerUpperUtils.walkStandardSources(sfu, original, StandardCharsets.UTF_8);

            sfu.start().join();

            for (AssertingWriter writer : writers) {
                Assertions.assertEquals(-1, writer.getReader().read(), "Reader not empty");
                writer.getReader().close();
            }
        }
    }
}
