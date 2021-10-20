package io.github.astrarre.sfu.test;

import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

class AssertingWriter extends Writer {

    private final Path path;
    private Reader reader;

    AssertingWriter(Path path) {
        this.path = path;
    }

    Reader getReader() throws IOException {
        if (reader == null) {
            reader = Files.newBufferedReader(path);
        }

        return reader;
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        char[] chars = new char[len];
        Assertions.assertEquals(len, getReader().read(chars, 0, len));

        for (int i = 0; i < len; i++) {
            Assertions.assertEquals(cbuf[off + i], chars[i], "Characters mismatch");
        }
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }
}
