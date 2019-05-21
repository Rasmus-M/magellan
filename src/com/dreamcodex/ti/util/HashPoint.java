package com.dreamcodex.ti.util;

import java.awt.*;

/**
 * Created with IntelliJ IDEA.
 * User: Rasmus
 * Date: 03-04-2015
 * Time: 08:34
 */
public class HashPoint extends Point {

    public HashPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public HashPoint(Point p) {
        x = p.x;
        y = p.y;
    }

    public int hashCode() {
        return y << 16 | x;
    }
}
