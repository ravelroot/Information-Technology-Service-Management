package io.pyhoma.desktop;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

final class StartMenuShortcut {

    private static final String SCRIPT = "$s=(New-Object -ComObject WScript.Shell).CreateShortcut($env:PYHOMA_LINK);"
            + "$s.TargetPath=$env:PYHOMA_TARGET;"
            + "$s.WorkingDirectory=$env:PYHOMA_DIR;"
            + "$s.IconLocation=$env:PYHOMA_TARGET+',0';"
            + "$s.Description='Pyhoma.';"
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
        Path link = programs.resolve("Pyhoma.lnk");
        if (!Files.isRegularFile(target) || Files.exists(link) || Files.exists(programs.resolve("Pyhoma").resolve("Pyhoma.lnk"))) {
            return;
        }
        String programData = System.getenv("ProgramData");
        if (programData != null && Files.exists(Path.of(programData, "Microsoft", "Windows", "Start Menu", "Programs", "Pyhoma", "Pyhoma.lnk"))) {
            return;
        }
        Thread worker = new Thread(() -> create(target, link), "pyhoma-start-menu");
        worker.setDaemon(true);
        worker.start();
    }

    private static void create(Path target, Path link) {
        try {
            Files.createDirectories(link.getParent());
            ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive",
                    "-ExecutionPolicy", "Bypass", "-WindowStyle", "Hidden", "-Command", SCRIPT);
            pb.environment().put("PYHOMA_LINK", link.toString());
            pb.environment().put("PYHOMA_TARGET", target.toString());
            pb.environment().put("PYHOMA_DIR", target.getParent().toString());
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            Process process = pb.start();
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (Exception e) {
            System.getLogger("pyhoma").log(System.Logger.Level.DEBUG, "Start menu shortcut not created", e);
        }
    }
}
