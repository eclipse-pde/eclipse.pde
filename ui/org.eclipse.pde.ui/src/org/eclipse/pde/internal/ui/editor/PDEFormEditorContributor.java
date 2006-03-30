/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IEditable;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.SubActionBars;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.editors.text.TextEditorActionContributor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.ide.IDEActionFactory;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.IUpdate;
import org.eclipse.ui.texteditor.RetargetTextEditorAction;

public class PDEFormEditorContributor
		extends
			MultiPageEditorActionBarContributor {
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
	private RetargetTextEditorAction fCorrectionAssist ;
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
			setText(PDEUIMessages.EditorActions_cut);
		}
		public void selectionChanged(ISelection selection) {
			setEnabled(isEditable() && editor.canCopy(selection));
		}
	}
	class CopyAction extends ClipboardAction {
		public CopyAction() {
			super(ActionFactory.COPY.getId());
			setText(PDEUIMessages.EditorActions_copy);
		}
		public void selectionChanged(ISelection selection) {
			setEnabled(editor.canCopy(selection));
		}
	}
	class PasteAction extends ClipboardAction {
		public PasteAction() {
			super(ActionFactory.PASTE.getId());
			setText(PDEUIMessages.EditorActions_paste);
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
		fCorrectionAssist = new RetargetTextEditorAction(PDESourcePage.getBundleForConstructedKeys(), "CorrectionAssistProposal."); //$NON-NLS-1$
		fCorrectionAssist.setActionDefinitionId(ITextEditorActionDefinitionIds.QUICK_ASSIST);
		sourceContributor = new TextEditorActionContributor() {
			public void contributeToMenu(IMenuManager mm) {
				super.contributeToMenu(mm);
				IMenuManager editMenu= mm.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
				if (editMenu != null) {
					editMenu.add(new Separator(IContextMenuConstants.GROUP_OPEN));
					editMenu.add(new Separator(IContextMenuConstants.GROUP_GENERATE));
					editMenu.add(new Separator(IContextMenuConstants.GROUP_ADDITIONS));

					editMenu.appendToGroup(IContextMenuConstants.GROUP_GENERATE, fCorrectionAssist);
				}
			}
			public void setActiveEditor(IEditorPart part) {

				super.setActiveEditor(part);

				IActionBars actionBars= getActionBars();
				IStatusLineManager manager= actionBars.getStatusLineManager();
				manager.setMessage(null);
				manager.setErrorMessage(null);

				ITextEditor textEditor = null;
				if (part instanceof ITextEditor)
					textEditor = (ITextEditor)part;

				fCorrectionAssist.setAction(getAction(textEditor, ITextEditorActionConstants.QUICK_ASSIST)); //$NON-NLS-1$
			}
		};
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
		saveAction.setText(PDEUIMessages.EditorActions_save);
		revertAction = new RevertAction();
		revertAction.setText(PDEUIMessages.EditorActions_revert);
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
