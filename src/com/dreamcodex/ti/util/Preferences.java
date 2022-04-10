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
    private ColorMode colorMode = ColorMode.COLOR_MODE_GRAPHICS_1;
    private int viewScale = 3;
    private boolean textCursor = false;
    private boolean showGrid = true;
    private int gridScale = 1;
    private boolean showPosition = true;
    private boolean base0Position = true;
    private boolean viewCharLayer = true;
    private boolean viewSpriteLayer = true;
    private boolean magnifySprites = false;
    private boolean snapSpritesToGrid = false;
    private boolean showSpritesPerLine = false;
    protected boolean exportComments = true;
    protected boolean includeCharNumbers = true;
    protected boolean currentMapOnly = false;
    protected boolean swapBoth = true;
    protected boolean swapImages = true;
    protected boolean allMaps = true;
    protected boolean wrap = false;
    protected boolean includeCharData = true;
    protected boolean includeSpriteData = false;
    protected boolean includeColorData = true;
    protected boolean includeMapData = true;
    protected boolean excludeBlank = false;
    protected int characterSetCapacity = CHARACTER_SET_BASIC;
    protected int defStartChar = TIGlobals.BASIC_FIRST_CHAR;
    protected int defEndChar = TIGlobals.BASIC_LAST_CHAR;
    protected int defStartSprite = TIGlobals.MIN_SPRITE;
    protected int defEndSprite = TIGlobals.MAX_SPRITE;
    protected int compression = MagellanExportDialog.COMPRESSION_NONE;
    protected TransitionType transitionType = TransitionType.BOTTOM_TO_TOP;
    protected int scrollFrames = 0;
    protected ArrayList<String> recentFiles = new ArrayList<String>();
    protected String currentDirectory;

    public ColorMode getColorMode() {
        return colorMode;
    }

    public void setColorMode(ColorMode colorMode) {
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

    public boolean isViewCharLayer() {
        return viewCharLayer;
    }

    public void setViewCharLayer(boolean viewCharLayer) {
        this.viewCharLayer = viewCharLayer;
    }

    public boolean isViewSpriteLayer() {
        return viewSpriteLayer;
    }

    public void setViewSpriteLayer(boolean viewSpriteLayer) {
        this.viewSpriteLayer = viewSpriteLayer;
    }

    public boolean isMagnifySprites() {
        return magnifySprites;
    }

    public void setMagnifySprites(boolean magnifySprites) {
        this.magnifySprites = magnifySprites;
    }

    public boolean isSnapSpritesToGrid() {
        return snapSpritesToGrid;
    }

    public void setSnapSpritesToGrid(boolean snapSpritesToGrid) {
        this.snapSpritesToGrid = snapSpritesToGrid;
    }

    public boolean isShowSpritesPerLine() {
        return showSpritesPerLine;
    }

    public void setShowSpritesPerLine(boolean showSpritesPerLine) {
        this.showSpritesPerLine = showSpritesPerLine;
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

    public boolean isIncludeCharData() {
        return includeCharData;
    }

    public void setIncludeCharData(boolean includeCharData) {
        this.includeCharData = includeCharData;
    }

    public boolean isIncludeSpriteData() {
        return includeSpriteData;
    }

    public boolean isIncludeColorData() {
        return includeColorData;
    }

    public void setIncludeColorData(boolean includeColorData) {
        this.includeColorData = includeColorData;
    }

    public boolean isIncludeMapData() {
        return includeMapData;
    }

    public void setIncludeMapData(boolean includeMapData) {
        this.includeMapData = includeMapData;
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

    public TransitionType getTransitionType() {
        return transitionType;
    }

    public void setTransitionType(TransitionType transitionType) {
        this.transitionType = transitionType;
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
        File prefsFile = new File(System.getProperty("user.home") + "/Magellan.prefs");
        if (prefsFile.exists()) {
            FileInputStream fis = new FileInputStream(prefsFile);
            appProperties.load(fis);
            fis.close();
        }
        viewScale = getIntegerProperty("magnif", viewScale);
        textCursor = getBooleanProperty("textCursor", textCursor);
        showGrid = getBooleanProperty("showGrid", showGrid);
        gridScale = getIntegerProperty("gridScale", gridScale);
        showPosition = getBooleanProperty("showPosition", showPosition);
        base0Position = getBooleanProperty("base0Position", base0Position);
        viewCharLayer = getBooleanProperty("viewCharLayer", viewCharLayer);
        viewSpriteLayer = getBooleanProperty("viewSpriteLayer", viewSpriteLayer);
        magnifySprites = getBooleanProperty("magnifySprites", magnifySprites);
        snapSpritesToGrid = getBooleanProperty("snapSpritesToGrid", snapSpritesToGrid);
        showSpritesPerLine = getBooleanProperty("showSpritesPerLIne", showSpritesPerLine);
        exportComments = getBooleanProperty("exportComments", exportComments);
        includeCharNumbers = getBooleanProperty("includeCharNumbers", includeCharNumbers);
        currentMapOnly = getBooleanProperty("currentMapOnly", currentMapOnly);
        wrap = getBooleanProperty("wrap", wrap);
        includeCharData = getBooleanProperty("includeCharData", includeCharData);
        includeSpriteData = getBooleanProperty("includeSpriteData", includeSpriteData);
        includeColorData = getBooleanProperty("includeColorData", includeColorData);
        includeMapData = getBooleanProperty("includeMapData", includeMapData);
        excludeBlank = getBooleanProperty("excludeBlank", excludeBlank);
        if (appProperties.getProperty("characterSetSize") != null) {
            characterSetCapacity = getIntegerProperty("characterSetSize", characterSetCapacity);
        } else if (appProperties.getProperty("expandCharacters") != null) {
            characterSetCapacity = appProperties.getProperty("expandCharacters").equalsIgnoreCase("true") ? CHARACTER_SET_EXPANDED : CHARACTER_SET_BASIC;
        }
        colorMode = ColorMode.values()[getIntegerProperty("colorMode", colorMode.ordinal())];
        defStartChar = getIntegerProperty("defStartChar", defStartChar);
        defEndChar = getIntegerProperty("defEndChar", defEndChar);
        defStartSprite = getIntegerProperty("defStartSprite", defStartSprite);
        defEndSprite = getIntegerProperty("defEndSprite", defEndSprite);
        compression = getIntegerProperty("compression", compression);
        transitionType = TransitionType.valueOf(appProperties.getProperty("transitionType", TransitionType.BOTTOM_TO_TOP.name()));
        scrollFrames = getIntegerProperty("scrollFrames", scrollFrames);
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
        appProperties.setProperty("includeCharData", includeCharData ? "true" : "false");
        appProperties.setProperty("includeSpriteData", includeSpriteData ? "true" : "false");
        appProperties.setProperty("includeColorData", includeColorData ? "true" : "false");
        appProperties.setProperty("includeMapData", includeMapData ? "true" : "false");
        appProperties.setProperty("excludeBlank", excludeBlank ? "true" : "false");
        appProperties.setProperty("wrap", wrap ? "true" : "false");
        appProperties.setProperty("characterSetSize", Integer.toString(characterSetCapacity));
        appProperties.setProperty("colorMode", Integer.toString(colorMode.ordinal()));
        appProperties.setProperty("showGrid", showGrid ? "true" : "false");
        appProperties.setProperty("gridScale", Integer.toString(gridScale));
        appProperties.setProperty("showPosition", showPosition ? "true" : "false");
        appProperties.setProperty("base0Position", base0Position ? "true" : "false");
        appProperties.setProperty("viewCharLayer", viewCharLayer ? "true" : "false");
        appProperties.setProperty("viewSpriteLayer", viewSpriteLayer ? "true" : "false");
        appProperties.setProperty("magnifySprites", magnifySprites ? "true" : "false");
        appProperties.setProperty("showSpritesPerLIne", showSpritesPerLine ? "true" : "false");
        appProperties.setProperty("snapSpritesToGrid", snapSpritesToGrid ? "true" : "false");
        appProperties.setProperty("defStartChar", "" + defStartChar);
        appProperties.setProperty("defEndChar", "" + defEndChar);
        appProperties.setProperty("defStartSprite", "" + defStartSprite);
        appProperties.setProperty("defEndSprite", "" + defEndSprite);
        appProperties.setProperty("compression", "" + compression);
        appProperties.setProperty("transitionType", transitionType.name());
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

    private int getIntegerProperty(String propertyName, int defaultValue) {
        if (appProperties.getProperty(propertyName) != null) {
            return Integer.parseInt(appProperties.getProperty(propertyName));
        } else {
            return defaultValue;
        }
    }

    private boolean getBooleanProperty(String propertyName, boolean defaultValue) {
        if (appProperties.getProperty(propertyName) != null) {
            return appProperties.getProperty(propertyName).equalsIgnoreCase("true");
        } else {
            return defaultValue;
        }
    }
}
