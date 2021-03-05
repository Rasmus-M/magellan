package com.dreamcodex.ti.exporters;

import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.ECMPalette;
import com.dreamcodex.ti.util.Globals;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;

public abstract class Exporter {

    public static final byte[] BIN_HEADER_MAG = {(byte) 'M', (byte) 'G'};
    public static final byte[] BIN_HEADER_VER = {(byte) '0', (byte) '1'};

    public static final byte BIN_CHUNK_COLORS = 1 << 1;
    public static final byte BIN_CHUNK_CHARS = 1 << 2;
    public static final byte BIN_CHUNK_SPRITES = 1 << 3;

    public static final byte BIN_MAP_HEADER_RESERVED1 = 0;
    public static final byte BIN_MAP_HEADER_RESERVED2 = 0;
    public static final byte BIN_MAP_HEADER_RESERVED3 = 0;
    public static final byte BIN_MAP_HEADER_RESERVED4 = 0;

    private final int ASM_LINELEN = 40;

    protected MapEditor mapdMain;
    protected int[][] clrSets;
    protected ECMPalette[] ecmPalettes;
    protected HashMap<Integer, int[][]> hmCharGrids;
    protected HashMap<Integer, int[][]> hmCharColors;
    protected ECMPalette[] ecmCharPalettes;
    protected boolean[] ecmCharTransparency;
    protected HashMap<Integer, int[][]> hmSpriteGrids;
    protected int[] spriteColors;
    protected ECMPalette[] ecmSpritePalettes;
    protected int colorMode;

    public Exporter() {
    }

    public Exporter(
        MapEditor mapdMain,
        ECMPalette[] ecmPalettes, int[][] clrSets,
        HashMap<Integer, int[][]> hmCharGrids,
        HashMap<Integer, int[][]> hmCharColors,
        ECMPalette[] ecmCharPalettes,
        boolean[] ecmCharTransparency,
        HashMap<Integer, int[][]> hmSpriteGrids,
        int[] spriteColors,
        ECMPalette[] ecmSpritePalettes,
        int colorMode
    ) {
        this.mapdMain = mapdMain;
        this.clrSets = clrSets;
        this.hmCharGrids = hmCharGrids;
        this.hmCharColors = hmCharColors;
        this.ecmPalettes = ecmPalettes;
        this.ecmCharPalettes = ecmCharPalettes;
        this.ecmCharTransparency = ecmCharTransparency;
        this.hmSpriteGrids = hmSpriteGrids;
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
}
