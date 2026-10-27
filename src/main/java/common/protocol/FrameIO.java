package common.protocol;

import java.io.*;

/** Một frame = int32 big-endian (số byte) + JSON UTF-8; tối đa 1 MiB. */
public final class FrameIO {
    public static final int MAX_FRAME_BYTES = 1024 * 1024;

    private FrameIO() {
    }

    public static byte[] read(DataInputStream input) throws IOException {
        int first = input.read();
        if (first < 0) {
            return null;
        } // EOF sạch giữa các frame
        int length = (first << 24) | (input.readUnsignedByte() << 16) | (input.readUnsignedByte() << 8)
                | input.readUnsignedByte();
        if (length <= 0 || length > MAX_FRAME_BYTES) {
            throw new IOException("Invalid frame length.");
        }
        byte[] bytes = new byte[length];
        input.readFully(bytes);
        return bytes;
    }

    public static void write(DataOutputStream output, byte[] bytes) throws IOException {
        if (bytes.length == 0 || bytes.length > MAX_FRAME_BYTES) {
            throw new IOException("Invalid frame length.");
        }
        output.writeInt(bytes.length);
        output.write(bytes);
        output.flush();
    }
}
