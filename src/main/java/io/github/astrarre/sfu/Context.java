package io.github.astrarre.sfu;

import java.util.Collection;

public interface Context {

    String getSuperclass(String type);

    Collection<String> getInterfaces(String type);
}
