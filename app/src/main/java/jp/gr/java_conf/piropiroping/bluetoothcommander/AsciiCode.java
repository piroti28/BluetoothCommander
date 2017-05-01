package jp.gr.java_conf.piropiroping.bluetoothcommander;

/**
 * Created by matsuzakihiroshi on 2017/03/25.
 */

public class AsciiCode {
    public static final String[] command = {
            "NUL", "SOH", "STX",
            "ETX", "EOT", "ENQ",
            "ACK", "BEL", "BS",
            "HT",  "LF",  "VT",
            "FF",  "CR",  "SO",
            "SI",  "DLE", "DC1",
            "DC2", "DC3", "DC4",
            "NAK", "SYN", "ETB",
            "CAN", "EM",  "SUB",
            "ESC", "FS",  "GS",
            "RS",  "US",  "DEL",
    };

    public static final byte[] data = {
            0x00, 0x01, 0x02,
            0x03, 0x04, 0x05,
            0x06, 0x07, 0x08,
            0x09, 0x10, 0x11,
            0x12, 0x13, 0x14,
            0x15, 0x16, 0x17,
            0x18, 0x19, 0x20,
            0x21, 0x22, 0x23,
            0x24, 0x25, 0x26,
            0x27, 0x28, 0x29,
            0x30, 0x31, 0x7f,
    };
}
