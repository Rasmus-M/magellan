package com.dreamcodex.ti;

import com.dreamcodex.ti.component.MagellanExportDialog;
import com.dreamcodex.ti.component.MapCanvas;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.io.*;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: RasmusM
 * Date: 02-11-13
 * Time: 17:22
 * <p/>
 * All file import/export methods moved here to shorten the main file.
 */
public class MagellanImportExport {

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

    private MapEditor mapdMain;
    private int[][] clrSets;
    private ECMPalette[] ecmPalettes;
    private HashMap<Integer, int[][]> hmCharGrids;
    private HashMap<Integer, int[][]> hmCharColors;
    private ECMPalette[] ecmCharPalettes;
    private boolean[] ecmCharTransparency;
    private final HashMap<Integer, int[][]> hmSpriteGrids;
    private final int[] spriteColors;
    private ECMPalette[] ecmSpritePalettes;
    private int colorMode;

    MagellanImportExport(
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

    protected int readDataFile(File mapDataFile) throws IOException {
        hmCharGrids.clear();
        BufferedReader br = new BufferedReader(new FileReader(mapDataFile));
        String lineIn = "";
        int mapY = 0;
        int mapX = 0;
        int mapCount = 1;
        int mapColor = 15;
        int mapWidth = 32;
        int mapHeight = 24;
        int charStart = TIGlobals.BASIC_FIRST_CHAR;
        int charEnd = TIGlobals.BASIC_LAST_CHAR;
        int charRead = charStart;
        int charRead1 = charStart;
        int charRead2 = charStart;
        int charColorRead = charStart;
        int cset = (int) (Math.floor(charStart / 8));
        int palNo = 0;
        int charPalNo = charStart;
        int charTransNo = charStart;
        int currMap = 0;
        int spriteRead = TIGlobals.MIN_SPRITE;
        int spriteRead1 = TIGlobals.MIN_SPRITE;
        int spriteRead2 = TIGlobals.MIN_SPRITE;
        int spritePalNo = 0;
        int spriteColNo = 0;
        do {
            lineIn = br.readLine();
            if (lineIn == null) {
                break;
            }
            else {
                if (lineIn.startsWith(Globals.KEY_COLORSET)) {
                    lineIn = lineIn.substring(Globals.KEY_COLORSET.length());
                    clrSets[cset][Globals.INDEX_CLR_FORE] = Integer.parseInt(lineIn.substring(0, lineIn.indexOf("|")));
                    clrSets[cset][Globals.INDEX_CLR_BACK] = Integer.parseInt(lineIn.substring(lineIn.indexOf("|") + 1));
                    cset++;
                }
                else if (lineIn.startsWith(Globals.KEY_CHARDATA)) {
                    lineIn = Globals.trimHex(lineIn.substring(Globals.KEY_CHARDATA.length()), 16);
                    hmCharGrids.put(charRead, Globals.getIntGrid(lineIn, 8));
                    charRead++;
                }
                else if (lineIn.startsWith(Globals.KEY_CHARDATA1) && (colorMode == Magellan.COLOR_MODE_ECM_2 || colorMode == Magellan.COLOR_MODE_ECM_3)) {
                    lineIn = Globals.trimHex(lineIn.substring(Globals.KEY_CHARDATA1.length()), 16);
                    int[][] charGrid = hmCharGrids.get(charRead1++);
                    int[][] charGrid1 = Globals.getIntGrid(lineIn, 8);
                    Globals.orGrid(charGrid1, charGrid, 1);
                }
                else if (lineIn.startsWith(Globals.KEY_CHARDATA2) && (colorMode == Magellan.COLOR_MODE_ECM_3)) {
                    lineIn = Globals.trimHex(lineIn.substring(Globals.KEY_CHARDATA2.length()), 16);
                    int[][] charGrid = hmCharGrids.get(charRead2++);
                    int[][] charGrid2 = Globals.getIntGrid(lineIn, 8);
                    Globals.orGrid(charGrid2, charGrid, 2);
                }
                else if (lineIn.startsWith(Globals.KEY_CHARCOLOR) && colorMode == Magellan.COLOR_MODE_BITMAP) {
                    lineIn = lineIn.substring(Globals.KEY_CHARCOLOR.length());
                    if (lineIn.length() > 16) {
                        lineIn = lineIn.substring(0, 16);
                    }
                    else if (lineIn.length() < 16) {
                        lineIn = lineIn + "1010101010101010";
                        lineIn = lineIn.substring(0, 16);
                    }
                    int[][] charColors = new int[8][2];
                    for (int y = 0; y < 8; y++) {
                        charColors[y][1] = Integer.parseInt(Character.toString(lineIn.charAt(y * 2)), 16);
                        charColors[y][0] = Integer.parseInt(Character.toString(lineIn.charAt(y * 2 + 1)), 16);
                    }
                    hmCharColors.put(charColorRead, charColors);
                    charColorRead++;
                }
                else if (lineIn.startsWith(Globals.KEY_PALETTE) && (colorMode == Magellan.COLOR_MODE_ECM_2 || colorMode == Magellan.COLOR_MODE_ECM_3)) {
                    lineIn = lineIn.substring(Globals.KEY_PALETTE.length());
                    ECMPalette ecmPalette = ecmPalettes[palNo++];
                    String[] hexCols = lineIn.split("\\|");
                    for (int i = 0; i < Math.min(hexCols.length, ecmPalette.getSize()); i++) {
                        Color color;
                        try {
                            color = new Color(Integer.parseInt(hexCols[i].substring(2), 16));
                        } catch (NumberFormatException e) {
                            color = TIGlobals.TI_COLOR_TRANSOPAQUE;
                        }
                        ecmPalette.setColor(i, color);
                    }
                }
                else if (lineIn.startsWith(Globals.KEY_CHARPALS) && (colorMode == Magellan.COLOR_MODE_ECM_2 || colorMode == Magellan.COLOR_MODE_ECM_3)) {
                    ECMPalette ecmPalette;
                    try {
                        ecmPalette = ecmPalettes[Integer.parseInt(lineIn.substring(Globals.KEY_CHARPALS.length()))];
                    } catch (NumberFormatException e) {
                        ecmPalette = ecmPalettes[0];
                    }
                    ecmCharPalettes[charPalNo++] = ecmPalette;
                }
                else if (lineIn.startsWith(Globals.KEY_CHARTRANS) && (colorMode == Magellan.COLOR_MODE_ECM_2 || colorMode == Magellan.COLOR_MODE_ECM_3)) {
                    ecmCharTransparency[charTransNo++] = "1".equals(lineIn.substring(Globals.KEY_CHARTRANS.length()));
                }
                else if (lineIn.startsWith(Globals.KEY_CHARRANG)) {
                    lineIn = lineIn.substring(Globals.KEY_CHARRANG.length());
                    charStart = Integer.parseInt(lineIn.substring(0, lineIn.indexOf("|")));
                    charEnd = Integer.parseInt(lineIn.substring(lineIn.indexOf("|") + 1));
                    charRead = charStart;
                    charRead1 = charStart;
                    charRead2 = charStart;
                    charColorRead = charStart;
                    cset = (int) (Math.floor(charStart / 8));
                    charPalNo = charStart;
                    charTransNo = charStart;
                }
                else if (lineIn.startsWith(Globals.KEY_SCRBACK)) {
                    lineIn = lineIn.substring(Globals.KEY_SCRBACK.length());
                    mapColor = Integer.parseInt(lineIn);
                    mapdMain.setColorScreen(mapColor);
                }
                else if (lineIn.startsWith(Globals.KEY_MAPCOUNT)) {
                    lineIn = lineIn.substring(Globals.KEY_MAPCOUNT.length());
                    mapCount = Integer.parseInt(lineIn);
                }
                else if (lineIn.equals(Globals.KEY_MAPSTART)) {
                    if (mapY > 0) {
                        mapdMain.addBlankMap(mapWidth, mapHeight);
                        currMap++;
                        mapdMain.setCurrentMapId(currMap);
                        mapY = 0;
                    }
                }
                else if (lineIn.equals(Globals.KEY_MAPEND)) {
                    mapdMain.setColorScreen(mapColor);
                    mapdMain.storeCurrentMap();
                }
                else if (lineIn.startsWith(Globals.KEY_MAPSIZE)) {
                    lineIn = lineIn.substring(Globals.KEY_MAPSIZE.length());
                    mapWidth = Integer.parseInt(lineIn.substring(0, lineIn.indexOf("|")));
                    mapHeight = Integer.parseInt(lineIn.substring(lineIn.indexOf("|") + 1));
                    mapdMain.setGridWidth(mapWidth);
                    mapdMain.setGridHeight(mapHeight);
                }
                else if (lineIn.startsWith(Globals.KEY_MAPBACK)) {
                    lineIn = lineIn.substring(Globals.KEY_MAPBACK.length());
                    mapColor = Integer.parseInt(lineIn);
                    mapdMain.setColorScreen(mapColor);
                }
                else if (lineIn.startsWith(Globals.KEY_MAPDATA)) {
                    if (mapY >= mapHeight) {
                        mapdMain.setColorScreen(mapColor);
                        mapdMain.storeCurrentMap();
                        mapdMain.addBlankMap(mapWidth, mapHeight);
                        currMap++;
                        mapdMain.setCurrentMapId(currMap);
                        mapY = 0;
                    }
                    lineIn = lineIn.substring(Globals.KEY_MAPDATA.length());
                    StringTokenizer stParse = new StringTokenizer(lineIn, "|", false);
                    while (stParse.hasMoreTokens()) {
                        String sVal = stParse.nextToken();
                        mapdMain.setGridAt(mapX, mapY, Integer.parseInt(sVal));
                        mapX++;
                    }
                    mapX = 0;
                    mapY++;
                }
                else if (lineIn.startsWith(Globals.KEY_SPRITE_LOCATION)) {
                    lineIn = lineIn.substring(Globals.KEY_MAPDATA.length());
                    String[] lineParts = lineIn.split("\\|");
                    if (lineParts.length == 3) {
                        HashPoint p = new HashPoint(Integer.parseInt(lineParts[0]), Integer.parseInt(lineParts[1]));
                        int spriteNum = Integer.parseInt(lineParts[2]);
                        HashMap<Point, ArrayList<Integer>> spriteMap = mapdMain.getSpriteMap();
                        ArrayList<Integer> spriteList = spriteMap.get(p);
                        if (spriteList == null) {
                            spriteList = new ArrayList<Integer>();
                            spriteMap.put(p, spriteList);
                        }
                        spriteList.add(spriteNum);
                    }
                }
                else if (lineIn.startsWith(Globals.KEY_SPRITE_PATTERN)) {
                    lineIn = Globals.trimHex(lineIn.substring(Globals.KEY_SPRITE_PATTERN.length()), 64);
                    hmSpriteGrids.put(spriteRead++, Globals.getIntGrid(lineIn, 16));
                }
                else if (lineIn.startsWith(Globals.KEY_SPRITE_PATTERN1) && (colorMode == Magellan.COLOR_MODE_ECM_2 || colorMode == Magellan.COLOR_MODE_ECM_3)) {
                    lineIn = Globals.trimHex(lineIn.substring(Globals.KEY_SPRITE_PATTERN1.length()), 64);
                    int[][] spriteGrid = hmSpriteGrids.get(spriteRead1++);
                    int[][] spriteGrid1 = Globals.getIntGrid(lineIn, 16);
                    Globals.orGrid(spriteGrid1, spriteGrid, 1);
                }
                else if (lineIn.startsWith(Globals.KEY_SPRITE_PATTERN2) && (colorMode == Magellan.COLOR_MODE_ECM_3)) {
                    lineIn = Globals.trimHex(lineIn.substring(Globals.KEY_SPRITE_PATTERN2.length()), 64);
                    int[][] spriteGrid = hmSpriteGrids.get(spriteRead2++);
                    int[][] spriteGrid2 = Globals.getIntGrid(lineIn, 16);
                    Globals.orGrid(spriteGrid2, spriteGrid, 2);
                }
                else if (lineIn.startsWith(Globals.KEY_SPRITE_COLOR)) {
                    int colorIndex;
                    try {
                        colorIndex = Integer.parseInt(lineIn.substring(Globals.KEY_SPRITE_COLOR.length()));
                    } catch (NumberFormatException e) {
                        colorIndex = 0;
                    }
                    if (colorMode == Magellan.COLOR_MODE_ECM_2 || colorMode == Magellan.COLOR_MODE_ECM_3) {
                        ecmSpritePalettes[spritePalNo++] = ecmPalettes[colorIndex];
                    }
                    else {
                        spriteColors[spriteColNo++] = colorIndex;
                    }
                }
            }
        } while (lineIn != null);
        br.close();
        if (colorMode == Magellan.COLOR_MODE_BITMAP && charColorRead == charStart) {
            // Bitmap color mode but no bitmap colors found - use color sets
            for (int i = charStart; i <= charEnd; i++) {
                if (hmCharGrids.get(i) != null) {
                    int[][] colorGrid = hmCharColors.get(i);
                    if (colorGrid == null) {
                        colorGrid = new int[8][2];
                        hmCharColors.put(i, colorGrid);
                    }
                    for (int row = 0; row < 8; row++) {
                        colorGrid[row][0] = clrSets[i / 8][Globals.INDEX_CLR_BACK];
                        colorGrid[row][1] = clrSets[i / 8][Globals.INDEX_CLR_FORE];
                    }
                }
            }
        }
        return charStart;
    }

    protected void readAppendDataFile(File mapDataFile) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(mapDataFile));
        String lineIn = "";
        int mapY = 0;
        int mapX = 0;
        int mapCount = 1;
        int mapColor = 15;
        int mapWidth = 32;
        int mapHeight = 24;
        int currMap = mapdMain.getMapCount();
        do {
            lineIn = br.readLine();
            if (lineIn == null) {
                break;
            }
            else {
                if (lineIn.startsWith(Globals.KEY_COLORSET)) {
                }
                else if (lineIn.startsWith(Globals.KEY_CHARDATA)) {
                }
                else if (lineIn.startsWith(Globals.KEY_SCRBACK)) {
                    lineIn = lineIn.substring(Globals.KEY_SCRBACK.length());
                    mapColor = Integer.parseInt(lineIn);
                }
                else if (lineIn.startsWith(Globals.KEY_MAPCOUNT)) {
                    lineIn = lineIn.substring(Globals.KEY_MAPCOUNT.length());
                    mapCount = Integer.parseInt(lineIn);
                }
                else if (lineIn.startsWith(Globals.KEY_MAPSIZE)) {
                    lineIn = lineIn.substring(Globals.KEY_MAPSIZE.length());
                    mapWidth = Integer.parseInt(lineIn.substring(0, lineIn.indexOf("|")));
                    mapHeight = Integer.parseInt(lineIn.substring(lineIn.indexOf("|") + 1));
                }
                else if (lineIn.equals(Globals.KEY_MAPSTART)) {
                    if (mapY > 0) {
                        mapdMain.addBlankMap(mapWidth, mapHeight);
                        currMap++;
                        mapdMain.setCurrentMapId(currMap);
                        mapY = 0;
                    }
                }
                else if (lineIn.equals(Globals.KEY_MAPEND)) {
                    mapdMain.setColorScreen(mapColor);
                    mapdMain.storeCurrentMap();
                }
                else if (lineIn.startsWith(Globals.KEY_MAPBACK)) {
                    lineIn = lineIn.substring(Globals.KEY_MAPBACK.length());
                    mapColor = Integer.parseInt(lineIn);
                    mapdMain.setColorScreen(mapColor);
                }
                else if (lineIn.startsWith(Globals.KEY_MAPDATA)) {
                    if (currMap == mapdMain.getMapCount()) {
                        mapdMain.storeCurrentMap();
                        mapdMain.addBlankMap(mapWidth, mapHeight);
                        mapdMain.setCurrentMapId(currMap);
                        mapY = 0;
                        mapdMain.setColorScreen(mapColor);
                    }
                    else if (mapY >= mapHeight) {
                        mapdMain.storeCurrentMap();
                        mapdMain.addBlankMap();
                        currMap++;
                        mapdMain.setCurrentMapId(currMap);
                        mapY = 0;
                        mapdMain.setColorScreen(mapColor);
                    }
                    lineIn = lineIn.substring(Globals.KEY_MAPDATA.length());
                    StringTokenizer stParse = new StringTokenizer(lineIn, "|", false);
                    while (stParse.hasMoreTokens()) {
                        String sVal = stParse.nextToken();
                        mapdMain.setGridAt(mapX, mapY, Integer.parseInt(sVal));
                        mapX++;
                    }
                    mapX = 0;
                    mapY++;
                }
            }
        } while (lineIn != null);
        br.close();
        mapdMain.updateComponents();
    }

    protected void readCharacterData(File mapDataFile) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(mapDataFile));
        String lineIn = "";
        int charStart = TIGlobals.BASIC_FIRST_CHAR;
        int charEnd = TIGlobals.BASIC_LAST_CHAR;
        int charRead = charStart;
        do {
            lineIn = br.readLine();
            if (lineIn == null) {
                break;
            }
            else {
                if (lineIn.startsWith(Globals.KEY_CHARDATA)) {
                    lineIn = lineIn.substring(Globals.KEY_CHARDATA.length());
                    hmCharGrids.put(charRead, Globals.getIntGrid(lineIn, 8));
                    charRead++;
                }
                else if (lineIn.startsWith(Globals.KEY_CHARRANG)) {
                    lineIn = lineIn.substring(Globals.KEY_CHARRANG.length());
                    charStart = Integer.parseInt(lineIn.substring(0, lineIn.indexOf("|")));
                    charEnd = Integer.parseInt(lineIn.substring(lineIn.indexOf("|") + 1));
                    charRead = charStart;
                }
            }
        } while (lineIn != null);
        br.close();
    }

    protected void readVramDumpFile(File vramDumpFile, int charTableOffset, int mapTableOffset, int colorTableOffset, int spriteTableOffset, int spriteAttrOffset, boolean bitmapMode) throws IOException {
        FileInputStream fib = new FileInputStream(vramDumpFile);
        boolean basicOffset = false;
        if (charTableOffset == mapTableOffset) {
            basicOffset = true;
        }
        int readPos = 0;
        int readInt;
        int charStart = TIGlobals.MIN_CHAR;
        int charEnd = TIGlobals.MAX_CHAR;
        StringBuilder sbChar = new StringBuilder();
        int charByte = 0;
        int charRead = charStart;
        int charTableLength = 2048;
        int colorTableLength = bitmapMode ? 2048 : 32;
        int mapCols = 32;
        int mapRows = 24;
        int mapTableLength = (mapCols * mapRows);
        int spriteTableLength = 2048;
        int spriteAttrLength = 128;
        int spritePatternByte = 0;
        while ((readInt = fib.read()) != -1) {
            if ((readPos >= charTableOffset) && (readPos < (charTableOffset + charTableLength))) {
                if (charRead <= charEnd) {
                    sbChar.append(readInt < 16 ? "0" : "").append(Integer.toHexString(readInt));
                    charByte++;
                    if (charByte >= 8) {
                        int mapChar = charRead;
                        if (basicOffset)
                            mapChar = (mapChar + 0xA0) & 0xFF;
                        hmCharGrids.put(mapChar, Globals.getIntGrid(sbChar.toString(), 8));
                        charRead++;
                        charByte = 0;
                        sbChar.delete(0, sbChar.length());
                    }
                }
            }
            if (readPos >= mapTableOffset && readPos < (mapTableOffset + mapTableLength)) {
                int mapCell = readPos - mapTableOffset;
                int mapRow = (int) (Math.floor(mapCell / mapCols));
                int mapCol = mapCell % mapCols;
                int mapChar = readInt;
                if (basicOffset)
                    mapChar = (mapChar + 0xA0) & 0xFF;
                mapdMain.setGridAt(mapCol, mapRow, mapChar);
            }
            if (readPos >= colorTableOffset && readPos < (colorTableOffset + colorTableLength)) {
                if (bitmapMode) {
                    if (colorMode == Magellan.COLOR_MODE_BITMAP) {
                        int colorByte = readPos - colorTableOffset;
                        int colorChar = colorByte / 8;
                        int[][] colorGrid = hmCharColors.get(colorChar);
                        if (colorGrid == null) {
                            colorGrid = new int[8][2];
                            hmCharColors.put(colorChar, colorGrid);
                        }
                        int row = colorByte % 8;
                        colorGrid[row][0] = readInt & 0x0F;
                        colorGrid[row][1] = (readInt & 0xF0) >> 4;
                    }
                }
                else {
                    int setNum = readPos - colorTableOffset;
                    int colorFore = (readInt & 0xF0) >> 4;
                    int colorBack = (readInt & 0x0F);
                    if (basicOffset)
                        setNum = (setNum + 20) & 0x1F;
                    clrSets[setNum][Globals.INDEX_CLR_FORE] = colorFore;
                    clrSets[setNum][Globals.INDEX_CLR_BACK] = colorBack;
                    if (colorMode == Magellan.COLOR_MODE_BITMAP) {
                        for (int colorChar = setNum * 8; colorChar < setNum * 8 + 8; colorChar++) {
                            int[][] colorGrid = hmCharColors.get(colorChar);
                            if (colorGrid == null) {
                                colorGrid = new int[8][2];
                                hmCharColors.put(colorChar, colorGrid);
                            }
                            for (int row = 0; row < 8; row++) {
                                colorGrid[row][0] = colorBack;
                                colorGrid[row][1] = colorFore;
                            }
                        }
                    }
                }
            }
            if (readPos >= spriteTableOffset && readPos < spriteTableOffset + spriteTableLength) {
                int offset = readPos - spriteTableOffset;
                int spriteNum = offset / 32;
                int byteOffset = offset % 32;
                int x0 = byteOffset >= 16 ? 8 : 0;
                int y0 = byteOffset >= 16 ? byteOffset - 16 : byteOffset;
                int mask = 0x80;
                int[][] grid = hmSpriteGrids.get(spriteNum);
                int[] row = grid[y0];
                for (int x = x0; x < x0 + 8; x++) {
                    row[x] = (readInt & mask) != 0 ? 1 : 0;
                    mask >>= 1;
                }
            }
            if (readPos >= spriteAttrOffset && readPos < spriteAttrOffset + spriteAttrLength) {
                int offset = readPos - spriteAttrOffset;
                if (offset % 4 == 2) {
                    spritePatternByte = readInt;
                }
                if (offset % 4 == 3) {
                    spriteColors[spritePatternByte / 4] = readInt & 0x0f;
                }
            }
            readPos++;
        }
        fib.close();
    }

    protected void readMapImageFile(File mapImageFile, int startIndex, int endIndex, int startPalette, int tolerance) throws Exception {
        startIndex &= 0xf8;
        int maxIndex = endIndex - startIndex;
        BufferedImage bufferedImage = ImageIO.read(mapImageFile);
        int width = bufferedImage.getWidth() / 8;
        int height = bufferedImage.getHeight() / 8;
        mapdMain.setGridWidth(width);
        mapdMain.setGridHeight(height);
        int[][] mapData = mapdMain.getMapData(mapdMain.getCurrentMapId());
        // Find unique patterns based on RGB values
        // Produces a list of pairs
        // First element of the pair is the RGB grid
        // Second element of the pair is the palette, which is only used for the ECM cases
        ArrayList<Pair<int[][], Pair<Set<Integer>, Integer>>> patterns = new ArrayList<Pair<int[][], Pair<Set<Integer>, Integer>>>();
        for (int o = 0; o < 2; o++) {
            for (int n = 0; n + o < height; n += 2) {
                int y1 = (n + o) * 8;
                for (int m = 0; m < width; m++) {
                    int x1 = m * 8;
                    int[][] rgbGrid = new int[8][8];
                    for (int y = 0; y < 8; y++) {
                        for (int x = 0; x < 8; x++) {
                            rgbGrid[y][x] = bufferedImage.getRGB(x + x1, y + y1);
                        }
                    }
                    // Find exact match in the existing characters
                    int ch = -1;
                    for (int i = 0; i < patterns.size() && ch == -1; i++) {
                        if (Globals.gridEquals(rgbGrid, patterns.get(i).getFirst())) {
                            ch = i;
                        }
                    }
                    // If no exact match, find the best match, and match if within tolerance
                    int chBest = 0;
                    if (ch == -1) {
                        double minDist = Double.MAX_VALUE;
                        for (int i = 0; i < patterns.size(); i++) {
                            double dist = Globals.gridColorDistance(rgbGrid, patterns.get(i).getFirst());
                            if (dist < minDist) {
                                minDist = dist;
                                chBest = i;
                                if (minDist < tolerance) {
                                    ch = i;
                                }
                            }
                        }
                    }
                    // If still no match, try to add it
                    if (ch == -1) {
                        // Is there room?
                        if (patterns.size() <= maxIndex) {
                            // At this point only the grid is initialized, the palette is set to null
                            patterns.add(new Pair<int[][], Pair<Set<Integer>, Integer>>(rgbGrid, null));
                            ch = patterns.size() - 1;
                        }
                        // else use best match
                        else {
                            ch = chBest;
                        }
                    }
                    mapData[n + o][m] = Math.min(ch, maxIndex);
                }
            }
        }
        System.out.println("Number of unique patterns: " + patterns.size());
        switch (colorMode) {
            case Magellan.COLOR_MODE_GRAPHICS_1:
                // Divide into char/color sets
                // Each charSet is stored as a pair
                // The first element is an integer key composed of the background and foreground colors
                // The second element is a list of indexes of characters that are members of the character set
                ArrayList<Pair<Integer, ArrayList<Integer>>> charSets = new ArrayList<Pair<Integer, java.util.ArrayList<Integer>>>();
                int[][][] charGrids = new int[256][8][8];
                for (int i = 0; i < patterns.size() && i <= maxIndex; i++) {
                    // Build a palette of the pattern grid
                    // Each entry in the palette consists of a RGB color and a count
                    int[][] rgbGrid = patterns.get(i).getFirst();
                    int[][] palette = new int[64][2]; // Max 64 unique colors in a 8x8 grid
                    int iMax = -1;
                    for (int[] row : rgbGrid) {
                        for (int color : row) {
                            boolean found = false;
                            for (int p = 0; p <= iMax && !found; p++) {
                                if (palette[p][0] == color) {
                                    palette[p][1]++;
                                    found = true;
                                }
                            }
                            if (!found) {
                                iMax++;
                                palette[iMax][0] = color;
                                palette[iMax][1] = 1;
                            }
                        }
                    }
                    // Background and foreground are set to the two most used colors of the palette
                    Globals.sortGrid(palette);
                    int backColorIndex = Globals.getClosestColorIndex(new Color(palette[0][0], true), TIGlobals.TI_PALETTE);
                    int foreColorIndex = iMax > 0 ? Globals.getClosestColorIndex(new Color(palette[1][0], true), TIGlobals.TI_PALETTE, backColorIndex) : backColorIndex;
                    int screenColor = mapdMain.getColorScreen();
                    boolean mono = backColorIndex == foreColorIndex;
                    if (!mono && foreColorIndex == screenColor) {
                        int tmp = foreColorIndex;
                        foreColorIndex = backColorIndex;
                        backColorIndex = tmp;
                    }
                    int key = (backColorIndex * 16) | foreColorIndex;
                    ArrayList<Integer> charSet = null;
                    boolean foundButNoRoom = false;
                    for (Pair<Integer, ArrayList<Integer>> charSetPair : charSets) {
                        if (charSet == null) {
                            int otherKey = charSetPair.getFirst();
                            int otherBackColorIndex = otherKey / 16;
                            int otherForeColorIndex = otherKey % 16;
                            boolean otherMono = otherBackColorIndex == otherForeColorIndex;
                            // Match to key of existing character color set
                            // if monochrome it's enough that one of the colors (background or foreground) matches
                            if (
                                otherKey == key
                                ||
                                (mono || otherMono) && (otherBackColorIndex == backColorIndex || otherForeColorIndex == foreColorIndex)
                                ||
                                otherBackColorIndex == foreColorIndex && otherForeColorIndex == backColorIndex
                            ) {
                                charSet = charSetPair.getSecond();
                                if (charSet.size() < 8) {
                                    charSet.add(i);
                                    if ((mono || otherMono) && (otherBackColorIndex == backColorIndex || otherForeColorIndex == foreColorIndex)) {
                                        if (mono) {
                                            foreColorIndex = otherForeColorIndex;
                                            backColorIndex = otherBackColorIndex;
                                        } else {
                                            // Update foreground color of the other character set
                                            if (otherBackColorIndex == backColorIndex) {
                                                charSetPair.setFirst((otherBackColorIndex * 16) | foreColorIndex);
                                            } else {
                                                charSetPair.setFirst((otherBackColorIndex * 16) | backColorIndex);
                                                foreColorIndex = backColorIndex;
                                            }
                                            backColorIndex = otherBackColorIndex;
                                        }
                                    } else if (otherBackColorIndex == foreColorIndex && otherForeColorIndex == backColorIndex) {
                                        // Inverse match
                                        foreColorIndex = otherForeColorIndex;
                                        backColorIndex = otherBackColorIndex;
                                    }
                                } else {
                                    charSet = null;
                                    foundButNoRoom = true;
                                }
                            }
                        }
                    }
                    if (charSet == null) {
                        // An exact match with an existing character set was not found
                        if (charSets.size() < (maxIndex + 1) / 8) {
                            // Make a new one if room
                            charSet = new ArrayList<Integer>(8);
                            charSet.add(i);
                            charSets.add(new Pair<Integer, ArrayList<Integer>>(key, charSet));
                        } else {
                            if (foundButNoRoom) {
                                // Remap the character
                                rgbGrid = patterns.get(i).getFirst();
                                int ch = -1;
                                double minDist = Double.MAX_VALUE;
                                for (int j = 0; j < i; j++) {
                                    if (patterns.get(j) != null) {
                                        double dist = Globals.gridColorDistance(rgbGrid, patterns.get(j).getFirst());
                                        if (dist < minDist) {
                                            minDist = dist;
                                            ch = j;
                                        }
                                    }
                                }
                                System.out.println("Char " + i + " remapped to " + ch);
                                for (int n = 0; n < height; n++) {
                                    for (int m = 0; m < width; m++) {
                                        if (mapData[n][m] == i) {
                                            mapData[n][m] = ch;
                                        }
                                    }
                                }
                                patterns.set(i, null);
                            } else {
                                // Find the best matching set, i.e. the one with the smallest color distance
                                double minDist = Double.MAX_VALUE;
                                int backIndex1 = backColorIndex;
                                int foreIndex1 = foreColorIndex;
                                for (Pair<Integer, ArrayList<Integer>> charSetPair : charSets) {
                                    if (charSetPair.getSecond().size() < 8) {
                                        int k = charSetPair.getFirst();
                                        int foreIndex2 = k / 16;
                                        int backIndex2 = k % 16;
                                        double dist =
                                            Globals.colorDistance(TIGlobals.TI_PALETTE[backIndex2], TIGlobals.TI_PALETTE[backIndex1]) +
                                            Globals.colorDistance(TIGlobals.TI_PALETTE[foreIndex2], TIGlobals.TI_PALETTE[foreIndex1]);
                                        if (dist < minDist) {
                                            minDist = dist;
                                            charSet = charSetPair.getSecond();
                                            backColorIndex = foreIndex2;
                                            foreColorIndex = backIndex2;
                                        }
                                    }
                                }
                                charSet.add(i);
                            }
                        }
                    }
                    if (charSet != null) {
                        // Update pattern to match character color set
                        int[][] charGrid = charGrids[i];
                        for (int y = 0; y < rgbGrid.length; y++) {
                            int[] row = rgbGrid[y];
                            for (int x = 0; x < row.length; x++) {
                                Color color = new Color(row[x], true);
                                charGrid[y][x] = Globals.colorDistance(color, TIGlobals.TI_PALETTE[backColorIndex]) <= Globals.colorDistance(color, TIGlobals.TI_PALETTE[foreColorIndex]) ? 0 : 1;
                            }
                        }
                        // Check if pattern is now a duplicate of another pattern in the character color set
                        boolean found = false;
                        for (int j = 0; j < charSet.size() - 2 && !found; j++) {
                            int k = charSet.get(j);
                            if (Globals.gridEquals(charGrid, charGrids[k])) {
                                // Found the same pattern - remove this one
                                charSet.remove(charSet.size() - 1);
                                for (int n = 0; n < height; n++) {
                                    for (int m = 0; m < width; m++) {
                                        if (mapData[n][m] == i) {
                                            mapData[n][m] = k;
                                        }
                                    }
                                }
                                patterns.set(i, null);
                                found = true;
                            }
                        }
                    }
                }
                // Maps old char numbers to new
                Map<Integer, Integer> charMap = new HashMap<Integer, Integer>();
                int colSet = startIndex / 8;
                int ch = 0;
                for (Pair<Integer, ArrayList<Integer>> charSetPair : charSets) {
                    ArrayList<Integer> charSet = charSetPair.getSecond();
                    for (Integer n : charSet) {
                        charMap.put(n, ch);
                        hmCharGrids.put(ch + startIndex, charGrids[n]);
                        ch++;
                    }
                    // Fill up character color set
                    while (ch % 8 != 0) {
                        hmCharGrids.put(ch + startIndex, new int[8][8]);
                        ch++;
                    }
                    int key = charSetPair.getFirst();
                    clrSets[colSet++] = new int[] {key / 16, key % 16};
                }
                // Remap chars
                for (int n = 0; n < height; n++) {
                    for (int m = 0; m < width; m++) {
                        Integer c = charMap.get(mapData[n][m]);
                        if (c != null) {
                            mapData[n][m] = c;
                        }
                        else {
                            System.out.println(mapData[n][m] + " at (" + m + "," + n + ") not found in charmap");
                        }
                    }
                }
                break;
            case Magellan.COLOR_MODE_BITMAP:
                for (int i = 0; i < patterns.size() && i <= maxIndex; i++) {
                    int[][] rgbGrid = patterns.get(i).getFirst();
                    int[][] charGrid = hmCharGrids.get(i + startIndex);
                    if (charGrid == null) {
                        charGrid = new int[8][8];
                        hmCharGrids.put(i + startIndex, charGrid);
                    }
                    int[][] charColors = hmCharColors.get(i + startIndex);
                    if (charColors == null) {
                        charColors = new int[8][2];
                        hmCharColors.put(i + startIndex, charColors);
                    }
                    int screenColor = mapdMain.getColorScreen();
                    for (int y = 0; y < rgbGrid.length; y++) {
                        int[] row = rgbGrid[y];
                        int[][] palette = new int[8][2];
                        int iMax = -1;
                        for (Integer color : row) {
                            boolean found = false;
                            for (int p = 0; p <= iMax && !found; p++) {
                                if (palette[p][0] == color) {
                                    palette[p][1]++;
                                    found = true;
                                }
                            }
                            if (!found) {
                                iMax++;
                                palette[iMax][0] = color;
                                palette[iMax][1] = 1;
                            }
                        }
                        Globals.sortGrid(palette);
                        int backColor = Globals.getClosestColorIndex(new Color(palette[0][0]), TIGlobals.TI_PALETTE);
                        int foreColor = iMax > 0 ? Globals.getClosestColorIndex(new Color(palette[1][0]), TIGlobals.TI_PALETTE, backColor) : backColor;
                        if (backColor == foreColor && foreColor != screenColor) {
                            backColor = screenColor;
                        }
                        else if (foreColor == screenColor) {
                            int tmp = foreColor;
                            foreColor = backColor;
                            backColor = tmp;
                        }
                        charColors[y][Globals.INDEX_CLR_BACK] = backColor;
                        charColors[y][Globals.INDEX_CLR_FORE] = foreColor;
                        // Pattern
                        for (int x = 0; x < row.length; x++) {
                            Color color = new Color(row[x]);
                            charGrid[y][x] = backColor != 0 && Globals.colorDistance(color, TIGlobals.TI_PALETTE[backColor]) <= Globals.colorDistance(color, TIGlobals.TI_PALETTE[foreColor]) ? 0 : 1;
                        }
                    }
                }
                break;
            case Magellan.COLOR_MODE_ECM_2:
            case Magellan.COLOR_MODE_ECM_3:
                int maxPaletteSize = colorMode == Magellan.COLOR_MODE_ECM_2 ? 4 : 8;
                int maxPalettes = (colorMode == Magellan.COLOR_MODE_ECM_2 ? 16 : 8) - startPalette;
                // Build palettes
                // Each palette consist of a set of RGB values and a count of how many characters use the palette
                ArrayList<Pair<Set<Integer>, Integer>> palettes = new ArrayList<Pair<Set<Integer>, Integer>>();
                for (Pair<int[][], Pair<Set<Integer>, Integer>> pattern : patterns) {
                    int[][] rgbGrid = pattern.getFirst();
                    HashSet<Integer> rgbPalette = new HashSet<Integer>();
                    for (int y = 0; y < 8; y++) {
                        for (int x = 0; x < 8; x++) {
                            rgbPalette.add(rgbGrid[y][x]);
                        }
                    }
                    for (Pair<Set<Integer>, Integer> palette : palettes) {
                        if (rgbPalette.equals(palette.getFirst())) {
                            pattern.setSecond(palette);
                            palette.setSecond(palette.getSecond() + 1);
                        }
                    }
                    if (pattern.getSecond() == null) {
                        Pair<Set<Integer>, Integer> newPalette = new Pair<Set<Integer>, Integer>(rgbPalette, 1);
                        pattern.setSecond(newPalette);
                        palettes.add(newPalette);
                    }
                }
                System.out.println("1. Number of unique palettes: " + palettes.size());
                // Sort with most used palettes first
                Collections.sort(palettes, new Comparator<Pair<Set<Integer>, Integer>>() {
                    public int compare(Pair<Set<Integer>, Integer> p1, Pair<Set<Integer>, Integer> p2) {
                        return p2.getSecond() - p1.getSecond();
                    }
                });
                // Eliminate subset palettes
                boolean found;
                do {
                    found = false;
                    for (int i = 0; i < palettes.size() && !found; i++) {
                        for (int j = 0; j < palettes.size() && !found; j++) {
                            if (i != j) {
                                if (palettes.get(i).getFirst().containsAll(palettes.get(j).getFirst())) {
                                    for (Pair<int[][], Pair<Set<Integer>, Integer>> pattern : patterns) {
                                        if (pattern.getSecond() == palettes.get(j)) {
                                            pattern.setSecond(palettes.get(i));
                                            palettes.get(i).setSecond(palettes.get(i).getSecond() + 1);
                                        }
                                    }
                                    palettes.remove(j);
                                    found = true;
                                }
                            }
                        }
                    }
                }
                while (found);
                System.out.println("2. Number of unique palettes: " + palettes.size());
                // Sort with largest palettes first
                Collections.sort(palettes, new Comparator<Pair<Set<Integer>, Integer>>() {
                    public int compare(Pair<Set<Integer>, Integer> p1, Pair<Set<Integer>, Integer> p2) {
                    return p2.getFirst().size() - p1.getFirst().size();
                    }
                });
                // Combine palettes
                do {
                    found = false;
                    for (int i = 0; i < palettes.size() && !found; i++) {
                        for (int j = 0; j < palettes.size() && !found; j++) {
                            if (i != j) {
                                Set<Integer> combinedPalette = new HashSet<Integer>(palettes.get(i).getFirst());
                                combinedPalette.addAll(palettes.get(j).getFirst());
                                if (combinedPalette.size() <= maxPaletteSize) {
                                    palettes.get(i).getFirst().clear();
                                    palettes.get(i).getFirst().addAll(combinedPalette);
                                    for (Pair<int[][], Pair<Set<Integer>, Integer>> pattern : patterns) {
                                        if (pattern.getSecond() == palettes.get(j)) {
                                            pattern.setSecond(palettes.get(i));
                                            palettes.get(i).setSecond(palettes.get(i).getSecond() + 1);
                                        }
                                    }
                                    palettes.remove(j);
                                    found = true;
                                }
                            }
                        }
                    }
                }
                while (found);
                System.out.println("3. Number of unique palettes: " + palettes.size());
                // Keep the most used colors of the palette
                for (int i = 0; i < Math.min(palettes.size(), maxPalettes); i++) {
                    Set<Integer> palette = palettes.get(i).getFirst();
                    Integer[] palArray = palette.toArray(new Integer[palette.size()]);

                    if (palette.size() > maxPaletteSize) {
                        int[] colorCounts = new int[palette.size()];
                        for (int j = 0; j < patterns.size(); j++) {
                            if (patterns.get(j).getSecond().getFirst() == palette) {
                                int[][] grid = patterns.get(j).getFirst();
                                for (int y = 0; y < 8; y++) {
                                    for (int x = 0; x < 8; x++) {
                                        int color = grid[y][x];
                                        for (int c = 0; c < palArray.length; c++) {
                                            if (palArray[c] != null && palArray[c] == color) {
                                                colorCounts[c]++;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        for (int k = 0; k < palArray.length; k++) {
                            for (int l = k + 1; l < palArray.length; l++) {
                                if (colorCounts[l] > colorCounts[k]) {
                                    int tmp = colorCounts[l];
                                    colorCounts[l] = colorCounts[k];
                                    colorCounts[k] = tmp;
                                    tmp = palArray[l];
                                    palArray[l] = palArray[k];
                                    palArray[k] = tmp;
                                }
                            }
                        }
                        palette.clear();
                        palette.addAll(Arrays.asList(palArray).subList(0, maxPaletteSize));
                    }
                }
                for (int i = 0; i < patterns.size(); i++) {
                    // Convert to indexed colors
                    int[][] grid = patterns.get(i).getFirst();
                    Pair<Set<Integer>, Integer> palette = patterns.get(i).getSecond();
                    int paletteIndex = palettes.indexOf(palette);
                    if (paletteIndex >= maxPalettes) {
                        // Find best matching palette
                        double minPalDist = Double.MAX_VALUE;
                        for (int n = 0; n < maxPalettes; n++) {
                            double dist = 0;
                            for (Integer color1 : palettes.get(n).getFirst()) {
                                for (Integer color2 : palettes.get(n).getFirst()) {
                                     dist += Globals.colorDistance(color1, color2);
                                }
                            }
                            if (dist < minPalDist) {
                                minPalDist = dist;
                                paletteIndex = n;
                            }
                        }
                        palette = palettes.get(paletteIndex);
                    }
                    Integer[] palArray = palette.getFirst().toArray(new Integer[palette.getFirst().size()]);
                    for (int y = 0; y < 8; y++) {
                        for (int x = 0; x < 8; x++) {
                            int color = grid[y][x];
                            found = false;
                            for (int n = 0; n < palArray.length && n < maxPaletteSize && !found; n++) {
                                if (palArray[n] != null && color == palArray[n]) {
                                    grid[y][x] = n;
                                    found = true;
                                }
                            }
                            if (!found) {
                                // Find best matching color
                                double minColDist = Double.MAX_VALUE;
                                int colorIndex = 0;
                                for (int n = 0; n < palArray.length && n < maxPaletteSize; n++) {
                                    if (palArray[n] != null) {
                                        double dist = Globals.colorDistance(color, palArray[n]);
                                        if (dist < minColDist) {
                                            minColDist = dist;
                                            colorIndex = n;
                                        }
                                    }
                                }
                                grid[y][x] = colorIndex;
                            }
                        }
                    }
                    // Build data structures
                    ecmPalettes[paletteIndex + startPalette].setSafeColors(palArray);
                    hmCharGrids.put(i + startIndex, grid);
                    ecmCharPalettes[i + startIndex] = ecmPalettes[paletteIndex + startPalette];
                }
                break;
        }
        // Adjust map for startIndex
        for (int n = 0; n < height; n++) {
            for (int m = 0; m < width; m++) {
                mapData[n][m] += startIndex;
            }
        }

    }

    protected void readSpriteFile(File file, int spriteIndex, int startPalette, int gap) throws Exception {
        int size = 16 + gap;
        BufferedImage image = ImageIO.read(file);
        // if (image.getWidth() % 16 == 0 && image.getHeight() % 16 == 0) {
            ColorModel colorModel = image.getColorModel();
            if (colorModel instanceof IndexColorModel) {
                IndexColorModel indexColorModel = (IndexColorModel) colorModel;
                if (indexColorModel.getMapSize() <= 256) {
                    Raster raster = image.getRaster();
                    int xSprites = image.getWidth() / size;
                    int ySprites = image.getHeight() / size;
                    for (int sy = 0; sy < ySprites; sy++) {
                        int y0 = sy * size;
                        for (int sx = 0; sx < xSprites; sx++) {
                            int x0 = sx * size;
                            if (spriteIndex <= TIGlobals.MAX_SPRITE) {
                                if (colorMode == Magellan.COLOR_MODE_GRAPHICS_1 || colorMode == Magellan.COLOR_MODE_BITMAP) {
                                    Map<Integer, int[][]> colorLayers = new TreeMap<Integer, int[][]>();
                                    int[] pixel = new int[1];
                                    for (int y = 0; y < 16; y++) {
                                        for (int x = 0; x < 16; x++) {
                                            raster.getPixel(x + x0, y + y0, pixel);
                                            int colorIndex = pixel[0];
                                            if (colorIndex != 0) { // 0 is transparent
                                                int[][] colorLayer = colorLayers.get(colorIndex);
                                                if (colorLayer == null) {
                                                    colorLayer = new int[16][16];
                                                    colorLayers.put(colorIndex, colorLayer);
                                                }
                                                colorLayer[y][x] = 1;
                                            }
                                        }
                                    }
                                    for (int colorIndex : colorLayers.keySet()) {
                                        hmSpriteGrids.put(spriteIndex, colorLayers.get(colorIndex));
                                        spriteColors[spriteIndex] = colorIndex;
                                        spriteIndex++;
                                    }
                                }
                                // F18A ECM sprites
                                else {
                                    int colors = colorMode == Magellan.COLOR_MODE_ECM_2 ? 4 : 8;
                                    int[][] grid = hmSpriteGrids.get(spriteIndex);
                                    if (grid == null) {
                                        grid = new int[16][16];
                                        hmSpriteGrids.put(spriteIndex, grid);
                                    }
                                    for (int y = 0; y < 16; y++) {
                                        for (int x = 0; x < 16; x++) {
                                            int[] pixel = new int[1];
                                            raster.getPixel(x + x0, y + y0, pixel);
                                            int colorIndex = pixel[0];
                                            grid[y][x] = colorIndex % colors;
                                        }
                                    }
                                    // Palette
                                    ECMPalette palette = ecmPalettes[startPalette];
                                    for (int i = 0; i < colors; i++) {
                                        palette.setSafeColor(i, indexColorModel.getRGB(i));
                                    }
                                    ecmSpritePalettes[spriteIndex] = palette;
                                    spriteIndex++;
                                }
                            }
                        }
                    }
                }
                else {
                    throw new Exception("Palette size must be max 256 colors.");
                }
            }
            else {
                throw new Exception("Image must be using an indexed color model.");
            }
        // }
        // else {
        //     throw new Exception("Image size must be 16 x 16 pixels.");
        // }
    }

    protected void writeDataFile(File mapDataFile) throws IOException {
        // store working map first
        mapdMain.storeCurrentMap();
        // get file output buffer
        BufferedWriter bw = new BufferedWriter(new FileWriter(mapDataFile));
        // Build ECM palette map
        Map<ECMPalette, Integer> paletteMap = new HashMap<ECMPalette, Integer>(16);
        int n = 0;
        for (ECMPalette ecmPalette : ecmPalettes) {
            paletteMap.put(ecmPalette, n++);
        }
        // Color mode
        bw.write("* COLOR MODE");
        bw.newLine();
        bw.write(Globals.KEY_COLOR_MODE + colorMode);
        bw.newLine();
        // output overall character range (this is for backwards compatibility with earlier Magellan releases, will be phased out)
        bw.write("* CHARACTER RANGE");
        bw.newLine();
        bw.write(Globals.KEY_CHARRANG + TIGlobals.MIN_CHAR + "|" + +TIGlobals.MAX_CHAR);
        bw.newLine();
        // save colorsets
        bw.write("* COLORSET");
        bw.newLine();
        for (int i = 0; i < clrSets.length; i++) {
            bw.write(Globals.KEY_COLORSET + clrSets[i][Globals.INDEX_CLR_FORE] + "|" + clrSets[i][Globals.INDEX_CLR_BACK]);
            bw.newLine();
        }
        // save palettes
        if (colorMode == Magellan.COLOR_MODE_ECM_2 || colorMode == Magellan.COLOR_MODE_ECM_3) {
            bw.write("* PALETTES");
            bw.newLine();
            for (ECMPalette ecmPalette : ecmPalettes) {
                bw.write(Globals.KEY_PALETTE);
                Color[] colors = ecmPalette.getColors();
                for (int i = 0; i < colors.length; i++) {
                    bw.write(Integer.toHexString(colors[i].getRGB()));
                    if (i < colors.length - 1) {
                        bw.write("|");
                    }
                }
                bw.newLine();
            }
            // save character palette numbers
            bw.write("* CHAR PALETTE NUMBERS");
            bw.newLine();
            for (ECMPalette charPalette : ecmCharPalettes) {
                bw.write(Globals.KEY_CHARPALS);
                bw.write(Integer.toString(paletteMap.get(charPalette)));
                bw.newLine();
            }
            // save character transparency
            bw.write("* CHAR TRANSPARENCY");
            bw.newLine();
            for (boolean trans : ecmCharTransparency) {
                bw.write(Globals.KEY_CHARTRANS);
                bw.write(trans ? "1" : "0");
                bw.newLine();
            }
        }
        // save chardefs
        bw.write("* CHAR DEFS");
        bw.newLine();
        for (int i = TIGlobals.MIN_CHAR; i <= TIGlobals.MAX_CHAR; i++) {
            bw.write(Globals.KEY_CHARDATA);
            if (hmCharGrids.get(i) != null) {
                String hexstr = Globals.getHexString(hmCharGrids.get(i));
                bw.write(hexstr, 0, hexstr.length());
            }
            else {
                bw.write(Globals.BLANKCHAR, 0, Globals.BLANKCHAR.length());
            }
            bw.newLine();
        }
        if (colorMode == Magellan.COLOR_MODE_ECM_2 || colorMode == Magellan.COLOR_MODE_ECM_3) {
            bw.write("* CHAR DEFS PLANE 1");
            bw.newLine();
            for (int i = TIGlobals.MIN_CHAR; i <= TIGlobals.MAX_CHAR; i++) {
                bw.write(Globals.KEY_CHARDATA1);
                if (hmCharGrids.get(i) != null) {
                    String hexstr = Globals.getHexString(hmCharGrids.get(i), 2);
                    bw.write(hexstr, 0, hexstr.length());
                }
                else {
                    bw.write(Globals.BLANKCHAR, 0, Globals.BLANKCHAR.length());
                }
                bw.newLine();
            }
        }
        if (colorMode == Magellan.COLOR_MODE_ECM_3) {
            bw.write("* CHAR DEFS PLANE 2");
            bw.newLine();
            for (int i = TIGlobals.MIN_CHAR; i <= TIGlobals.MAX_CHAR; i++) {
                bw.write(Globals.KEY_CHARDATA2);
                if (hmCharGrids.get(i) != null) {
                    String hexstr = Globals.getHexString(hmCharGrids.get(i), 4);
                    bw.write(hexstr, 0, hexstr.length());
                }
                else {
                    bw.write(Globals.BLANKCHAR, 0, Globals.BLANKCHAR.length());
                }
                bw.newLine();
            }
        }
        // Save char colors (bitmap mode)
        if (colorMode == Magellan.COLOR_MODE_BITMAP) {
            bw.write("* CHAR COLORS");
            bw.newLine();
            for (int i = TIGlobals.MIN_CHAR; i <= TIGlobals.MAX_CHAR; i++) {
                bw.write(Globals.KEY_CHARCOLOR);
                int[][] charColors = hmCharColors.get(i);
                if (charColors != null) {
                    String hexstr = Globals.getColorHexString(charColors);
                    bw.write(hexstr, 0, hexstr.length());
                }
                else {
                    bw.write(Globals.BLANKCHAR, 0, Globals.BLANKCHAR.length());
                }
                bw.newLine();
            }
        }
        // save map parameters
        bw.write("* MAPS");
        bw.newLine();
        bw.write(Globals.KEY_MAPCOUNT + mapdMain.getMapCount());
        bw.newLine();
        // save map(s)
        for (int m = 0; m < mapdMain.getMapCount(); m++) {
            bw.write("* MAP #" + (m + 1));
            bw.newLine();
            bw.write(Globals.KEY_MAPSTART);
            bw.newLine();
            int[][] mapToSave = mapdMain.getMapData(m);
            bw.write(Globals.KEY_MAPSIZE + mapToSave[0].length + "|" + mapToSave.length);
            bw.newLine();
            bw.write(Globals.KEY_MAPBACK + mapdMain.getScreenColor(m));
            bw.newLine();
            for (int y = 0; y < mapToSave.length; y++) {
                bw.write(Globals.KEY_MAPDATA);
                for (int x = 0; x < mapToSave[y].length; x++) {
                    bw.write((x > 0 ? "|" : "") + mapToSave[y][x]);
                }
                bw.newLine();
            }
            bw.write("* SPRITE LOCATIONS");
            bw.newLine();
            HashMap<Point, ArrayList<Integer>> spriteMap = mapdMain.getSpriteMap(m);
            for (Point p : spriteMap.keySet()) {
                ArrayList<Integer> spriteList = spriteMap.get(p);
                for (Integer spriteNum : spriteList) {
                    bw.write(Globals.KEY_SPRITE_LOCATION + p.x + "|" + p.y + "|" + spriteNum);
                    bw.newLine();
                }
            }
            bw.write(Globals.KEY_MAPEND);
            bw.newLine();
        }
        // Save sprites
        bw.write("* SPRITES PATTERNS");
        bw.newLine();
        for (int i = TIGlobals.MIN_SPRITE; i <= TIGlobals.MAX_SPRITE; i++) {
            bw.write(Globals.KEY_SPRITE_PATTERN);
            if (hmSpriteGrids.get(i) != null) {
                bw.write(Globals.getHexString(hmSpriteGrids.get(i)));
            }
            else {
                bw.write(Globals.BLANKSPRITE);
            }
            bw.newLine();
        }
        if (colorMode == Magellan.COLOR_MODE_ECM_2 || colorMode == Magellan.COLOR_MODE_ECM_3) {
            bw.write("* SPRITES PATTERNS PLANE 1");
            bw.newLine();
            for (int i = TIGlobals.MIN_SPRITE; i <= TIGlobals.MAX_SPRITE; i++) {
                bw.write(Globals.KEY_SPRITE_PATTERN1);
                if (hmSpriteGrids.get(i) != null) {
                    bw.write(Globals.getHexString(hmSpriteGrids.get(i), 2));
                }
                else {
                    bw.write(Globals.BLANKSPRITE);
                }
                bw.newLine();
            }
        }
        if (colorMode == Magellan.COLOR_MODE_ECM_3) {
            bw.write("* SPRITES PATTERNS PLANE 2");
            bw.newLine();
            for (int i = TIGlobals.MIN_SPRITE; i <= TIGlobals.MAX_SPRITE; i++) {
                bw.write(Globals.KEY_SPRITE_PATTERN2);
                if (hmSpriteGrids.get(i) != null) {
                    bw.write(Globals.getHexString(hmSpriteGrids.get(i), 4));
                }
                else {
                    bw.write(Globals.BLANKSPRITE);
                }
                bw.newLine();
            }
        }
        bw.write("* SPRITE COLORS/PALETTES");
        bw.newLine();
        for (int i = TIGlobals.MIN_SPRITE; i <= TIGlobals.MAX_SPRITE; i++) {
            bw.write(Globals.KEY_SPRITE_COLOR);
            if (colorMode == Magellan.COLOR_MODE_GRAPHICS_1 || colorMode == Magellan.COLOR_MODE_BITMAP) {
                bw.write(Integer.toString(spriteColors[i]));
            }
            else {
                bw.write(Integer.toString(paletteMap.get(ecmSpritePalettes[i])));
            }
            bw.newLine();
        }
        bw.flush();
        bw.close();
    }

    protected void writeXBDataFile(File mapDataFile, int startChar, int endChar, int codeLine, int charLine, int mapLine, int interLine, int exportType, boolean includeComments, boolean currMapOnly, boolean excludeBlank) throws IOException {
        int currLine = codeLine;
        int itemCount = 0;
        int colorDataStart = 0;
        int colorSetStart = (int) (Math.floor(startChar / 8));
        int colorSetEnd = (int) (Math.floor(endChar / 8));
        int colorSetNum = (int) (Math.floor((startChar - TIGlobals.BASIC_FIRST_CHAR) / 8)) + 1;
        int colorCount = (colorSetEnd - colorSetStart) + 1;
        int charDataStart;
        int charCount = 0;
        int[] mapDataStart = new int[mapdMain.getMapCount()];
        int mapCols;
        int mapRows;
        StringBuffer sbOutLine = new StringBuffer();
        charDataStart=charLine;

        mapdMain.storeCurrentMap();
        BufferedWriter bw = new BufferedWriter(new FileWriter(mapDataFile));
        if (exportType == Globals.XB_PROGRAM || exportType == Globals.XB256_PROGRAM || exportType == Globals.BASIC_DATA ) {
            colorDataStart = currLine;
            if (includeComments) {
                bw.write("REM COLORSET DECLARATIONS");
                bw.newLine();
                bw.newLine();
            }
            int blockCount = 0;
            bw.write(currLine + " DATA ");
            for (int i = colorSetStart; i < (colorSetStart + colorCount); i++) {
                if (blockCount > 7) {
                    bw.newLine();
                    currLine = currLine + interLine;
                    bw.write(currLine + " DATA ");
                    blockCount = 0;
                }
                if (blockCount > 0) {
                    bw.write(",");
                }
                bw.write(colorSetNum + "," + (clrSets[i][Globals.INDEX_CLR_FORE] + 1) + "," + (clrSets[i][Globals.INDEX_CLR_BACK] + 1));
                colorSetNum++;
                blockCount++;
            }
            bw.newLine();
            currLine = currLine + interLine;
            if (includeComments) {
                bw.newLine();
                bw.write("REM CHARACTER DATA");
                bw.newLine();
                bw.newLine();
            }
            currLine = charLine;

            for (int i = startChar; i <= endChar; i++) {
                String hexstr;
                if (hmCharGrids.get(i) != null) {
                    hexstr = Globals.getHexString(hmCharGrids.get(i));
                } else {
                    hexstr = Globals.BLANKCHAR;
                }
                if (!hexstr.equals(Globals.BLANKCHAR) || !excludeBlank) {
                    if (itemCount == 0) {
                        sbOutLine.append(currLine + " DATA ");
                    } else {
                        sbOutLine.append(",");
                    }
                    sbOutLine.append(i + "," + '"' + hexstr.toUpperCase() + '"');
                    itemCount++;
                    if (itemCount >= 4) {
                        bw.write(sbOutLine.toString());
                        bw.newLine();
                        sbOutLine.delete(0, sbOutLine.length());
                        currLine = currLine + interLine;
                        itemCount = 0;
                    }
                    charCount++;
                }
            }
            if (sbOutLine.length() > 0) {
                bw.write(sbOutLine.toString());
                bw.newLine();
            }
            currLine = currLine + interLine;
            itemCount = 0;
            sbOutLine.delete(0, sbOutLine.length());
            currLine = mapLine;
            if (includeComments) {
                bw.newLine();
                bw.write("REM MAP DATA");
                bw.newLine();
            }
            int mapTicker = 1;
            for (int m = 0; m < mapdMain.getMapCount(); m++) {
                if (!currMapOnly || m == mapdMain.getCurrentMapId()) {
                    int[][] mapToSave = mapdMain.getMapData(m);
                    mapCols = mapToSave[0].length;
                    mapRows = mapToSave.length;
                    mapDataStart[m] = currLine;
                    if (includeComments) {
                        bw.newLine();
                        bw.write("REM MAP #" + mapTicker);
                        bw.newLine();
                    }
                    int mapColor = mapdMain.getScreenColorTI(m);
                    if (includeComments) {
                        bw.write("REM MAP #" + mapTicker + " WIDTH, HEIGHT, SCREEN COLOR");
                        bw.newLine();
                    }
                    bw.write(currLine + " DATA " + mapCols + "," + mapRows + "," + mapColor);
                    bw.newLine();
                    if (includeComments) {
                        bw.write("REM MAP #" + mapTicker + " DATA");
                        bw.newLine();
                    }
                    currLine = currLine + interLine;
                    int mapTileVal = TIGlobals.SPACECHAR;
                    for (int y = 0; y < mapRows; y++) {
                        for (int x = 0; x < mapCols; x++) {
                            if (itemCount == 0) {
                                sbOutLine.append(currLine + " DATA ");
                            } else {
                                sbOutLine.append(",");
                            }
                            mapTileVal = ((mapToSave[y][x] < startChar || mapToSave[y][x] > endChar) ? TIGlobals.SPACECHAR : mapToSave[y][x]);
                            sbOutLine.append("" + mapTileVal);
                            itemCount++;
                            if (itemCount >= 16) {
                                bw.write(sbOutLine.toString());
                                bw.newLine();
                                sbOutLine.delete(0, sbOutLine.length());
                                currLine = currLine + interLine;
                                itemCount = 0;
                            }
                        }
                    }
                    mapTicker++;
                }
            }
        }
        if (exportType == Globals.XB_PROGRAM || exportType == Globals.XB256_PROGRAM) {
            if (exportType == Globals.XB256_PROGRAM) {
                bw.newLine();
                bw.write(currLine + " CALL LINK(\"SCRN2\")");
                currLine = currLine + interLine;
                bw.newLine();
            }
            if (includeComments) {
                bw.newLine();
                bw.write("REM LOAD COLORSET");
                bw.newLine();
                bw.newLine();
            }
            bw.write(currLine + " RESTORE " + colorDataStart + "::FOR C=1 TO " + colorCount + "::READ CS,CF,CB::" + (exportType == Globals.XB_PROGRAM ? "CALL COLOR(CS,CF,CB)" : "CALL LINK(\"COLOR2\",CS,CF,CB)") + "::NEXT C");
            currLine = currLine + interLine;
            bw.newLine();
            if (includeComments) {
                bw.newLine();
                bw.write("REM LOAD CHARACTERS");
                bw.newLine();
                bw.newLine();
            }
            bw.write(currLine + " RESTORE " + charDataStart + "::FOR C=1 TO " + charCount + "::READ CN,CC$::" + (exportType == Globals.XB_PROGRAM ? "CALL CHAR(CN,CC$)" : "CALL LINK(\"CHAR2\",CN,CC$)") + "::NEXT C");
            currLine = currLine + interLine;
            bw.newLine();
            if (includeComments) {
                bw.newLine();
                bw.write("REM DRAW MAP(S)");
                bw.newLine();
                bw.newLine();
            }
            for (int mp = 0; mp < mapdMain.getMapCount(); mp++) {
                if (!currMapOnly || mp == mapdMain.getCurrentMapId()) {
                    bw.write(currLine + " CALL CLEAR");
                    currLine = currLine + interLine;
                    bw.newLine();
                    bw.write(currLine + " RESTORE " + mapDataStart[mp]);
                    currLine = currLine + interLine;
                    bw.newLine();
                    bw.write(currLine + " READ W,H,SC::CALL SCREEN(SC)::CALL CLEAR");
                    currLine = currLine + interLine;
                    bw.newLine();
                    bw.write(currLine + " FOR Y=1 TO H");
                    currLine = currLine + interLine;
                    bw.newLine();
                    bw.write(currLine + " FOR X=1 TO W");
                    currLine = currLine + interLine;
                    bw.newLine();
                    bw.write(currLine + " READ CP::CALL VCHAR(Y,X,CP)");
                    currLine = currLine + interLine;
                    bw.newLine();
                    bw.write(currLine + " NEXT X");
                    currLine = currLine + interLine;
                    bw.newLine();
                    bw.write(currLine + " NEXT Y");
                    currLine = currLine + interLine;
                    bw.newLine();
                    bw.write(currLine + " CALL KEY(0,K,S)::IF S=0 THEN " + currLine);
                    currLine = currLine + interLine;
                    bw.newLine();
                }
            }
            bw.write(currLine + " END");
            currLine = currLine + interLine;
            bw.newLine();
        }

        if (exportType == Globals.BASIC_PROGRAM) {


            currLine = codeLine;


            if (includeComments) {
                bw.newLine();
                bw.write("REM Define Color Set");
                bw.newLine();
                bw.newLine();
            }

                colorSetNum = (int) (Math.floor((startChar - TIGlobals.BASIC_FIRST_CHAR) / 8)) + 1;

            for (int i = colorSetStart; i < (colorSetStart + colorCount); i++) {

                bw.write(currLine + " CALL COLOR(");
                bw.write(colorSetNum + "," + (clrSets[i][Globals.INDEX_CLR_FORE] + 1) + "," + (clrSets[i][Globals.INDEX_CLR_BACK] + 1));
                colorSetNum++;
                bw.write(")");
                bw.newLine();
                currLine = currLine + interLine;
            }
            bw.newLine();
            currLine = currLine + interLine;

            currLine = charLine;
            if (includeComments) {
                bw.newLine();
                bw.write("REM Define Characters");
                bw.newLine();
                bw.newLine();
            }


            for (int i = startChar; i <= endChar; i++) {
                String hexstr;
                if (hmCharGrids.get(i) != null) {
                    hexstr = Globals.getHexString(hmCharGrids.get(i));
                } else {
                    hexstr = Globals.BLANKCHAR;
                }

                sbOutLine.delete(0, sbOutLine.length());
                if (!hexstr.equals(Globals.BLANKCHAR) || !excludeBlank) {
                    sbOutLine.append(currLine + " CALL CHAR(");
                    sbOutLine.append(i + "," + '"' + hexstr.toUpperCase() + '"');
                    sbOutLine.append(")");
                    bw.write(sbOutLine.toString());
                    bw.newLine();
                    sbOutLine.delete(0, sbOutLine.length());
                    currLine = currLine + interLine;
                }
            }
            if (sbOutLine.length() > 0) {
                bw.write(sbOutLine.toString());
                bw.newLine();
            }

            if (currLine > mapLine) {
                currLine += 100;
            } else {
                currLine = mapLine;
            }

            if (includeComments) {
                bw.newLine();
                bw.write("REM PRINT Map");
                bw.newLine();
            }


            int mapTicker = 1;
            for (int m = 0; m < mapdMain.getMapCount(); m++) {
                if (!currMapOnly || m == mapdMain.getCurrentMapId()) {
                    int[][] mapToSave = mapdMain.getMapData(m);
                    mapCols = mapToSave[0].length;
                    mapRows = mapToSave.length;
                    if (m!=0){
                        currLine=100+((int)(currLine/100)*100);
                    };
                    mapDataStart[m] = currLine;
                    if (includeComments) {
                        bw.newLine();
                        bw.write("REM MAP #" + mapTicker);
                        bw.newLine();
                    }


                    int endChar2 = endChar;
                    if (endChar2 > 127) {
                        endChar2 = 127;
                    }

                    int mapTileVal = TIGlobals.SPACECHAR;

                    for (int y = 0; y < mapRows - 1; y++) {
                        Boolean AllSpaces=true;
                        for (int x = 2; x < mapCols - 2; x++) {
                            mapTileVal = ((mapToSave[y][x] > endChar2) ? TIGlobals.SPACECHAR : mapToSave[y][x]);
                            if (mapTileVal == 34) {
                                mapTileVal = TIGlobals.SPACECHAR;
                            }
                            if (mapTileVal!=TIGlobals.SPACECHAR)
                            {
                                AllSpaces=false;
                            }
                        }
                        if (AllSpaces)
                        {
                            if (itemCount > 0) {
                                sbOutLine.append('"');
                                bw.write(sbOutLine.toString());
                                bw.newLine();
                                sbOutLine.delete(0, sbOutLine.length());
                                itemCount = 0;
                            }
                            bw.write(currLine + " PRINT ");
                            currLine = currLine + interLine;
                            bw.newLine();
                        }
                        else {
                            for (int x = 2; x < mapCols - 2; x++) {
                                if (itemCount == 0) {
                                    sbOutLine.append(currLine + " PRINT " + '"');
                                    currLine = currLine + interLine;
                                }

                                mapTileVal = ((mapToSave[y][x] > endChar2) ? TIGlobals.SPACECHAR : mapToSave[y][x]);
                                if (mapTileVal == 34) {
                                    mapTileVal = TIGlobals.SPACECHAR;
                                }
                                sbOutLine.append((char) mapTileVal);
                                itemCount++;
                                if (itemCount >= 84) {
                                    sbOutLine.append('"');
                                    bw.write(sbOutLine.toString());
                                    bw.newLine();
                                    sbOutLine.delete(0, sbOutLine.length());
                                    itemCount = 0;
                                }
                            }
                        }
                    }
                    if (itemCount != 0) {
                        sbOutLine.append('"');
                        bw.write(sbOutLine.toString());
                        bw.newLine();
                        sbOutLine.delete(0, sbOutLine.length());
                        itemCount = 0;
                    }

                    mapTicker++;
                }
            }

            if (itemCount != 0) {
                sbOutLine.append('"');
                bw.write(sbOutLine.toString());
                bw.newLine();
                sbOutLine.delete(0, sbOutLine.length());
                itemCount = 0;
            }

            currLine += 100;

            int[] mapFillChars = new int[mapdMain.getMapCount() + 1];
            int[] illegalChars = new int[mapdMain.getMapCount() + 1];
            int totalIllegalChars=0;
            mapTicker = 1;
            for (int m = 0; m < mapdMain.getMapCount(); m++) {
                if (!currMapOnly || m == mapdMain.getCurrentMapId()) {
                    int[][] mapToSave = mapdMain.getMapData(m);
                    mapCols = mapToSave[0].length;
                    mapRows = mapToSave.length;


                    if (includeComments) {
                        bw.newLine();
                        bw.write("REM Optional MAP #" + mapTicker + " DATA");
                        bw.newLine();
                    }

                    mapDataStart[m] = currLine;

                    int mapTileVal = TIGlobals.SPACECHAR;
                    for (int y = 0; y < mapRows; y++) {
                        for (int x = 0; x < mapCols; x++) {

                            mapTileVal = ((mapToSave[y][x] < 33 || mapToSave[y][x] > endChar) ? TIGlobals.SPACECHAR : mapToSave[y][x]);
                            if (y == 23 || x < 2 || x > 29 || mapTileVal > 127 || mapTileVal == 34) {

                                if (mapTileVal != TIGlobals.SPACECHAR) {
                                    illegalChars[m]+=1;
                                    totalIllegalChars++;
                                    if (itemCount == 0) {
                                        sbOutLine.append(currLine + " DATA ");
                                    } else {
                                        sbOutLine.append(",");
                                    }
                                    sbOutLine.append((y + 1) + "," + (x + 1) + "," + mapTileVal);
                                    mapFillChars[mapTicker] += 1;
                                    itemCount++;
                                }
                            }
                            if (itemCount >= 8) {
                                bw.write(sbOutLine.toString());
                                bw.newLine();
                                sbOutLine.delete(0, sbOutLine.length());
                                currLine = currLine + interLine;
                                itemCount = 0;
                            }
                        }
                    }
                    if (itemCount >= 0) {
                        bw.write(sbOutLine.toString());
                        bw.newLine();
                        sbOutLine.delete(0, sbOutLine.length());
                        currLine = currLine + interLine;
                        itemCount = 0;
                    }
                    mapTicker++;
                }
            }

            if (totalIllegalChars!=0) {
                currLine += 10;

                if (includeComments) {
                    bw.newLine();
                    bw.write("REM Complete Map with borders and other illegal characters not supported by Print");
                    bw.newLine();
                    bw.write("REM If you did not use any illegal characters or borders then you can omit this code");
                    bw.newLine();
                    bw.newLine();
                }

                for (int mp = 0; mp < mapdMain.getMapCount(); mp++) {
                    if (illegalChars[mp] > 0) {
                        if (!currMapOnly || mp == mapdMain.getCurrentMapId()) {

                            bw.write(currLine + " RESTORE " + mapDataStart[mp]);
                            currLine = currLine + interLine;
                            bw.newLine();

                            bw.write(currLine + " FOR FILL=1 TO " + mapFillChars[mp + 1]);
                            currLine = currLine + interLine;
                            bw.newLine();
                            bw.write(currLine + " READ Y,X,CP");
                            currLine = currLine + interLine;
                            bw.newLine();
                            bw.write(currLine + " CALL VCHAR(Y,X,CP)");
                            currLine = currLine + interLine;
                            bw.newLine();
                            bw.write(currLine + " NEXT FILL");
                            currLine = currLine + interLine;

                            bw.newLine();
                            bw.write(currLine + " CALL KEY(0,K,S)");
                            currLine = currLine + interLine;

                            bw.newLine();
                            bw.write(currLine + " IF S=0 THEN " + (currLine - interLine));
                            currLine = currLine + interLine;
                            bw.newLine();
                        }
                    }
                }
                bw.write(currLine + " END");
                currLine = currLine + interLine;
                bw.newLine();
            }
        }

        bw.flush();
        bw.close();
    }

    protected void writeASMDataFile(File mapDataFile, int startChar, int endChar, int startSprite, int endSprite, int compression, boolean includeComments, boolean currMapOnly, boolean includeCharNumbers, boolean includeSpriteData) throws Exception {
        if ((compression == MagellanExportDialog.COMPRESSION_RLE_BYTE || compression == MagellanExportDialog.COMPRESSION_RLE_WORD) && endChar > 127) {
            throw new Exception("RLE Compression not supported for characters > 127.");
        }
        // Build ECM palette map
        Map<ECMPalette, Integer> paletteMap = new HashMap<ECMPalette, Integer>(16);
        {
            int n = 0;
            for (ECMPalette ecmPalette : ecmPalettes) {
                paletteMap.put(ecmPalette, n++);
            }
        }
        StringBuilder sbLine = new StringBuilder();
        BufferedWriter bw = null;
        mapdMain.storeCurrentMap();
        bw = new BufferedWriter(new FileWriter(mapDataFile));
        if (includeComments) {
            printPaddedLine(bw, "****************************************", false);
            printPaddedLine(bw, "* Colorset Definitions", false);
            printPaddedLine(bw, "****************************************", false);
        }
        if (colorMode == Magellan.COLOR_MODE_GRAPHICS_1) {
            int itemCount = 0;
            printPaddedLine(bw, "CLRNUM DATA " + (clrSets.length), includeComments);
            for (int i = 0; i < clrSets.length; i++) {
                if (itemCount == 0) {
                    if (i == 0) {
                        sbLine.append("CLRSET BYTE ");
                    }
                    else {
                        sbLine.append("       BYTE ");
                    }
                }
                if (itemCount > 0) {
                    sbLine.append(",");
                }
                sbLine.append(">");
                sbLine.append(Integer.toHexString(clrSets[i][Globals.INDEX_CLR_FORE]).toUpperCase());
                sbLine.append(Integer.toHexString(clrSets[i][Globals.INDEX_CLR_BACK]).toUpperCase());
                itemCount++;
                if (itemCount > 3) {
                    printPaddedLine(bw, sbLine.toString(), includeComments);
                    sbLine.delete(0, sbLine.length());
                    itemCount = 0;
                }
            }
        }
        else if (colorMode == Magellan.COLOR_MODE_BITMAP) {
            sbLine.delete(0, sbLine.length());
            for (int i = startChar; i <= endChar; i++) {
                int[][] charColors = hmCharColors.get(i);
                if (charColors != null) {
                    if (includeCharNumbers) {
                        printPaddedLine(bw, "CCH" + i + (i < 10 ? "  " : (i < 100 ? " " : "")) + " DATA " + i, includeComments);
                    }
                    sbLine.append("COL").append(i).append(i < 10 ? "  " : (i < 100 ? " " : "")).append(" DATA ");
                    for (int row = 0; row < 8; row += 2) {
                        sbLine.append(">");
                        int[] rowColors = charColors[row];
                        sbLine.append(Integer.toHexString(rowColors[1]).toUpperCase());
                        sbLine.append(Integer.toHexString(rowColors[0]).toUpperCase());
                        rowColors = charColors[row + 1];
                        sbLine.append(Integer.toHexString(rowColors[1]).toUpperCase());
                        sbLine.append(Integer.toHexString(rowColors[0]).toUpperCase());
                        if (row < 6) {
                            sbLine.append(",");
                        }
                    }
                    printPaddedLine(bw, sbLine.toString(), includeComments);
                    sbLine.delete(0, sbLine.length());
                }
            }
        }
        else if (colorMode == Magellan.COLOR_MODE_ECM_2 || colorMode == Magellan.COLOR_MODE_ECM_3) {
            sbLine.delete(0, sbLine.length());
            for (int i = 0; i < ecmPalettes.length; i++) {
                ECMPalette ecmPalette = ecmPalettes[i];
                for (int l = 0; l < ecmPalette.getSize() / 4; l++) {
                    if (l == 0) {
                        sbLine.append("PAL").append(i).append(i < 10 ? " " : "").append("  DATA ");
                    }
                    else {
                        sbLine.append("       DATA ");
                    }
                    for (int c = 0; c < 4; c++) {
                        Color color = ecmPalette.getColor(c + l * 4);
                        sbLine.append(">0");
                        sbLine.append(Integer.toHexString((int) Math.round((double) color.getRed() / 17)).toUpperCase());
                        sbLine.append(Integer.toHexString((int) Math.round((double) color.getGreen() / 17)).toUpperCase());
                        sbLine.append(Integer.toHexString((int) Math.round((double) color.getBlue() / 17)).toUpperCase());
                        if (c < 3) {
                            sbLine.append(",");
                        }
                    }
                    printPaddedLine(bw, sbLine.toString(), includeComments);
                    sbLine.delete(0, sbLine.length());
                }
            }
            if (includeComments) {
                printPaddedLine(bw, "****************************************", false);
                printPaddedLine(bw, "* Tile Attributes", false);
                printPaddedLine(bw, "****************************************", false);
            }
            sbLine.delete(0, sbLine.length());
            for (int i = startChar; i <= endChar; i++) {
                sbLine.append("TAT").append(i).append(i < 10 ? "   " : (i < 100 ? "  " : " "));
                sbLine.append("BYTE >").append(Globals.toHexString((paletteMap.get(ecmCharPalettes[i]) << (colorMode == Magellan.COLOR_MODE_ECM_3 ? 1 : 0)) | (ecmCharTransparency[i] ? 0x10 : 0), 2));
                printPaddedLine(bw, sbLine.toString(), includeComments);
                sbLine.delete(0, sbLine.length());
            }
        }
        if (sbLine.length() > 0) {
            printPaddedLine(bw, sbLine.toString(), includeComments);
            sbLine.delete(0, sbLine.length());
        }
        if (includeComments) {
            printPaddedLine(bw, "****************************************", false);
            printPaddedLine(bw, "* Character Patterns" + (colorMode == Magellan.COLOR_MODE_ECM_2 || colorMode == Magellan.COLOR_MODE_ECM_3 ? " Plane 0" : ""), false);
            printPaddedLine(bw, "****************************************", false);
        }
        sbLine.delete(0, sbLine.length());
        for (int i = startChar; i <= endChar; i++) {
            String hexstr = Globals.BLANKCHAR;
            if (hmCharGrids.get(i) != null) {
                hexstr = Globals.getHexString(hmCharGrids.get(i)).toUpperCase();
            }
            if (includeCharNumbers) {
                printPaddedLine(bw, "PCH" + i + (i < 10 ? "  " : (i < 100 ? " " : "")) + " DATA " + i, includeComments);
            }
            sbLine.append((colorMode == Magellan.COLOR_MODE_GRAPHICS_1 || colorMode == Magellan.COLOR_MODE_BITMAP ? "PAT" : "P0_") + i + (i < 10 ? "  " : (i < 100 ? " " : "")) + " DATA ");
            sbLine.append(">" + hexstr.substring(0, 4) + ",");
            sbLine.append(">" + hexstr.substring(4, 8) + ",");
            sbLine.append(">" + hexstr.substring(8, 12) + ",");
            sbLine.append(">" + hexstr.substring(12, 16));
            printPaddedLine(bw, sbLine.toString(), includeComments);
            sbLine.delete(0, sbLine.length());
        }
        if (colorMode == Magellan.COLOR_MODE_ECM_2 || colorMode == Magellan.COLOR_MODE_ECM_3) {
            if (includeComments) {
                printPaddedLine(bw, "****************************************", false);
                printPaddedLine(bw, "* Character Patterns Plane 1", false);
                printPaddedLine(bw, "****************************************", false);
            }
            for (int i = startChar; i <= endChar; i++) {
                String hexstr = Globals.BLANKCHAR;
                if (hmCharGrids.get(i) != null) {
                    hexstr = Globals.getHexString(hmCharGrids.get(i), 2).toUpperCase();
                }
                sbLine.append("P1_" + i + (i < 10 ? "  " : (i < 100 ? " " : "")) + " DATA ");
                sbLine.append(">" + hexstr.substring(0, 4) + ",");
                sbLine.append(">" + hexstr.substring(4, 8) + ",");
                sbLine.append(">" + hexstr.substring(8, 12) + ",");
                sbLine.append(">" + hexstr.substring(12, 16));
                printPaddedLine(bw, sbLine.toString(), includeComments);
                sbLine.delete(0, sbLine.length());
            }
        }
        if (colorMode == Magellan.COLOR_MODE_ECM_3) {
            if (includeComments) {
                printPaddedLine(bw, "****************************************", false);
                printPaddedLine(bw, "* Character Patterns Plane 2", false);
                printPaddedLine(bw, "****************************************", false);
            }
            for (int i = startChar; i <= endChar; i++) {
                String hexstr = Globals.BLANKCHAR;
                if (hmCharGrids.get(i) != null) {
                    hexstr = Globals.getHexString(hmCharGrids.get(i), 4).toUpperCase();
                }
                sbLine.append("P2_" + i + (i < 10 ? "  " : (i < 100 ? " " : "")) + " DATA ");
                sbLine.append(">" + hexstr.substring(0, 4) + ",");
                sbLine.append(">" + hexstr.substring(4, 8) + ",");
                sbLine.append(">" + hexstr.substring(8, 12) + ",");
                sbLine.append(">" + hexstr.substring(12, 16));
                printPaddedLine(bw, sbLine.toString(), includeComments);
                sbLine.delete(0, sbLine.length());
            }
        }
        if (includeSpriteData) {
            if (includeComments) {
                printPaddedLine(bw, "****************************************", false);
                printPaddedLine(bw, "* Sprite Patterns" + (colorMode == Magellan.COLOR_MODE_ECM_2 || colorMode == Magellan.COLOR_MODE_ECM_3 ? " Plane 0" : ""), false);
                printPaddedLine(bw, "****************************************", false);
            }
            sbLine.delete(0, sbLine.length());
            for (int i = startSprite; i <= endSprite; i++) {
                if (hmSpriteGrids.get(i) != null) {
                    String hexstr = Globals.getSpriteHexString(hmSpriteGrids.get(i)).toUpperCase();
                    sbLine.append((colorMode == Magellan.COLOR_MODE_GRAPHICS_1 || colorMode == Magellan.COLOR_MODE_BITMAP ? "SPR" : "S0_") + i + (i < 10 ? "  " : (i < 100 ? " " : "")) + " DATA ");
                    for (int pos = 0; pos < 64; pos += 4) {
                        if (pos > 0 && pos % 16 == 0) {
                            sbLine.append("       DATA ");
                        }
                        sbLine.append(">" + hexstr.substring(pos, pos + 4) + (pos % 16 != 12 ? "," : ""));
                        if (pos % 16 == 12) {
                            printPaddedLine(bw, sbLine.toString(), includeComments ? (pos == 12 ? "Color " + spriteColors[i] : "") : null);
                            sbLine.delete(0, sbLine.length());
                        }
                    }
                }
            }
            if (colorMode == Magellan.COLOR_MODE_ECM_2 || colorMode == Magellan.COLOR_MODE_ECM_3) {
                if (includeComments) {
                    printPaddedLine(bw, "****************************************", false);
                    printPaddedLine(bw, "* Sprite Patterns Plane 1", false);
                    printPaddedLine(bw, "****************************************", false);
                }
                sbLine.delete(0, sbLine.length());
                for (int i = startSprite; i <= endSprite; i++) {
                    if (hmSpriteGrids.get(i) != null) {
                        String hexstr = Globals.getSpriteHexString(hmSpriteGrids.get(i), 2).toUpperCase();
                        sbLine.append("S1_" + i + (i < 10 ? "  " : (i < 100 ? " " : "")) + " DATA ");
                        for (int pos = 0; pos < 64; pos += 4) {
                            if (pos > 0 && pos % 16 == 0) {
                                sbLine.append("       DATA ");
                            }
                            sbLine.append(">" + hexstr.substring(pos, pos + 4) + (pos % 16 != 12 ? "," : ""));
                            if (pos % 16 == 12) {
                                printPaddedLine(bw, sbLine.toString(), includeComments);
                                sbLine.delete(0, sbLine.length());
                            }
                        }
                    }
                }
            }
            if (colorMode == Magellan.COLOR_MODE_ECM_3) {
                if (includeComments) {
                    printPaddedLine(bw, "****************************************", false);
                    printPaddedLine(bw, "* Sprite Patterns Plane 2", false);
                    printPaddedLine(bw, "****************************************", false);
                }
                sbLine.delete(0, sbLine.length());
                for (int i = startSprite; i <= endSprite; i++) {
                    if (hmSpriteGrids.get(i) != null) {
                        String hexstr = Globals.getSpriteHexString(hmSpriteGrids.get(i), 4).toUpperCase();
                        sbLine.append("S2_" + i + (i < 10 ? "  " : (i < 100 ? " " : "")) + " DATA ");
                        for (int pos = 0; pos < 64; pos += 4) {
                            if (pos > 0 && pos % 16 == 0) {
                                sbLine.append("       DATA ");
                            }
                            sbLine.append(">" + hexstr.substring(pos, pos + 4) + (pos % 16 != 12 ? "," : ""));
                            if (pos % 16 == 12) {
                                printPaddedLine(bw, sbLine.toString(), includeComments);
                                sbLine.delete(0, sbLine.length());
                            }
                        }
                    }
                }
            }
        }
        if (includeComments) {
            printPaddedLine(bw, "****************************************", false);
            printPaddedLine(bw, "* Map Data", false);
            printPaddedLine(bw, "****************************************", false);
        }
        printPaddedLine(bw, "MCOUNT DATA " + (currMapOnly ? 1 : mapdMain.getMapCount()), includeComments);
        for (int m = 0; m < mapdMain.getMapCount(); m++) {
            if (!currMapOnly || m == mapdMain.getCurrentMapId()) {
                int[][] mapToSave = mapdMain.getMapData(m);
                if (includeComments) {
                    printPaddedLine(bw, "* == Map #" + m + " == ", false);
                }
                printPaddedLine(bw, "MC" + m + (m < 10 ? "   " : (m < 100 ? "  " : (m < 1000 ? " " : ""))) + " DATA " + mapdMain.getScreenColor(m), includeComments);
                sbLine.delete(0, sbLine.length());
                sbLine.append("MS").append(m).append(m < 10 ? "   " : (m < 100 ? "  " : (m < 1000 ? " " : ""))).append(" DATA >");
                sbLine.append(Globals.toHexString(mapToSave[0].length, 4));
                sbLine.append(",>").append(Globals.toHexString(mapToSave.length, 4));
                sbLine.append(",>").append(Globals.toHexString(mapToSave[0].length * mapToSave.length, 4));
                printPaddedLine(bw, sbLine.toString(), includeComments ? "Width, Height, Size" : null);
                sbLine.delete(0, sbLine.length());
                if (compression == MagellanExportDialog.COMPRESSION_NONE) {
                    boolean isFirstByte;
                    String hexChunk;
                    isFirstByte = true;
                    for (int y = 0; y < mapToSave.length; y++) {
                        if (includeComments) {
                            printPaddedLine(bw, "* -- Map Row " + y + " -- ", false);
                        }
                        for (int cl = 0; cl < Math.ceil((double) mapToSave[y].length / 8); cl++) {
                            if (y == 0 && cl == 0) {
                                sbLine.append("MD").append(m).append(m < 10 ? "   " : (m < 100 ? "  " : (m < 1000 ? " " : ""))).append(" DATA ");
                            }
                            else {
                                sbLine.append("       DATA ");
                            }
                            for (int colpos = (cl * 8); colpos < Math.min((cl + 1) * 8, mapToSave[y].length); colpos++) {
                                if (isFirstByte) {
                                    if (colpos > (cl * 8)) {
                                        sbLine.append(",");
                                    }
                                    sbLine.append(">");
                                }
                                hexChunk = Integer.toHexString(mapToSave[y][colpos]).toUpperCase();
                                if (mapToSave[y][colpos] == MapCanvas.NOCHAR) {
                                    hexChunk = "00";
                                }
                                sbLine.append(hexChunk.length() < 1 ? "00" : (hexChunk.length() < 2 ? "0" : "")).append(hexChunk);
                                isFirstByte = !isFirstByte;
                            }
                            if (!isFirstByte) {
                                sbLine.append("XX"); // If odd, pad with an illegal value
                                isFirstByte = !isFirstByte;
                            }
                            printPaddedLine(bw, sbLine.toString(), includeComments);
                            sbLine.delete(0, sbLine.length());
                        }
                    }
                }
                else if (compression == MagellanExportDialog.COMPRESSION_RLE_BYTE) {
                    // RLE compression (byte)
                    // We assume all characters are < 128. If msb is set, the next byte determines
                    // how many times (2 - 256) the current byte (with msb cleared) should be repeated.
                    // A repeat count of 0 is used as end marker.
                    System.out.println("Uncompressed size: " + (mapToSave.length * mapToSave[0].length));
                    int i = 0;
                    int n = 0;
                    int current;
                    int last = -1;
                    int count = 0;
                    for (int y = 0; y < mapToSave.length; y++) {
                        for (int x = 0; x < mapToSave[0].length; x++) {
                            current = mapToSave[y][x];
                            if (last != -1) {
                                if (current == last && count < 255) {
                                    // Same byte, increment count
                                    count++;
                                }
                                else if (count > 0) {
                                    // End of run of bytes
                                    i = printByte(bw, sbLine, i, last | 128);
                                    i = printByte(bw, sbLine, i, count);
                                    count = 0;
                                    n += 2;
                                }
                                else {
                                    // Different byte
                                    i = printByte(bw, sbLine, i, last);
                                    n++;
                                }
                            }
                            last = current;
                        }
                    }
                    if (count > 0) {
                        i = printByte(bw, sbLine, i, last | 128);
                        i = printByte(bw, sbLine, i, count);
                        n += 2;
                    }
                    else {
                        // Different byte
                        i = printByte(bw, sbLine, i, last);
                        n++;
                    }
                    // End marker
                    i = printByte(bw, sbLine, i, 128);
                    i = printByte(bw, sbLine, i, 0);
                    n += 2;
                    printPaddedLine(bw, sbLine.toString(), false);
                    n += i;
                    System.out.println("Compressed size: " + n);
                }
                else if (compression == MagellanExportDialog.COMPRESSION_RLE_WORD) {
                    // RLE compression (word)
                    // We assume all characters are < 128. If msb of the MSB is set, the byte following the
                    // current word determines how many times (2 - 256) the current word (with msb cleared)
                    // should be repeated. A repeat count of 0 is used as end marker.
                    System.out.println("Uncompressed size: " + (mapToSave.length * mapToSave[0].length));
                    int i = 0;
                    int n = 0;
                    int current;
                    int last = -1;
                    int count = 0;
                    for (int y = 0; y < mapToSave.length; y++) {
                        for (int x = 0; x < mapToSave[0].length - 1; x += 2) {
                            current = mapToSave[y][x] << 8 | mapToSave[y][x + 1];
                            if (last != -1) {
                                if (current == last && count < 255) {
                                    // Same word, increment count
                                    count++;
                                }
                                else if (count > 0) {
                                    // End of run of words
                                    i = printByte(bw, sbLine, i, (last & 0xFF00) >> 8 | 128);
                                    i = printByte(bw, sbLine, i, last & 0x00FF);
                                    i = printByte(bw, sbLine, i, count);
                                    count = 0;
                                    n += 3;
                                }
                                else {
                                    // Different byte
                                    i = printByte(bw, sbLine, i, (last & 0xFF00) >> 8);
                                    i = printByte(bw, sbLine, i, last & 0x00FF);
                                    n += 2;
                                }
                            }
                            last = current;
                        }
                    }
                    if (count > 0) {
                        i = printByte(bw, sbLine, i, (last & 0xFF00) >> 8 | 128);
                        i = printByte(bw, sbLine, i, last & 0x00FF);
                        i = printByte(bw, sbLine, i, count);
                        n += 2;
                    }
                    else {
                        // Different byte
                        i = printByte(bw, sbLine, i, (last & 0xFF00) >> 8);
                        i = printByte(bw, sbLine, i, last & 0x00FF);
                        n++;
                    }
                    // End marker
                    i = printByte(bw, sbLine, i, 128);
                    i = printByte(bw, sbLine, i, 0);
                    i = printByte(bw, sbLine, i, 0);
                    n += 3;
                    printPaddedLine(bw, sbLine.toString(), false);
                    n += i;
                    System.out.println("Compressed size: " + n);
                }
                else if (compression == MagellanExportDialog.COMPRESSION_META_2 || compression == MagellanExportDialog.COMPRESSION_META_4) {
                    int size = compression == MagellanExportDialog.COMPRESSION_META_2 ? 2 : 4;
                    int height = mapToSave.length / size;
                    int width = mapToSave[0].length / size;
                    Map<String, MetaTile> metaTileLookup = new HashMap<String, MetaTile>();
                    MetaTile[][] metaTileMap = new MetaTile[height][width];
                    int n = 0;
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            StringBuilder keyBuffer = new StringBuilder();
                            int[] tiles = new int[size * size];
                            for (int y1 = 0; y1 < size; y1++) {
                                for (int x1 = 0; x1 < size; x1++) {
                                    int tile = mapToSave[y * size + y1][x * size + x1];
                                    keyBuffer.append(Globals.toHexString(tile, 2));
                                    tiles[y1 * size + x1] = tile;
                                }
                            }
                            String key = keyBuffer.toString();
                            MetaTile metaTile = metaTileLookup.get(key);
                            if (metaTile == null) {
                                metaTile = new MetaTile(n++, tiles);
                                metaTileLookup.put(key, metaTile);
                            }
                            metaTileMap[y][x] = metaTile;
                        }
                    }
                    System.out.println("Number of meta tiles: " + metaTileLookup.size());
                    if (metaTileLookup.size() > 256) {
                        throw new Exception("Cannot support more than 256 meta tiles (" + metaTileLookup.size() + " found)");
                    }
                    boolean first = true;
                    n = 0;
                    for (MetaTile[] row : metaTileMap) {
                        for (MetaTile metaTile : row) {
                            n = printByte(bw, sbLine, n, metaTile.getNumber(), first ? "MD" + m : null);
                            first = false;
                        }
                    }
                    printPaddedLine(bw, sbLine.toString(), false);
                    if (includeComments) {
                        printPaddedLine(bw, "****************************************", false);
                        printPaddedLine(bw, "* Meta Tiles", false);
                        printPaddedLine(bw, "****************************************", false);
                    }
                    ArrayList<MetaTile> metaTiles = new ArrayList<MetaTile>(metaTileLookup.values());
                    Collections.sort(metaTiles, new Comparator<MetaTile>() {
                        public int compare(MetaTile m1, MetaTile m2) {
                            return m1.getNumber() - m2.getNumber();
                        }
                    });
                    for (MetaTile metaTile : metaTiles) {
                        sbLine.delete(0, sbLine.length());
                        n = 0;
                        first = true;
                        int[] tiles = metaTile.getTiles();
                        for (int tile : tiles) {
                            n = printByte(bw, sbLine, n, tile, first ? "MT" + m + Globals.padl(metaTile.getNumber(), 3) : null);
                            first = false;
                        }
                        if (metaTile.getTiles().length < 8) {
                            printPaddedLine(bw, sbLine.toString(), false);
                        }
                    }
                }
                else if (compression == MagellanExportDialog.COMPRESSION_NYBBLES) {
                    boolean isFirstByte;
                    String hexChunk;
                    for (int y = 0; y < mapToSave.length; y++) {
                        if (includeComments) {
                            printPaddedLine(bw, "* -- Map Row " + y + " -- ", false);
                        }
                        for (int cl = 0; cl < Math.ceil((double) mapToSave[y].length / 16); cl++) {
                            if (y == 0 && cl == 0) {
                                sbLine.append("MD").append(m).append(m < 10 ? "   " : (m < 100 ? "  " : (m < 1000 ? " " : ""))).append(" DATA ");
                            }
                            else {
                                sbLine.append("       DATA ");
                            }
                            for (int colpos = (cl * 16); colpos < Math.min((cl + 1) * 16, mapToSave[y].length); colpos++) {
                                if (colpos % 4 == 0) {
                                    if (colpos > (cl * 16)) {
                                        sbLine.append(",");
                                    }
                                    sbLine.append(">");
                                }
                                int value = mapToSave[y][colpos];
                                if (value >= 16) {
                                    throw new RuntimeException("Compression mode not supported for characters >= 16");
                                }
                                hexChunk = value != MapCanvas.NOCHAR ? Integer.toHexString(value).toUpperCase() : "0";
                                sbLine.append(hexChunk);
                            }
                            printPaddedLine(bw, sbLine.toString(), includeComments);
                            sbLine.delete(0, sbLine.length());
                        }
                    }
                }
                else {
                    throw new RuntimeException("Compression mode not yet supported.");
                }
                if (includeSpriteData) {
                    HashMap<Point, ArrayList<Integer>> spriteMap = mapdMain.getSpriteMap(m);
                    if (spriteMap.size() > 0) {
                        if (includeComments) {
                            printPaddedLine(bw, "* Sprite Locations", false);
                        }
                        boolean smallMap = mapdMain.getGridWidth() <= 32 && mapdMain.getGridHeight() <= 32;
                        boolean first = true;
                        for (Point p : spriteMap.keySet()) {
                            ArrayList<Integer> spriteList = spriteMap.get(p);
                            for (Integer spriteNum : spriteList) {
                                int color = (colorMode == Magellan.COLOR_MODE_GRAPHICS_1 || colorMode == Magellan.COLOR_MODE_BITMAP) ? spriteColors[spriteNum] : paletteMap.get(ecmSpritePalettes[spriteNum]) * (colorMode == Magellan.COLOR_MODE_ECM_3 ? 2 : 1);
                                printPaddedLine(bw, (first ? "SL" + m + (m < 10 ? "   " : (m < 100 ? "  " : (m < 1000 ? " " : ""))) : "      ") +
                                    (smallMap ? " BYTE " + ((p.y * 8 - 1) & 0xFF) : " DATA " + (p.y * 8)) + "," + (p.x * 8) + "," + spriteNum * 4 + "," + color, includeComments ? (first ? "y, x, pattern#, color#" : "") : null);
                                first = false;
                            }
                        }
                    }
                }
            }
        }
        bw.flush();
        bw.close();
    }

    protected void writeScrollFile(File mapDataFile, int orientation, boolean wrap, int compression, boolean includeComments, boolean currMapOnly, boolean includeCharNumbers, int frames, boolean animate) throws Exception {
        if (Magellan.ISOMETRIC && orientation == Magellan.SCROLL_ORIENTATION_ISOMETRIC) {
            writeIsometricFile(mapDataFile);
            return;
        }
        boolean vertical = orientation == Magellan.SCROLL_ORIENTATION_VERTICAL;
        // store working map first
        mapdMain.storeCurrentMap();
        BufferedWriter bw = null;
        ArrayList<int[][]> transMaps = new ArrayList<int[][]>();
        Map<String, TransChar> transChars = new HashMap<String, TransChar>();
        Map<Integer, ArrayList<TransChar>> colorSets = new TreeMap<Integer, ArrayList<TransChar>>();
        TransChar[] transCharSet = new TransChar[256];
        boolean[] usedChars = new boolean[256];
        int startChar = 255;
        int endChar = 0;
        int imax = 0;
        boolean allColorsOK = true;
        for (int m = 0; m < mapdMain.getMapCount(); m++) {
            if (!currMapOnly || m == mapdMain.getCurrentMapId()) {
                int[][] mapData = mapdMain.getMapData(m);
                if (mapData.length > 1 && mapData[0].length > 1) {
                    int i = 0;
                    for (int y = (vertical && !wrap ? 1 : 0); y < mapData.length; y++) {
                        for (int x = 0; x < mapData[0].length - (vertical || wrap ? 0 : 1); x++) {
                            int fromChar = mapData[y][x];
                            if (fromChar < startChar) {
                                startChar = fromChar;
                            }
                            if (fromChar > endChar) {
                                endChar = fromChar;
                            }
                            usedChars[fromChar] = true;
                            int toChar = vertical ? mapData[y > 0 ? y - 1 : mapData.length - 1][x] : mapData[y][x < mapData[0].length - 1 ? x + 1 : 0];
                            if (toChar < startChar) {
                                startChar = toChar;
                            }
                            if (toChar > endChar) {
                                endChar = toChar;
                            }
                            usedChars[toChar] = true;
                            String key = fromChar + "-" + toChar;
                            TransChar transChar = transChars.get(key);
                            if (transChar != null) {
                                transChar.incCount();
                            }
                            else {
                                boolean colorsOK = true;
                                boolean invert = false;
                                if (colorMode == Magellan.COLOR_MODE_BITMAP) {
                                    int[][] charColors = new int[8][8];
                                    if (!vertical) {
                                        int[][] fromColorGrid = hmCharColors.get(fromChar);
                                        int[][] toColorGrid = hmCharColors.get(toChar);
                                        for (int r = 0; r < 8 && colorsOK; r++) {
                                            int[] fromColorRow = fromColorGrid[r];
                                            int[] toColorRow = toColorGrid[r];
                                            int screenColor = mapdMain.getColorScreen();
                                            int fromForeColor = fromColorRow[Globals.INDEX_CLR_FORE] != 0 ? fromColorRow[Globals.INDEX_CLR_FORE] : screenColor;
                                            int toForeColor = toColorRow[Globals.INDEX_CLR_FORE] != 0 ? toColorRow[Globals.INDEX_CLR_FORE] : screenColor;
                                            if (fromForeColor == toForeColor) {
                                                charColors[r][Globals.INDEX_CLR_FORE] = fromForeColor;
                                            }
                                            else if (!Globals.arrayContains(hmCharGrids.get(fromChar)[r], Globals.INDEX_CLR_FORE)) {
                                                charColors[r][Globals.INDEX_CLR_FORE] = toForeColor;
                                            }
                                            else if (!Globals.arrayContains(hmCharGrids.get(toChar)[r], Globals.INDEX_CLR_FORE)) {
                                                charColors[r][Globals.INDEX_CLR_FORE] = fromForeColor;
                                            }
                                            else {
                                                charColors[r][Globals.INDEX_CLR_FORE] = fromForeColor;
                                                colorsOK = false;
                                                allColorsOK = false;
                                            }
                                            int fromBackColor = fromColorRow[Globals.INDEX_CLR_BACK] != 0 ? fromColorRow[Globals.INDEX_CLR_BACK] : screenColor;
                                            int toBackColor = toColorRow[Globals.INDEX_CLR_BACK] != 0 ? toColorRow[Globals.INDEX_CLR_BACK] : screenColor;
                                            if (fromBackColor == toBackColor) {
                                                charColors[r][Globals.INDEX_CLR_BACK] = fromBackColor;
                                            }
                                            else if (!Globals.arrayContains(hmCharGrids.get(fromChar)[r], Globals.INDEX_CLR_BACK)) {
                                                charColors[r][Globals.INDEX_CLR_BACK] = toBackColor;
                                            }
                                            else if (!Globals.arrayContains(hmCharGrids.get(toChar)[r], Globals.INDEX_CLR_BACK)) {
                                                charColors[r][Globals.INDEX_CLR_BACK] = fromBackColor;
                                            }
                                            else {
                                                charColors[r][Globals.INDEX_CLR_BACK] = fromBackColor;
                                                colorsOK = false;
                                                allColorsOK = false;
                                            }
                                        }
                                    }
                                    transChar = new TransChar(fromChar, toChar, i, colorsOK, charColors);
                                    transChars.put(key, transChar);
                                    transCharSet[i] = transChar;
                                    imax = i++;
                                    if (imax > 255) {
                                        throw new Exception("Character Set Full: Scrolling this map requires more than 256 characters.");
                                    }
                                }
                                else {
                                    int screenColor = mapdMain.getColorScreen();
                                    int[] fromClrSet = clrSets[fromChar / 8];
                                    int[] toClrSet = clrSets[toChar / 8];
                                    int foreColor;
                                    int backColor;
                                    int fromForeColor = fromClrSet[Globals.INDEX_CLR_FORE] != 0 ? fromClrSet[Globals.INDEX_CLR_FORE] : screenColor;
                                    int toForeColor = toClrSet[Globals.INDEX_CLR_FORE] != 0 ? toClrSet[Globals.INDEX_CLR_FORE] : screenColor;
                                    if (fromForeColor == toForeColor) {
                                        foreColor = fromForeColor;
                                    }
                                    else if (!Globals.arrayContains(hmCharGrids.get(fromChar), Globals.INDEX_CLR_FORE)) {
                                        foreColor = toForeColor;
                                    }
                                    else if (!Globals.arrayContains(hmCharGrids.get(toChar), Globals.INDEX_CLR_FORE)) {
                                        foreColor = fromForeColor;
                                    }
                                    else {
                                        foreColor = fromForeColor;
                                        colorsOK = false;
                                        // allColorsOK = false;
                                        System.out.println("Colors not OK: fromChar=" + fromChar + " toChar=" + toChar + " fromForeColor=" + fromForeColor + " toForeColor=" + toForeColor);
                                    }
                                    int fromBackColor = fromClrSet[Globals.INDEX_CLR_BACK] != 0 ? fromClrSet[Globals.INDEX_CLR_BACK] : screenColor;
                                    int toBackColor = toClrSet[Globals.INDEX_CLR_BACK] != 0 ? toClrSet[Globals.INDEX_CLR_BACK] : screenColor;
                                    if (fromBackColor == toBackColor) {
                                        backColor = fromBackColor;
                                    }
                                    else if (!Globals.arrayContains(hmCharGrids.get(fromChar), Globals.INDEX_CLR_BACK)) {
                                        backColor = toBackColor;
                                    }
                                    else if (!Globals.arrayContains(hmCharGrids.get(toChar), Globals.INDEX_CLR_BACK)) {
                                        backColor = fromBackColor;
                                    }
                                    else {
                                        backColor = fromBackColor;
                                        colorsOK = false;
                                        // allColorsOK = false;
                                        System.out.println("Colors not OK: fromChar=" + fromChar + " toChar=" + toChar + " fromBackColor=" + fromBackColor + " toBackColor=" + toBackColor);
                                    }
                                    if (!colorsOK && Magellan.INVERT_SUPPORTED) {
                                        // Invert color set and pattern of to character
                                        colorsOK = true;
                                        toForeColor = toClrSet[Globals.INDEX_CLR_BACK] != 0 ? toClrSet[Globals.INDEX_CLR_BACK] : screenColor;
                                        if (fromForeColor == toForeColor) {
                                            foreColor = fromForeColor;
                                        }
                                        else if (!Globals.arrayContains(hmCharGrids.get(fromChar), Globals.INDEX_CLR_FORE)) {
                                            foreColor = toForeColor;
                                        }
                                        else if (!Globals.arrayContains(hmCharGrids.get(toChar), Globals.INDEX_CLR_BACK)) {
                                            foreColor = fromForeColor;
                                        }
                                        else {
                                            foreColor = fromForeColor;
                                            colorsOK = false;
                                            allColorsOK = false;
                                        }
                                        toBackColor = toClrSet[Globals.INDEX_CLR_FORE] != 0 ? toClrSet[Globals.INDEX_CLR_FORE] : screenColor;
                                        if (fromBackColor == toBackColor) {
                                            backColor = fromBackColor;
                                        }
                                        else if (!Globals.arrayContains(hmCharGrids.get(fromChar), Globals.INDEX_CLR_BACK)) {
                                            backColor = toBackColor;
                                        }
                                        else if (!Globals.arrayContains(hmCharGrids.get(toChar), Globals.INDEX_CLR_FORE)) {
                                            backColor = fromBackColor;
                                        }
                                        else {
                                            backColor = fromBackColor;
                                            colorsOK = false;
                                            allColorsOK = false;
                                        }
                                        invert = colorsOK;
                                    }
                                    transChar = new TransChar(fromChar, toChar, -1, colorsOK, foreColor, backColor);
                                    transChar.setInvert(invert);
                                    transChars.put(key, transChar);
                                    int ckey = backColor + (foreColor << 4);
                                    ArrayList<TransChar> colorSet = colorSets.get(ckey);
                                    if (colorSet == null) {
                                        colorSet = new ArrayList<TransChar>();
                                        colorSets.put(ckey, colorSet);
                                    }
                                    colorSet.add(transChar);
                                }
                            }
                        }
                    }
                    if (colorMode != Magellan.COLOR_MODE_BITMAP) {
                        // Organize into color sets
                        i = 0;
                        for (int ckey : colorSets.keySet()) {
                            ArrayList<TransChar> colorSet = colorSets.get(ckey);
                            for (TransChar transChar : colorSet) {
                                transChar.setIndex(i);
                                transCharSet[i] = transChar;
                                imax = i++;
                                if (i > 255) {
                                    throw new Exception("Character Set Full: Scrolling this map requires more than 32 color sets.");
                                }
                                if (imax > 255) {
                                    throw new Exception("Character Set Full: Scrolling this map requires more than 256 characters.");
                                }
                            }
                            while (i % 8 != 0) {
                                i++;
                            }
                            if (i > 256) {
                                throw new Exception("Character Set Full: Scrolling this map requires more than 32 color sets.");
                            }
                        }
                    }
                    int[][] transMap = new int[mapData.length - (vertical && !wrap ? 1 : 0)][mapData[0].length - (vertical || wrap ? 0 : 1)];
                    transMaps.add(transMap);
                    for (int y = (vertical && !wrap ? 1 : 0); y < mapData.length; y++) {
                        for (int x = 0; x < mapData[0].length - (vertical || wrap ? 0 : 1); x++) {
                            int fromChar = mapData[y][x];
                            int toChar = vertical ? mapData[y > 0 ? y - 1 : mapData.length - 1][x] : mapData[y][x < mapData[0].length - 1 ? x + 1 : 0];
                            String key = fromChar + "-" + toChar;
                            TransChar transChar = transChars.get(key);
                            transMap[y - (vertical && !wrap ? 1 : 0)][x] = transChar.getIndex();
                        }
                    }
                }
            }
        }
        if (!allColorsOK) {
            JOptionPane.showMessageDialog(null, "Warning - Some character transitions have incompatible colors. This may cause color spills when the map is scrolled.", "Invalid Color Transitions", JOptionPane.INFORMATION_MESSAGE);
        }
        // Remap original characters
        ArrayList<Integer> remappedChars = new ArrayList<Integer>();
        TransChar[] remappedTransCharSet = new TransChar[transCharSet.length];
        for (int i = 0; i < transCharSet.length; i++) {
            TransChar transChar = transCharSet[i];
            remappedTransCharSet[i] = transChar != null ? new TransChar(transChar) : null;
        }
        int mapTo = 0;
        for (int mapFrom = startChar; mapFrom <= (Magellan.ANIMATE_SCROLLED_FRAMES ? hmCharGrids.size() - 1 : endChar); mapFrom++) {
            if (usedChars[mapFrom]) {
                remappedChars.add(mapFrom);
                for (int i = 0; i < remappedTransCharSet.length; i++) {
                    TransChar transChar = remappedTransCharSet[i];
                    if (transChar != null) {
                        if (transChar.getFromChar() == mapFrom) {
                            transChar.setFromChar(mapTo);
                        }
                        if (transChar.getToChar() == mapFrom) {
                            transChar.setToChar(mapTo);
                        }
                    }
                }
                mapTo++;
            }
        }
        // Write out result
        bw = new BufferedWriter(new FileWriter(mapDataFile));
        if (includeComments) {
            printPaddedLine(bw, "****************************************", false);
            printPaddedLine(bw, "* Original Character Patterns", false);
            printPaddedLine(bw, "****************************************", false);
        }
        for (int j = 0; j < remappedChars.size(); j++) {
            int i = remappedChars.get(j);
            String hexstr = Globals.BLANKCHAR;
            if (hmCharGrids.get(i) != null) {
                hexstr = Globals.getHexString(hmCharGrids.get(i)).toUpperCase();
            }
            if (includeCharNumbers) {
                printPaddedLine(bw, "PCH" + i + (i < 10 ? "  " : (i < 100 ? " " : "")) + " DATA >" + Globals.toHexString(i, 2), includeComments);
            }
            StringBuilder sbLine = new StringBuilder();
            sbLine.append("PAT").append(j).append(j < 10 ? "  " : (j < 100 ? " " : "")).append(" DATA ");
            sbLine.append(">").append(hexstr.substring(0, 4)).append(",");
            sbLine.append(">").append(hexstr.substring(4, 8)).append(",");
            sbLine.append(">").append(hexstr.substring(8, 12)).append(",");
            sbLine.append(">").append(hexstr.substring(12, 16));
            printPaddedLine(bw, sbLine.toString(), includeComments ? "#" + Globals.toHexString(j, 2) + (i != j ? " (" + Globals.toHexString(i, 2) + ")" : "") : null);
        }
        if (includeComments) {
            printPaddedLine(bw, "****************************************", false);
            printPaddedLine(bw, "* Colorset Definitions", false);
            printPaddedLine(bw, "****************************************", false);
        }
        if (colorMode == Magellan.COLOR_MODE_BITMAP) {
            for (int i = startChar; i <= (Magellan.ANIMATE_SCROLLED_FRAMES ? hmCharColors.size() - 1 : endChar); i++) {
                int[][] charColors = hmCharColors.get(i);
                if (charColors != null && !Globals.isColorGridEmpty(charColors)) {
                    if (includeCharNumbers) {
                        printPaddedLine(bw, "CCH" + i + (i < 10 ? "  " : (i < 100 ? " " : "")) + " DATA " + Globals.toHexString(i, 2), includeComments);
                    }
                    StringBuilder sbLine = new StringBuilder();
                    sbLine.append("COL").append(i).append(i < 10 ? "  " : (i < 100 ? " " : "")).append(" DATA ");
                    for (int row = 0; row < 8; row += 2) {
                        sbLine.append(">");
                        int[] rowColors = charColors[row];
                        sbLine.append(Integer.toHexString(rowColors[1]).toUpperCase());
                        sbLine.append(Integer.toHexString(rowColors[0]).toUpperCase());
                        rowColors = charColors[row + 1];
                        sbLine.append(Integer.toHexString(rowColors[1]).toUpperCase());
                        sbLine.append(Integer.toHexString(rowColors[0]).toUpperCase());
                        if (row < 6) {
                            sbLine.append(",");
                        }
                    }
                    printPaddedLine(bw, sbLine.toString(), includeComments ? (usedChars[i] ? "" : "unused") : null);
                }
            }
        }
        else {
            int nColorSets = 0;
            for (int ckey : colorSets.keySet()) {
                int size = colorSets.get(ckey).size();
                nColorSets += size / 8 + (size % 8 == 0 ? 0 : 1);
            }
            printPaddedLine(bw, "CLRNUM DATA " + nColorSets, includeComments);
            boolean first = true;
            StringBuilder sbLine = new StringBuilder();
            int itemCount = 0;
            for (int ckey : colorSets.keySet()) {
                int size = colorSets.get(ckey).size();
                for (int n = 0; n < size / 8 + (size % 8 == 0 ? 0 : 1); n++) {
                    if (itemCount == 0) {
                        if (first) {
                            sbLine.append("CLRSET BYTE ");
                            first = false;
                        }
                        else {
                            sbLine.append("       BYTE ");
                        }
                    }
                    if (itemCount > 0) {
                        sbLine.append(",");
                    }
                    sbLine.append(">");
                    sbLine.append(Globals.toHexString(ckey, 2));
                    itemCount++;
                    if (itemCount > 3) {
                        printPaddedLine(bw, sbLine.toString(), includeComments);
                        sbLine = new StringBuilder();
                        itemCount = 0;
                    }
                }
            }
            if (sbLine.length() > 0) {
                printPaddedLine(bw, sbLine.toString(), includeComments);
            }
        }
        if (includeComments) {
            printPaddedLine(bw, "****************************************", false);
            printPaddedLine(bw, "* Transition Character Pairs (from, to) ", false);
            printPaddedLine(bw, "****************************************", false);
        }
        printPaddedLine(bw, "TCHNUM DATA " + (imax + 1), includeComments);
        for (int i = 0; i <= imax; i++) {
            TransChar transChar = remappedTransCharSet[i];
            if (transChar != null) {
                printPaddedLine(bw,
                    (i == 0 ? "TCHARS" : "      ") + " BYTE >" + Globals.toHexString(transChar.getFromChar(), 2) + ",>" + Globals.toHexString(transChar.getToChar(), 2),
                    !includeComments ? null :
                        "#" + Globals.toHexString(transChar.getIndex(), 2) +
                        (colorMode != Magellan.COLOR_MODE_BITMAP ? " color " + Globals.toHexString(transChar.getForeColor(), 1) + "/" + Globals.toHexString(transChar.getBackColor(), 1) : "") +
                        (transChar.isInvert() ? " invert" : "") +
                        (transChar.isColorsOK() ? "" : " ERROR")
                );
            }
            else {
                printPaddedLine(bw, (i == 0 ? "TCHARS" : "      ") + " BYTE >FF,>FF", !includeComments ? null : "#" + Globals.toHexString(i, 2) + " unused");
            }
        }
        if (Magellan.INVERT_SUPPORTED && colorMode == Magellan.COLOR_MODE_GRAPHICS_1) {
            boolean found = false;
            for (int i = 0; i <= imax && !found; i++) {
                TransChar transChar = transCharSet[i];
                if (transChar != null && transChar.isInvert()) {
                    found = true;
                }
            }
            if (found) {
                if (includeComments) {
                    printPaddedLine(bw, "*************************************************", false);
                    printPaddedLine(bw, "* Transition chars with inverted 'to' characters ", false);
                    printPaddedLine(bw, "*************************************************", false);
                }
                for (int i = 0; i <= imax; i++) {
                    TransChar transChar = transCharSet[i];
                    if (transChar != null) {
                        printPaddedLine(bw,
                                (i == 0 ? "ICHARS" : "      ") + " BYTE >" + (transChar.isInvert() ? "FF" : "00"),
                                !includeComments ? null : "#" + Globals.toHexString(transChar.getIndex(), 2)
                        );
                    } else {
                        printPaddedLine(bw, (i == 0 ? "ICHARS" : "      ") + " BYTE >00", !includeComments ? null : "#" + Globals.toHexString(i, 2) + " unused");
                    }
                }
            }
        }
        if (includeComments) {
            printPaddedLine(bw, "****************************************", false);
            printPaddedLine(bw, "* Transition Map Data", false);
            printPaddedLine(bw, "****************************************", false);
        }
        for (int m = 0; m < transMaps.size(); m++) {
            StringBuilder sbLine = new StringBuilder();
            int[][] mapToSave = transMaps.get(m);
            if (includeComments) {
                printPaddedLine(bw, "* == Map #" + m + " == ", false);
            }
            printPaddedLine(bw, "MC" + m + (m < 10 ? "   " : (m < 100 ? "  " : (m < 1000 ? " " : ""))) + " DATA " + mapdMain.getScreenColor(m), includeComments);
            sbLine.append("MS").append(m).append(m < 10 ? "   " : (m < 100 ? "  " : (m < 1000 ? " " : ""))).append(" DATA >");
            sbLine.append(Globals.toHexString(mapToSave[0].length, 4));
            sbLine.append(",>").append(Globals.toHexString(mapToSave.length, 4));
            sbLine.append(",>").append(Globals.toHexString(mapToSave[0].length * mapToSave.length, 4));
            printPaddedLine(bw, sbLine.toString(), "Width, Height, Size");
            sbLine.delete(0, sbLine.length());
            if (compression == MagellanExportDialog.COMPRESSION_NONE) {
                boolean isFirstByte = true;
                for (int y = 0; y < mapToSave.length; y++) {
                    if (includeComments) {
                        printPaddedLine(bw, "* -- Map Row " + y + " -- ", false);
                    }
                    for (int cl = 0; cl < Math.ceil((double) mapToSave[y].length / 8); cl++) {
                        if (y == 0 && cl == 0) {
                            sbLine.append("MD").append(m).append(m < 10 ? "   " : (m < 100 ? "  " : (m < 1000 ? " " : ""))).append(" DATA ");
                        }
                        else {
                            sbLine.append("       DATA ");
                        }
                        for (int colpos = (cl * 8); colpos < Math.min((cl + 1) * 8, mapToSave[y].length); colpos++) {
                            if (isFirstByte) {
                                if (colpos > (cl * 8)) {
                                    sbLine.append(",");
                                }
                                sbLine.append(">");
                            }
                            sbLine.append(mapToSave[y][colpos] == MapCanvas.NOCHAR ? "00" : Globals.toHexString(mapToSave[y][colpos], 2));
                            isFirstByte = !isFirstByte;
                        }
                        if (!isFirstByte) {
                            sbLine.append("XX"); // If odd, pad with an illegal value
                            isFirstByte = !isFirstByte;
                        }
                        printPaddedLine(bw, sbLine.toString(), includeComments);
                        sbLine.delete(0, sbLine.length());
                    }
                }
                ArrayList<int[]> uniqueRows = new ArrayList<int[]>();
                for (int[] row : mapToSave) {
                    boolean found = false;
                    for (int y = 0; y < uniqueRows.size() && !found; y++) {
                        if (Arrays.equals(row, uniqueRows.get(y))) {
                            found = true;
                        }
                    }
                    if (!found) {
                        uniqueRows.add(row);
                    }
                }
                // System.out.println("Unique rows: " + uniqueRows.size());
            }
            else if (compression == MagellanExportDialog.COMPRESSION_RLE_BYTE) {
                // RLE compression (byte)
                // We assume all characters are < 128. If msb is set, the next byte determines
                // how many times (2 - 256) the current byte (with msb cleared) should be repeated.
                // A repeat count of 0 is used as end marker.
                // System.out.println("Uncompressed size: " + (mapToSave.length * mapToSave[0].length));
                int i = 0;
                int n = 0;
                int current;
                int last = -1;
                int count = 0;
                for (int y = 0; y < mapToSave.length; y++) {
                    for (int x = 0; x < mapToSave[0].length; x++) {
                        current = mapToSave[y][x];
                        if (last != -1) {
                            if (current == last && count < 255) {
                                // Same byte, increment count
                                count++;
                            }
                            else if (count > 0) {
                                // End of run of bytes
                                i = printByte(bw, sbLine, i, last | 128);
                                i = printByte(bw, sbLine, i, count);
                                count = 0;
                                n += 2;
                            }
                            else {
                                // Different byte
                                i = printByte(bw, sbLine, i, last);
                                n++;
                            }
                        }
                        last = current;
                    }
                }
                if (count > 0) {
                    i = printByte(bw, sbLine, i, last | 128);
                    i = printByte(bw, sbLine, i, count);
                    n += 2;
                }
                else {
                    // Different byte
                    i = printByte(bw, sbLine, i, last);
                    n++;
                }
                // End marker
                i = printByte(bw, sbLine, i, 128);
                i = printByte(bw, sbLine, i, 0);
                n += 2;
                printPaddedLine(bw, sbLine.toString(), false);
                n += i;
                // System.out.println("Compressed size: " + n);
            }
            else if (compression == MagellanExportDialog.COMPRESSION_RLE_WORD) {
                // RLE compression (word)
                // We assume all characters are < 128. If msb of the MSB is set, the byte following the
                // current word determines how many times (2 - 256) the current word (with msb cleared)
                // should be repeated. A repeat count of 0 is used as end marker.
                // System.out.println("Uncompressed size: " + (mapToSave.length * mapToSave[0].length));
                int i = 0;
                int n = 0;
                int current;
                int last = -1;
                int count = 0;
                for (int y = 0; y < mapToSave.length; y++) {
                    for (int x = 0; x < mapToSave[0].length - 1; x += 2) {
                        current = mapToSave[y][x] << 8 | mapToSave[y][x + 1];
                        if (last != -1) {
                            if (current == last && count < 255) {
                                // Same word, increment count
                                count++;
                            }
                            else if (count > 0) {
                                // End of run of words
                                i = printByte(bw, sbLine, i, (last & 0xFF00) >> 8 | 128);
                                i = printByte(bw, sbLine, i, last & 0x00FF);
                                i = printByte(bw, sbLine, i, count);
                                count = 0;
                                n += 3;
                            }
                            else {
                                // Different byte
                                i = printByte(bw, sbLine, i, (last & 0xFF00) >> 8);
                                i = printByte(bw, sbLine, i, last & 0x00FF);
                                n += 2;
                            }
                        }
                        last = current;
                    }
                }
                if (count > 0) {
                    i = printByte(bw, sbLine, i, (last & 0xFF00) >> 8 | 128);
                    i = printByte(bw, sbLine, i, last & 0x00FF);
                    i = printByte(bw, sbLine, i, count);
                    n += 2;
                }
                else {
                    // Different byte
                    i = printByte(bw, sbLine, i, (last & 0xFF00) >> 8);
                    i = printByte(bw, sbLine, i, last & 0x00FF);
                    n++;
                }
                // End marker
                i = printByte(bw, sbLine, i, 128);
                i = printByte(bw, sbLine, i, 0);
                i = printByte(bw, sbLine, i, 0);
                n += 3;
                printPaddedLine(bw, sbLine.toString(), false);
                n += i;
                // System.out.println("Compressed size: " + n);
            }
            else {
                throw new RuntimeException("Compression mode not yet supported.");
            }
        }
        if (frames > 0 || vertical && frames == -1) {
            if (includeComments) {
                printPaddedLine(bw, "****************************************", false);
                printPaddedLine(bw, "* Scrolled Character Patterns", false);
                printPaddedLine(bw, "****************************************", false);
            }
            if (frames > 0) {
                for (int f = 0; f < frames; f++) {
                    int offset = f * 8 / frames;
                    for (int i = 0; i <= imax; i++) {
                        StringBuilder sbLine = new StringBuilder();
                        if (i == 0) {
                            sbLine.append(vertical ? "V" : "H").append("PFRM").append(f);
                        }
                        else {
                            sbLine.append("      ");
                        }
                        sbLine.append(" DATA ");
                        String hexstr;
                        TransChar transChar = transCharSet[i];
                        if (transChar != null) {
                            int[][] fromGrid = hmCharGrids.get(transChar.getFromChar() + (animate ? f * 32 : 0));
                            int[][] toGrid = hmCharGrids.get(transChar.getToChar() + (animate ? f * 32 : 0));
                            if (transChar.isInvert()) {
                                toGrid = Globals.cloneGrid(toGrid);
                                Globals.invertGrid(toGrid, 1);
                            }
                            int[][] scrollGrid = new int[8][8];
                            if (vertical) {
                                int y1 = 0;
                                for (int y = 8 - offset; y < 8; y++) {
                                    System.arraycopy(toGrid[y], 0, scrollGrid[y1], 0, 8);
                                    y1++;
                                }
                                for (int y = 0; y < 8 - offset; y++) {
                                    System.arraycopy(fromGrid[y], 0, scrollGrid[y1], 0, 8);
                                    y1++;
                                }
                            }
                            else {
                                int x1 = 0;
                                for (int x = offset; x < 8; x++) {
                                    for (int y = 0; y < 8; y++) {
                                        scrollGrid[y][x1] = fromGrid[y][x];
                                    }
                                    x1++;
                                }
                                for (int x = 0; x < offset; x++) {
                                    for (int y = 0; y < 8; y++) {
                                        scrollGrid[y][x1] = toGrid[y][x];
                                    }
                                    x1++;
                                }
                            }
                            hexstr = Globals.getHexString(scrollGrid).toUpperCase();
                        }
                        else {
                            hexstr = Globals.BLANKCHAR;
                        }
                        sbLine.append(">").append(hexstr.substring(0, 4)).append(",");
                        sbLine.append(">").append(hexstr.substring(4, 8)).append(",");
                        sbLine.append(">").append(hexstr.substring(8, 12)).append(",");
                        sbLine.append(">").append(hexstr.substring(12, 16));
                        printPaddedLine(bw, sbLine.toString(), includeComments ? "#" + Globals.toHexString(i, 2) + (transChar == null ? " unused" : "") : null);
                    }
                }
            }
            // Pattern strips
            else {
                for (int i = 0; i <= imax; i++) {
                    String hexstr;
                    TransChar transChar = transCharSet[i];
                    if (transChar != null) {
                        hexstr =
                            Globals.getHexString(hmCharGrids.get(transChar.getToChar())).toUpperCase() +
                            Globals.getHexString(hmCharGrids.get(transChar.getFromChar())).toUpperCase();
                    }
                    else {
                        hexstr = Globals.BLANKCHAR + Globals.BLANKCHAR;
                    }
                    StringBuilder sbLine = new StringBuilder();
                    sbLine.append(i == 0 ? "PSTRIP" : "      ").append(" DATA ");
                    sbLine.append(">").append(hexstr.substring(0, 4)).append(",");
                    sbLine.append(">").append(hexstr.substring(4, 8)).append(",");
                    sbLine.append(">").append(hexstr.substring(8, 12)).append(",");
                    sbLine.append(">").append(hexstr.substring(12, 16));
                    printPaddedLine(bw, sbLine.toString(), includeComments ? "#" + Globals.toHexString(i, 2) + " " + (transChar == null ? "unused" : "- " + Globals.toHexString(transChar.getToChar(), 2)) : null);
                    sbLine = new StringBuilder();
                    sbLine.append("       DATA ");
                    sbLine.append(">").append(hexstr.substring(16, 20)).append(",");
                    sbLine.append(">").append(hexstr.substring(20, 24)).append(",");
                    sbLine.append(">").append(hexstr.substring(24, 28)).append(",");
                    sbLine.append(">").append(hexstr.substring(28, 32));
                    printPaddedLine(bw, sbLine.toString(), includeComments ? "#" + Globals.toHexString(i, 2) + " " + (transChar == null ? " unused" : "- " + Globals.toHexString(transChar.getFromChar(), 2)) : null);
                }
            }
        }
        if ((frames > 0 || vertical && frames == -1) && colorMode == Magellan.COLOR_MODE_BITMAP) {
            if (includeComments) {
                printPaddedLine(bw, "****************************************", false);
                printPaddedLine(bw, "* Scrolled Character Colors", false);
                printPaddedLine(bw, "****************************************", false);
            }
            if (frames > 0) {
                for (int f = 0; f < (vertical ? frames : 1); f++) {
                    int offset = f * 8 / frames;
                    for (int i = 0; i <= imax; i++) {
                        StringBuilder sbLine = new StringBuilder();
                        if (i == 0) {
                            sbLine.append(vertical ? "V" : "H").append("CFRM").append(f);
                        }
                        else {
                            sbLine.append("      ");
                        }
                        sbLine.append(" DATA ");
                        String hexstr;
                        TransChar transChar = transCharSet[i];
                        if (transChar != null) {
                            int[][] fromGrid = hmCharColors.get(transChar.getFromChar() + (animate ? f * 32 : 0));
                            int[][] toGrid = hmCharColors.get(transChar.getToChar() + (animate ? f * 32 : 0));
                            hexstr = "";
                            if (vertical) {
                                for (int y = 8 - offset; y < 8; y++) {
                                    hexstr += Integer.toHexString(toGrid[y][1]).toUpperCase();
                                    hexstr += Integer.toHexString(toGrid[y][0]).toUpperCase();
                                }
                                for (int y = 0; y < 8 - offset; y++) {
                                    hexstr += Integer.toHexString(fromGrid[y][1]).toUpperCase();
                                    hexstr += Integer.toHexString(fromGrid[y][0]).toUpperCase();
                                }
                            }
                            else {
                                for (int y = 0; y < 8; y++) {
                                    int foreColor = 0;
                                    int backColor = 0;
                                    int fromForeColor = fromGrid[y][Globals.INDEX_CLR_FORE] != 0 ? fromGrid[y][Globals.INDEX_CLR_FORE] : mapdMain.getColorScreen();
                                    int toForeColor = toGrid[y][Globals.INDEX_CLR_FORE] != 0 ? toGrid[y][Globals.INDEX_CLR_FORE] : mapdMain.getColorScreen();
                                    if (fromForeColor == toForeColor) {
                                        foreColor = fromForeColor;
                                    }
                                    else if (!Globals.arrayContains(hmCharGrids.get(transChar.getFromChar())[y], Globals.INDEX_CLR_FORE)) {
                                        foreColor = toForeColor;
                                    }
                                    else if (!Globals.arrayContains(hmCharGrids.get(transChar.getToChar())[y], Globals.INDEX_CLR_FORE)) {
                                        foreColor = fromForeColor;
                                    }
                                    int fromBackColor = fromGrid[y][Globals.INDEX_CLR_BACK] != 0 ? fromGrid[y][Globals.INDEX_CLR_BACK] : mapdMain.getColorScreen();
                                    int toBackColor = toGrid[y][Globals.INDEX_CLR_BACK] != 0 ? toGrid[y][Globals.INDEX_CLR_BACK] : mapdMain.getColorScreen();
                                    if (fromBackColor == toBackColor) {
                                        backColor = fromBackColor;
                                    }
                                    else if (!Globals.arrayContains(hmCharGrids.get(transChar.getFromChar())[y], Globals.INDEX_CLR_BACK)) {
                                        backColor = toBackColor;
                                    }
                                    else if (!Globals.arrayContains(hmCharGrids.get(transChar.getToChar())[y], Globals.INDEX_CLR_BACK)) {
                                        backColor = fromBackColor;
                                    }
                                    hexstr += Integer.toHexString(foreColor).toUpperCase();
                                    hexstr += Integer.toHexString(backColor).toUpperCase();
                                }
                            }
                        }
                        else {
                            hexstr = Globals.BLANKCHAR;
                        }
                        sbLine.append(">").append(hexstr.substring(0, 4)).append(",");
                        sbLine.append(">").append(hexstr.substring(4, 8)).append(",");
                        sbLine.append(">").append(hexstr.substring(8, 12)).append(",");
                        sbLine.append(">").append(hexstr.substring(12, 16));
                        printPaddedLine(bw, sbLine.toString(), includeComments ? "#" + Globals.toHexString(i, 2) + (transChar == null ? " unused" : "") : null);
                    }
                }
            }
            // Color strips
            else {
                for (int i = 0; i <= imax; i++) {
                    String hexstr = "";
                    TransChar transChar = transCharSet[i];
                    if (transChar != null) {
                        int[][] toColors = hmCharColors.get(transChar.getToChar());
                        for (int row = 0; row < 8; row++) {
                            hexstr += Integer.toHexString(toColors[row][1]) + Integer.toHexString(toColors[row][0]);
                        }
                        int[][] fromColors = hmCharColors.get(transChar.getFromChar());
                        for (int row = 0; row < 8; row++) {
                            hexstr += Integer.toHexString(fromColors[row][1]) + Integer.toHexString(fromColors[row][0]);
                        }
                    }
                    else {
                        hexstr = Globals.BLANKCHAR + Globals.BLANKCHAR;
                    }
                    hexstr = hexstr.toUpperCase();
                    StringBuilder sbLine = new StringBuilder();
                    sbLine.append(i == 0 ? "CSTRIP" : "      ").append(" DATA ");
                    sbLine.append(">").append(hexstr.substring(0, 4)).append(",");
                    sbLine.append(">").append(hexstr.substring(4, 8)).append(",");
                    sbLine.append(">").append(hexstr.substring(8, 12)).append(",");
                    sbLine.append(">").append(hexstr.substring(12, 16));
                    printPaddedLine(bw, sbLine.toString(), includeComments ? "#" + Globals.toHexString(i, 2) + " " + (transChar == null ? "unused" : "- " + Globals.toHexString(transChar.getToChar(), 2)) : null);
                    sbLine = new StringBuilder();
                    sbLine.append("       DATA ");
                    sbLine.append(">").append(hexstr.substring(16, 20)).append(",");
                    sbLine.append(">").append(hexstr.substring(20, 24)).append(",");
                    sbLine.append(">").append(hexstr.substring(24, 28)).append(",");
                    sbLine.append(">").append(hexstr.substring(28, 32));
                    printPaddedLine(bw, sbLine.toString(), includeComments ? "#" + Globals.toHexString(i, 2) + " " + (transChar == null ? " unused" : "- " + Globals.toHexString(transChar.getFromChar(), 2)) : null);
                }
            }
        }
        bw.flush();
        bw.close();
    }

    protected void writeIsometricFile(File mapDataFile) throws Exception {
        mapdMain.storeCurrentMap();
        FileWriter writer = null;
        try {
            writer = new FileWriter(mapDataFile);
            int[][] mapData = mapdMain.getMapData(mapdMain.getCurrentMapId());
            int[][] transMapData = new int[mapData.length - 1][mapData[0].length - 2];
            Map<String, Integer> transCharLookup = new HashMap<String, Integer>();
            int[][][] transCharGrids = new int[256][16][24];
            int iMax = 0;
            for (int y = 0; y < mapData.length - 1; y++) {
                for (int x = 0; x < mapData[y].length - 2; x++) {
                    String key =
                        mapData[y][x] + "-" + mapData[y][x + 1] + "-" + mapData[y][x + 2] + "-" +
                        mapData[y + 1][x] + "-" + mapData[y + 1][x + 1] + "-" + mapData[y + 1][x + 2];
                    Integer i = transCharLookup.get(key);
                    if (i == null) {
                        // Found a new transition
                        i = iMax++;
                        transCharLookup.put(key, i);
                        Globals.copyGrid(hmCharGrids.get(mapData[y][x]), transCharGrids[i], 0, 0);
                        Globals.copyGrid(hmCharGrids.get(mapData[y][x + 1]), transCharGrids[i], 8, 0);
                        Globals.copyGrid(hmCharGrids.get(mapData[y][x + 2]), transCharGrids[i], 16, 0);
                        Globals.copyGrid(hmCharGrids.get(mapData[y + 1][x]), transCharGrids[i], 0, 8);
                        Globals.copyGrid(hmCharGrids.get(mapData[y + 1][x + 1]), transCharGrids[i], 8, 8);
                        Globals.copyGrid(hmCharGrids.get(mapData[y + 1][x + 2]), transCharGrids[i], 16, 8);
                        // Globals.printGrid(transCharGrids[i]);
                    }
                    transMapData[y][x] = i;
                }
            }
            for (int f = 0; f < 8; f++) {
                for (int i = 0; i < iMax; i++) {
                    int[][] scrollGrid = new int[8][8];
                    int[][] transCharGrid = transCharGrids[i];
                    for (int y = 8 - f, y1 = 0; y < 16 - f; y++, y1++) {
                        System.arraycopy(transCharGrid[y], 2 * f, scrollGrid[y1], 0, 8);
                    }
                    // System.out.println("Char " + i + " frame " + f);
                    // Globals.printGrid(scrollGrid);
                    writer.write(i == 0 ? "PATFR" + f : "      ");
                    writer.write(" DATA ");
                    String hexString = Globals.getHexString(scrollGrid);
                    writer.write(">" + hexString.substring(0, 4) + ",");
                    writer.write(">" + hexString.substring(4, 8) + ",");
                    writer.write(">" + hexString.substring(8, 12) + ",");
                    writer.write(">" + hexString.substring(12, 16) + "\n");
                }
            }
            for (int yStart = transMapData.length - 40; yStart < transMapData.length; yStart++) {
                int y = yStart;
                for (int x = 0; x < transMapData[0].length; x+= 2) {
                    if (x % 8 == 0) {
                        writer.write(x == 0 && y == transMapData.length - 40 ? "MAP    " : "       ");
                        writer.write("DATA ");
                    }
                    // System.out.println("(x,y)=(" + x + "," + y + ")");
                    writer.write(">" + Globals.toHexString(transMapData[y][x], 2) + Globals.toHexString(transMapData[y][x + 1], 2));
                    writer.write(x % 8 < 6 ? "," : "\n");
                    y--;
                }
            }
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    protected void writeArtragFile(File mapDataFile, boolean wrap, boolean includeComments, boolean includeCharNumbers, int frames) throws Exception {

        Map<String, String> colorLinkHints = new HashMap<String, String>();
        colorLinkHints.put("0-73", "0-23");
        colorLinkHints.put("73-0", "24-0");
        colorLinkHints.put("0-74", "9-10");
        colorLinkHints.put("74-0", "10-11");
        colorLinkHints.put("0-75", "0-18");
        colorLinkHints.put("75-0", "18-0");
        colorLinkHints.put("0-73", "0-23");

        // store working map first
        mapdMain.storeCurrentMap();
        BufferedWriter bw = null;
        int[][][] transMaps = new int[2][][];
        Map<String, TransChar>[] transCharsArr = new Map[2];
        TransChar[][] transCharSetArr = new TransChar[2][];
        boolean[] usedChars = new boolean[256];
        int startChar = 255;
        int endChar = 0;
        int imax = 0;
        boolean allColorsOK = true;
        for (int m = 0; m < 2; m++) {
            Map<String, TransChar> transChars = new HashMap<String, TransChar>();
            transCharsArr[m] = transChars;
            TransChar[] transCharSet = new TransChar[256];
            transCharSetArr[m] = transCharSet;
            int yOffset = m == 0 ? 8 : 0;
            int[][] mapData = mapdMain.getMapData(mapdMain.getCurrentMapId());
            if (mapData.length > 1 && mapData[0].length > 1) {
                int i = 0;
                for (int y = yOffset; y < yOffset + 8; y++) {
                    for (int x = 0; x < mapData[0].length - (wrap ? 0 : 1); x++) {
                        int fromChar = mapData[y][x];
                        if (fromChar < startChar) {
                            startChar = fromChar;
                        }
                        if (fromChar > endChar) {
                            endChar = fromChar;
                        }
                        usedChars[fromChar] = true;
                        int toChar = mapData[y][x < mapData[0].length - 1 ? x + 1 : 0];
                        if (toChar < startChar) {
                            startChar = toChar;
                        }
                        if (toChar > endChar) {
                            endChar = toChar;
                        }
                        usedChars[toChar] = true;
                        String key = fromChar + "-" + toChar;
                        TransChar transChar = transChars.get(key);
                        if (transChar != null) {
                            transChar.incCount();
                        }
                        else {
                            boolean colorsOK = true;
                            int[][] charColors = new int[8][8];
                            int[][] fromCharGrid = hmCharGrids.get(fromChar);
                            int[][] toCharGrid = hmCharGrids.get(toChar);
                            int[][] fromColorGrid = hmCharColors.get(fromChar);
                            int[][] toColorGrid = hmCharColors.get(toChar);
                            for (int r = 0; r < 8 && colorsOK; r++) {
                                int[] fromColorRow = fromColorGrid[r];
                                int[] toColorRow = toColorGrid[r];
                                int screenColor = mapdMain.getColorScreen();
                                int fromForeColor = fromColorRow[Globals.INDEX_CLR_FORE] != 0 ? fromColorRow[Globals.INDEX_CLR_FORE] : screenColor;
                                int toForeColor = toColorRow[Globals.INDEX_CLR_FORE] != 0 ? toColorRow[Globals.INDEX_CLR_FORE] : screenColor;
                                if (!Globals.arrayContains(fromCharGrid[r], Globals.INDEX_CLR_FORE) && !Globals.arrayContains(toCharGrid[r], Globals.INDEX_CLR_FORE)) {
                                    charColors[r][Globals.INDEX_CLR_FORE] = 0; // don't care
                                }
                                else if (fromForeColor == toForeColor) {
                                    charColors[r][Globals.INDEX_CLR_FORE] = fromForeColor;
                                }
                                else if (!Globals.arrayContains(fromCharGrid[r], Globals.INDEX_CLR_FORE)) {
                                    charColors[r][Globals.INDEX_CLR_FORE] = toForeColor;
                                }
                                else if (!Globals.arrayContains(toCharGrid[r], Globals.INDEX_CLR_FORE)) {
                                    charColors[r][Globals.INDEX_CLR_FORE] = fromForeColor;
                                }
                                else {
                                    charColors[r][Globals.INDEX_CLR_FORE] = fromForeColor;
                                    colorsOK = false;
                                    allColorsOK = false;
                                }
                                int fromBackColor = fromColorRow[Globals.INDEX_CLR_BACK] != 0 ? fromColorRow[Globals.INDEX_CLR_BACK] : screenColor;
                                int toBackColor = toColorRow[Globals.INDEX_CLR_BACK] != 0 ? toColorRow[Globals.INDEX_CLR_BACK] : screenColor;
                                if (!Globals.arrayContains(fromCharGrid[r], Globals.INDEX_CLR_BACK) && !Globals.arrayContains(toCharGrid[r], Globals.INDEX_CLR_BACK)) {
                                    charColors[r][Globals.INDEX_CLR_BACK] = 0; // don't care
                                }
                                else if (fromBackColor == toBackColor) {
                                    charColors[r][Globals.INDEX_CLR_BACK] = fromBackColor;
                                }
                                else if (!Globals.arrayContains(fromCharGrid[r], Globals.INDEX_CLR_BACK)) {
                                    charColors[r][Globals.INDEX_CLR_BACK] = toBackColor;
                                }
                                else if (!Globals.arrayContains(toCharGrid[r], Globals.INDEX_CLR_BACK)) {
                                    charColors[r][Globals.INDEX_CLR_BACK] = fromBackColor;
                                }
                                else {
                                    charColors[r][Globals.INDEX_CLR_BACK] = fromBackColor;
                                    colorsOK = false;
                                    allColorsOK = false;
                                }
                            }
                            if (m == 0) {
                                transChar = new TransChar(fromChar, toChar, i, colorsOK, charColors);
                                transChars.put(key, transChar);
                                transCharSet[i] = transChar;
                                imax = i++;
                                if (imax > 255) {
                                    throw new Exception("Character Set Full: Scrolling this map requires more than 256 characters.");
                                }
                            }
                            else {
                                String hexString = Globals.getColorHexString(charColors);
                                // Second half of the map - this has a separate character set from the first part
                                // but both parts share the same color set, so we must try to match the colors
                                int l = transCharsArr[0].size();
                                boolean matchFound = false;
                                int s = 0;
                                if (colorLinkHints.containsKey(key)) {
                                    s = transCharsArr[0].get(colorLinkHints.get(key)).getIndex();
                                }
                                for (int t = s; t < l && !matchFound; t++) {
                                    TransChar tc = transCharSetArr[0][t];
                                    if (transCharSet[tc.getIndex()] == null) {
                                        boolean colorsMatch = true;
                                        int[][] tColors = tc.getColorGrid();
                                        int[][] newColors = new int[8][2];
                                        for (int r = 0; r < 8 && colorsMatch; r++) {
                                            for (int c = 0; c < 2; c++) {
                                                int color1 = tColors[r][c];
                                                int color2 = charColors[r][c];
                                                if (color1 != color2) {
                                                    if (color1 != 0 && color2 != 0) {
                                                        colorsMatch = false;
                                                    }
                                                    else if (color1 == 0) {
                                                        newColors[r][c] = color2;
                                                    }
                                                    else {
                                                        newColors[r][c] = color1;
                                                    }
                                                }
                                                else {
                                                    newColors[r][c] = color1;
                                                }
                                            }
                                        }
                                        if (colorsMatch) {
                                            String hexString2 = Globals.getColorHexString(tColors);
                                            tc.setColorGrid(newColors);
                                            transChar = new TransChar(fromChar, toChar, tc.getIndex(), colorsOK, newColors);
                                            transChars.put(key, transChar);
                                            transCharSet[tc.getIndex()] = transChar;
                                            matchFound = true;
                                            System.out.println("Match found for " + key + " (" + hexString + "): " + tc.getIndex() + " " + tc.getFromChar() + "-" + tc.getToChar() + " before: " + hexString2 + " now: " + Globals.getColorHexString(newColors));
                                        }
                                    }
                                }
                                if (!matchFound) {
                                    System.out.println("No matching colors found for " + key + " (" + hexString + ")");
                                    throw new Exception("No matching colors found for " + key + " (" + hexString+ ")");
                                }
                            }
                        }
                    }
                }
                int[][] transMap = new int[mapData.length][mapData[0].length - (wrap ? 0 : 1)];
                transMaps[m] = transMap;
                for (int y = yOffset; y < yOffset + 8; y++) {
                    for (int x = 0; x < mapData[0].length - (wrap ? 0 : 1); x++) {
                        int fromChar = mapData[y][x];
                        int toChar = mapData[y][x < mapData[0].length - 1 ? x + 1 : 0];
                        String key = fromChar + "-" + toChar;
                        TransChar transChar = transChars.get(key);
                        transMap[y][x] = transChar.getIndex();
                    }
                }
            }
        }
        if (!allColorsOK) {
            JOptionPane.showMessageDialog(null, "Warning - Some character transitions have incompatible colors. This may cause color spills when the map is scrolled.", "Invalid Color Transitions", JOptionPane.INFORMATION_MESSAGE);
        }
        bw = new BufferedWriter(new FileWriter(mapDataFile));
//            if (includeComments) {
//                printPaddedLine(bw, "****************************************", false);
//                printPaddedLine(bw, "* Original Character Patterns", false);
//                printPaddedLine(bw, "****************************************", false);
//            }
//            for (int i = startChar; i <= endChar; i++) {
//                String hexstr = Globals.BLANKCHAR;
//                if (hmCharGrids.get(i) != null) {
//                    hexstr = Globals.getByteString(hmCharGrids.get(i)).toUpperCase();
//                }
//                if (includeCharNumbers) {
//                    printPaddedLine(bw, "PCH" + i + (i < 10 ? "  " : (i < 100 ? " " : "")) + " DATA >" + Globals.toHexString(i, 2), includeComments);
//                }
//                printPaddedLine(bw, "PAT" + i + (i < 10 ? "  " : (i < 100 ? " " : "")) + " DATA " + ">" +
//                    hexstr.substring(0, 4) + "," + ">" +
//                    hexstr.substring(4, 8) + "," + ">" +
//                    hexstr.substring(8, 12) + "," + ">" +
//                    hexstr.substring(12, 16), includeComments ? (usedChars[i] ? "" : "unused") : null);
//            }
//            if (includeComments) {
//                printPaddedLine(bw, "****************************************", false);
//                printPaddedLine(bw, "* Colorset Definitions", false);
//                printPaddedLine(bw, "****************************************", false);
//            }
//            for (int i = startChar; i <= endChar; i++) {
//                int[][] charColors = hmCharColors.get(i);
//                if (charColors != null && !Globals.isColorGridEmpty(charColors)) {
//                    if (includeCharNumbers) {
//                        printPaddedLine(bw, "CCH" + i + (i < 10 ? "  " : (i < 100 ? " " : "")) + " DATA " + Globals.toHexString(i, 2), includeComments);
//                    }
//                    StringBuilder sbLine = new StringBuilder();
//                    sbLine.append("COL").append(i).append(i < 10 ? "  " : (i < 100 ? " " : "")).append(" DATA ");
//                    for (int row = 0; row < 8; row += 2) {
//                        sbLine.append(">");
//                        int[] rowColors = charColors[row];
//                        sbLine.append(Integer.toHexString(rowColors[1]).toUpperCase());
//                        sbLine.append(Integer.toHexString(rowColors[0]).toUpperCase());
//                        rowColors = charColors[row + 1];
//                        sbLine.append(Integer.toHexString(rowColors[1]).toUpperCase());
//                        sbLine.append(Integer.toHexString(rowColors[0]).toUpperCase());
//                        if (row < 6) {
//                            sbLine.append(",");
//                        }
//                    }
//                    printPaddedLine(bw, sbLine.toString(), includeComments ? (usedChars[i] ? "" : "unused") : null);
//                }
//            }
//            if (includeComments) {
//                printPaddedLine(bw, "****************************************", false);
//                printPaddedLine(bw, "* Transition Character Pairs (from, to) ", false);
//                printPaddedLine(bw, "****************************************", false);
//            }
//            printPaddedLine(bw, "TCHNUM DATA " + (imax + 1), includeComments);
//            for (int m = 0; m < 2; m++) {
//                for (int i = 0; i <= imax; i++) {
//                    TransChar transChar = transCharSetArr[m][i];
//                    if (transChar != null) {
//                        printPaddedLine(bw,
//                                (i == 0 ? "TCHRS" + m : "      ") + " BYTE >" + Globals.toHexString(transChar.getFromChar(), 2) + ",>" + Globals.toHexString(transChar.getToChar(), 2),
//                                !includeComments ? null :
//                                        "#" + Globals.toHexString(transChar.getIndex(), 2) +
//                                                (colorMode != Magellan.COLOR_MODE_BITMAP ? " color " + Globals.toHexString(transChar.getForeColor(), 1) + "/" + Globals.toHexString(transChar.getBackColor(), 1) : "") +
//                                                (transChar.isColorsOK() ? "" : " ERROR")
//                        );
//                    }
//                    else {
//                        printPaddedLine(bw, (i == 0 ? "TCHRS0" : "      ") + " BYTE >FF,>FF", !includeComments ? null : "#" + Globals.toHexString(i, 2) + " unused");
//                    }
//                }
//            }
        if (includeComments) {
            printPaddedLine(bw, "****************************************", false);
            printPaddedLine(bw, "* Transition Map Data", false);
            printPaddedLine(bw, "****************************************", false);
        }
        for (int m = 1; m >= 0; m--) {
            StringBuilder sbLine = new StringBuilder();
            int[][] mapToSave = transMaps[m];
            if (includeComments) {
                printPaddedLine(bw, "* == Map #" + m + " == ", false);
            }
            printPaddedLine(bw, "MC" + m + (m < 10 ? "   " : (m < 100 ? "  " : (m < 1000 ? " " : ""))) + " DATA " + mapdMain.getScreenColor(m), includeComments);
            sbLine.append("MS").append(m).append(m < 10 ? "   " : (m < 100 ? "  " : (m < 1000 ? " " : ""))).append(" DATA >");
            sbLine.append(Globals.toHexString(mapToSave[0].length, 4));
            sbLine.append(",>").append(Globals.toHexString(mapToSave.length, 4));
            sbLine.append(",>").append(Globals.toHexString(mapToSave[0].length * mapToSave.length, 4));
            printPaddedLine(bw, sbLine.toString(), "Width, Height, Size");
            sbLine.delete(0, sbLine.length());

            boolean isFirstByte = true;
            int yOffset = m == 0 ? 8 : 0;
            for (int y = yOffset; y < yOffset + 8; y++) {
                if (includeComments) {
                    printPaddedLine(bw, "* -- Map Row " + y + " -- ", false);
                }
                for (int cl = 0; cl < Math.ceil((double) mapToSave[y].length / 8); cl++) {
                    if (y == 0 && cl == 0) {
                        sbLine.append(m == 0 ? "MAPBOT" : "MAPTOP").append(" DATA ");
                    }
                    else {
                        sbLine.append("       DATA ");
                    }
                    for (int colpos = (cl * 8); colpos < Math.min((cl + 1) * 8, mapToSave[y].length); colpos++) {
                        if (isFirstByte) {
                            if (colpos > (cl * 8)) {
                                sbLine.append(",");
                            }
                            sbLine.append(">");
                        }
                        sbLine.append(mapToSave[y][colpos] == MapCanvas.NOCHAR ? "00" : Globals.toHexString(mapToSave[y][colpos], 2));
                        isFirstByte = !isFirstByte;
                    }
                    if (!isFirstByte) {
                        sbLine.append("XX"); // If odd, pad with an illegal value
                        isFirstByte = !isFirstByte;
                    }
                    printPaddedLine(bw, sbLine.toString(), includeComments);
                    sbLine.delete(0, sbLine.length());
                }
            }
            ArrayList<int[]> uniqueRows = new ArrayList<int[]>();
            for (int[] row : mapToSave) {
                boolean found = false;
                for (int y = 0; y < uniqueRows.size() && !found; y++) {
                    if (Arrays.equals(row, uniqueRows.get(y))) {
                        found = true;
                    }
                }
                if (!found) {
                    uniqueRows.add(row);
                }
            }
        }
        if (frames > 0) {
            for (int m = 1; m >= 0; m--) {
                if (includeComments) {
                    printPaddedLine(bw, "****************************************", false);
                    printPaddedLine(bw, "* Scrolled Character Patterns", false);
                    printPaddedLine(bw, "****************************************", false);
                }
                for (int f = 0; f < frames; f++) {
                    int offset = f * 8 / frames;
                    for (int i = 0; i <= imax; i++) {
                        StringBuilder sbLine = new StringBuilder();
                        if (i == 0) {
                            sbLine.append(m == 0 ? "PTBOT" : "PTTOP").append(f);
                        }
                        else {
                            sbLine.append("      ");
                        }
                        sbLine.append(" DATA ");
                        String hexstr;
                        TransChar transChar = transCharSetArr[m][i];
                        if (transChar != null) {
                            int[][] fromGrid = hmCharGrids.get(transChar.getFromChar());
                            int[][] toGrid = hmCharGrids.get(transChar.getToChar());
                            if (transChar.isInvert()) {
                                toGrid = Globals.cloneGrid(toGrid);
                                Globals.invertGrid(toGrid, 1);
                            }
                            int[][] scrollGrid = new int[8][8];
                            int x1 = 0;
                            for (int x = offset; x < 8; x++) {
                                for (int y = 0; y < 8; y++) {
                                    scrollGrid[y][x1] = fromGrid[y][x];
                                }
                                x1++;
                            }
                            for (int x = 0; x < offset; x++) {
                                for (int y = 0; y < 8; y++) {
                                    scrollGrid[y][x1] = toGrid[y][x];
                                }
                                x1++;
                            }
                            hexstr = Globals.getHexString(scrollGrid).toUpperCase();
                        }
                        else {
                            hexstr = Globals.BLANKCHAR;
                        }
                        sbLine.append(">").append(hexstr.substring(0, 4)).append(",");
                        sbLine.append(">").append(hexstr.substring(4, 8)).append(",");
                        sbLine.append(">").append(hexstr.substring(8, 12)).append(",");
                        sbLine.append(">").append(hexstr.substring(12, 16));
                        printPaddedLine(bw, sbLine.toString(), includeComments ? "#" + Globals.toHexString(i, 2) + (transChar == null ? " unused" : "") : null);
                    }
                }
            }
            if (includeComments) {
                printPaddedLine(bw, "****************************************", false);
                printPaddedLine(bw, "* Character Colors", false);
                printPaddedLine(bw, "****************************************", false);
            }
            for (int i = 0; i <= imax; i++) {
                StringBuilder sbLine = new StringBuilder();
                if (i == 0) {
                    sbLine.append("COLORS");
                }
                else {
                    sbLine.append("      ");
                }
                sbLine.append(" DATA ");
                String hexstr;
                TransChar transChar = transCharSetArr[0][i];
                if (transChar != null) {
                    hexstr = Globals.getColorHexString(transChar.getColorGrid()).toUpperCase();
                }
                else {
                    hexstr = Globals.BLANKCHAR;
                }
                sbLine.append(">").append(hexstr.substring(0, 4)).append(",");
                sbLine.append(">").append(hexstr.substring(4, 8)).append(",");
                sbLine.append(">").append(hexstr.substring(8, 12)).append(",");
                sbLine.append(">").append(hexstr.substring(12, 16));
                printPaddedLine(bw, sbLine.toString(), includeComments ? "#" + Globals.toHexString(i, 2) + (transChar == null ? " unused" : "") : null);
            }
        }
        bw.flush();
        bw.close();
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

    protected void writeBinaryFile(File mapDataFile, byte chunkFlags, int startChar, int endChar, boolean currMapOnly) throws IOException {
        // store working map first
        mapdMain.storeCurrentMap();
        // get file output buffer
        FileOutputStream fos = new FileOutputStream(mapDataFile);

        // write File Header
        fos.write(BIN_HEADER_MAG);
        fos.write(BIN_HEADER_VER);
        fos.write(chunkFlags);
        byte mapCount = (byte) (currMapOnly ? 1 : mapdMain.getMapCount());
        fos.write(mapCount);

        // write Colorset Chunk (if present)
        if ((chunkFlags & BIN_CHUNK_COLORS) == BIN_CHUNK_COLORS) {
            if (colorMode == Magellan.COLOR_MODE_BITMAP) {
                byte charCount = (byte) ((endChar - startChar) + 1);
                fos.write(charCount);
                for (int bc = startChar; bc <= endChar; bc++) {
                    fos.write(getColorBytes(bc));
                }
            }
            else {
                for (int i = 0; i < clrSets.length; i++) {
                    fos.write((byte) (clrSets[i][Globals.INDEX_CLR_FORE] << 4 | clrSets[i][Globals.INDEX_CLR_BACK]));
                }
            }
        }

        // write Character Chunk (if present)
        if ((chunkFlags & BIN_CHUNK_CHARS) == BIN_CHUNK_CHARS) {
            byte charCount = (byte) ((endChar - startChar) + 1);
            fos.write(charCount);
            for (int bc = startChar; bc <= endChar; bc++) {
                fos.write(getCharBytes(bc));
            }
        }

        // write Maps
        for (int m = 0; m < mapdMain.getMapCount(); m++) {
            if (!currMapOnly || m == mapdMain.getCurrentMapId()) {
                int[][] mapToSave = mapdMain.getMapData(m);
                int mapCols = mapToSave[0].length;
                int mapRows = mapToSave.length;
                int mapSize = mapCols * mapRows;
                int mapScreenColor = mapdMain.getScreenColor(m);

                // write Map Header
                //   reserved bytes for Magellan use
                fos.write(BIN_MAP_HEADER_RESERVED1);
                fos.write(BIN_MAP_HEADER_RESERVED2);
                fos.write(BIN_MAP_HEADER_RESERVED3);
                fos.write(BIN_MAP_HEADER_RESERVED4);
                //   map size as a series of bytes
                int mapSizeChunk = mapSize;
                for (int i = 0; i < 8; i++) {
                    if (mapSizeChunk > 255) {
                        fos.write(255);
                        mapSizeChunk -= 255;
                    }
                    else if (mapSizeChunk > 0) {
                        fos.write((byte) mapSizeChunk);
                        mapSizeChunk = 0;
                    }
                    else {
                        fos.write(0);
                    }
                }
                //   map columns as a byte pair
                if (mapCols > 255) {
                    fos.write(255);
                    fos.write(mapCols - 255);
                }
                else {
                    fos.write(mapCols);
                    fos.write(0);
                }
                //   map rows as a byte pair
                if (mapRows > 255) {
                    fos.write(255);
                    fos.write(mapRows - 255);
                }
                else {
                    fos.write(mapRows);
                    fos.write(0);
                }
                //   map screen color
                fos.write(mapScreenColor);

                // write Map Data
                for (int y = 0; y < mapRows; y++) {
                    for (int x = 0; x < mapCols; x++) {
                        fos.write(mapToSave[y][x]);
                    }
                }

                // write Sprite Chunk (if present)
                if ((chunkFlags & BIN_CHUNK_SPRITES) == BIN_CHUNK_SPRITES) {
                    // not yet implemented
                }
            }
        }

        fos.flush();
        fos.close();
    }

    protected void writeBinaryMap(File mapDataFile) throws IOException {
        // store working map first
        mapdMain.storeCurrentMap();
        // get file output buffer
        FileOutputStream fos = new FileOutputStream(mapDataFile);
        // write map
        int[][] mapToSave = mapdMain.getMapData(mapdMain.getCurrentMapId());
        for (int[] mapRow : mapToSave) {
            for (int mapChar : mapRow) {
                fos.write(mapChar);
            }
        }
        fos.flush();
        fos.close();
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

    protected void printPaddedLine(BufferedWriter bw, String str, boolean isCommand) throws IOException {
        printPaddedLine(bw, str, isCommand, ASM_LINELEN, null);
    }

    protected void printPaddedLine(BufferedWriter bw, String str, String comment) throws IOException {
        printPaddedLine(bw, str, comment != null, ASM_LINELEN, comment);
    }

    protected void readCharImageMono(BufferedImage buffImg) {
        // get character glyphs
        int rowOffset = 0;
        int colOffset = 0;
        for (int charNum = TIGlobals.MIN_CHAR; charNum <= TIGlobals.MAX_CHAR; charNum++) {
            int[][] newCharArray = new int[8][8];
            if (hmCharGrids.containsKey(charNum)) {
                hmCharGrids.remove(charNum);
            }
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    newCharArray[y][x] = (buffImg.getRGB((colOffset * 8) + x, (rowOffset * 8) + y) != -1 ? 1 : 0);
                }
            }
            hmCharGrids.put(charNum, newCharArray);
            colOffset++;
            if (colOffset >= 8) {
                colOffset = 0;
                rowOffset++;
            }
        }
    }

    protected void readCharImageColor(BufferedImage buffImg) {
        // get colorsets
        for (int cs = 0; cs < 32; cs++) {
            int colorFore = -1;
            int colorBack = -1;
            int testY = 0;
            int testX = 0;
            while (testY < 8 && (colorFore == -1 || colorBack == -1)) {
                int pixelColor = buffImg.getRGB(testX, (cs * 8) + testY);
                int tiColor = getTIColorForPixel(pixelColor);
                if ((tiColor > 0) && (colorFore == -1)) {
                    colorFore = tiColor;
                }
                else if ((tiColor > 0) && (tiColor != colorFore)) {
                    colorBack = tiColor;
                }
                testX++;
                if (testX >= 64) {
                    testX = 0;
                    testY++;
                }
            }
            if (colorFore == -1) {
                colorFore = 1;
            }
            if (colorBack == -1) {
                colorBack = 0;
            }
            clrSets[cs][Globals.INDEX_CLR_FORE] = colorFore;
            clrSets[cs][Globals.INDEX_CLR_BACK] = colorBack;
        }
        // Global ECM palette
        ECMPalette ecmGlobalPalette = null;
        if (colorMode == Magellan.COLOR_MODE_ECM_2 || colorMode == Magellan.COLOR_MODE_ECM_3) {
            ECMPalette basePalette = ecmPalettes[0];
            int ecmColorIndex = 0;
            for (int y = 0; y < buffImg.getHeight(); y++) {
                for (int x = 0; x < buffImg.getWidth(); x++) {
                    int color = buffImg.getRGB(x, y);
                    boolean found = false;
                    for (int i = 0; i < ecmColorIndex && !found; i++) {
                        if (basePalette.getColor(i).getRGB() == color) {
                            found = true;
                        }
                    }
                    if (!found && ecmColorIndex < basePalette.getSize()) {
                        basePalette.setColor(ecmColorIndex++, new Color(color));
                    }
                }
            }
            if (ecmColorIndex < basePalette.getSize()) {
                for (int i = ecmColorIndex; i < basePalette.getSize(); i++) {
                    basePalette.setColor(i, Color.WHITE);
                }
                basePalette.sort();
                ecmGlobalPalette = basePalette;
            }
        }
        // get character glyphs
        int rowOffset = 0;
        int colOffset = 0;
        int cSet = 0;
        int ecmPaletteIndex = 1;
        for (int charNum = TIGlobals.MIN_CHAR; charNum <= TIGlobals.MAX_CHAR; charNum++) {
            int[][] newCharArray = new int[8][8];
            int[][] newColorArray = new int[8][2];
            // ECM palette for character
            int[] ecmColors = colorMode == Magellan.COLOR_MODE_ECM_2 ? new int[4] : (colorMode == Magellan.COLOR_MODE_ECM_3 ? new int[8] : null);
            int ecmColorIndex = 0;
            if (ecmGlobalPalette == null && (colorMode == Magellan.COLOR_MODE_ECM_2 || colorMode == Magellan.COLOR_MODE_ECM_3)) {
                for (int y = 0; y < 8; y++) {
                    for (int x = 0; x < 8; x++) {
                        int color = buffImg.getRGB((colOffset * 8) + x, (rowOffset * 8) + y);
                        boolean found = false;
                        for (int i = 0; i < Math.min(ecmColorIndex, ecmColors.length) && !found; i++) {
                            if (ecmColors[i] == color) {
                                found = true;
                            }
                        }
                        if (!found) {
                            if (ecmColorIndex < ecmColors.length) {
                                ecmColors[ecmColorIndex++] = color;
                            }
                        }
                    }
                }
                for (int i = ecmColorIndex; i < ecmColors.length; i++) {
                    ecmColors[i] = Color.WHITE.getRGB();
                }
                Arrays.sort(ecmColors);
            }
            // ...
            if (hmCharGrids.containsKey(charNum)) {
                hmCharGrids.remove(charNum);
            }
            for (int y = 0; y < 8; y++) {
                int[] newColors = newColorArray[y];
                newColors[0] = -1;
                newColors[1] = -1;
                for (int x = 0; x < 8; x++) {
                    int color = buffImg.getRGB((colOffset * 8) + x, (rowOffset * 8) + y) | 0xff000000;
                    if (colorMode == Magellan.COLOR_MODE_GRAPHICS_1) {
                        newCharArray[y][x] = (color == TIGlobals.TI_PALETTE_OPAQUE[clrSets[cSet][Globals.INDEX_CLR_FORE]].getRGB() ? 1 : 0);
                    }
                    else if (colorMode == Magellan.COLOR_MODE_BITMAP) {
                        int tiColor = getTIColorForPixel(color);
                        if (newColors[0] == -1) {
                            newColors[0] = tiColor;
                        }
                        else if (newColors[1] == -1 && newColors[0] != tiColor) {
                            newColors[1] = tiColor;
                        }
                        newCharArray[y][x] = tiColor == newColors[0] ? 0 : 1;
                    }
                    else {
                        boolean found = false;
                        if (ecmGlobalPalette == null) {
                            for (int i = 0; i < ecmColors.length && !found; i++) {
                                if (ecmColors[i] == color) {
                                    newCharArray[y][x] = i;
                                    found = true;
                                }
                            }
                        }
                        else {
                            for (int i = 0; i < ecmGlobalPalette.getSize() && !found; i++) {
                                if (ecmGlobalPalette.getColor(i).getRGB() == color) {
                                    newCharArray[y][x] = i;
                                    found = true;
                                }
                            }
                        }
                    }
                }
            }
            hmCharGrids.put(charNum, newCharArray);
            if (colorMode == Magellan.COLOR_MODE_BITMAP) {
                hmCharColors.put(charNum, newColorArray);
            }
            if (colorMode == Magellan.COLOR_MODE_ECM_2 || colorMode == Magellan.COLOR_MODE_ECM_3) {
                if (ecmGlobalPalette == null) {
                    // Match char palette to existing palettes
                    boolean found = false;
                    for (int i = 0; i < Math.min(ecmPaletteIndex, ecmPalettes.length); i++) {
                        if (ecmPalettes[i].startsWith(ecmColors, ecmColorIndex)) {
                            ecmCharPalettes[charNum] = ecmPalettes[i];
                            found = true;
                        }
                    }
                    if (!found) {
                        if (ecmPaletteIndex < ecmPalettes.length - 1) {
                            ecmPalettes[ecmPaletteIndex].setIntColors(ecmColors);
                            ecmCharPalettes[charNum] = ecmPalettes[ecmPaletteIndex];
                            ecmPaletteIndex++;
                        }
                        else {
                            ecmCharPalettes[charNum] = ecmPalettes[ecmPaletteIndex];
                        }
                    }
                }
                else {
                    ecmCharPalettes[charNum] = ecmGlobalPalette;
                }
            }
            colOffset++;
            if (colOffset >= 8) {
                colOffset = 0;
                rowOffset++;
                cSet++;
            }
        }
    }

    protected void writeCharImage(File imageOut, int tileCols, boolean isColor) throws IOException {
        int charCount = hmCharGrids.size();
        int tileRows = (int) Math.ceil(charCount / tileCols);
        BufferedImage bufferCharImage = new BufferedImage(tileCols * 8, tileRows * 8, BufferedImage.TYPE_INT_ARGB);
        if (bufferCharImage == null) {
            bufferCharImage = new BufferedImage(tileCols * 8, tileRows * 8, BufferedImage.TYPE_INT_ARGB);
        }
        try {
            Thread.sleep(500);
        } catch (InterruptedException ie) {
            System.out.println(ie.getMessage());
        }
        if (bufferCharImage == null) {
            System.out.println("Unable to initialize BufferedImage");
        }
        else {
            int[][] charGrid;
            int[][] charColors;
            int drawRow = 0;
            int drawCol = 0;
            int setNum = 0;
            boolean shouldDraw;
            Graphics2D gf = (Graphics2D) (bufferCharImage.getGraphics());
            if (isColor) {
                gf.setComposite(AlphaComposite.Clear);
                gf.setColor(TIGlobals.TI_COLOR_TRANSPARENT);
            }
            else {
                gf.setComposite(AlphaComposite.SrcOver);
                gf.setColor(TIGlobals.TI_COLOR_WHITE);
            }
            gf.fillRect(0, 0, bufferCharImage.getWidth(), bufferCharImage.getHeight());
            gf.setColor(TIGlobals.TI_COLOR_BLACK);
            for (int ch = 0; ch < charCount; ch++) {
                gf.setComposite(AlphaComposite.SrcOver);
                if (hmCharGrids.containsKey(ch)) {
                    charGrid = hmCharGrids.get(ch);
                    charColors = colorMode == Magellan.COLOR_MODE_BITMAP ? hmCharColors.get(ch) : null;
                    for (int y = 0; y < charGrid.length; y++) {
                        for (int x = 0; x < charGrid[y].length; x++) {
                            shouldDraw = true;
                            if (colorMode == Magellan.COLOR_MODE_GRAPHICS_1 || colorMode == Magellan.COLOR_MODE_BITMAP) {
                                if (charGrid[y][x] == 1) {
                                    if (isColor) {
                                        if (charColors == null) {
                                            if (clrSets[setNum][Globals.INDEX_CLR_FORE] == 0) {
                                                shouldDraw = false;
                                            }
                                            gf.setColor(TIGlobals.TI_PALETTE[clrSets[setNum][Globals.INDEX_CLR_FORE]]);
                                        }
                                        else {
                                            shouldDraw = charColors[y][1] != 0;
                                            gf.setColor(TIGlobals.TI_PALETTE[charColors[y][1]]);
                                        }
                                    }
                                    else {
                                        gf.setColor(TIGlobals.TI_COLOR_BLACK);
                                    }
                                }
                                else {
                                    if (isColor) {
                                        if (charColors == null) {
                                            if (clrSets[setNum][Globals.INDEX_CLR_BACK] == 0) {
                                                shouldDraw = false;
                                            }
                                            gf.setColor(TIGlobals.TI_PALETTE[clrSets[setNum][Globals.INDEX_CLR_BACK]]);
                                        }
                                        else {
                                            shouldDraw = charColors[y][0] != 0;
                                            gf.setColor(TIGlobals.TI_PALETTE[charColors[y][0]]);
                                        }
                                    }
                                    else {
                                        gf.setColor(TIGlobals.TI_COLOR_WHITE);
                                    }
                                }
                            }
                            else {
                                if (isColor) {
                                    gf.setColor(ecmCharPalettes[ch].getColor(charGrid[y][x]));
                                }
                                else {
                                    gf.setColor(charGrid[y][x] != 0 ? TIGlobals.TI_COLOR_BLACK : TIGlobals.TI_COLOR_WHITE);
                                }
                            }
                            if (shouldDraw) {
                                gf.fillRect((drawCol * 8) + x, (drawRow * 8) + y, 1, 1);
                            }
                        }
                    }
                }
                drawCol++;
                if (drawCol >= tileCols) {
                    drawCol = 0;
                    drawRow++;
                    setNum++;
                }
            }
            gf.dispose();
            ImageIO.write(bufferCharImage, "png", imageOut);
        }
    }

    protected void writeMapImage(File imageOut) throws IOException {
        ImageIO.write(mapdMain.getBuffer(), "png", imageOut);
    }

    protected int getTIColorForPixel(int pixelRBG) {
        for (int c = 0; c < TIGlobals.TI_PALETTE_OPAQUE.length; c++) {
            if (pixelRBG == TIGlobals.TI_PALETTE_OPAQUE[c].getRGB()) {
                return c;
            }
        }
        return 0;
    }

    protected byte[] getColorBytes(int charnum) {
        byte[] colorbytes = new byte[8];
        int[][] colorarray = hmCharColors.get(charnum);
        for (int y = 0; y < 8; y++) {
            byte background = (byte) colorarray[y][0];
            byte foreground = (byte) (colorarray[y][1] << 4);
            colorbytes[y] = (byte) (background | foreground);
        }
        return colorbytes;
    }

    protected byte[] getCharBytes(int charnum) {
        byte[] charbytes = new byte[8];
        int[][] chararray = hmCharGrids.get(charnum);
        if (chararray != null) {
            int bcount = 0;
            byte byteval = (byte) 0;
            int bytepos = 0;
            boolean goHigh = true;
            for (int y = 0; y < 8; y++) {
                bytepos = 0;
                for (int x = 0; x < 8; x++) {
                    if (chararray[y][x] > 0) {
                        byteval = (byte) (byteval | (bytepos == 0 ? 8 : (bytepos == 1 ? 4 : (bytepos == 2 ? 2 : 1))));
                    }
                    bytepos++;
                    if (bytepos > 3) {
                        charbytes[bcount] = (byte) (charbytes[bcount] | (goHigh ? byteval << 4 : byteval));
                        if (!goHigh) {
                            bcount++;
                        }
                        goHigh = !goHigh;
                        byteval = (byte) 0;
                        bytepos = 0;
                    }
                }
            }
        }
        else {
            for (int i = 0; i < charbytes.length; i++) {
                charbytes[i] = (byte) 0;
            }
        }
        return charbytes;
    }
}
