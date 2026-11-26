package airburn.am2playground.preloader;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Resolves classpath URLs (including percent-encoded spaces like %20) safely to local Files. */
public final class PreloaderUrlFix {
    private PreloaderUrlFix() {}

    public static File toFile(URL url) {
        if (url == null) {
            return new File("");
        }
        if ("jar".equalsIgnoreCase(url.getProtocol())) {
            String spec = url.getFile();
            int bang = spec != null ? spec.indexOf("!/") : -1;
            if (bang > 0) {
                try {
                    return toFile(new URL(spec.substring(0, bang)));
                } catch (Exception ignored) {
                }
            }
        }
        if ("file".equalsIgnoreCase(url.getProtocol())) {
            try {
                URI uri = url.toURI();
                if (!uri.isOpaque()) {
                    return new File(uri);
                }
            } catch (Exception ignored) {
                // Fall back to RFC 3986 percent-decoding (preserving literal '+') for URLs with raw '[' / ']' / spaces.
            }
            String path = url.getPath();
            if (path != null && !path.isEmpty()) {
                return new File(decodePercent(path));
            }
        }
        String file = url.getFile();
        return new File(file == null ? "" : decodePercent(file));
    }

    /** Percent-decodes UTF-8 %XX sequences without turning literal '+' characters into spaces. */
    public static String decodePercent(String input) {
        if (input == null || input.indexOf('%') < 0) {
            return input;
        }
        int len = input.length();
        StringBuilder out = new StringBuilder(len);
        byte[] buf = null;
        int i = 0;
        while (i < len) {
            char c = input.charAt(i);
            if (c == '%' && i + 2 < len) {
                int h1 = hex(input.charAt(i + 1));
                int h2 = hex(input.charAt(i + 2));
                if (h1 >= 0 && h2 >= 0) {
                    if (buf == null) {
                        buf = new byte[(len - i) / 3 + 1];
                    }
                    int pos = 0;
                    while (i + 2 < len && input.charAt(i) == '%') {
                        int a = hex(input.charAt(i + 1));
                        int b = hex(input.charAt(i + 2));
                        if (a < 0 || b < 0) {
                            break;
                        }
                        buf[pos++] = (byte) ((a << 4) | b);
                        i += 3;
                    }
                    out.append(new String(buf, 0, pos, StandardCharsets.UTF_8));
                    continue;
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    private static int hex(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        return -1;
    }
}
