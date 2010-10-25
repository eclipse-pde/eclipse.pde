/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import java.util.*;
import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jface.action.*;
import org.eclipse.pde.internal.ui.editor.actions.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.editors.text.TextEditorActionContributor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.texteditor.*;

public class PDEFormTextEditorContributor extends PDEFormEditorContributor {

	private RetargetTextEditorAction fCorrectionAssist;
	private HyperlinkAction fHyperlinkAction;
	private FormatAction fFormatAction;
	private RetargetTextEditorAction fContentAssist;

	private TextEditorActionContributor fSourceContributor;
	private SubActionBars fSourceActionBars;

	class PDETextEditorActionContributor extends TextEditorActionContributor {
		public void contributeToMenu(IMenuManager mm) {
			super.contributeToMenu(mm);
			IMenuManager editMenu = mm.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
			if (editMenu != null) {
				editMenu.add(new Separator(IContextMenuConstants.GROUP_OPEN));
				editMenu.add(new Separator(IContextMenuConstants.GROUP_GENERATE));
				editMenu.add(new Separator(IContextMenuConstants.GROUP_ADDITIONS));
				if (fContentAssist != null)
					editMenu.prependToGroup(ITextEditorActionConstants.GROUP_ASSIST, fContentAssist);
			}
		}

		public void contributeToToolBar(IToolBarManager toolBarManager) {
			super.contributeToToolBar(toolBarManager);
			if (fHyperlinkAction != null)
				toolBarManager.add(fHyperlinkAction);
		}

		public void setActiveEditor(IEditorPart part) {
			super.setActiveEditor(part);
			IActionBars actionBars = getActionBars();
			IStatusLineManager manager = actionBars.getStatusLineManager();
			manager.setMessage(null);
			manager.setErrorMessage(null);

			ITextEditor textEditor = (part instanceof ITextEditor) ? (ITextEditor) part : null;
			if (fCorrectionAssist != null)
				fCorrectionAssist.setAction(getAction(textEditor, ITextEditorActionConstants.QUICK_ASSIST));
			if (fHyperlinkAction != null)
				fHyperlinkAction.setTextEditor(textEditor);
			if (fFormatAction != null)
				fFormatAction.setTextEditor(textEditor);
			if (fContentAssist != null)
				fContentAssist.setAction(getAction(textEditor, ITextEditorActionConstants.CONTENT_ASSIST));
		}
	}

