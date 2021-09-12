package com.dreamcodex.ti.util;

public class Lists {

    public static String commaSeparatedList(int[] values) {
        StringBuilder sb = new StringBuilder();
        for (int value : values) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(value);
        }
        return sb.toString();
    }
}
