package com.dreamcodex.ti.actions.app;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.EditorAction;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Preferences;

import javax.swing.JOptionPane;
import java.awt.event.ActionEvent;

public class NewProjectAction extends EditorAction {

    public NewProjectAction(String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
    }

    @Override
    protected void performAction(ActionEvent e) {
        int userResponse = parent.showConfirmation("Confirm New Project", "This will delete all current data.\n\rAre you sure?", false);
        if (userResponse == JOptionPane.YES_OPTION) {
            parent.newProject();
            parent.setAppTitle();
            parent.editDefault();
        }
    }
}
