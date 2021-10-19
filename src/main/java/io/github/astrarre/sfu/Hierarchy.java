package io.github.astrarre.sfu;

import java.util.Collection;

public interface Hierarchy {

    String getSuperclass(String type);

    Collection<String> getInterfaces(String type);
}
