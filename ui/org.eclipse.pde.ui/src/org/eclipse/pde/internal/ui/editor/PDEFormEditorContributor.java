/*
 * Created on Jan 29, 2004
 * 
 * To change the template for this generated file go to Window - Preferences -
 * Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.editor;
import java.util.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.core.*;
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
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class PDEFormEditorContributor
		extends
			MultiPageEditorActionBarContributor {
	public static final String ACTIONS_SAVE = "EditorActions.save";
	public static final String ACTIONS_CUT = "EditorActions.cut";
	public static final String ACTIONS_COPY = "EditorActions.copy";
	public static final String ACTIONS_PASTE = "EditorActions.paste";
	public static final String ACTIONS_REVERT = "EditorActions.revert";
	private SubActionBars sourceActionBars;
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
			if (editor == null)
				return false;
			IBaseModel model = editor.getAggregateModel();
			if (model instanceof IEditable)
				return ((IEditable) model).isEditable();
			return false;
		}
	}
	class CutAction extends ClipboardAction {
		public CutAction() {
			super(ActionFactory.CUT.getId());
			setText(PDEPlugin.getResourceString(ACTIONS_CUT));
		}
		public void selectionChanged(ISelection selection) {
			setEnabled(isEditable() && editor.canCopy(selection));
		}
	}
	class CopyAction extends ClipboardAction {
		public CopyAction() {
			super(ActionFactory.COPY.getId());
			setText(PDEPlugin.getResourceString(ACTIONS_COPY));
		}
		public void selectionChanged(ISelection selection) {
			setEnabled(editor.canCopy(selection));
		}
	}
	class PasteAction extends ClipboardAction {
		public PasteAction() {
			super(ActionFactory.PASTE.getId());
			setText(PDEPlugin.getResourceString(ACTIONS_PASTE));
			//selectionChanged(null);
		}
		public void selectionChanged(ISelection selection) {
			setEnabled(isEditable() && editor.canPasteFromClipboard());
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
			} else
				setEnabled(false);
		}
	}
	public PDEFormEditorContributor(String menuName) {
		sourceContributor = new TextEditorActionContributor();
		makeActions();
	}
	
	public IEditorActionBarContributor getSourceContributor() {
		return sourceContributor;
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
		}
		mng.add(saveAction);
	}
	public void contributeToMenu(IMenuManager mm) {
	}
	public void contributeToStatusLine(IStatusLineManager slm) {
	}
	public void contributeToToolBar(IToolBarManager tbm) {
	}
	public void contributeToCoolBar(ICoolBarManager cbm) {
	}
	public void dispose() {
		sourceContributor.dispose();
		sourceActionBars.dispose();
		super.dispose();
	}
	public void init(IActionBars bars) {
		super.init(bars);
		sourceActionBars = new SubActionBars(bars);
		sourceContributor.init(sourceActionBars);
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
		// undo/redo
		addGlobalAction(ActionFactory.UNDO.getId());
		addGlobalAction(ActionFactory.REDO.getId());
		// select/find
		addGlobalAction(ActionFactory.SELECT_ALL.getId());
		addGlobalAction(ActionFactory.FIND.getId());
		// bookmark
		addGlobalAction(IDEActionFactory.BOOKMARK.getId());
		// save/revert
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
			PDESourcePage page = (PDESourcePage) targetEditor;
			PDEPlugin.getActivePage().activate(page.getEditor());
			return;
		}
		if (targetEditor instanceof PDEFormEditor)
			this.editor = (PDEFormEditor) targetEditor;
		else
			return;
		editor.updateUndo(getGlobalAction(ActionFactory.UNDO.getId()),
				getGlobalAction(ActionFactory.REDO.getId()));
		IEditorPart page = editor.getActiveEditor();
		setActivePage(page);
		updateSelectableActions(editor.getSelection());
	}
	public void setActivePage(IEditorPart newEditor) {
		if (editor == null)
			return;
		IFormPage oldPage = page;
		IFormPage newPage = editor.getActivePageInstance();
		this.page = newPage;
		if (newPage == null)
			return;
		updateActions();
		if (oldPage != null && oldPage.isEditor() == false
				&& newPage.isEditor() == false) {
			getActionBars().updateActionBars();
			return;
		}
		PDESourcePage sourcePage = null;
		if (newPage instanceof PDESourcePage)
			sourcePage = (PDESourcePage) newPage;
		if (sourcePage != null && sourcePage.equals(oldPage))
			return;
		sourceContributor.setActiveEditor(sourcePage);
		setSourceActionBarsActive(sourcePage != null);
	}
	private void setSourceActionBarsActive(boolean active) {
		IActionBars rootBars = getActionBars();
		rootBars.clearGlobalActionHandlers();
		//PlatformUI.getWorkbench().getCommandSupport().removeHandlerSubmissions(new ArrayList());
		rootBars.updateActionBars();
		if (active) {
			sourceActionBars.activate();
			Map handlers = sourceActionBars.getGlobalActionHandlers();
			if (handlers != null) {
				Set keys = handlers.keySet();
				for (Iterator iter = keys.iterator(); iter.hasNext();) {
					String id = (String) iter.next();
					rootBars.setGlobalActionHandler(id, (IAction) handlers
							.get(id));
				}
			}
		} else {
			sourceActionBars.deactivate();
			registerGlobalActionHandlers();
		}
		rootBars.updateActionBars();
	}
	private void registerGlobalActionHandlers() {
		registerGlobalAction(ActionFactory.DELETE.getId());
		registerGlobalAction(ActionFactory.UNDO.getId());
		registerGlobalAction(ActionFactory.REDO.getId());
		registerGlobalAction(ActionFactory.CUT.getId());
		registerGlobalAction(ActionFactory.COPY.getId());
		registerGlobalAction(ActionFactory.PASTE.getId());
		registerGlobalAction(ActionFactory.SELECT_ALL.getId());
		registerGlobalAction(ActionFactory.FIND.getId());
		// hook revert
		getActionBars().setGlobalActionHandler(ActionFactory.REVERT.getId(), revertAction);
	}
	private void registerGlobalAction(String id) {
		IAction action = getGlobalAction(id);
		getActionBars().setGlobalActionHandler(id, action);
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