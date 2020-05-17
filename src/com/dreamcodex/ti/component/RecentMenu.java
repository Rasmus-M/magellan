package com.dreamcodex.ti.component;

import com.dreamcodex.ti.util.Globals;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

public class RecentMenu extends JMenu implements MenuListener {

    private final ArrayList<String> recentFiles;
    private final ActionListener actionListener;

    public RecentMenu(ArrayList<String> recentFiles, ActionListener actionListener) {
        super("Open Recent");
        this.recentFiles = recentFiles;
        this.actionListener = actionListener;
        addMenuListener(this);
    }

    @Override
    public void menuSelected(MenuEvent e) {
        removeAll();
        for (String recentFile : recentFiles) {
            File file = new File(recentFile);
            if (file.exists()) {
                JMenuItem recentFileItem = new JMenuItem(file.getName());
                recentFileItem.setActionCommand(Globals.CMD_OPEN_RECENT + file.getAbsolutePath());
                recentFileItem.addActionListener(actionListener);
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
