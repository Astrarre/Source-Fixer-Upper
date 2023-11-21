package io.github.astrarre.sfu.impl;

import java.util.Arrays;

@SuppressWarnings("unused") // see RepackagedJavacProvider
public class JavacHack {

    public static Object[][] addConstant(Object[][] objects) {
        Object[][] expanded = Arrays.copyOf(objects, objects.length + 1);
        expanded[objects.length] = new Object[] { "java.lang.constant.*", "hidden" };
        return expanded;
    }

    /**
     * called in asm code in repackaging
     */
    public static String repackage(String path) {
        String str = "sfu_rpkg." + path;
        try {
            Class.forName(str, false, JavacHack.class.getClassLoader());
            return str;
        } catch (ClassNotFoundException e) {
            return path;
        }
    }

    public static String unrepackage(String path) {
        return path.replace("sfu_rpkg.", "");
    }
}
