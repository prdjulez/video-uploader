package youtubeconverter;

import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;

public class AudioToVideoConverter {

public void convert(File imageFile, File audioDirectory, File outputFile, File overlayFile, String title, String scheduleDate) throws Exception {
        // 1. Audio-Dateien filtern
        File[] audioFiles = audioDirectory.listFiles(file ->
                file.isFile() &&
                file.length() > 0 &&
                (file.getName().toLowerCase().endsWith(".mp3") || file.getName().toLowerCase().endsWith(".wav"))
        );

        if (audioFiles == null || audioFiles.length == 0) {
            throw new RuntimeException("Keine Audio-Dateien gefunden.");
        }

        Arrays.sort(audioFiles); // Reihenfolge beibehalten

        // 2. concat input.txt für ffmpeg erzeugen
        File concatList = new File(audioDirectory, "input.txt");
        try (FileWriter writer = new FileWriter(concatList)) {
            for (File file : audioFiles) {
                writer.write("file '" + file.getAbsolutePath().replace("\\", "/") + "'\n");
            }
        }

        // 3. kombiniertes Audio erzeugen
        File combinedAudio = new File(audioDirectory, "combined_audio.mp3");
        ProcessBuilder pb1 = new ProcessBuilder(
                "C:/Users/User/Downloads/ffmpeg-master-latest-win64-gpl-shared/ffmpeg-master-latest-win64-gpl-shared/bin/ffmpeg.exe",
                "-f", "concat",
                "-safe", "0",
                "-i", concatList.getAbsolutePath(),
                "-c", "copy",
                combinedAudio.getAbsolutePath()
        );
        pb1.inheritIO().start().waitFor();

        // 4. Video mit Overlay rendern
        ProcessBuilder pb2 = new ProcessBuilder(
                "C:/Users/User/Downloads/ffmpeg-master-latest-win64-gpl-shared/ffmpeg-master-latest-win64-gpl-shared/bin/ffmpeg.exe",
                "-stream_loop", "-1", "-i", overlayFile.getAbsolutePath(), // Overlay
                "-loop", "1", "-i", imageFile.getAbsolutePath(),           // Hintergrund
                "-i", combinedAudio.getAbsolutePath(),                     // Audio
                "-filter_complex",
                "[0:v]format=rgba,colorchannelmixer=aa=0.3[ov];" +
                "[1:v]format=rgba[bg];" +
                "[bg][ov]overlay=format=auto[out]",
                "-map", "[out]",
                "-map", "2:a",
                "-c:v", "libx264",
                "-c:a", "aac",
                "-shortest",
                "-t", "10800",
                "-pix_fmt", "yuv420p",
                outputFile.getAbsolutePath()
        );
        pb2.inheritIO().start().waitFor();

        // 5. Video auf YouTube hochladen (nach dem Rendern)
          String pythonPath = "python";
    String scriptPath = "upload_video.py";
    String videoPath = outputFile.getAbsolutePath();

    ProcessBuilder upload = new ProcessBuilder(
        pythonPath,
        scriptPath,
        videoPath,
        title,
        "Automatisch generiertes Video",
        "private",
        scheduleDate != null ? scheduleDate : ""
    );

    upload.inheritIO();
    upload.start().waitFor();
}

}
