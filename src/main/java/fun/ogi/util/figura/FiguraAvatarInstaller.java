package fun.ogi.util.figura;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static fun.ogi.module.Module.mc;

public class FiguraAvatarInstaller {
    private static final String RESOURCE_ROOT =
            "assets/cheap/textures/figura_avatars";
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);

    private static final AtomicInteger INSTALLED_FILES = new AtomicInteger();
    private static final AtomicInteger SKIPPED_FILES = new AtomicInteger();
    private static final AtomicInteger REPLACED_FILES = new AtomicInteger();

    private static volatile boolean finished;
    private static volatile Throwable lastError;

    private FiguraAvatarInstaller() {
    }
    public static void installAsync() {
        if (!RUNNING.compareAndSet(false, true)) {
            return;
        }

        Thread thread = new Thread(() -> {
            try {
                prepareCounters();
                installNow();
                finished = true;
                lastError = null;
            } catch (Throwable t) {
                finished = false;
                lastError = t;
            } finally {
                RUNNING.set(false);
            }
        }, "Rich-Figura-Avatar-Installer");

        thread.setDaemon(true);
        thread.start();
    }

    public static void installBlocking() throws Exception {
        if (!RUNNING.compareAndSet(false, true)) {
            waitForRunningInstall();
            rethrowLastErrorIfPresent();
            return;
        }

        try {
            prepareCounters();
            installNow();
            finished = true;
            lastError = null;
        } catch (Throwable t) {
            finished = false;
            lastError = t;
            throwAsException(t);
        } finally {
            RUNNING.set(false);
        }
    }

    public static boolean isRunning() {
        return RUNNING.get();
    }

    public static boolean isFinished() {
        return finished;
    }

    public static int getInstalledFiles() {
        return INSTALLED_FILES.get();
    }
    public static int getSkippedFiles() {
        return SKIPPED_FILES.get();
    }

    public static int getReplacedFiles() {
        return REPLACED_FILES.get();
    }

    public static Throwable getLastError() {
        return lastError;
    }

    private static void prepareCounters() {
        finished = false;
        INSTALLED_FILES.set(0);
        SKIPPED_FILES.set(0);
        REPLACED_FILES.set(0);
        lastError = null;
    }

    private static void waitForRunningInstall() throws InterruptedException {
        while (RUNNING.get()) {
            Thread.sleep(10L);
        }
    }

    private static void rethrowLastErrorIfPresent() throws Exception {
        Throwable error = lastError;
        if (error != null) {
            throwAsException(error);
        }
    }

    private static void throwAsException(Throwable throwable) throws Exception {
        if (throwable instanceof Exception exception) {
            throw exception;
        }

        if (throwable instanceof Error error) {
            throw error;
        }

        throw new Exception(throwable);
    }

    private static void installNow() throws Exception {
        if (mc == null || mc.runDirectory == null) {
            return;
        }

        Path avatarsDir = mc.runDirectory.toPath()
                .resolve("figura")
                .resolve("avatars")
                .normalize();

        Files.createDirectories(avatarsDir);

        List<Throwable> errors = new ArrayList<>();
        boolean sourceFound = false;

        try {
            Path source = net.fabricmc.loader.api.FabricLoader.getInstance()
                    .getModContainer("cheap")
                    .orElseThrow(() -> new IOException("Mod cheap not found"))
                    .findPath(RESOURCE_ROOT)
                    .orElseThrow(() -> new IOException(
                            "Resource not found: " + RESOURCE_ROOT
                    ));

            if (Files.isDirectory(source)) {
                sourceFound = true;
                copyDirectory(source, avatarsDir);
            }
        } catch (Throwable t) {
            errors.add(t);
        }

        if (totalFiles() == 0) {
            URL resource = FiguraAvatarInstaller.class
                    .getClassLoader()
                    .getResource(RESOURCE_ROOT);

            if (resource != null) {
                sourceFound = true;
                copyResourceRoot(resource, avatarsDir);
            }
        }

        if (totalFiles() == 0) {
            try {
                copyFromCodeSourceJar(avatarsDir);
            } catch (Throwable t) {
                errors.add(t);
            }
        }

        if (totalFiles() == 0) {
            if (!sourceFound && !errors.isEmpty()) {
                throw new IOException(
                        "Failed to install avatars: " +
                                errors.get(errors.size() - 1).getMessage()
                );
            }

            throw new IOException(
                    "Avatar resource root '" + RESOURCE_ROOT + "' contained no files"
            );
        }
    }

    private static int totalFiles() {
        return INSTALLED_FILES.get() + SKIPPED_FILES.get() + REPLACED_FILES.get();
    }

    private static void copyResourceRoot(URL root, Path avatarsDir) throws Exception {
        String protocol = root.getProtocol();

        if ("file".equalsIgnoreCase(protocol)) {
            Path rootPath = Path.of(root.toURI()).normalize();
            copyDirectory(rootPath, avatarsDir);
            return;
        }

        if ("jar".equalsIgnoreCase(protocol)) {
            JarURLConnection connection = (JarURLConnection) root.openConnection();
            String entryName = connection.getEntryName();

            if (entryName == null || entryName.isEmpty()) {
                entryName = RESOURCE_ROOT;
            }

            try (JarFile jar = connection.getJarFile()) {
                copyFromJar(jar, entryName, avatarsDir);
            }
        }
    }

    private static void copyFromCodeSourceJar(Path avatarsDir) throws Exception {
        URL location = FiguraAvatarInstaller.class.getProtectionDomain().getCodeSource().getLocation();
        if (location == null) {
            return;
        }

        URI uri = location.toURI();
        Path path = Path.of(uri).normalize();

        if (Files.isDirectory(path)) {
            Path root = path.resolve(RESOURCE_ROOT).normalize();
            if (Files.isDirectory(root)) {
                copyDirectory(root, avatarsDir);
            }
            return;
        }

        if (Files.isRegularFile(path)) {
            try (JarFile jar = new JarFile(path.toFile())) {
                copyFromJar(jar, RESOURCE_ROOT, avatarsDir);
            }
        }
    }

    private static void copyDirectory(Path root, Path avatarsDir) throws IOException {
        if (!Files.isDirectory(root)) {
            return;
        }

        Path normalizedRoot = root.normalize();
        Path normalizedAvatarsDir = avatarsDir.normalize();

        try (java.util.stream.Stream<Path> stream = Files.walk(normalizedRoot)) {
            stream
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            Path normalizedFile = file.normalize();
                            Path relativePath = normalizedRoot.relativize(normalizedFile).normalize();

                            if (relativePath.isAbsolute() || startsWithParentTraversal(relativePath)) {
                                return;
                            }

                            Path output = normalizedAvatarsDir.resolve(relativePath).normalize();
                            copyFile(normalizedFile, output, normalizedAvatarsDir);
                        } catch (Throwable ignored) {
                        }
                    });
        }
    }

    private static void copyFromJar(JarFile jar, String rootEntry, Path avatarsDir) throws IOException {
        Path normalizedAvatarsDir = avatarsDir.normalize();
        String prefix = rootEntry.endsWith("/") ? rootEntry : rootEntry + "/";
        Enumeration<JarEntry> entries = jar.entries();

        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();

            if (entry == null || entry.isDirectory()) {
                continue;
            }

            String name = entry.getName();
            if (!name.startsWith(prefix)) {
                continue;
            }

            String relativeName = name.substring(prefix.length());
            if (relativeName.isEmpty() || relativeName.contains("\\")) {
                continue;
            }

            Path relativePath = Path.of(relativeName).normalize();
            if (relativePath.isAbsolute() || startsWithParentTraversal(relativePath)) {
                continue;
            }

            Path output = normalizedAvatarsDir.resolve(relativePath).normalize();
            if (!output.startsWith(normalizedAvatarsDir)) {
                continue;
            }

            Files.createDirectories(output.getParent());

            try (InputStream input = jar.getInputStream(entry)) {
                copyStream(input, output, normalizedAvatarsDir, entry.getSize());
            }
        }
    }

    private static boolean startsWithParentTraversal(Path path) {
        return path.getNameCount() > 0 && "..".equals(path.getName(0).toString());
    }

    private static void copyFile(Path input, Path output, Path avatarsDir) throws IOException {
        Path normalizedOutput = output.normalize();
        Path normalizedAvatarsDir = avatarsDir.normalize();

        if (!normalizedOutput.startsWith(normalizedAvatarsDir)) {
            return;
        }

        Files.createDirectories(normalizedOutput.getParent());

        long sourceSize = Files.size(input);

        if (Files.exists(normalizedOutput) && Files.size(normalizedOutput) == sourceSize) {
            SKIPPED_FILES.incrementAndGet();
            return;
        }

        if (Files.exists(normalizedOutput)) {
            REPLACED_FILES.incrementAndGet();
        } else {
            INSTALLED_FILES.incrementAndGet();
        }

        Files.copy(input, normalizedOutput, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
    }

    private static void copyStream(InputStream input, Path output, Path avatarsDir, long sourceSize) throws IOException {
        Path normalizedOutput = output.normalize();
        Path normalizedAvatarsDir = avatarsDir.normalize();

        if (!normalizedOutput.startsWith(normalizedAvatarsDir)) {
            return;
        }

        Files.createDirectories(normalizedOutput.getParent());

        if (Files.exists(normalizedOutput) && sourceSize >= 0L && Files.size(normalizedOutput) == sourceSize) {
            SKIPPED_FILES.incrementAndGet();
            return;
        }

        if (Files.exists(normalizedOutput)) {
            REPLACED_FILES.incrementAndGet();
        } else {
            INSTALLED_FILES.incrementAndGet();
        }

        Files.copy(input, normalizedOutput, StandardCopyOption.REPLACE_EXISTING);
    }
}

