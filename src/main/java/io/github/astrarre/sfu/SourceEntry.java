package io.github.astrarre.sfu;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * A source Java file, either as an input or on the source path
 */
public record SourceEntry(String pathName, ByteBuffer content, Charset charset) {
}
