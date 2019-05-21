package com.dreamcodex.ti.util;

import javax.swing.*;

/**
 * Created with IntelliJ IDEA.
 * User: Rasmus
 * Date: 08-09-13
 * Time: 21:52
 */
public class NamedIcon {

    private Icon icon;
    private String name;

    public NamedIcon(Icon icon, String name) {
        this.icon = icon;
        this.name = name;
    }

    public Icon getIcon() {
        return icon;
    }

    public String toString() {
        return name;
    }
}
