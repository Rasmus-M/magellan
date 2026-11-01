package com.dreamcodex.ti.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Converts maps between character numbers and metatile numbers.
 */
public class MetatileConverter {

    public static class Result {

        private final List<int[][]> maps;
        private final HashMap<Integer, int[][]> metatiles;

        Result(List<int[][]> maps, HashMap<Integer, int[][]> metatiles) {
            this.maps = maps;
            this.metatiles = metatiles;
        }

        public List<int[][]> getMaps() {
            return maps;
        }

        public HashMap<Integer, int[][]> getMetatiles() {
            return metatiles;
        }
    }

    /**
     * Creates a set of metatiles that are all filled with the blank character.
     */
    public static HashMap<Integer, int[][]> createBlankMetatiles(int size, int count, int blankChar) {
        HashMap<Integer, int[][]> metatiles = new HashMap<>();
        for (int i = 0; i < count; i++) {
            metatiles.put(i, createBlankMetatile(size, blankChar));
        }
        return metatiles;
    }

    public static int[][] createBlankMetatile(int size, int blankChar) {
        int[][] metatile = new int[size][size];
        for (int[] row : metatile) {
            Arrays.fill(row, blankChar);
        }
        return metatile;
    }

    /**
     * Expands a map of metatile numbers into a map of character numbers.
     * Undefined metatiles become blank characters.
     */
    public static int[][] expand(int[][] map, HashMap<Integer, int[][]> metatiles, int size, int blankChar) {
        int[][] charMap = new int[map.length * size][map[0].length * size];
        for (int y = 0; y < map.length; y++) {
            for (int x = 0; x < map[y].length; x++) {
                int[][] metatile = metatiles != null ? metatiles.get(map[y][x]) : null;
                for (int my = 0; my < size; my++) {
                    for (int mx = 0; mx < size; mx++) {
                        charMap[y * size + my][x * size + mx] = metatile != null ? metatile[my][mx] : blankChar;
                    }
                }
            }
        }
        return charMap;
    }

    /**
     * Converts maps of character numbers into maps of metatile numbers.
     * Existing metatile definitions of the same size are reused, and new blocks are stored in
     * slots that are spare copies of the blank metatile. Maps whose dimensions are not
     * multiples of the metatile size are padded with the blank character.
     *
     * @return the converted maps and metatiles, or null if more than count metatiles are needed
     */
    public static Result compress(List<int[][]> charMaps, int size, HashMap<Integer, int[][]> existing, int count, int blankChar) {
        HashMap<Integer, int[][]> metatiles = new HashMap<>();
        boolean reuse = existing != null && hasSize(existing, size, count);
        for (int i = 0; i < count; i++) {
            metatiles.put(i, reuse ? Globals.cloneGrid(existing.get(i)) : createBlankMetatile(size, blankChar));
        }
        // Index the definitions; the first occurrence of a block wins
        HashMap<String, Integer> index = new HashMap<>();
        String blankKey = getKey(createBlankMetatile(size, blankChar));
        ArrayDeque<Integer> freeSlots = new ArrayDeque<>();
        for (int i = 0; i < count; i++) {
            String key = getKey(metatiles.get(i));
            if (!index.containsKey(key)) {
                index.put(key, i);
            }
            else if (key.equals(blankKey)) {
                freeSlots.add(i);
            }
        }
        List<int[][]> maps = new ArrayList<>();
        for (int[][] charMap : charMaps) {
            int height = (charMap.length + size - 1) / size;
            int width = (charMap[0].length + size - 1) / size;
            int[][] map = new int[height][width];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int[][] block = new int[size][size];
                    for (int my = 0; my < size; my++) {
                        for (int mx = 0; mx < size; mx++) {
                            int cy = y * size + my;
                            int cx = x * size + mx;
                            block[my][mx] = cy < charMap.length && cx < charMap[cy].length ? charMap[cy][cx] : blankChar;
                        }
                    }
                    String key = getKey(block);
                    Integer metatileNum = index.get(key);
                    if (metatileNum == null) {
                        if (freeSlots.isEmpty()) {
                            return null;
                        }
                        metatileNum = freeSlots.poll();
                        metatiles.put(metatileNum, block);
                        index.put(key, metatileNum);
                    }
                    map[y][x] = metatileNum;
                }
            }
            maps.add(map);
        }
        return new Result(maps, metatiles);
    }

    private static boolean hasSize(HashMap<Integer, int[][]> metatiles, int size, int count) {
        for (int i = 0; i < count; i++) {
            int[][] metatile = metatiles.get(i);
            if (metatile == null || metatile.length != size || metatile[0].length != size) {
                return false;
            }
        }
        return true;
    }

    private static String getKey(int[][] block) {
        return Arrays.deepToString(block);
    }
}
