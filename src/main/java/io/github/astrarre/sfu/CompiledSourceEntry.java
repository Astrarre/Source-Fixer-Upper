package io.github.astrarre.sfu;

import java.nio.ByteBuffer;

/**
 * A compiled {@code .class} file
 */
public record CompiledSourceEntry(String pathName, ByteBuffer content) {
}
