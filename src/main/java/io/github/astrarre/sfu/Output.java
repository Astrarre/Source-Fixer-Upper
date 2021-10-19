package io.github.astrarre.sfu;

import java.io.Writer;

/**
 * An output sink for processed source files, which could be backed by a true storage device (virtual or physical) or
 * otherwise
 */
public interface Output {

    /**
     * Opens a writer for writing a source file. This method must be able to run concurrently thread, but the writer
     * instance is guaranteed to only be run on the thread accept was called on
     *
     * @param fileName The output file name, including file extensions and such
     * @return A writer for writing the file
     */
    Writer accept(String fileName);
}
