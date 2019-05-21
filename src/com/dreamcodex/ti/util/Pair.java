package com.dreamcodex.ti.util;

import java.util.Comparator;

/**
 * Created with IntelliJ IDEA.
 * User: Rasmus
 * Date: 17-12-13
 * Time: 19:07
 */
public class Pair<S, T> {

    private S first;
    private T second;

    public Pair(S first, T second) {
        this.first = first;
        this.second = second;
    }

    public S getFirst() {
        return first;
    }

    public void setFirst(S first) {
        this.first = first;
    }

    public T getSecond() {
        return second;
    }

    public void setSecond(T second) {
        this.second = second;
    }
}
