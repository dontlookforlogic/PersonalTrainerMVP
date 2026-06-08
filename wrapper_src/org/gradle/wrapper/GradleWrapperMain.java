
package org.gradle.wrapper;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Minimal Gradle wrapper implementation.
 * Downloads the Gradle distribution specified in gradle-wrapper.properties
 * and delegates to the extracted 'gradle' executable.
 */
public class GradleWrapperMain {
    public static void main(String[] args) throws Exception {
        Path projectDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        Path propsPath = projectDir.resolve("gradle").resolve("wrapper").resolve("gradle-wrapper.properties");
        if (!Files.exists(propsPath)) {
            throw new FileNotFoundException("Missing " + propsPath);
        }

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(propsPath)) {
            props.load(in);
        }

        String distUrl = props.getProperty("distributionUrl");
        if (distUrl == null || distUrl.isBlank()) {
            throw new IllegalStateException("distributionUrl is missing in gradle-wrapper.properties");
        }

        String fileName = distUrl.substring(distUrl.lastIndexOf('/') + 1);
        String baseName = fileName.replace(".zip", "");
        Path gradleHome = Paths.get(System.getProperty("user.home"), ".gradle");
        Path wrapperHome = gradleHome.resolve("mvp-wrapper");
        Path zipPath = wrapperHome.resolve(fileName);
        Path extractDir = wrapperHome.resolve(baseName);

        Files.createDirectories(wrapperHome);

        if (!Files.exists(zipPath)) {
            System.out.println("Downloading Gradle: " + distUrl);
            download(distUrl, zipPath);
        }

        if (!Files.exists(extractDir)) {
            System.out.println("Extracting Gradle to: " + extractDir);
            unzip(zipPath, wrapperHome);
        }

        // Gradle distributions usually contain a top directory matching baseName
        // e.g. gradle-8.2.1/bin/gradle
        Path gradleBin = extractDir.resolve("bin").resolve(isWindows() ? "gradle.bat" : "gradle");
        if (!Files.exists(gradleBin)) {
            // Fallback: search for gradle launcher
            gradleBin = findGradleLauncher(wrapperHome, baseName);
        }
        if (gradleBin == null || !Files.exists(gradleBin)) {
            throw new FileNotFoundException("Could not find Gradle launcher after extraction.");
        }

        if (!isWindows()) {
            try {
                gradleBin.toFile().setExecutable(true);
            } catch (Exception ignored) {}
        }

        ProcessBuilder pb = new ProcessBuilder();
        pb.command(buildCommand(gradleBin.toString(), args));
        pb.directory(projectDir.toFile());
        pb.inheritIO();
        Process p = pb.start();
        int code = p.waitFor();
        System.exit(code);
    }

    private static String[] buildCommand(String gradleExec, String[] args) {
        String[] cmd = new String[args.length + 1];
        cmd[0] = gradleExec;
        System.arraycopy(args, 0, cmd, 1, args.length);
        return cmd;
    }

    private static void download(String url, Path out) throws IOException {
        URL u = new URL(url);
        try (InputStream in = u.openStream()) {
            Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void unzip(Path zipFile, Path targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path newPath = zipSlipProtect(entry, targetDir);
                if (entry.isDirectory()) {
                    Files.createDirectories(newPath);
                } else {
                    Files.createDirectories(newPath.getParent());
                    Files.copy(zis, newPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }

    private static Path zipSlipProtect(ZipEntry entry, Path targetDir) throws IOException {
        Path target = targetDir.resolve(entry.getName());
        Path normalized = target.normalize();
        if (!normalized.startsWith(targetDir)) {
            throw new IOException("Bad zip entry: " + entry.getName());
        }
        return normalized;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private static Path findGradleLauncher(Path wrapperHome, String baseName) throws IOException {
        // Search up to a few levels for bin/gradle(.bat)
        final String want = isWindows() ? "gradle.bat" : "gradle";
        try (var stream = Files.walk(wrapperHome, 4)) {
            return stream
                .filter(p -> p.getFileName().toString().equals(want))
                .filter(p -> p.toString().replace('\\','/').contains(baseName + "/bin/"))
                .findFirst()
                .orElse(null);
        }
    }
}
