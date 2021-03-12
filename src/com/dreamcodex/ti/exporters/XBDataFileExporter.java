package com.dreamcodex.ti.exporters;

import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class XBDataFileExporter extends Exporter {

    public XBDataFileExporter(MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(mapEditor, dataSet, preferences);
    }

    public void writeXBDataFile(File mapDataFile, int startChar, int endChar, int codeLine, int charLine, int mapLine, int interLine, int exportType, boolean includeComments, boolean currMapOnly, boolean excludeBlank) throws IOException {
        int currLine = codeLine;
        int itemCount = 0;
        int colorDataStart = 0;
        int colorSetStart = (int) (Math.floor(startChar / 8));
        int colorSetEnd = (int) (Math.floor(endChar / 8));
        int colorSetNum = (int) (Math.floor((startChar - TIGlobals.BASIC_FIRST_CHAR) / 8)) + 1;
        int colorCount = (colorSetEnd - colorSetStart) + 1;
        int charDataStart;
        int charCount = 0;
        int[] mapDataStart = new int[mapEditor.getMapCount()];
        int mapCols;
        int mapRows;
        StringBuffer sbOutLine = new StringBuffer();
        charDataStart=charLine;

        mapEditor.storeCurrentMap();
        BufferedWriter bw = new BufferedWriter(new FileWriter(mapDataFile));
        if (exportType == Globals.XB_PROGRAM || exportType == Globals.XB256_PROGRAM || exportType == Globals.BASIC_DATA ) {
            colorDataStart = currLine;
            if (includeComments) {
                bw.write("REM COLORSET DECLARATIONS");
                bw.newLine();
                bw.newLine();
            }
            int blockCount = 0;
            bw.write(currLine + " DATA ");
            for (int i = colorSetStart; i < (colorSetStart + colorCount); i++) {
                if (blockCount > 7) {
                    bw.newLine();
                    currLine = currLine + interLine;
                    bw.write(currLine + " DATA ");
                    blockCount = 0;
                }
                if (blockCount > 0) {
                    bw.write(",");
                }
                bw.write(colorSetNum + "," + (clrSets[i][Globals.INDEX_CLR_FORE] + 1) + "," + (clrSets[i][Globals.INDEX_CLR_BACK] + 1));
                colorSetNum++;
                blockCount++;
            }
            bw.newLine();
            currLine = currLine + interLine;
            if (includeComments) {
                bw.newLine();
                bw.write("REM CHARACTER DATA");
                bw.newLine();
                bw.newLine();
            }
            currLine = charLine;

            for (int i = startChar; i <= endChar; i++) {
                String hexstr;
                if (charGrids.get(i) != null) {
                    hexstr = Globals.getHexString(charGrids.get(i));
                } else {
                    hexstr = Globals.BLANKCHAR;
                }
                if (!hexstr.equals(Globals.BLANKCHAR) || !excludeBlank) {
                    if (itemCount == 0) {
                        sbOutLine.append(currLine + " DATA ");
                    } else {
                        sbOutLine.append(",");
                    }
                    sbOutLine.append(i + "," + '"' + hexstr.toUpperCase() + '"');
                    itemCount++;
                    if (itemCount >= 4) {
                        bw.write(sbOutLine.toString());
                        bw.newLine();
                        sbOutLine.delete(0, sbOutLine.length());
                        currLine = currLine + interLine;
                        itemCount = 0;
                    }
                    charCount++;
                }
            }
            if (sbOutLine.length() > 0) {
                bw.write(sbOutLine.toString());
                bw.newLine();
            }
            currLine = currLine + interLine;
            itemCount = 0;
            sbOutLine.delete(0, sbOutLine.length());
            currLine = mapLine;
            if (includeComments) {
                bw.newLine();
                bw.write("REM MAP DATA");
                bw.newLine();
            }
            int mapTicker = 1;
            for (int m = 0; m < mapEditor.getMapCount(); m++) {
                if (!currMapOnly || m == mapEditor.getCurrentMapId()) {
                    int[][] mapToSave = mapEditor.getMapData(m);
                    mapCols = mapToSave[0].length;
                    mapRows = mapToSave.length;
                    mapDataStart[m] = currLine;
                    if (includeComments) {
                        bw.newLine();
                        bw.write("REM MAP #" + mapTicker);
                        bw.newLine();
                    }
                    int mapColor = mapEditor.getScreenColorTI(m);
                    if (includeComments) {
                        bw.write("REM MAP #" + mapTicker + " WIDTH, HEIGHT, SCREEN COLOR");
                        bw.newLine();
                    }
                    bw.write(currLine + " DATA " + mapCols + "," + mapRows + "," + mapColor);
                    bw.newLine();
                    if (includeComments) {
                        bw.write("REM MAP #" + mapTicker + " DATA");
                        bw.newLine();
                    }
                    currLine = currLine + interLine;
                    int mapTileVal = TIGlobals.SPACECHAR;
                    for (int y = 0; y < mapRows; y++) {
                        for (int x = 0; x < mapCols; x++) {
                            if (itemCount == 0) {
                                sbOutLine.append(currLine + " DATA ");
                            } else {
                                sbOutLine.append(",");
                            }
                            mapTileVal = ((mapToSave[y][x] < startChar || mapToSave[y][x] > endChar) ? TIGlobals.SPACECHAR : mapToSave[y][x]);
                            sbOutLine.append("" + mapTileVal);
                            itemCount++;
                            if (itemCount >= 16) {
                                bw.write(sbOutLine.toString());
                                bw.newLine();
                                sbOutLine.delete(0, sbOutLine.length());
                                currLine = currLine + interLine;
                                itemCount = 0;
                            }
                        }
                    }
                    mapTicker++;
                }
            }
        }
        if (exportType == Globals.XB_PROGRAM || exportType == Globals.XB256_PROGRAM) {
            if (exportType == Globals.XB256_PROGRAM) {
                bw.newLine();
                bw.write(currLine + " CALL LINK(\"SCRN2\")");
                currLine = currLine + interLine;
                bw.newLine();
            }
            if (includeComments) {
                bw.newLine();
                bw.write("REM LOAD COLORSET");
                bw.newLine();
                bw.newLine();
            }
            bw.write(currLine + " RESTORE " + colorDataStart + "::FOR C=1 TO " + colorCount + "::READ CS,CF,CB::" + (exportType == Globals.XB_PROGRAM ? "CALL COLOR(CS,CF,CB)" : "CALL LINK(\"COLOR2\",CS,CF,CB)") + "::NEXT C");
            currLine = currLine + interLine;
            bw.newLine();
            if (includeComments) {
                bw.newLine();
                bw.write("REM LOAD CHARACTERS");
                bw.newLine();
                bw.newLine();
            }
            bw.write(currLine + " RESTORE " + charDataStart + "::FOR C=1 TO " + charCount + "::READ CN,CC$::" + (exportType == Globals.XB_PROGRAM ? "CALL CHAR(CN,CC$)" : "CALL LINK(\"CHAR2\",CN,CC$)") + "::NEXT C");
            currLine = currLine + interLine;
            bw.newLine();
            if (includeComments) {
                bw.newLine();
                bw.write("REM DRAW MAP(S)");
                bw.newLine();
                bw.newLine();
            }
            for (int mp = 0; mp < mapEditor.getMapCount(); mp++) {
                if (!currMapOnly || mp == mapEditor.getCurrentMapId()) {
                    bw.write(currLine + " CALL CLEAR");
                    currLine = currLine + interLine;
                    bw.newLine();
                    bw.write(currLine + " RESTORE " + mapDataStart[mp]);
                    currLine = currLine + interLine;
                    bw.newLine();
                    bw.write(currLine + " READ W,H,SC::CALL SCREEN(SC)::CALL CLEAR");
                    currLine = currLine + interLine;
                    bw.newLine();
                    bw.write(currLine + " FOR Y=1 TO H");
                    currLine = currLine + interLine;
                    bw.newLine();
                    bw.write(currLine + " FOR X=1 TO W");
                    currLine = currLine + interLine;
                    bw.newLine();
                    bw.write(currLine + " READ CP::CALL VCHAR(Y,X,CP)");
                    currLine = currLine + interLine;
                    bw.newLine();
                    bw.write(currLine + " NEXT X");
                    currLine = currLine + interLine;
                    bw.newLine();
                    bw.write(currLine + " NEXT Y");
                    currLine = currLine + interLine;
                    bw.newLine();
                    bw.write(currLine + " CALL KEY(0,K,S)::IF S=0 THEN " + currLine);
                    currLine = currLine + interLine;
                    bw.newLine();
                }
            }
            bw.write(currLine + " END");
            currLine = currLine + interLine;
            bw.newLine();
        }

        if (exportType == Globals.BASIC_PROGRAM) {


            currLine = codeLine;


            if (includeComments) {
                bw.newLine();
                bw.write("REM Define Color Set");
                bw.newLine();
                bw.newLine();
            }

            colorSetNum = (int) (Math.floor((startChar - TIGlobals.BASIC_FIRST_CHAR) / 8)) + 1;

            for (int i = colorSetStart; i < (colorSetStart + colorCount); i++) {

                bw.write(currLine + " CALL COLOR(");
                bw.write(colorSetNum + "," + (clrSets[i][Globals.INDEX_CLR_FORE] + 1) + "," + (clrSets[i][Globals.INDEX_CLR_BACK] + 1));
                colorSetNum++;
                bw.write(")");
                bw.newLine();
                currLine = currLine + interLine;
            }
            bw.newLine();
            currLine = currLine + interLine;

            currLine = charLine;
            if (includeComments) {
                bw.newLine();
                bw.write("REM Define Characters");
                bw.newLine();
                bw.newLine();
            }


            for (int i = startChar; i <= endChar; i++) {
                String hexstr;
                if (charGrids.get(i) != null) {
                    hexstr = Globals.getHexString(charGrids.get(i));
                } else {
                    hexstr = Globals.BLANKCHAR;
                }

                sbOutLine.delete(0, sbOutLine.length());
                if (!hexstr.equals(Globals.BLANKCHAR) || !excludeBlank) {
                    sbOutLine.append(currLine + " CALL CHAR(");
                    sbOutLine.append(i + "," + '"' + hexstr.toUpperCase() + '"');
                    sbOutLine.append(")");
                    bw.write(sbOutLine.toString());
                    bw.newLine();
                    sbOutLine.delete(0, sbOutLine.length());
                    currLine = currLine + interLine;
                }
            }
            if (sbOutLine.length() > 0) {
                bw.write(sbOutLine.toString());
                bw.newLine();
            }

            if (currLine > mapLine) {
                currLine += 100;
            } else {
                currLine = mapLine;
            }

            if (includeComments) {
                bw.newLine();
                bw.write("REM PRINT Map");
                bw.newLine();
            }


            int mapTicker = 1;
            for (int m = 0; m < mapEditor.getMapCount(); m++) {
                if (!currMapOnly || m == mapEditor.getCurrentMapId()) {
                    int[][] mapToSave = mapEditor.getMapData(m);
                    mapCols = mapToSave[0].length;
                    mapRows = mapToSave.length;
                    if (m!=0){
                        currLine=100+((int)(currLine/100)*100);
                    };
                    mapDataStart[m] = currLine;
                    if (includeComments) {
                        bw.newLine();
                        bw.write("REM MAP #" + mapTicker);
                        bw.newLine();
                    }


                    int endChar2 = endChar;
                    if (endChar2 > 127) {
                        endChar2 = 127;
                    }

                    int mapTileVal = TIGlobals.SPACECHAR;

                    for (int y = 0; y < mapRows - 1; y++) {
                        Boolean AllSpaces=true;
                        for (int x = 2; x < mapCols - 2; x++) {
                            mapTileVal = ((mapToSave[y][x] > endChar2) ? TIGlobals.SPACECHAR : mapToSave[y][x]);
                            if (mapTileVal == 34) {
                                mapTileVal = TIGlobals.SPACECHAR;
                            }
                            if (mapTileVal!=TIGlobals.SPACECHAR)
                            {
                                AllSpaces=false;
                            }
                        }
                        if (AllSpaces)
                        {
                            if (itemCount > 0) {
                                sbOutLine.append('"');
                                bw.write(sbOutLine.toString());
                                bw.newLine();
                                sbOutLine.delete(0, sbOutLine.length());
                                itemCount = 0;
                            }
                            bw.write(currLine + " PRINT ");
                            currLine = currLine + interLine;
                            bw.newLine();
                        }
                        else {
                            for (int x = 2; x < mapCols - 2; x++) {
                                if (itemCount == 0) {
                                    sbOutLine.append(currLine + " PRINT " + '"');
                                    currLine = currLine + interLine;
                                }

                                mapTileVal = ((mapToSave[y][x] > endChar2) ? TIGlobals.SPACECHAR : mapToSave[y][x]);
                                if (mapTileVal == 34) {
                                    mapTileVal = TIGlobals.SPACECHAR;
                                }
                                sbOutLine.append((char) mapTileVal);
                                itemCount++;
                                if (itemCount >= 84) {
                                    sbOutLine.append('"');
                                    bw.write(sbOutLine.toString());
                                    bw.newLine();
                                    sbOutLine.delete(0, sbOutLine.length());
                                    itemCount = 0;
                                }
                            }
                        }
                    }
                    if (itemCount != 0) {
                        sbOutLine.append('"');
                        bw.write(sbOutLine.toString());
                        bw.newLine();
                        sbOutLine.delete(0, sbOutLine.length());
                        itemCount = 0;
                    }

                    mapTicker++;
                }
            }

            if (itemCount != 0) {
                sbOutLine.append('"');
                bw.write(sbOutLine.toString());
                bw.newLine();
                sbOutLine.delete(0, sbOutLine.length());
                itemCount = 0;
            }

            currLine += 100;

            int[] mapFillChars = new int[mapEditor.getMapCount() + 1];
            int[] illegalChars = new int[mapEditor.getMapCount() + 1];
            int totalIllegalChars=0;
            mapTicker = 1;
            for (int m = 0; m < mapEditor.getMapCount(); m++) {
                if (!currMapOnly || m == mapEditor.getCurrentMapId()) {
                    int[][] mapToSave = mapEditor.getMapData(m);
                    mapCols = mapToSave[0].length;
                    mapRows = mapToSave.length;


                    if (includeComments) {
                        bw.newLine();
                        bw.write("REM Optional MAP #" + mapTicker + " DATA");
                        bw.newLine();
                    }

                    mapDataStart[m] = currLine;

                    int mapTileVal = TIGlobals.SPACECHAR;
                    for (int y = 0; y < mapRows; y++) {
                        for (int x = 0; x < mapCols; x++) {

                            mapTileVal = ((mapToSave[y][x] < 33 || mapToSave[y][x] > endChar) ? TIGlobals.SPACECHAR : mapToSave[y][x]);
                            if (y == 23 || x < 2 || x > 29 || mapTileVal > 127 || mapTileVal == 34) {

                                if (mapTileVal != TIGlobals.SPACECHAR) {
                                    illegalChars[m]+=1;
                                    totalIllegalChars++;
                                    if (itemCount == 0) {
                                        sbOutLine.append(currLine + " DATA ");
                                    } else {
                                        sbOutLine.append(",");
                                    }
                                    sbOutLine.append((y + 1) + "," + (x + 1) + "," + mapTileVal);
                                    mapFillChars[mapTicker] += 1;
                                    itemCount++;
                                }
                            }
                            if (itemCount >= 8) {
                                bw.write(sbOutLine.toString());
                                bw.newLine();
                                sbOutLine.delete(0, sbOutLine.length());
                                currLine = currLine + interLine;
                                itemCount = 0;
                            }
                        }
                    }
                    if (itemCount >= 0) {
                        bw.write(sbOutLine.toString());
                        bw.newLine();
                        sbOutLine.delete(0, sbOutLine.length());
                        currLine = currLine + interLine;
                        itemCount = 0;
                    }
                    mapTicker++;
                }
            }

            if (totalIllegalChars!=0) {
                currLine += 10;

                if (includeComments) {
                    bw.newLine();
                    bw.write("REM Complete Map with borders and other illegal characters not supported by Print");
                    bw.newLine();
                    bw.write("REM If you did not use any illegal characters or borders then you can omit this code");
                    bw.newLine();
                    bw.newLine();
                }

                for (int mp = 0; mp < mapEditor.getMapCount(); mp++) {
                    if (illegalChars[mp] > 0) {
                        if (!currMapOnly || mp == mapEditor.getCurrentMapId()) {

                            bw.write(currLine + " RESTORE " + mapDataStart[mp]);
                            currLine = currLine + interLine;
                            bw.newLine();

                            bw.write(currLine + " FOR FILL=1 TO " + mapFillChars[mp + 1]);
                            currLine = currLine + interLine;
                            bw.newLine();
                            bw.write(currLine + " READ Y,X,CP");
                            currLine = currLine + interLine;
                            bw.newLine();
                            bw.write(currLine + " CALL VCHAR(Y,X,CP)");
                            currLine = currLine + interLine;
                            bw.newLine();
                            bw.write(currLine + " NEXT FILL");
                            currLine = currLine + interLine;

                            bw.newLine();
                            bw.write(currLine + " CALL KEY(0,K,S)");
                            currLine = currLine + interLine;

                            bw.newLine();
                            bw.write(currLine + " IF S=0 THEN " + (currLine - interLine));
                            currLine = currLine + interLine;
                            bw.newLine();
                        }
                    }
                }
                bw.write(currLine + " END");
                currLine = currLine + interLine;
                bw.newLine();
            }
        }

        bw.flush();
        bw.close();
    }
}
