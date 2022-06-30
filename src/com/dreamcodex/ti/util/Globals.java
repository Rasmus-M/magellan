package com.dreamcodex.ti.util;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.image.ImageFilter;
import java.awt.image.RGBImageFilter;
import java.util.*;

public class Globals {
    public static final String CMD_NEW = "new";
    public static final String CMD_OPEN = "open";
    public static final String CMD_OPEN_RECENT = "openrecent";
    public static final String CMD_SAVE = "save";
    public static final String CMD_SAVEAS = "saveas";
    public static final String CMD_APPEND = "append";
    public static final String CMD_EXIT = "exit";
    public static final String CMD_MPCIMGMN = "importcharimagemono";
    public static final String CMD_MPCIMGCL = "importcharimagecolor";
    public static final String CMD_MPMAPIMG = "importmapimage";
    public static final String CMD_MPVDP = "importvramdump";
    public static final String CMD_MPSPRITES = "importsprites";
    public static final String CMD_XPDATA = "exportdata";
    public static final String CMD_XPEXEC = "exportexec";
    public static final String CMD_XPXB256 = "exportxb256";
    public static final String CMD_BASIC = "exportbasic";
    public static final String CMD_XPASM = "exportasm";
    public static final String CMD_XPBIN = "exportbin";
    public static final String CMD_XPBINMAP = "exportbinmap";
    public static final String CMD_XPCIMGMN = "exportcharimagemono";
    public static final String CMD_XPCIMGCL = "exportcharimagecolor";
    public static final String CMD_XPMAPIMG = "exportmapimage";
    public static final String CMD_XPSCROLL = "exportscroll";

    public static final String CMD_GRID_CHR = "togglegridchr";
    public static final String CMD_UNDO_CHR = "undochr";
    public static final String CMD_REDO_CHR = "redochr";
    public static final String CMD_FILL_CHR = "fillchr";
    public static final String CMD_FLOOD_FILL = "floodfill";
    public static final String CMD_CLEAR_CHR = "clearchr";
    public static final String CMD_ROTATEL_CHR = "rotatelchr";
    public static final String CMD_ROTATER_CHR = "rotaterchr";
    public static final String CMD_FLIPH_CHR = "fliphchr";
    public static final String CMD_FLIPV_CHR = "flipvchr";
    public static final String CMD_SHIFTU_CHR = "shiftuchr";
    public static final String CMD_SHIFTD_CHR = "shiftdchr";
    public static final String CMD_SHIFTL_CHR = "shiftlchr";
    public static final String CMD_SHIFTR_CHR = "shiftrchr";
    public static final String CMD_INVERT_CHR = "invertchr";
    public static final String CMD_EDIT_CHR = "editchr";
    public static final String CMD_UPDATE_CHR = "updatechr";
    public static final String CMD_COPY_CHR = "copychr";
    public static final String CMD_PASTE_CHR = "pastechr";
    public static final String CMD_CLRFORE_CHR = "clrforechr";
    public static final String CMD_CLRBACK_CHR = "clrbackchr";
    public static final String CMD_CLRCHOOSE_CHR = "clrchoosechr";
    public static final String CMD_PALSELECT_CHR = "palselectchr";
    public static final String CMD_ROTATEL_MAP = "rotatelmap";
    public static final String CMD_ROTATER_MAP = "rotatermap";
    public static final String CMD_FLIPH_MAP = "fliphmap";
    public static final String CMD_FLIPV_MAP = "flipvmap";

    public static final String CMD_GRID_SPR = "togglegridspr";
    public static final String CMD_UNDO_SPR = "undospr";
    public static final String CMD_REDO_SPR = "redospr";
    public static final String CMD_FILL_SPR = "fillspr";
    public static final String CMD_CLEAR_SPR = "clearspr";
    public static final String CMD_ROTATEL_SPR = "rotatelspr";
    public static final String CMD_ROTATER_SPR = "rotaterspr";
    public static final String CMD_FLIPH_SPR = "fliphspr";
    public static final String CMD_FLIPV_SPR = "flipvspr";
    public static final String CMD_SHIFTU_SPR = "shiftuspr";
    public static final String CMD_SHIFTD_SPR = "shiftdspr";
    public static final String CMD_SHIFTL_SPR = "shiftlspr";
    public static final String CMD_SHIFTR_SPR = "shiftrspr";
    public static final String CMD_INVERT_SPR = "invertspr";
    public static final String CMD_EDIT_SPR = "editspr";
    public static final String CMD_UPDATE_SPR = "updatespr";
    public static final String CMD_COPY_SPR = "copyspr";
    public static final String CMD_PASTE_SPR = "pastespr";
    public static final String CMD_CLRFORE_SPR = "clrforespr";
    public static final String CMD_CLRBACK_SPR = "clrbackspr";
    public static final String CMD_CLRCHOOSE_SPR = "clrchoosespr";
    public static final String CMD_PALSELECT_SPR = "palselectspr";

