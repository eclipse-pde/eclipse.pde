/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
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
