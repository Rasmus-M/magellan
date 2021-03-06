package com.dreamcodex.ti.util;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.component.MagellanExportDialog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import static com.dreamcodex.ti.Magellan.*;

public class Preferences {

    private final Properties appProperties = new Properties();
    private int colorMode = COLOR_MODE_GRAPHICS_1;
    private int viewScale = 3;
    private boolean textCursor = false;
    private boolean showGrid = true;
    private int gridScale = 1;
    private boolean showPosition = true;
    private boolean base0Position = true;
    protected boolean exportComments = true;
    protected boolean includeCharNumbers = true;
    protected boolean currentMapOnly = false;
    protected boolean swapBoth = true;
    protected boolean swapImages = true;
    protected boolean allMaps = true;
    protected boolean wrap = false;
    protected boolean includeSpriteData = false;
    protected boolean excludeBlank = false;
    protected int characterSetCapacity = CHARACTER_SET_BASIC;
    protected int defStartChar = TIGlobals.BASIC_FIRST_CHAR;
    protected int defEndChar = TIGlobals.BASIC_LAST_CHAR;
    protected int defStartSprite = TIGlobals.MIN_SPRITE;
    protected int defEndSprite = TIGlobals.MAX_SPRITE;
    protected int compression = MagellanExportDialog.COMPRESSION_NONE;
    protected int scrollOrientation = SCROLL_ORIENTATION_VERTICAL;
    protected int scrollFrames = 0;
    protected ArrayList<String> recentFiles = new ArrayList<String>();
    protected String currentDirectory;

    public int getColorMode() {
        return colorMode;
    }

    public void setColorMode(int colorMode) {
        this.colorMode = colorMode;
    }

    public int getViewScale() {
        return viewScale;
    }

    public void setViewScale(int viewScale) {
        this.viewScale = viewScale;
    }

    public boolean isTextCursor() {
        return textCursor;
    }

    public void setTextCursor(boolean textCursor) {
        this.textCursor = textCursor;
    }

