package org.eclipse.pde.internal.editor;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.swt.graphics.*;
import org.eclipse.jface.action.*;
import org.eclipse.ui.*;
import org.eclipse.ui.part.*;
import org.eclipse.ui.actions.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.ui.texteditor.*;
import java.util.*;

public abstract class PDEEditorContributor extends EditorActionBarContributor {
	public static final String ACTIONS_SAVE = "EditorActions.save";
	private PDEMultiPageEditor editor;
	private IPDEEditorPage page;
	private SaveAction saveAction;
	private Hashtable globalActions = new Hashtable();
	private String menuName;

	class GlobalAction extends Action {
		private String id;
		public GlobalAction(String id) {
			this.id = id;
		}
		public void run() {
			editor.performGlobalAction(id);
		}
	}

	class SaveAction extends Action implements IUpdate {
		public SaveAction() {
		}
		public void run() {
			if (editor != null)
				PDEPlugin.getActivePage().saveEditor(editor, false);
		}
		public void update() {
			if (editor != null) {
				setEnabled(editor.isDirty());
			} else
				setEnabled(false);
		}
	}

public PDEEditorContributor(String menuName) {
	this.menuName = menuName;
	makeActions();
}
private void addGlobalAction(String id) {
	GlobalAction action = new GlobalAction(id);
	globalActions.put(id, action);
}
public void contextMenuAboutToShow(IMenuManager mng) {
	mng.add(saveAction);
}
public void contributeToMenu(IMenuManager mm) {
}
public void contributeToStatusLine(IStatusLineManager slm) {
}
public void contributeToToolBar(IToolBarManager tbm) {
}
public PDEMultiPageEditor getEditor() {
	return editor;
}
public IAction getGlobalAction(String id) {
	return (IAction)globalActions.get(id);
}

public IAction getSaveAction() {
	return saveAction;
}
public IStatusLineManager getStatusLineManager() {
	return getActionBars().getStatusLineManager();
}
protected void makeActions() {
	addGlobalAction(ITextEditorActionConstants.DELETE);
	addGlobalAction(ITextEditorActionConstants.UNDO);
	addGlobalAction(ITextEditorActionConstants.REDO);
	addGlobalAction(ITextEditorActionConstants.CUT);
	addGlobalAction(ITextEditorActionConstants.COPY);
	addGlobalAction(ITextEditorActionConstants.PASTE);
	addGlobalAction(ITextEditorActionConstants.SELECT_ALL);
	addGlobalAction(ITextEditorActionConstants.FIND);
	addGlobalAction(ITextEditorActionConstants.BOOKMARK);

	saveAction = new SaveAction();
	saveAction.setText(PDEPlugin.getResourceString(ACTIONS_SAVE));

}
public void setActiveEditor(IEditorPart targetEditor) {
	this.editor = (PDEMultiPageEditor) targetEditor;
	IPDEEditorPage page = editor.getCurrentPage();
	setActivePage(page);
}
public void setActivePage(IPDEEditorPage newPage) {
	IPDEEditorPage oldPage = page;
	this.page = newPage;
	if (newPage==null) return;
	updateActions();
	if (oldPage!=null && oldPage.isSource() == false && newPage.isSource() == false)
		return;

	IActionBars bars = getActionBars();
	// update global actions
	bars.setGlobalActionHandler(
		IWorkbenchActionConstants.DELETE,
		page.getAction(ITextEditorActionConstants.DELETE));
	bars.setGlobalActionHandler(
		IWorkbenchActionConstants.UNDO,
		page.getAction(ITextEditorActionConstants.UNDO));
	bars.setGlobalActionHandler(
		IWorkbenchActionConstants.REDO,
		page.getAction(ITextEditorActionConstants.REDO));
	bars.setGlobalActionHandler(
		IWorkbenchActionConstants.CUT,
		page.getAction(ITextEditorActionConstants.CUT));
	bars.setGlobalActionHandler(
		IWorkbenchActionConstants.COPY,
		page.getAction(ITextEditorActionConstants.COPY));
	bars.setGlobalActionHandler(
		IWorkbenchActionConstants.PASTE,
		page.getAction(ITextEditorActionConstants.PASTE));
	bars.setGlobalActionHandler(
		IWorkbenchActionConstants.SELECT_ALL,
		page.getAction(ITextEditorActionConstants.SELECT_ALL));
	bars.setGlobalActionHandler(
		IWorkbenchActionConstants.FIND,
		page.getAction(ITextEditorActionConstants.FIND));
	bars.setGlobalActionHandler(
		IWorkbenchActionConstants.BOOKMARK,
		page.getAction(ITextEditorActionConstants.BOOKMARK));
	bars.updateActionBars();
}
public void updateActions() {
	saveAction.update();
}
}
