/*
 * Copyright (c) 2002 IBM Corp.  All rights reserved.
 * This file is made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 */
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.jface.action.IAction;
import org.eclipse.pde.core.IModelChangeProvider;

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