    public boolean isShowGrid() {
        return showGrid;
    }

    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
    }

    public int getGridScale() {
        return gridScale;
    }

    public void setGridScale(int gridScale) {
        this.gridScale = gridScale;
    }

    public boolean isShowPosition() {
        return showPosition;
    }

    public void setShowPosition(boolean showPosition) {
        this.showPosition = showPosition;
    }

    public boolean isBase0Position() {
        return base0Position;
    }

    public void setBase0Position(boolean base0Position) {
        this.base0Position = base0Position;
    }

    public boolean isExportComments() {
        return exportComments;
    }

    public void setExportComments(boolean exportComments) {
        this.exportComments = exportComments;
    }

    public boolean isIncludeCharNumbers() {
        return includeCharNumbers;
    }

    public void setIncludeCharNumbers(boolean includeCharNumbers) {
        this.includeCharNumbers = includeCharNumbers;
    }

    public boolean isCurrentMapOnly() {
        return currentMapOnly;
    }

    public void setCurrentMapOnly(boolean currentMapOnly) {
        this.currentMapOnly = currentMapOnly;
    }

    public boolean isSwapBoth() {
        return swapBoth;
    }

    public void setSwapBoth(boolean swapBoth) {
        this.swapBoth = swapBoth;
    }

    public boolean isSwapImages() {
        return swapImages;
    }

    public void setSwapImages(boolean swapImages) {
        this.swapImages = swapImages;
    }

    public boolean isAllMaps() {
        return allMaps;
    }

    public void setAllMaps(boolean allMaps) {
        this.allMaps = allMaps;
    }

    public boolean isWrap() {
        return wrap;
    }

    public void setWrap(boolean wrap) {
        this.wrap = wrap;
    }

    public boolean isIncludeSpriteData() {
        return includeSpriteData;
    }

    public void setIncludeSpriteData(boolean includeSpriteData) {
        this.includeSpriteData = includeSpriteData;
    }

    public boolean isExcludeBlank() {
        return excludeBlank;
    }

    public void setExcludeBlank(boolean excludeBlank) {
        this.excludeBlank = excludeBlank;
    }

    public int getCharacterSetCapacity() {
        return characterSetCapacity;
    }

    public void setCharacterSetCapacity(int characterSetCapacity) {
        this.characterSetCapacity = characterSetCapacity;
    }

    public int getDefStartChar() {
        return defStartChar;
    }

    public void setDefStartChar(int defStartChar) {
        this.defStartChar = defStartChar;
    }

    public int getDefEndChar() {
        return defEndChar;
    }

    public void setDefEndChar(int defEndChar) {
        this.defEndChar = defEndChar;
    }

    public int getDefStartSprite() {
        return defStartSprite;
    }

    public void setDefStartSprite(int defStartSprite) {
        this.defStartSprite = defStartSprite;
    }

    public int getDefEndSprite() {
        return defEndSprite;
    }

    public void setDefEndSprite(int defEndSprite) {
        this.defEndSprite = defEndSprite;
    }

    public int getCompression() {
        return compression;
    }

    public void setCompression(int compression) {
        this.compression = compression;
    }

    public int getScrollOrientation() {
        return scrollOrientation;
    }

    public void setScrollOrientation(int scrollOrientation) {
        this.scrollOrientation = scrollOrientation;
    }

    public int getScrollFrames() {
        return scrollFrames;
    }

    public void setScrollFrames(int scrollFrames) {
        this.scrollFrames = scrollFrames;
    }

    public ArrayList<String> getRecentFiles() {
        return recentFiles;
    }

    public void setRecentFiles(ArrayList<String> recentFiles) {
        this.recentFiles = recentFiles;
    }

    public String getCurrentDirectory() {
        return currentDirectory;
    }

    public void setCurrentDirectory(String currentDirectory) {
        this.currentDirectory = currentDirectory;
    }

    public int getCharacterSetStart() {
        return Magellan.getCharacterSetStart(characterSetCapacity);
    }

    public int getCharacterSetEnd() {
        return Magellan.getCharacterSetEnd(characterSetCapacity);
    }

    public int getCharacterSetSize() {
        return Magellan.getCharacterSetSize(characterSetCapacity);
    }

    public int getSpriteSetEnd() {
        return Magellan.getSpriteSetEnd(characterSetCapacity);
    }

    public int getSpriteSetSize() {
        return Magellan.getSpriteSetSize(characterSetCapacity);
    }

    public void readPreferences() throws IOException {
        // Read application properties (if exist)
        File prefsFile = new File(System.getProperty("user.home") + "/Magellan.prefs");
        if (prefsFile.exists()) {
            FileInputStream fis = new FileInputStream(prefsFile);
            appProperties.load(fis);
            fis.close();
        }
        if (appProperties.getProperty("magnif") != null) {
            viewScale = Integer.parseInt(appProperties.getProperty("magnif"));
        }
        if (appProperties.getProperty("textCursor") != null) {
            textCursor = appProperties.getProperty("textCursor").equalsIgnoreCase("true");
        }
        if (appProperties.getProperty("showGrid") != null) {
            showGrid = appProperties.getProperty("showGrid").equalsIgnoreCase("true");
        }
        if (appProperties.getProperty("gridScale") != null) {
            gridScale = Integer.parseInt(appProperties.getProperty("gridScale"));
        }
        if (appProperties.getProperty("showPosition") != null) {
            showPosition = appProperties.getProperty("showPosition").equalsIgnoreCase("true");
        }
        if (appProperties.getProperty("base0Position") != null) {
            base0Position = appProperties.getProperty("base0Position").equalsIgnoreCase("true");
        }
        if (appProperties.getProperty("exportComments") != null) {
            exportComments = appProperties.getProperty("exportComments").equalsIgnoreCase("true");
        }
        if (appProperties.getProperty("includeCharNumbers") != null) {
            includeCharNumbers = appProperties.getProperty("includeCharNumbers").equalsIgnoreCase("true");
        }
        if (appProperties.getProperty("currentMapOnly") != null) {
            currentMapOnly = appProperties.getProperty("currentMapOnly").equalsIgnoreCase("true");
        }
        if (appProperties.getProperty("wrap") != null) {
            wrap = appProperties.getProperty("wrap").equalsIgnoreCase("true");
        }
        if (appProperties.getProperty("includeSpriteData") != null) {
            includeSpriteData = appProperties.getProperty("includeSpriteData").equalsIgnoreCase("true");
        }
        if (appProperties.getProperty("excludeBlank") != null) {
            excludeBlank = appProperties.getProperty("excludeBlank").equalsIgnoreCase("true");
        }
        if (appProperties.getProperty("characterSetSize") != null) {
            characterSetCapacity = Integer.parseInt(appProperties.getProperty("characterSetSize"));
        } else if (appProperties.getProperty("expandCharacters") != null) {
            characterSetCapacity = appProperties.getProperty("expandCharacters").equalsIgnoreCase("true") ? CHARACTER_SET_EXPANDED : CHARACTER_SET_BASIC;
        }
        if (appProperties.getProperty("colorMode") != null) {
            colorMode = Integer.parseInt(appProperties.getProperty("colorMode"));
        }
        if (appProperties.getProperty("defStartChar") != null) {
            defStartChar = Integer.parseInt(appProperties.getProperty("defStartChar"));
        }
        if (appProperties.getProperty("defEndChar") != null) {
            defEndChar = Integer.parseInt(appProperties.getProperty("defEndChar"));
        }
        if (appProperties.getProperty("defStartSprite") != null) {
            defStartSprite = Integer.parseInt(appProperties.getProperty("defStartSprite"));
        }
        if (appProperties.getProperty("defEndSprite") != null) {
            defEndSprite = Integer.parseInt(appProperties.getProperty("defEndSprite"));
        }
        if (appProperties.getProperty("compression") != null) {
            compression = Integer.parseInt(appProperties.getProperty("compression"));
        }
        if (appProperties.getProperty("scrollOrientation") != null) {
            scrollOrientation = Integer.parseInt(appProperties.getProperty("scrollOrientation"));
        }
        if (appProperties.getProperty("scrollFrames") != null) {
            scrollFrames = Integer.parseInt(appProperties.getProperty("scrollFrames"));
        }
        currentDirectory = appProperties.getProperty("filePath");
        if (currentDirectory == null || currentDirectory.length() == 0) {
            currentDirectory = ".";
        }
        String recentFileList = appProperties.getProperty("recentFiles");
        if (recentFileList != null) {
            String[] recentFilesArray = recentFileList.split("\\|");
            for (int i = recentFilesArray.length - 1; i >= 0; i--) {
                addRecentFile(recentFilesArray[i]);
            }
        }
    }

    public void savePreferences() throws IOException {
        FileOutputStream fos = new FileOutputStream(System.getProperty("user.home") + "/Magellan.prefs");
        appProperties.setProperty("magnif", "" + viewScale);
        appProperties.setProperty("textCursor", textCursor ? "true" : "false");
        appProperties.setProperty("exportComments", exportComments ? "true" : "false");
        appProperties.setProperty("includeCharNumbers", includeCharNumbers ? "true" : "false");
        appProperties.setProperty("currentMapOnly", currentMapOnly ? "true" : "false");
        appProperties.setProperty("includeSpriteData", includeSpriteData ? "true" : "false");
        appProperties.setProperty("excludeBlank", excludeBlank ? "true" : "false");
        appProperties.setProperty("wrap", wrap ? "true" : "false");
        appProperties.setProperty("characterSetSize", Integer.toString(characterSetCapacity));
        appProperties.setProperty("colorMode", Integer.toString(colorMode));
        appProperties.setProperty("showGrid", showGrid ? "true" : "false");
        appProperties.setProperty("gridScale", Integer.toString(gridScale));
        appProperties.setProperty("showPosition", showPosition ? "true" : "false");
        appProperties.setProperty("base0Position", base0Position ? "true" : "false");
        appProperties.setProperty("defStartChar", "" + defStartChar);
        appProperties.setProperty("defEndChar", "" + defEndChar);
        appProperties.setProperty("defStartSprite", "" + defStartSprite);
        appProperties.setProperty("defEndSprite", "" + defEndSprite);
        appProperties.setProperty("compression", "" + compression);
        appProperties.setProperty("scrollOrientation", "" + scrollOrientation);
        appProperties.setProperty("scrollFrames", "" + scrollFrames);
        appProperties.setProperty("filePath", currentDirectory != null ? currentDirectory : ".");
        StringBuilder recentFileList = new StringBuilder();
        for (String filePath : recentFiles) {
            if (filePath != null && new File(filePath).exists()) {
                if (recentFileList.length() > 0) {
                    recentFileList.append("|");
                }
                recentFileList.append(filePath);
            }
        }
        appProperties.setProperty("recentFiles", recentFileList.toString());
        appProperties.store(fos, null);
        fos.flush();
        fos.close();
    }

    public void addRecentFile(String filePath) {
        recentFiles.remove(filePath);
        recentFiles.add(0, filePath);
        while (recentFiles.size() > 10) {
            recentFiles.remove(recentFiles.size() - 1);
        }
    }
}
