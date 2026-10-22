package io.talos.desktop;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

final class StartMenuShortcut {

    private static final String SCRIPT = "$s=(New-Object -ComObject WScript.Shell).CreateShortcut($env:TALOS_LINK);"
            + "$s.TargetPath=$env:TALOS_TARGET;"
            + "$s.WorkingDirectory=$env:TALOS_DIR;"
            + "$s.IconLocation=$env:TALOS_TARGET+',0';"
            + "$s.Description='Talos.';"
            + "$s.Save()";

    private StartMenuShortcut() {
    }

    static void ensure() {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            return;
        }
        String exe = System.getProperty("jpackage.app-path");
        String appData = System.getenv("APPDATA");
        if (exe == null || exe.isBlank() || appData == null || appData.isBlank()) {
            return;
        }
        Path target = Path.of(exe);
        Path programs = Path.of(appData, "Microsoft", "Windows", "Start Menu", "Programs");
        Path link = programs.resolve("Talos.lnk");
        if (!Files.isRegularFile(target) || Files.exists(link) || Files.exists(programs.resolve("Talos").resolve("Talos.lnk"))) {
            return;
        }
        String programData = System.getenv("ProgramData");
        if (programData != null && Files.exists(Path.of(programData, "Microsoft", "Windows", "Start Menu", "Programs", "Talos", "Talos.lnk"))) {
            return;
        }
        Thread worker = new Thread(() -> create(target, link), "talos-start-menu");
        worker.setDaemon(true);
        worker.start();
    }

    private static void create(Path target, Path link) {
        try {
            Files.createDirectories(link.getParent());
            ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive",
                    "-ExecutionPolicy", "Bypass", "-WindowStyle", "Hidden", "-Command", SCRIPT);
            pb.environment().put("TALOS_LINK", link.toString());
            pb.environment().put("TALOS_TARGET", target.toString());
            pb.environment().put("TALOS_DIR", target.getParent().toString());
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            Process process = pb.start();
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (Exception e) {
            System.getLogger("talos").log(System.Logger.Level.DEBUG, "Start menu shortcut not created", e);
        }
    }
}
