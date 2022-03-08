package com.dreamcodex.ti.exporters;

import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Preferences;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class MapImageExporter extends Exporter {

    public MapImageExporter(MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(mapEditor, dataSet, preferences);
    }

    public void writeMapImage(File imageOut) throws IOException {
        String formatName = imageOut.getName().toLowerCase().endsWith("gif") ? "gif" : "png";
        ImageIO.write(mapEditor.getBuffer(), formatName, imageOut);
    }
}
