package com.dreamcodex.ti.util;

import com.dreamcodex.ti.iface.UndoRedoListener;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Rasmus
 * Date: 09-11-13
 * Time: 09:54
 */
public class NotifyingUndoManager extends UndoManager {

    private ArrayList<UndoRedoListener> undoRedoListeners = new ArrayList<UndoRedoListener>();

    public void addUndoRedoListener(UndoRedoListener listener) {
        undoRedoListeners.add(listener);
    }

    public void removeUndoRedoListener(UndoRedoListener listener) {
        undoRedoListeners.remove(listener);
    }

    private void notifyUndoRedoListeners() {
        for (UndoRedoListener listener : undoRedoListeners) {
            listener.undoRedoStateChanged(canUndo(), canRedo(), null);
        }
    }

    public synchronized boolean addEdit(UndoableEdit anEdit) {
        boolean retVal =  super.addEdit(anEdit);
        notifyUndoRedoListeners();
        return retVal;
    }

    public synchronized void undo() throws CannotUndoException {
        super.undo();
        notifyUndoRedoListeners();
    }

    public synchronized void redo() throws CannotRedoException {
        super.redo();
        notifyUndoRedoListeners();
    }

    public synchronized void discardAllEdits() {
        super.discardAllEdits();
        notifyUndoRedoListeners();
    }

    protected void trimEdits(int from, int to) {
        super.trimEdits(from, to);
        notifyUndoRedoListeners();
    }
}
