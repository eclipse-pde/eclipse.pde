/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.editor;
import java.util.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.internal.model.*;
import org.eclipse.pde.model.*;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.texteditor.IUpdate;
import org.eclipse.pde.internal.PDEPlugin;

/**
 * @version 	1.0
 * @author
 */
public abstract class ModelUndoManager
	implements IModelUndoManager, IModelChangedListener {
	private static final String KEY_NO_UNDO = "UpdateManager.noUndo";
	private static final String KEY_NO_REDO = "UpdateManager.noRedo";
	private static final String KEY_UNDO = "UpdateManager.undo";
	private static final String KEY_REDO = "UpdateManager.redo";
	private static final String KEY_OP_ADD = "UpdateManager.op.add";
	private static final String KEY_OP_REMOVE = "UpdateManager.op.remove";
	private static final String KEY_OP_CHANGE = "UpdateManager.op.change";
	private boolean ignoreChanges;
	private List operations;
	private int undoLevelLimit = 10;
	private int cursor = -1;
	private IAction undoAction;
	private IAction redoAction;
	private PDEMultiPageEditor editor;
	
	public ModelUndoManager(PDEMultiPageEditor editor) {
		this.editor = editor;
	}

	/*
	 * @see IModelUndoManager#connect(IModelChangeProvider)
	 */
	public void connect(IModelChangeProvider provider) {
		provider.addModelChangedListener(this);
		initialize();
	}

	/*
	 * @see IModelUndoManager#disconnect(IModelChangeProvider)
	 */
	public void disconnect(IModelChangeProvider provider) {
		provider.removeModelChangedListener(this);
	}

	private void initialize() {
		operations = new Vector();
		cursor = -1;
		updateActions();
	}

	/*
	 * @see IModelUndoManager#isUndoable()
	 */
	public boolean isUndoable() {
		return cursor>=0;
	}

	/*
	 * @see IModelUndoManager#isRedoable()
	 */
	public boolean isRedoable() {
		return (cursor+1)<operations.size();
	}

	/*
	 * @see IModelUndoManager#undo()
	 */
	public void undo() {
		IModelChangedEvent op = getCurrentOperation();
		if (op==null) return;
		ignoreChanges = true;
		openRelatedPage(op);
		execute(op, true);
		cursor --;
		updateActions();
		ignoreChanges = false;
	}

	/*
	 * @see IModelUndoManager#redo()
	 */
	public void redo() {
		cursor ++;
		IModelChangedEvent op = getCurrentOperation();
		if (op==null) return;
		ignoreChanges = true;
		openRelatedPage(op);
		execute(op, false);
		ignoreChanges = false;
		updateActions();
	}
	
	protected abstract String getPageId(Object object);
	
	protected abstract void execute(IModelChangedEvent op, boolean undo);
	
	private void openRelatedPage(IModelChangedEvent op) {
		Object obj = op.getChangedObjects()[0];
		String pageId = getPageId(obj);
		if (pageId!=null) {
			IPDEEditorPage cpage = editor.getCurrentPage();
			IPDEEditorPage newPage = editor.getPage(pageId);
			if (cpage != newPage) 
				editor.showPage(newPage);
		}
	}

	/*
	 * @see IModelChangedListener#modelChanged(IModelChangedEvent)
	 */
	public void modelChanged(IModelChangedEvent event) {
		if (ignoreChanges)
			return;
			
		if (event.getChangeType()==IModelChangedEvent.WORLD_CHANGED) {
			initialize();
			return;
		}
		addOperation(event);
	}
	
	private IModelChangedEvent getCurrentOperation() {
		if (cursor == -1 || cursor == operations.size()) return null;
		return (IModelChangedEvent)operations.get(cursor);
	}
	
	private IModelChangedEvent getNextOperation() {
		int peekCursor = cursor+1;
		if (peekCursor >= operations.size()) return null;
		return (IModelChangedEvent)operations.get(peekCursor);
	}
	
	private void addOperation(IModelChangedEvent operation) {
		operations.add(operation);
		int size = operations.size();
		if (size > undoLevelLimit) {
			int extra = size-undoLevelLimit;
			// trim
			for (int i=0; i<extra; i++) {
				operations.remove(i);
			}
		}
		cursor = operations.size() -1;
		updateActions();
	}
	
	public void setActions(IAction undoAction, IAction redoAction) {
		this.undoAction = undoAction;
		this.redoAction = redoAction;
		updateActions();
	}
	
	private void updateActions() {
		if (undoAction!=null && redoAction!=null) {
			undoAction.setEnabled(isUndoable());
			//undoAction.setText(getUndoText());
			redoAction.setEnabled(isRedoable());
			//redoAction.setText(getRedoText());
		}
	}
	
	private String getUndoText() {
		IModelChangedEvent op = getCurrentOperation();
		if (op==null) {
			return PDEPlugin.getResourceString(KEY_NO_UNDO);
		}
		else {
			String opText = getOperationText(op);
			return PDEPlugin.getFormattedMessage(KEY_UNDO, opText);
		}
	}

	private String getRedoText() {
		IModelChangedEvent op = getNextOperation();
		if (op==null) {
			return PDEPlugin.getResourceString(KEY_NO_REDO);
		}
		else {
			String opText = getOperationText(op);
			return PDEPlugin.getFormattedMessage(KEY_REDO, opText);
		}
	}
		
	private String getOperationText(IModelChangedEvent op) {
		String opText="";
		switch (op.getChangeType()) {
			case IModelChangedEvent.INSERT:
			opText = PDEPlugin.getResourceString(KEY_OP_ADD);
			break;
			case IModelChangedEvent.REMOVE:
			opText = PDEPlugin.getResourceString(KEY_OP_REMOVE);
			break;
			case IModelChangedEvent.CHANGE:
			opText = PDEPlugin.getResourceString(KEY_OP_CHANGE);
			break;
		}
		return opText;
	}
	
	public void setUndoLevelLimit(int limit) {
		this.undoLevelLimit = limit;
	}

	public void setIgnoreChanges(boolean ignore) {
		this.ignoreChanges = ignore;
	}
}