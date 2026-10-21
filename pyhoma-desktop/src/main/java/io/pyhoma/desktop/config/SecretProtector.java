package io.pyhoma.desktop.config;

import com.sun.jna.platform.win32.Crypt32Util;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class SecretProtector {

    private static final String DPAPI = "dpapi:";
    private static final String DPAPI2 = "dpapi2:";
    private static final byte[] ENTROPY = "io.pyhoma.desktop/db-secret/v2".getBytes(StandardCharsets.UTF_8);
    private static final int UI_FORBIDDEN = 0x1;
    private static final String AES = "aes:";

    private final Path keyFile;

    public SecretProtector(Path configDir) {
        this.keyFile = configDir.resolve(".key");
    }

    public String protect(String secret) {
        if (secret == null || secret.isEmpty()) {
            return "";
        }
        byte[] plain = secret.getBytes(StandardCharsets.UTF_8);
        if (AppPaths.isWindows()) {
            try {
                return DPAPI2 + Base64.getEncoder().encodeToString(
                        Crypt32Util.cryptProtectData(plain, ENTROPY, UI_FORBIDDEN, "Pyhoma.", null));
            } catch (Throwable e) {
                throw new IllegalStateException("Windows could not encrypt the database password", e);
            } finally {
                java.util.Arrays.fill(plain, (byte) 0);
            }
        }
        try {
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            byte[] enc = c.doFinal(plain);
            byte[] all = new byte[iv.length + enc.length];
            System.arraycopy(iv, 0, all, 0, iv.length);
            System.arraycopy(enc, 0, all, iv.length, enc.length);
            return AES + Base64.getEncoder().encodeToString(all);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot protect secret", e);
        }
    }

    public String reveal(String stored) {
        if (stored == null || stored.isEmpty()) {
            return "";
        }
        try {
            if (stored.startsWith(DPAPI2)) {
                byte[] plain = Crypt32Util.cryptUnprotectData(Base64.getDecoder().decode(stored.substring(DPAPI2.length())),
                        ENTROPY, UI_FORBIDDEN, null);
                return new String(plain, StandardCharsets.UTF_8);
            }
            if (stored.startsWith(DPAPI)) {
                byte[] plain = Crypt32Util.cryptUnprotectData(Base64.getDecoder().decode(stored.substring(DPAPI.length())));
                return new String(plain, StandardCharsets.UTF_8);
            }
            if (stored.startsWith(AES)) {
                byte[] all = Base64.getDecoder().decode(stored.substring(AES.length()));
                Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
                c.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, all, 0, 12));
                return new String(c.doFinal(all, 12, all.length - 12), StandardCharsets.UTF_8);
            }
        } catch (Throwable e) {
            throw new IllegalStateException("The saved database password cannot be decrypted on this account", e);
        }
        throw new IllegalStateException("Unknown secret format");
    }

    private SecretKeySpec key() throws Exception {
        if (!Files.exists(keyFile)) {
            byte[] k = new byte[32];
            new SecureRandom().nextBytes(k);
            Files.writeString(keyFile, Base64.getEncoder().encodeToString(k));
            AppPaths.restrict(keyFile);
        }
        return new SecretKeySpec(Base64.getDecoder().decode(Files.readString(keyFile).trim()), "AES");
    }
}
