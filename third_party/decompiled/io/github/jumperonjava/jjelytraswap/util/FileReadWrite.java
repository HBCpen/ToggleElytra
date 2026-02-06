/*
 * Decompiled with CFR 0.152.
 */
package io.github.jumperonjava.jjelytraswap.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

public class FileReadWrite {
    public static void write(File file, String text) {
        try {
            file.getParentFile().mkdirs();
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] strToBytes = text.getBytes();
            outputStream.write(strToBytes);
            outputStream.close();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String read(File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()));
        }
        catch (IOException e) {
            FileReadWrite.write(file, "");
            return FileReadWrite.read(file);
        }
    }
}

