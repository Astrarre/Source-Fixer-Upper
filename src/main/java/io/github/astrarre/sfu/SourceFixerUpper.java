package io.github.astrarre.sfu;

import io.github.astrarre.sfu.impl.SFUImpl;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import net.fabricmc.mappingio.tree.MappingTreeView;

public interface SourceFixerUpper {

    static SourceFixerUpper create() {
        return new SFUImpl();
    }

    SourceFixerUpper charset(Charset charset);

    SourceFixerUpper mappings(MappingTreeView tree, int srcNamespace, int dstNamespace);

    SourceFixerUpper input(Path input, Path output);

    SourceFixerUpper sourcepath(Path root);

    SourceFixerUpper classpath(Path root);

    void process() throws IOException;
}
