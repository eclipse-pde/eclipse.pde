/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.base.model;
import org.eclipse.jface.action.IAction;

/**
 * @version 	1.0
 * @author
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