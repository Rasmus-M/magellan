package com.dreamcodex.ti.iface;

/**
 * Created with IntelliJ IDEA.
 * User: Rasmus
 * Date: 09-05-13
 * Time: 09:04
 */
public interface UndoRedoListener {

    public void undoRedoStateChanged(boolean canUndo, boolean canRedo, Object source);
}