    public static final String CMD_LOOK = "look";
    public static final String CMD_CLONE = "clone";
    public static final String CMD_ADDMAP = "addmap";
    public static final String CMD_DELMAP = "delmap";
    public static final String CMD_PREVMAP = "prevmap";
    public static final String CMD_NEXTMAP = "nextmap";
    public static final String CMD_BACKMAP = "backmap";
    public static final String CMD_FORWMAP = "forwmap";
    public static final String CMD_TCURSOR = "textcursor";
    public static final String CMD_SHOWPOS = "toggleshowpos";
    public static final String CMD_BASE0POS = "base0position";
    public static final String CMD_XPXBDISMER = "screenMerge";
    public static final String CMD_SWAPCHARS = "swapcharacters";
    public static final String CMD_ANALYZECHARUSAGE = "analyzecharusage";
    public static final String CMD_ANALYZECHARTRANS = "analyzechartrans";

    public static final String CMD_BASICCHARSETSIZE = "basiccharsetsize";
    public static final String CMD_EXPANDEDCHARSETSIZE = "expandedcharsetsize";
    public static final String CMD_SUPERCHARSETSIZE = "supercharsetsize";
    public static final String CMD_GRAPHICSCOLORMODE = "graphicscolormode";
    public static final String CMD_BITMAPCOLORMODE = "bitmapcolormode";
    public static final String CMD_ECM2COLORMODE = "ecm2colormode";
    public static final String CMD_ECM3COLORMODE = "ecm3colormode";
    public static final String CMD_TRANSPARENCY = "transparency";
    public static final String CMD_ABOUT = "helpabout";
    public static final String CMD_VIEW_CHAR_LAYER = "viewchars";
    public static final String CMD_VIEW_SPRITE_LAYER = "viewsprites";
    public static final String CMD_MAGNIFY_SPRITES = "magnifysprites";
    public static final String CMD_SNAP_SPRITES_TO_GRID = "snapspritestogrid";

    public static final String KEY_SCRBACK = "SB:"; // phasing out
    public static final String KEY_COLOR_MODE = "CM:";
    public static final String KEY_COLORSET = "CC:";
    public static final String KEY_PALETTE = "PL:"; // Color palette  (ECM)
    public static final String KEY_CHARPALS = "CP:"; // Char palettes (ECM)
    public static final String KEY_CHARTRANS = "CT:"; // Char transparency (ECM)
    public static final String KEY_CHARDATA = "CH:";
    public static final String KEY_CHARDATA1 = "C1:"; // Char data plane 1 (ECM)
    public static final String KEY_CHARDATA2 = "C2:"; // Char data plane 2 (ECM)
    public static final String KEY_CHARCOLOR = "CO:";
    public static final String KEY_CHARRANG = "CR:";
    public static final String KEY_MAPCOUNT = "MC:";
    public static final String KEY_MAPSTART = "M+";
    public static final String KEY_MAPSIZE = "MS:";
    public static final String KEY_MAPBACK = "MB:";
    public static final String KEY_MAPDATA = "MP:";
    public static final String KEY_SPRITE_PATTERN = "SP:";
    public static final String KEY_SPRITE_PATTERN1 = "S1:";
    public static final String KEY_SPRITE_PATTERN2 = "S2:";
    public static final String KEY_SPRITE_COLOR = "SC:";
    public static final String KEY_SPRITE_LOCATION = "SL:";
    public static final String KEY_SPRITE_LOCATION_PIXELS = "SX:";
    public static final String KEY_MAPEND = "M-";

    public static final Color CLR_COMPONENTBACK = new Color(255, 255, 255);
    public static final Color CLR_BUTTON_NORMAL = new Color(255, 255, 255);
    public static final Color CLR_BUTTON_ACTIVE = new Color(232, 212, 255);
    public static final Color CLR_BUTTON_INACTIVE = new Color(96, 96, 96);
    public static final Color CLR_BUTTON_TRANS = new Color(232, 232, 192);
    public static final Color CLR_BUTTON_SHIFT = new Color(192, 255, 232);
    public static final Color CLR_HIGHLIGHT = new Color(232, 255, 26);

