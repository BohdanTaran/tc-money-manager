package org.tc.mtracker.support.factory;

import org.springframework.core.io.ByteArrayResource;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class MultipartTestResourceFactory {

    private static final byte[] JPEG_BYTES = new byte[]{
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xDB
    };
    private static final byte[] PNG_BYTES = new byte[]{
            (byte) 0x89, 'P', 'N', 'G', (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A
    };
    private static final byte[] GIF_BYTES = new byte[]{
            'G', 'I', 'F', '8', '9', 'a'
    };
    private static final byte[] WEBP_BYTES = new byte[]{
            'R', 'I', 'F', 'F', 0x00, 0x00, 0x00, 0x00, 'W', 'E', 'B', 'P'
    };
    private static final byte[] PDF_BYTES = new byte[]{
            '%', 'P', 'D', 'F', '-', '1', '.', '7'
    };

    private MultipartTestResourceFactory() {
    }

    public static ByteArrayResource resource(String filename, String content) {
        return resource(filename, content.getBytes(StandardCharsets.UTF_8));
    }

    public static ByteArrayResource resource(String filename, byte[] content) {
        return new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }

    public static ByteArrayResource jpegImage(String filename) {
        return resource(filename, jpegBytes());
    }

    public static ByteArrayResource pngImage(String filename) {
        return resource(filename, pngBytes());
    }

    public static ByteArrayResource gifImage(String filename) {
        return resource(filename, gifBytes());
    }

    public static ByteArrayResource webpImage(String filename) {
        return resource(filename, webpBytes());
    }

    public static ByteArrayResource pdfDocument(String filename) {
        return resource(filename, pdfBytes());
    }

    public static byte[] jpegBytes() {
        return Arrays.copyOf(JPEG_BYTES, JPEG_BYTES.length);
    }

    public static byte[] pngBytes() {
        return Arrays.copyOf(PNG_BYTES, PNG_BYTES.length);
    }

    public static byte[] gifBytes() {
        return Arrays.copyOf(GIF_BYTES, GIF_BYTES.length);
    }

    public static byte[] webpBytes() {
        return Arrays.copyOf(WEBP_BYTES, WEBP_BYTES.length);
    }

    public static byte[] pdfBytes() {
        return Arrays.copyOf(PDF_BYTES, PDF_BYTES.length);
    }
}
