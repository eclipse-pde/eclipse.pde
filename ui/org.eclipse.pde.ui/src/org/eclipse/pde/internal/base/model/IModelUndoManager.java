/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.base.model;
import org.eclipse.jface.action.IAction;

/**
 * Classes that implement this interface provide undo/redo
 * capability linked to changes reported by model change
 * providers. Model change events carry sufficient data
 * to be used in an undo/redo stack and reverted to or
 * reapplied after the change.
 * <p>Model undo manager adds itself as a change listener
 * after being connected to the provider. It is expected
 * to stop listening to change events after being disconnected.
 * Changes reported while being connected are kept in the
 * operation stack whose size can be controlled.
 * <p>The part that uses the undo manager is responsible
 * for supplying Undo and Redo action objects for
 * the purpose of controlling their availability. 
 * Undo manager should keep track of its current
 * operation stack pointer and adjust Undo/Redo action
 * availability by calling 'setEnabled' on the
 * provided action objects. Implementation of this
 * interface may also opt to modify Undo/Redo action
 * labels in order to better indicate the effect
 * of the operations if selected (for example,
 * 'Undo Delete' instead of 'Undo').
 */
public interface IModelUndoManager {
	public void connect(IModelChangeProvider provider);
	public void disconnect(IModelChangeProvider provider);
	public boolean isUndoable();
	public boolean isRedoable();
	public void undo();
	public void redo();
	public void setUndoLevelLimit(int limit);
	public void setIgnoreChanges(boolean ignore);
	public void setActions(IAction undoAction, IAction redoAction);
}