	public PDEFormTextEditorContributor(String menuName) {
		super(menuName);
		fSourceContributor = createSourceContributor();
		if (supportsCorrectionAssist()) {
			fCorrectionAssist = new RetargetTextEditorAction(PDESourcePage.getBundleForConstructedKeys(), "CorrectionAssistProposal."); //$NON-NLS-1$
			fCorrectionAssist.setActionDefinitionId(ITextEditorActionDefinitionIds.QUICK_ASSIST);
		}
		if (supportsHyperlinking()) {
			fHyperlinkAction = new HyperlinkAction();
			fHyperlinkAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_EDITOR);
		}
		if (supportsFormatAction()) {
			fFormatAction = new FormatAction();
			fFormatAction.setActionDefinitionId(PDEActionConstants.DEFN_FORMAT);
		}
		if (supportsContentAssist()) {
			fContentAssist = new RetargetTextEditorAction(PDESourcePage.getBundleForConstructedKeys(), "ContentAssistProposal."); //$NON-NLS-1$
			fContentAssist.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		}
	}

	public boolean supportsCorrectionAssist() {
		return false;
	}

	public boolean supportsContentAssist() {
		return false;
	}

	public boolean supportsFormatAction() {
		return false;
	}

	public boolean supportsHyperlinking() {
		return false;
	}

	public IEditorActionBarContributor getSourceContributor() {
		return fSourceContributor;
	}

	public void init(IActionBars bars) {
		super.init(bars);
		fSourceActionBars = new SubActionBars(bars);
		fSourceContributor.init(fSourceActionBars);
	}

	public void dispose() {
		fSourceActionBars.dispose();
		fSourceContributor.dispose();
		super.dispose();
	}

	protected void setSourceActionBarsActive(boolean active) {
		IActionBars rootBars = getActionBars();
		rootBars.clearGlobalActionHandlers();
		rootBars.updateActionBars();
		if (active) {
			fSourceActionBars.activate();
			Map handlers = fSourceActionBars.getGlobalActionHandlers();
			if (handlers != null) {
				Set keys = handlers.keySet();
				for (Iterator iter = keys.iterator(); iter.hasNext();) {
					String id = (String) iter.next();
					rootBars.setGlobalActionHandler(id, (IAction) handlers.get(id));
				}
			}
		} else {
			fSourceActionBars.deactivate();
			registerGlobalActionHandlers();
		}
		rootBars.setGlobalActionHandler(PDEActionConstants.OPEN, active ? fHyperlinkAction : null);
		rootBars.setGlobalActionHandler(PDEActionConstants.FORMAT, active ? fFormatAction : null);
		// Register the revert action
		rootBars.setGlobalActionHandler(ActionFactory.REVERT.getId(), getRevertAction());

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
	}

	private void registerGlobalAction(String id) {
		IAction action = getGlobalAction(id);
		getActionBars().setGlobalActionHandler(id, action);
	}

	public void setActivePage(IEditorPart newEditor) {
		if (fEditor == null)
			return;

		IFormPage oldPage = fPage;
		fPage = fEditor.getActivePageInstance();
		if (fPage == null)
			return;
		// Update the quick outline action to the navigate menu
		updateQuickOutlineMenuEntry();

		updateActions();
		if (oldPage != null && !oldPage.isEditor() && !fPage.isEditor()) {
			getActionBars().updateActionBars();
			return;
		}

		boolean isSourcePage = fPage instanceof PDESourcePage;
		if (isSourcePage && fPage.equals(oldPage))
			return;
		fSourceContributor.setActiveEditor(fPage);
		setSourceActionBarsActive(isSourcePage);
	}

	/**
	 * 
	 */
	private void updateQuickOutlineMenuEntry() {
		// Get the main action bar
		IActionBars actionBars = getActionBars();
		IMenuManager menuManager = actionBars.getMenuManager();
		// Get the navigate menu
		IMenuManager navigateMenu = menuManager.findMenuUsingPath(IWorkbenchActionConstants.M_NAVIGATE);
		// Ensure there is a navigate menu
		if (navigateMenu == null) {
			return;
		}
		// Remove the previous version of the quick outline menu entry - if
		// one exists 
		// Prevent duplicate menu entries 
		// Prevent wrong quick outline menu from being brought up for the wrong
		// page
		navigateMenu.remove(PDEActionConstants.COMMAND_ID_QUICK_OUTLINE);
		// Ensure the active page is a source page
		// Only add the quick outline menu to the source pages
		if ((fPage instanceof PDEProjectionSourcePage) == false) {
			return;
		}
		PDEProjectionSourcePage page = (PDEProjectionSourcePage) fPage;
		// Only add the action if the source page supports it
		if (page.isQuickOutlineEnabled() == false) {
			return;
		}
		// Get the appropriate quick outline action associated with the active
		// source page
		IAction quickOutlineAction = page.getAction(PDEActionConstants.COMMAND_ID_QUICK_OUTLINE);
		// Ensure it is defined
		if (quickOutlineAction == null) {
			return;
		}
		// Add the quick outline action after the "Show In" menu contributed
		// by JDT
		// This could break if JDT changes the "Show In" menu ID
		try {
			navigateMenu.insertAfter("showIn", quickOutlineAction); //$NON-NLS-1$
		} catch (IllegalArgumentException e) {
			// Ignore
		}
	}

	protected TextEditorActionContributor createSourceContributor() {
		return new PDETextEditorActionContributor();
	}

	protected HyperlinkAction getHyperlinkAction() {
		return fHyperlinkAction;
	}

	protected FormatAction getFormatAction() {
		return fFormatAction;
	}

}
