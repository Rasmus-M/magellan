package com.dreamcodex.ti.importers;

import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class CharacterDataImporter extends Importer {

    public CharacterDataImporter(MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(mapEditor, dataSet, preferences);
    }

    public void readCharacterData(File mapDataFile) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(mapDataFile));
        String lineIn = "";
        int charStart = TIGlobals.BASIC_FIRST_CHAR;
        int charEnd = TIGlobals.BASIC_LAST_CHAR;
        int charRead = charStart;
        do {
            lineIn = br.readLine();
            if (lineIn == null) {
                break;
            }
            else {
                if (lineIn.startsWith(Globals.KEY_CHARDATA)) {
                    lineIn = lineIn.substring(Globals.KEY_CHARDATA.length());
                    charGrids.put(charRead, Globals.getIntGrid(lineIn, 8));
                    charRead++;
                }
                else if (lineIn.startsWith(Globals.KEY_CHARRANG)) {
                    lineIn = lineIn.substring(Globals.KEY_CHARRANG.length());
                    charStart = Integer.parseInt(lineIn.substring(0, lineIn.indexOf("|")));
                    charEnd = Integer.parseInt(lineIn.substring(lineIn.indexOf("|") + 1));
                    charRead = charStart;
                }
            }
        } while (lineIn != null);
        br.close();
    }
}
