package io.github.astrarre.sfu;

import io.github.astrarre.sfu.impl.SFUImpl;
import java.nio.file.Path;
import net.fabricmc.mappingio.tree.MappingTreeView;

public interface SourceFixerUpper {

    static SourceFixerUpper create() {
        return new SFUImpl();
    }

    SourceFixerUpper mappings(MappingTreeView tree, int srcNamespace, int dstNamespace);

    SourceFixerUpper input(Path root);

    SourceFixerUpper sourcepath(Path root);

    SourceFixerUpper classpath(Path root);

    SourceFixerUpper output(Path output);

    void process();
}
