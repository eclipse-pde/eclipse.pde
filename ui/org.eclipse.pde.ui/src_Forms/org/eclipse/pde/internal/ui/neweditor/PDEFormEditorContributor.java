/*
 * Created on Jan 29, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor;

import java.util.Hashtable;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.IEditable;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.editors.text.TextEditorActionContributor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.ide.IDEActionFactory;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;
import org.eclipse.ui.texteditor.IUpdate;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class PDEFormEditorContributor
		extends
			MultiPageEditorActionBarContributor {
	public static final String ACTIONS_SAVE = "EditorActions.save";
	public static final String ACTIONS_CUT = "EditorActions.cut";
	public static final String ACTIONS_COPY = "EditorActions.copy";
	public static final String ACTIONS_PASTE = "EditorActions.paste";
	public static final String ACTIONS_REVERT = "EditorActions.revert";
	private SubMenuManager subMenuManager;
	private SubStatusLineManager subStatusManager;
	private SubToolBarManager subToolbarManager;

	private PDEFormEditor editor;	
	private IFormPage page;
	private SaveAction saveAction;
	private RevertAction revertAction;
	private ClipboardAction cutAction;
	private ClipboardAction copyAction;
	private ClipboardAction pasteAction;
	private Hashtable globalActions = new Hashtable();
	private TextEditorActionContributor sourceContributor;

	class GlobalAction extends Action implements IUpdate {
		private String id;
		public GlobalAction(String id) {
			this.id = id;
		}
		public void run() {
			editor.performGlobalAction(id);
			updateSelectableActions(editor.getSelection());
		}
		public void update() {
			getActionBars().updateActionBars();
		}
	}

	class ClipboardAction extends GlobalAction {
		public ClipboardAction(String id) {
			super(id);
			setEnabled(false);
		}
		public void selectionChanged(ISelection selection) {
		}
		public boolean isEditable() {
			if (editor==null) return false;
			IModel model = editor.getAggregateModel();
			if (model instanceof IEditable)
				return ((IEditable)model).isEditable();
			return false;
		}
	}

	class CutAction extends ClipboardAction {
		public CutAction() {
			super(ActionFactory.CUT.getId());
			setText(PDEPlugin.getResourceString(ACTIONS_CUT));
		}
		public void selectionChanged(ISelection selection) {
			//setEnabled(isEditable() && editor.canCopy(selection));
		}
	}

	class CopyAction extends ClipboardAction {
		public CopyAction() {
			super(ActionFactory.COPY.getId());
			setText(PDEPlugin.getResourceString(ACTIONS_COPY));
		}
		public void selectionChanged(ISelection selection) {
			//setEnabled(editor.canCopy(selection));
		}
	}

	class PasteAction extends ClipboardAction {
		public PasteAction() {
			super(ActionFactory.PASTE.getId());
			setText(PDEPlugin.getResourceString(ACTIONS_PASTE));
			//selectionChanged(null);
		}
		public void selectionChanged(ISelection selection) {
			//setEnabled(isEditable()&& editor.canPasteFromClipboard());
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
	
	class RevertAction extends Action implements IUpdate {
		public RevertAction() {
		}
		public void run() {
			if (editor != null)
			editor.doRevert();
		}
		public void update() {
			if (editor != null) {
				setEnabled(editor.isDirty());
			}
			else 
				setEnabled(false);
		}
	}

	public PDEFormEditorContributor(String menuName) {
		sourceContributor = new TextEditorActionContributor();
		makeActions();
	}
	private void addGlobalAction(String id) {
		GlobalAction action = new GlobalAction(id);
		addGlobalAction(id, action);
	}
	private void addGlobalAction(String id, Action action) {
		globalActions.put(id, action);
	}
	public void addClipboardActions(IMenuManager mng) {
		mng.add(cutAction);
		mng.add(copyAction);
		mng.add(pasteAction);
		mng.add(new Separator());
		mng.add(revertAction);

	}
	
	public void contextMenuAboutToShow(IMenuManager mng) {
		contextMenuAboutToShow(mng, true);
	}
	public void contextMenuAboutToShow(IMenuManager mng, boolean addClipboard) {
		if (editor != null)	
			updateSelectableActions(editor.getSelection());
		if (addClipboard) {
			addClipboardActions(mng);
			//mng.add(new Separator());
		}
		mng.add(saveAction);
	}
	public void contributeToMenu(IMenuManager mm) {
		subMenuManager = new SubMenuManager(mm);
		sourceContributor.contributeToMenu(subMenuManager);
	}
	public void contributeToStatusLine(IStatusLineManager slm) {
		subStatusManager = new SubStatusLineManager(slm);
		sourceContributor.contributeToStatusLine(subStatusManager);
	}
	public void contributeToToolBar(IToolBarManager tbm) {
		subToolbarManager = new SubToolBarManager(tbm);
		sourceContributor.contributeToToolBar(subToolbarManager);
	}
	public PDEFormEditor getEditor() {
		return editor;
	}
	public IAction getGlobalAction(String id) {
		return (IAction) globalActions.get(id);
	}

	public IAction getSaveAction() {
		return saveAction;
	}
	
	public IAction getRevertAction() {
		return revertAction;
	}
	
	public IStatusLineManager getStatusLineManager() {
		return getActionBars().getStatusLineManager();
	}

	protected void makeActions() {
		// clipboard actions
		cutAction = new CutAction();
		copyAction = new CopyAction();
		pasteAction = new PasteAction();
		addGlobalAction(ActionFactory.CUT.getId(), cutAction);
		addGlobalAction(ActionFactory.COPY.getId(), copyAction);
		addGlobalAction(ActionFactory.PASTE.getId(), pasteAction);

		addGlobalAction(ActionFactory.DELETE.getId());
		addGlobalAction(ActionFactory.UNDO.getId());
		addGlobalAction(ActionFactory.REDO.getId());
		addGlobalAction(ActionFactory.SELECT_ALL.getId());
		addGlobalAction(ActionFactory.FIND.getId());
		addGlobalAction(IDEActionFactory.BOOKMARK.getId());

		saveAction = new SaveAction();
		saveAction.setText(PDEPlugin.getResourceString(ACTIONS_SAVE));
		
		revertAction = new RevertAction();
		revertAction.setText(PDEPlugin.getResourceString(ACTIONS_REVERT));

	}
	public void setActiveEditor(IEditorPart targetEditor) {
		//if (editor != null)
		//	editor.updateUndo(null, null);
		if (targetEditor instanceof PDESourcePage) {
			// Fixing the 'goto line' problem -
			// the action is thinking that source page
			// is a standalone editor and tries to activate it
			// #19361
			PDESourcePage page = (PDESourcePage)targetEditor;
			PDEPlugin.getActivePage().activate(page.getEditor());
			return;
		}
	    if (targetEditor instanceof PDEFormEditor)
	    	this.editor = (PDEFormEditor) targetEditor;
	    else
	    	return;
	    /*
		editor.updateUndo(
			getGlobalAction(ActionFactory.UNDO.getId()),
			getGlobalAction(ActionFactory.REDO.getId()));
		*/
		IEditorPart page = editor.getActiveEditor();
		setActivePage(page);
		updateSelectableActions(editor.getSelection());
	}
	public void setActivePage(IEditorPart newEditor) {
		if (editor==null) return;
		IFormPage oldPage = page;
		IFormPage newPage = editor.getActivePageInstance();
		this.page = newPage;
		if (newPage==null) return;
		updateActions();
		if (oldPage != null
			&& oldPage.isSource() == false
			&& newPage.isSource() == false)
			return;

		IActionBars bars = getActionBars();
		PDESourcePage sourcePage = null;
		
		if (newPage instanceof PDESourcePage)
			sourcePage = (PDESourcePage)newPage;
		
		subMenuManager.setVisible(sourcePage!=null);
		subStatusManager.setVisible(sourcePage!=null);
		subToolbarManager.setVisible(sourcePage!=null);

		sourceContributor.setActiveEditor(sourcePage);
		// update global actions
		bars.setGlobalActionHandler(
			ActionFactory.DELETE.getId(),
			getPageAction(page, ActionFactory.DELETE.getId()));
		bars.setGlobalActionHandler(
			ActionFactory.UNDO.getId(),
			getPageAction(page, ActionFactory.UNDO.getId()));
		bars.setGlobalActionHandler(
			ActionFactory.REDO.getId(),
			getPageAction(page, ActionFactory.REDO.getId()));
		bars.setGlobalActionHandler(
			ActionFactory.CUT.getId(),
			getPageAction(page, ActionFactory.CUT.getId()));
		bars.setGlobalActionHandler(
			ActionFactory.COPY.getId(),
			getPageAction(page, ActionFactory.COPY.getId()));
		bars.setGlobalActionHandler(
			ActionFactory.PASTE.getId(),
			getPageAction(page, ActionFactory.PASTE.getId()));
		bars.setGlobalActionHandler(
			ActionFactory.SELECT_ALL.getId(),
			getPageAction(page, ActionFactory.SELECT_ALL.getId()));
		bars.setGlobalActionHandler(
			ActionFactory.FIND.getId(),
			getPageAction(page, ActionFactory.FIND.getId()));
		bars.setGlobalActionHandler(
			IDEActionFactory.BOOKMARK.getId(),
			getPageAction(page, IDEActionFactory.BOOKMARK.getId()));
		bars.updateActionBars();
	}
	private IAction getPageAction(IFormPage page, String id) {
		if (page instanceof PDESourcePage)
			return ((PDESourcePage)page).getAction(id);
		return getGlobalAction(id);
	}
	
	public void updateActions() {
		saveAction.update();
		revertAction.update();
	}

	public void updateSelectableActions(ISelection selection) {
		if (editor != null) {
			cutAction.selectionChanged(selection);
			copyAction.selectionChanged(selection);
			pasteAction.selectionChanged(selection);
		}
	}
}