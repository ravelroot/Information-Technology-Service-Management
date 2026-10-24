import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Regrava os .jar de uma pasta sem compressao interna (metodo STORED).
 * O instalador comprime tudo junto e fica bem menor do que com jars ja compactados.
 *
 * Rewrites every .jar in a folder without internal compression (STORED entries).
 * The installer then compresses everything together, which is much smaller.
 *
 * Uso / usage: java scripts/StoreJars.java <pasta-com-jars>
 */
public class StoreJars {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Uso / usage: java StoreJars.java <dir>");
            System.exit(2);
        }
        long before = 0, after = 0;
        int count = 0;
        try (DirectoryStream<Path> jars = Files.newDirectoryStream(Path.of(args[0]), "*.jar")) {
            for (Path jar : jars) {
                before += Files.size(jar);
                store(jar);
                after += Files.size(jar);
                count++;
            }
        }
        System.out.printf("StoreJars: %d jars, %.1f MB -> %.1f MB%n", count, before / 1048576.0, after / 1048576.0);
    }

    private static void store(Path jar) throws IOException {
        Path tmp = jar.resolveSibling(jar.getFileName() + ".tmp");
        byte[] buffer = new byte[64 * 1024];
        try (ZipFile in = new ZipFile(jar.toFile());
             ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(tmp))) {
            out.setMethod(ZipOutputStream.STORED);
            Set<String> seen = new HashSet<>();
            Enumeration<? extends ZipEntry> entries = in.entries();
            while (entries.hasMoreElements()) {
                ZipEntry src = entries.nextElement();
                if (!seen.add(src.getName())) {
                    continue;
                }
                byte[] data;
                try (InputStream is = in.getInputStream(src)) {
                    data = is.readAllBytes();
                }
                CRC32 crc = new CRC32();
                crc.update(data);
                ZipEntry dst = new ZipEntry(src.getName());
                dst.setMethod(ZipEntry.STORED);
                dst.setSize(data.length);
                dst.setCompressedSize(data.length);
                dst.setCrc(crc.getValue());
                dst.setTime(src.getTime());
                if (src.getComment() != null) {
                    dst.setComment(src.getComment());
                }
                out.putNextEntry(dst);
                out.write(data);
                out.closeEntry();
            }
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(tmp);
            throw new IOException("Falha ao regravar / failed to rewrite " + jar + ": " + e.getMessage(), e);
        }
        Files.move(tmp, jar, StandardCopyOption.REPLACE_EXISTING);
    }
}
