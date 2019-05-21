package com.dreamcodex.ti.util;

/**
 * Created with IntelliJ IDEA.
 * User: Rasmus
 * Date: 03-07-2014
 * Time: 20:16
 */
public class MetaTile {

    int number;
    int[] tiles;

    public MetaTile(int number, int[] tiles) {
        this.number = number;
        this.tiles = tiles;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public int[] getTiles() {
        return tiles;
    }

    public void setTiles(int[] tiles) {
        this.tiles = tiles;
    }
}
