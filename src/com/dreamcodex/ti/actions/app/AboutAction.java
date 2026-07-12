package com.dreamcodex.ti.actions.app;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.EditorAction;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Preferences;

import java.awt.event.ActionEvent;

public class AboutAction extends EditorAction {

    public AboutAction(String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
    }

    @Override
    protected void performAction(ActionEvent e) {
        parent.showInformation(
            "About Magellan",
            "<html>" +
                "<h1>Magellan, version " + Magellan.VERSION_NUMBER + "</h1>" +
                "<p>© 2010 Howard Kistler/Dream Codex Retrogames (<a href=\"http://www.dreamcodex.com\">www.dreamcodex.com</a>)</p>" +
                "<p>Magellan is free software maintained by the TI-99/4A community.</p>" +
                "<p>Modified by:</p>" +
                "<ul>" +
                    "<li>Retroclouds (2011)</li>" +
                    "<li>Sometimes99er (2013)</li>" +
                    "<li>David Vella (2016)</li>" +
                    "<li>Visrealm (2025)</li>" +
                    "<li>Rasmus Moustgaard (2013 - ongoing)</li>" +
                "</ul>" +
                "<p>Source code available from: <a href=\"https://github.com/Rasmus-M/magellan\">github.com/Rasmus-M/magellan</a></p>" +
                "<p>Java runtime version: " + System.getProperty("java.version") + "</p>" +
            "</html>"
        );
    }
}
