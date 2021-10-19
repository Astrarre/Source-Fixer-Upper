package io.github.astrarre.sfu;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record SourceEntry(String pathName, ByteBuffer content, Charset charset) {
}
