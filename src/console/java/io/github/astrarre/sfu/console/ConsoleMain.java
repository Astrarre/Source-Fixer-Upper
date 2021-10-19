package io.github.astrarre.sfu.console;

import io.github.astrarre.sfu.SourceFixerUpper;

public class ConsoleMain {

    public static void main(String[] args) {
        SourceFixerUpper.create()
                .withMappings(null, "a", "a")
                .withContext(null)
                .withOutput(null)
                .input(null)
                .start()
                .join();
    }
}
