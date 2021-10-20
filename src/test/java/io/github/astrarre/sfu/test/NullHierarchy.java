package io.github.astrarre.sfu.test;

import io.github.astrarre.sfu.Hierarchy;

import java.util.Collection;
import java.util.List;

public class NullHierarchy implements Hierarchy {

    @Override
    public String getSuperclass(String type) {
        return type;
    }

    @Override
    public Collection<String> getInterfaces(String type) {
        return List.of();
    }
}