    public static final int INDEX_CLR_BACK = 0;
    public static final int INDEX_CLR_FORE = 1;

    public static Dimension DM_TOOL = new Dimension(24, 24);
    public static Dimension DM_TEXT = new Dimension(48, 24);
    public static Dimension DM_SPRITE = new Dimension(64, 64);

    public static Border bordButtonNormal = BorderFactory.createLineBorder(new Color(164, 164, 164), 2);
    public static Border bordButtonActive = BorderFactory.createLineBorder(CLR_HIGHLIGHT, 2);

    public static final String BLANKCHAR = "0000000000000000";
    public static final String BLANKSPRITE = "0000000000000000000000000000000000000000000000000000000000000000";

    public static final int BASIC_DATA      = 0;
    public static final int BASIC_PROGRAM   = 1;
    public static final int XB_PROGRAM      = 2;
    public static final int XB256_PROGRAM   = 3;

    public static String padHexString(String s, int length) {
        if (s == null) {
            s = "";
        }
        if (s.length() < length) {
            while (s.length() < length) {
                s += "0";
            }
        }
        else if (s.length() > length) {
            s = s.substring(0, length);
        }
        return s;
    }

    public static String getHexString(int[][] grid) {
        return getHexString(grid, 1);
    }

    public static String getHexString(int[][] grid, int mask) {
        StringBuilder hex = new StringBuilder();
        int bytepos = 0;
        int byteval = 0;
        int charrow = (grid.length / 8);
        int charcol = (grid[0].length / 8);
        for (int cc = 0; cc < charcol; cc++) {
            for (int cr = 0; cr < charrow; cr++) {
                for (int y = 0; y < 8; y++) {
                    int cy = y + (cr * 8);
                    bytepos = 0;
                    for (int x = 0; x < 8; x++) {
                        int cx = x + (cc * 8);
                        if ((grid[cy][cx] & mask) > 0) {
                            byteval += 8 >> bytepos;
                        }
                        bytepos++;
                        if (bytepos > 3) {
                            hex.append(Integer.toHexString(byteval));
                            byteval = 0;
                            bytepos = 0;
                        }
                    }
                    if (bytepos != 0) {
                        hex.append(Integer.toHexString(byteval));
                        byteval = 0;
                    }
                }
            }
        }
        return hex.toString();
    }

    public static String getSpriteHexString(int[][] grid) {
        return getSpriteHexString(grid, 1);
    }

    public static String getSpriteHexString(int[][] grid, int mask) {
        StringBuilder hex = new StringBuilder();
        int nybble = 0;
        int bit = 8;
        for (int col = 0; col < 16; col += 8) {
            for (int row = 0; row < 16; row++) {
                for (int x = 0; x < 8; x++) {
                    if ((grid[row][col + x] & mask) != 0) {
                        nybble |= bit;
                    }
                    bit >>= 1;
                    if (bit == 0) {
                        hex.append(Integer.toHexString(nybble));
                        bit = 8;
                        nybble = 0;
                    }
                }
            }
        }
        return hex.toString();
    }

    public static int[][] getSpriteIntGrid(String hex) {
        int[][] grid = new int[16][16];
        for (int col = 0; col < 16; col += 8) {
            for (int row = 0; row < 16; row++) {
                int pos = col * 4 + row * 2;
                int b = Integer.parseInt(hex.substring(pos, pos + 2), 16);
                int bit = 0x80;
                for (int x = 0; x < 8; x++) {
                    if ((b & bit) != 0) {
                        grid[row][col + x] = 1;
                    }
                    bit >>= 1;
                }
            }
        }
        return grid;
    }

    public static String getColorHexString(int[][] colorGrid) {
        StringBuilder hex = new StringBuilder();
        for (int[] row : colorGrid) {
            hex.append(Integer.toHexString(row[1]));
            hex.append(Integer.toHexString(row[0]));
        }
        return hex.toString();
    }

    public static int[][] parseColorHexString(String hex) {
        int[][] charColors = new int[8][2];
        for (int y = 0; y < 8; y++) {
            charColors[y][1] = Integer.parseInt(Character.toString(hex.charAt(y * 2)), 16);
            charColors[y][0] = Integer.parseInt(Character.toString(hex.charAt(y * 2 + 1)), 16);
        }
        return charColors;
    }

