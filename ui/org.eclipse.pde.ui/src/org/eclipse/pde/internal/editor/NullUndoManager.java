/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.editor;

import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.model.*;
import org.eclipse.jface.action.IAction;

/**
 * @version 	1.0
 * @author
 */
public class NullUndoManager implements IModelUndoManager {

	/*
	 * @see IModelUndoManager#connect(IModelChangeProvider)
	 */
	public void connect(IModelChangeProvider provider) {
	}

	/*
	 * @see IModelUndoManager#disconnect(IModelChangeProvider)
	 */
	public void disconnect(IModelChangeProvider provider) {
	}

	/*
	 * @see IModelUndoManager#isUndoable()
	 */
	public boolean isUndoable() {
		return false;
	}

	/*
	 * @see IModelUndoManager#isRedoable()
	 */
	public boolean isRedoable() {
		return false;
	}

	/*
	 * @see IModelUndoManager#undo()
	 */
	public void undo() {
	}

	/*
	 * @see IModelUndoManager#redo()
	 */
	public void redo() {
	}

	/*
	 * @see IModelUndoManager#setUndoLevelLimit(int)
	 */
	public void setUndoLevelLimit(int limit) {
	}

	/*
	 * @see IModelUndoManager#setIgnoreChanges(boolean)
	 */
	public void setIgnoreChanges(boolean ignore) {
	}
	
	public void setActions(IAction undoAction, IAction redoAction) {
		if (undoAction!=null) undoAction.setEnabled(false);
		if (redoAction!=null) redoAction.setEnabled(false);
	}
}
