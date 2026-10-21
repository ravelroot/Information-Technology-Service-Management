package io.pyhoma.domain.support;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

public final class DocumentPolicy {

    public enum Kind { WEB, MAIL, FILE }

    public static final int MAX_LENGTH = 1000;

    private static final Set<String> BLOCKED = Set.of(
            "exe", "com", "bat", "cmd", "ps1", "psm1", "psd1", "ps1xml", "vbs", "vbe", "js", "jse", "wsf", "wsh", "wsc",
            "hta", "msi", "msp", "mst", "msc", "scr", "pif", "cpl", "lnk", "url", "reg", "inf", "jar", "jnlp", "dll",
            "sys", "drv", "ocx", "application", "appref-ms", "gadget", "msh", "msh1", "msh2", "mshxml", "scf", "sct",
            "shb", "shs", "xbap", "iso", "img", "vhd", "vhdx", "library-ms", "searchconnector-ms", "settingcontent-ms",
            "diagcab", "appx", "appxbundle", "msix", "msixbundle", "chm", "website", "ws", "py", "pyw", "rb", "pl", "sh",
            "app", "command", "workflow", "action", "bin", "run", "deb", "rpm", "apk");

    private DocumentPolicy() {
    }

    public static Kind classify(String uri) {
        if (uri == null || uri.isBlank() || uri.length() > MAX_LENGTH) {
            throw new BusinessException("error.unsafeDocument");
        }
        String u = uri.strip();
        if (u.chars().anyMatch(c -> c < 0x20 || c == 0x7F)) {
            throw new BusinessException("error.unsafeDocument");
        }
        String lower = u.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            try {
                URI parsed = new URI(u);
                if (parsed.getHost() == null || parsed.getUserInfo() != null) {
                    throw new BusinessException("error.unsafeDocument");
                }
            } catch (java.net.URISyntaxException e) {
                throw new BusinessException("error.unsafeDocument");
            }
            return Kind.WEB;
        }
        if (lower.startsWith("mailto:")) {
            return Kind.MAIL;
        }
        if (lower.matches("^[a-z][a-z0-9+.-]*:.*") && !lower.matches("^[a-z]:[\\\\/].*")) {
            throw new BusinessException("error.unsafeDocument");
        }
        if (isBlockedFile(u)) {
            throw new BusinessException("error.unsafeDocument");
        }
        return Kind.FILE;
    }

    public static boolean isBlockedFile(String path) {
        String name = path.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).strip().toLowerCase(Locale.ROOT);
        while (name.endsWith(".") || name.endsWith(" ")) {
            name = name.substring(0, name.length() - 1);
        }
        if (name.isEmpty()) {
            return false;
        }
        if (name.contains(":")) {
            return true;
        }
        int dot = name.lastIndexOf('.');
        return dot >= 0 && BLOCKED.contains(name.substring(dot + 1));
    }
}