    public static int[][] getIntGrid(String hex, int rows) {
        int charcol = (hex.length() / rows) * 4;
        int[][] barray = new int[rows][charcol];
        int charpos = 0;
        int rowpos = 0;
        int colpos = 0;
        for (int cc = 0; cc < hex.length(); cc++) {
            String ch = "" + hex.charAt(cc);
            int chi = Integer.parseInt(ch, 16);
            barray[rowpos][charpos + colpos] = (((chi & 8) == 8) ? 1 : 0);
            charpos++;
            barray[rowpos][charpos + colpos] = (((chi & 4) == 4) ? 1 : 0);
            charpos++;
            barray[rowpos][charpos + colpos] = (((chi & 2) == 2) ? 1 : 0);
            charpos++;
            barray[rowpos][charpos + colpos] = (((chi & 1) == 1) ? 1 : 0);
            charpos++;
            if (charpos >= 8) {
                charpos = 0;
                rowpos++;
                if (rowpos >= rows) {
                    rowpos = 0;
                    colpos += 8;
                }
            }
        }
        return barray;
    }

    public static int[][] flipGrid(int[][] grid, boolean isVertical) {
        int height = grid.length;
        int width = grid[0].length;
        int flipx = 0;
        int flipy = 0;
        int[][] flippedGrid = new int[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (isVertical) {
                    flipx = x;
                    flipy = (height - 1) - y;
                }
                else {
                    flipx = (width - 1) - x;
                    flipy = y;
                }
                flippedGrid[flipy][flipx] = grid[y][x];
            }
        }
        grid = flippedGrid;
        return grid;
    }

    public static int[][] rotateGrid(int[][] grid, boolean isLeft) {
        int height = grid.length;
        int width = grid[0].length;
        int rotx = 0;
        int roty = 0;
        int[][] rotatedGrid = new int[width][height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (isLeft) {
                    rotx = y;
                    roty = (height - 1) - x;
                }
                else {
                    rotx = (width - 1) - y;
                    roty = x;
                }
                rotatedGrid[roty][rotx] = grid[y][x];
            }
        }
        grid = rotatedGrid;
        return grid;
    }

    public static int[][] invertGrid(int[][] grid, int maxValue) {
        int height = grid.length;
        int width = grid[0].length;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                grid[y][x] = maxValue - grid[y][x];
            }
        }
        return grid;
    }

    public static int[][] swapGridValues(int[][] grid, int value1, int value2) {
        int height = grid.length;
        int width = grid[0].length;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int value = grid[y][x];
                if (value == value1) {
                    grid[y][x] = value2;
                }
                else if (value == value2) {
                    grid[y][x] = value1;
                }
            }
        }
        return grid;
    }

    public static int[][] shiftGrid(int[][] grid, int xshift, int yshift) {
        int y = (yshift < 0 ? grid.length - 1 : 0);
        while (y >= 0 && y <= (grid.length - 1)) {
            int getY = y + yshift;
            int x = (xshift < 0 ? grid[y].length - 1 : 0);
            while (x >= 0 && x <= (grid[y].length - 1)) {
                int getX = x + xshift;
                if (getY >= 0 && getY < grid.length && getX >= 0 && getX < grid[y].length) {
                    grid[y][x] = grid[getY][getX];
                }
                else {
                    grid[y][x] = 0;
                }
                x = x + (xshift < 0 ? -1 : 1);
            }
            y = y + (yshift < 0 ? -1 : 1);
        }
        return grid;
    }

    public static int[][] cycleGridLeft(int[][] grid) {
        for (int[] row : grid) {
            int temp = row[0];
            System.arraycopy(row, 1, row, 0, row.length - 1);
            row[row.length - 1] = temp;
        }
        return grid;
    }

    public static int[][] cycleGridRight(int[][] grid) {
        for (int[] row : grid) {
            int temp = row[row.length - 1];
            System.arraycopy(row, 0, row, 1, row.length - 1);
            row[0] = temp;
        }
        return grid;
    }

    public static int[][] cycleGridUp(int[][] grid) {
        int[] tempRow = grid[0];
        System.arraycopy(grid, 1, grid, 0, grid.length - 1);
        grid[grid.length - 1] = tempRow;
        return grid;
    }

    public static int[][] cycleGridDown(int[][] grid) {
        int[] tempRow = grid[grid.length - 1];
        System.arraycopy(grid, 0, grid, 1, grid.length - 1);
        grid[0] = tempRow;
        return grid;
    }

    public static void copyGrid(int[][] grid, int[][] destination) {
        copyGrid(grid, destination, 0, 0);
    }

    public static void copyGrid(int[][] grid, int[][] destination, int xPos, int yPos) {
        for (int y = 0; y < grid.length; y++) {
            int[] row = grid[y];
            System.arraycopy(row, 0, destination[y + yPos], xPos, row.length);
        }
    }

    public static int[][] cloneGrid(int[][] grid) {
        int[][] clone = new int[grid.length][grid[0].length];
        for (int y = 0; y < grid.length; y++) {
            int[] row = grid[y];
            System.arraycopy(row, 0, clone[y], 0, row.length);
        }
        return clone;
    }

    public static void orGrid(int[][] sourceGrid, int[][] destGrid, int shift) {
        for (int y = 0; y < sourceGrid.length; y++) {
            for (int x = 0; x < sourceGrid[0].length; x++) {
                destGrid[y][x] |= sourceGrid[y][x] << shift;
            }
        }
    }

    public static boolean gridEquals(int[][] grid1, int[][] grid2) {
        for (int y = 0; y < grid1.length; y++) {
            for (int x = 0; x < grid1[0].length; x++) {
                if (grid2[y][x] != grid1[y][x]) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void gridReplace(int[][] grid, int value, int replaceWith) {
        for (int[] row : grid) {
            for (int i = 0; i < row.length; i++) {
                if (row[i] == value) {
                    row[i] = replaceWith;
                }
            }
        }
    }

    public static void sortGrid(int[][] grid) {
        Arrays.sort(grid, new Comparator<int[]>() {
            public int compare(int[] a1, int[] a2) {
                return a2[1] - a1[1];
            }
        });
    }

    public static void printGrid(int[][] grid) {
        for (int[] row : grid) {
            for (int x = 0; x < row.length; x++) {
                System.out.print(row[x] + (x < row.length - 1 ? "," : "\n"));
            }
        }
        System.out.println();
    }

    public static boolean arrayContains(int[][] grid, int value) {
        for (int[] row : grid) {
            if (arrayContains(row, value)) {
                return true;
            }
        }
        return false;
    }

    public static boolean arrayContains(int[] row, int value) {
        for (int cell : row) {
            if (cell == value) {
                return true;
            }
        }
        return false;
    }

    public static String toHexString(int n, int length) {
        String value = Integer.toHexString(n).toUpperCase();
        while (value.length() < length) {
            value = "0" + value;
        }
        return value;
    }

    public static boolean isColorGridEmpty(int[][] grid) {
        if (grid != null) {
            for (int[] row : grid) {
                if (row[0] != 0 || row[1] != 0) { // 0 = Transparent
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean isGridEmpty(int[][] grid) {
        if (grid != null) {
            for (int[] row : grid) {
                for (int i : row) {
                    if (i != 0) {  // 0 = Transparent
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static boolean isByteGrid(int[][] grid) {
        for (int[] row : grid) {
            for (int i : row) {
                if (i > 255) {
                    return false;
                }
            }
        }
        return true;
    }

    public static double gridColorDistance(int[][] grid1, int[][] grid2) {
        double dist = 0;
        for (int y = 0; y < grid1.length; y++) {
            for (int x = 0; x < grid1[0].length; x++) {
                dist += colorDistance(grid1[y][x], grid2[y][x]);
            }
        }
        return dist;
    }

    public static boolean isColorTransitionOK(int foreColor1, int foreColor2, int backColor1, int backColor2, int screenColor, int[][] charGrid1, int[][] charGrid2) {
        return
            (((foreColor1 != 0 ? foreColor1 : screenColor) == (foreColor2 != 0 ? foreColor2 : screenColor) ||
            !arrayContains(charGrid1, INDEX_CLR_FORE) || !arrayContains(charGrid2, INDEX_CLR_FORE)) &&
            ((backColor1 != 0 ? backColor1 : screenColor) == (backColor2 != 0 ? backColor2 : screenColor) ||
            !arrayContains(charGrid1, INDEX_CLR_BACK) || !arrayContains(charGrid2, INDEX_CLR_BACK)))
            ||
            // Invert color set and pattern of character 2
            (((foreColor1 != 0 ? foreColor1 : screenColor) == (backColor2 != 0 ? backColor2 : screenColor) ||
            !arrayContains(charGrid1, INDEX_CLR_FORE) || !arrayContains(charGrid2, INDEX_CLR_BACK)) &&
            ((backColor1 != 0 ? backColor1 : screenColor) == (foreColor2 != 0 ? foreColor2 : screenColor) ||
            !arrayContains(charGrid1, INDEX_CLR_BACK) || !arrayContains(charGrid2, INDEX_CLR_FORE)));

    }

    public static boolean isColorTransitionOK(int foreColor1, int foreColor2, int backColor1, int backColor2, int screenColor, int[] charGrid1, int[] charGrid2) {
        return
            ((foreColor1 != 0 ? foreColor1 : screenColor) == (foreColor2 != 0 ? foreColor2 : screenColor) ||
            !arrayContains(charGrid1, INDEX_CLR_FORE) || !arrayContains(charGrid2, INDEX_CLR_FORE)) &&
            ((backColor1 != 0 ? backColor1 : screenColor) == (backColor2 != 0 ? backColor2 : screenColor) ||
            !arrayContains(charGrid1, INDEX_CLR_BACK) || !arrayContains(charGrid2, INDEX_CLR_BACK));

    }

    public static Color getClosestColor(Color color, Color[] palette) {
        return palette[getClosestColorIndex(color, palette)];
    }

    public static int getClosestColorIndex(Color color, Color[] palette) {
        return getClosestColorIndex(color, palette, -1);
    }

    public static int getClosestColorIndex(Color color, Color[] palette, int excludeIndex) {
        // Find best matching color
        double minColDist = Double.MAX_VALUE;
        int colorIndex = 0;
        for (int n = 0; n < palette.length; n++) {
            if (palette[n] != null && (excludeIndex == -1 || n != excludeIndex)) {
                double dist = colorDistance(color, palette[n]);
                if (dist < minColDist) {
                    minColDist = dist;
                    colorIndex = n;
                }
            }
        }
        return colorIndex;
    }

    public static double getClosestColorDistance(Color color, Color[] palette, int excludeIndex) {
        return colorDistance(color, palette[getClosestColorIndex(color, palette, excludeIndex)]);
    }

    public static double colorDistance(int color1, int color2) {
        return colorDistance(new Color(color1, true), new Color(color2, true));
    }

    public static double colorDistance(Color c1, Color c2) {
        if (c1.getAlpha() == 0 && c2.getAlpha() == 0) {
            // Two fully transparent colors are treated as identical
            return 0;
        }
        else if (c1.getAlpha() != c2.getAlpha()) {
            return Double.MAX_VALUE / 64;
        }
        else {
            return Math.sqrt(Math.pow(c1.getRed() - c2.getRed(), 2) + Math.pow(c1.getGreen() - c2.getGreen(), 2) + Math.pow(c1.getBlue() - c2.getBlue(), 2));
        }
    }

    public static Color getECMSafeColor(Color color) {
        int newRed = (int) Math.round((double) color.getRed() / 17d) * 17;
        int newGreen = (int) Math.round((double) color.getGreen() / 17d) * 17;
        int newBlue =  (int) Math.round((double) color.getBlue() / 17d) * 17;
        return new Color(newRed, newGreen, newBlue, 255);
    }

    public static ImageFilter ifTrans = new RGBImageFilter() {

        private final int tagTrans = TIGlobals.TI_COLOR_TRANSOPAQUE.getRGB();

        public final int filterRGB(int x, int y, int rgb) {
            if (rgb == tagTrans) {
                return 0x00000000;
            }
            return rgb;
        }
    };

    public static String padr(String s, int length) {
        if (s.length() < length) {
            while (s.length() < length) {
                s += " ";
            }
        }
        else if (s.length() > length) {
            s = s.substring(0, length);
        }
        return s;
    }

    public static String padl(String s, int length) {
        return padl(s, length, ' ');
    }

    public static String padl(int n, int length) {
        return padl(Integer.toString(n), length, '0');
    }

    public static String padl(String s, int length, char ch) {
        if (s.length() < length) {
            while (s.length() < length) {
                s = ch + s;
            }
        }
        else if (s.length() > length) {
            s = s.substring(0, length);
        }
        return s;
    }

    public static String trimHex(String s, int length) {
        if (s == null) {
            s = "";
        }
        if (s.length() > length) {
            s = s.substring(0, length);
        }
        else if (s.length() < length) {
            while (s.length() < length) {
                s = "0" + s;
            }
        }
        return s;
    }
}
