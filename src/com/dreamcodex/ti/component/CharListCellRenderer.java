package com.dreamcodex.ti.component;

import com.dreamcodex.ti.util.NamedIcon;

import javax.swing.*;
import java.awt.*;

/**
 * Created with IntelliJ IDEA.
 * User: Rasmus
 * Date: 08-09-13
 * Time: 21:53
 */
public class CharListCellRenderer extends DefaultListCellRenderer {

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        setIcon(((NamedIcon) value).getIcon());
        return component;
    }
}
