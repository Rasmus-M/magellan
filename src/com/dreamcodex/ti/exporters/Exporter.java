package com.dreamcodex.ti.exporters;

import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;

public abstract class Exporter {

    public static final byte[] BIN_HEADER_MAG = {(byte) 'M', (byte) 'G'};
    public static final byte[] BIN_HEADER_VER = {(byte) '0', (byte) '2'};

    public static final byte BIN_CHUNK_COLORS = 1 << 1;
    public static final byte BIN_CHUNK_CHARS = 1 << 2;
    public static final byte BIN_CHUNK_SPRITES = 1 << 3;

    public static final byte BIN_MAP_HEADER_RESERVED1 = 0;
    public static final byte BIN_MAP_HEADER_RESERVED2 = 0;
    public static final byte BIN_MAP_HEADER_RESERVED3 = 0;
    public static final byte BIN_MAP_HEADER_RESERVED4 = 0;

    private final int ASM_LINELEN = 40;

    protected MapEditor mapEditor;
    protected int[][] clrSets;
    protected ECMPalette[] ecmPalettes;
    protected HashMap<Integer, int[][]> charGrids;
    protected HashMap<Integer, int[][]> charColors;
    protected ECMPalette[] ecmCharPalettes;
    protected boolean[] ecmCharTransparency;
    protected HashMap<Integer, int[][]> spriteGrids;
    protected int[] spriteColors;
    protected ECMPalette[] ecmSpritePalettes;
    protected ColorMode colorMode;

    public Exporter() {
    }

    public Exporter(MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        this(
                mapEditor,
                dataSet.getEcmPalettes(),
                dataSet.getClrSets(),
                dataSet.getCharGrids(),
                dataSet.getCharColors(),
                dataSet.getEcmCharPalettes(),
                dataSet.getEcmCharTransparency(),
                dataSet.getSpriteGrids(),
                dataSet.getSpriteColors(),
                dataSet.getEcmSpritePalettes(),
                preferences.getColorMode()
        );
    }

    public Exporter(
        MapEditor mapEditor,
        ECMPalette[] ecmPalettes, int[][] clrSets,
        HashMap<Integer, int[][]> charGrids,
        HashMap<Integer, int[][]> charColors,
        ECMPalette[] ecmCharPalettes,
        boolean[] ecmCharTransparency,
        HashMap<Integer, int[][]> spriteGrids,
        int[] spriteColors,
        ECMPalette[] ecmSpritePalettes,
        ColorMode colorMode
    ) {
        this.mapEditor = mapEditor;
        this.clrSets = clrSets;
        this.charGrids = charGrids;
        this.charColors = charColors;
        this.ecmPalettes = ecmPalettes;
        this.ecmCharPalettes = ecmCharPalettes;
        this.ecmCharTransparency = ecmCharTransparency;
        this.spriteGrids = spriteGrids;
        this.spriteColors = spriteColors;
        this.ecmSpritePalettes = ecmSpritePalettes;
        this.colorMode = colorMode;
    }

    protected void printPaddedLine(BufferedWriter bw, String str, boolean isCommand) throws IOException {
        printPaddedLine(bw, str, isCommand, ASM_LINELEN, null);
    }

    protected void printPaddedLine(BufferedWriter bw, String str, String comment) throws IOException {
        printPaddedLine(bw, str, comment != null, ASM_LINELEN, comment);
    }

    protected void printPaddedLine(BufferedWriter bw, String str, boolean isCommand, int padlen, String comment)
            throws IOException {
        if (str.length() < padlen) {
            StringBuffer sbOut = new StringBuffer();
            sbOut.append(str);
            for (int i = str.length(); i < padlen; i++) {
                if ((i == (padlen - 1)) && isCommand) {
                    sbOut.append(";");
                }
                else {
                    sbOut.append(" ");
                }
            }
            if (comment != null) {
                sbOut.append(" ").append(comment);
            }
            bw.write(sbOut.toString());
        }
        else {
            bw.write(str);
        }
        bw.newLine();
    }

    protected int printByte(BufferedWriter bw, StringBuilder sbLine, int i, int b) throws IOException {
        return printByte(bw, sbLine, i, b, null);
    }

    protected int printByte(BufferedWriter bw, StringBuilder sbLine, int i, int b, String label) throws IOException {
        sbLine.append(i == 0 ? (label != null ? Globals.padr(label, 6) : "      ") + " BYTE " : ",").append(">").append(Globals.toHexString(b, 2));
        i++;
        if (i == 8) {
            printPaddedLine(bw, sbLine.toString(), false);
            sbLine.delete(0, sbLine.length());
            i = 0;
        }
        return i;
    }

    protected String rightPad(int n, int width) {
        StringBuilder sb = new StringBuilder(Integer.toString(n));
        while (sb.length() < width) {
            sb.append(" ");
        }
        return sb.toString();
    }
}
