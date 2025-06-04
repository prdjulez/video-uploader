package youtubeconverter;

import java.io.*;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtil {

    public static void extract(File zipFile, File targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File newFile = new File(targetDir, entry.getName());

                if (entry.isDirectory()) {
                    if (!newFile.exists() && !newFile.mkdirs()) {
                        throw new IOException("Fehler beim Erstellen von Verzeichnis " + newFile);
                    }
                } else {
                    File parent = newFile.getParentFile();
                    if (!parent.exists() && !parent.mkdirs()) {
                        throw new IOException("Fehler beim Erstellen von Verzeichnis " + parent);
                    }
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        Files.copy(zis, newFile.toPath());
                    }
                }
                zis.closeEntry();
            }
        }
    }
}
