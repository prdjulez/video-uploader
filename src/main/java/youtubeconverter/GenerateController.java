package youtubeconverter;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@RestController
@RequestMapping("/api")
public class GenerateController {

    private final AudioToVideoConverter converter = new AudioToVideoConverter();

    @PostMapping("/generate")
public ResponseEntity<byte[]> generateVideo(
        @RequestParam("image") MultipartFile image,
        @RequestParam("audioZip") MultipartFile audioZip,
        @RequestParam("overlay") String overlayName,
        @RequestParam("title") String title,
        @RequestParam(value = "schedule", required = false) String scheduleDate) throws Exception {

    File tempDir = Files.createTempDirectory("video-gen").toFile();
    File imageFile = new File(tempDir, image.getOriginalFilename());
    File zipFile = new File(tempDir, audioZip.getOriginalFilename());

    image.transferTo(imageFile);
    audioZip.transferTo(zipFile);

    File audioDir = new File(tempDir, "audios");
    audioDir.mkdirs();
    unzip(zipFile, audioDir);

    File overlayFile = new File("overlays/" + overlayName + ".mp4");
    File outputVideo = new File(tempDir, "output.mp4");

    converter.convert(imageFile, audioDir, outputVideo, overlayFile, title, scheduleDate);

    byte[] videoBytes = Files.readAllBytes(outputVideo.toPath());

    return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"video.mp4\"")
            .body(videoBytes);
}

    private void unzip(File zipFile, File destDir) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile.toPath()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File newFile = new File(destDir, entry.getName());
                String destDirPath = destDir.getCanonicalPath();
                String newFilePath = newFile.getCanonicalPath();
                if (!newFilePath.startsWith(destDirPath + File.separator)) {
                    throw new IllegalStateException("Ungültiger ZIP-Eintrag: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    newFile.mkdirs();
                    continue;
                }

                File parent = newFile.getParentFile();
                if (!parent.exists()) parent.mkdirs();

                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                    zis.transferTo(fos);
                }
            }
        }
    }
}
