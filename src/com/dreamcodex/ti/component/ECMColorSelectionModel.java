package com.dreamcodex.ti.component;

import javax.swing.colorchooser.DefaultColorSelectionModel;
import java.awt.*;

/**
 * Created with IntelliJ IDEA.
 * User: Rasmus
 * Date: 29-11-13
 * Time: 17:48
 */
public class ECMColorSelectionModel extends DefaultColorSelectionModel {

    public ECMColorSelectionModel(Color color) {
        super(color);
    }

    public void setSelectedColor(Color color) {
        // Color oldColor = getSelectedColor();
        int newRed = (int) Math.round((double) color.getRed() / 17d) * 17;
        int newGreen = (int) Math.round((double) color.getGreen() / 17d) * 17;
        int newBlue =  (int) Math.round((double) color.getBlue() / 17d) * 17;
        Color newColor = new Color(newRed, newGreen, newBlue, 255);
        super.setSelectedColor(newColor);
        // if (oldColor.equals(newColor)) {
        //     fireStateChanged();
        // }
    }
}
