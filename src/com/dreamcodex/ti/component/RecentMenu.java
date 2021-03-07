package com.dreamcodex.ti.component;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.importing.OpenDataFileAction;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Preferences;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.io.File;
import java.util.ArrayList;

public class RecentMenu extends JMenu implements MenuListener {

    private final ArrayList<String> recentFiles;
    private final Magellan parent;
    private final MapEditor mapEditor;
    private final DataSet dataSet;
    private final Preferences preferences;

    public RecentMenu(ArrayList<String> recentFiles, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super("Open Recent");
        this.recentFiles = recentFiles;
        this.parent = parent;
        this.mapEditor = mapEditor;
        this.dataSet = dataSet;
        this.preferences = preferences;
        addMenuListener(this);
    }

    @Override
    public void menuSelected(MenuEvent e) {
        removeAll();
        for (String recentFile : recentFiles) {
            File file = new File(recentFile);
            if (file.exists()) {
                JMenuItem recentFileItem = new JMenuItem();
                recentFileItem.setAction(new OpenDataFileAction(file.getName(), file.getAbsolutePath(), parent, mapEditor, dataSet, preferences));
                add(recentFileItem);
            }
        }
    }

    @Override
    public void menuDeselected(MenuEvent e) {

    }

    @Override
    public void menuCanceled(MenuEvent e) {

    }
}
