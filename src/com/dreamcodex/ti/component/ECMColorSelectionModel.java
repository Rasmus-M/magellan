package com.dreamcodex.ti.component;

import javax.swing.colorchooser.DefaultColorSelectionModel;
import java.awt.*;

import static com.dreamcodex.ti.util.Globals.getECMSafeColor;

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
        super.setSelectedColor(getECMSafeColor(color));
    }
}
