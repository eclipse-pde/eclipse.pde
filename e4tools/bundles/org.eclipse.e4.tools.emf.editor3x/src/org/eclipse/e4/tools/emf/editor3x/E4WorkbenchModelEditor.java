/*******************************************************************************
 * Copyright (c) 2010 BestSolution.at and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Tom Schindl <tom.schindl@bestsolution.at> - initial API and implementation
 ******************************************************************************/
package org.eclipse.e4.tools.emf.editor3x;

import java.util.ResourceBundle;

import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.tools.compat.parts.DIEditorPart;
import org.eclipse.e4.tools.emf.ui.internal.wbm.ApplicationModelEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.texteditor.FindReplaceAction;
import org.osgi.framework.FrameworkUtil;

@SuppressWarnings("restriction")
public class E4WorkbenchModelEditor extends
	DIEditorPart<ApplicationModelEditor> {
	private UndoAction undoAction;
	private RedoAction redoAction;
	private Runnable tabChangedListener;

	public E4WorkbenchModelEditor() {
		super(ApplicationModelEditor.class, COPY | CUT | PASTE);
	}

	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		setPartName(getEditorInput().getName());
	}

	@Override
	protected void makeActions() {
		super.makeActions();
		undoAction = new UndoAction(getComponent().getModelProvider());
		undoAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_UNDO);

		redoAction = new RedoAction(getComponent().getModelProvider());
		redoAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_REDO);

		getEditorSite().getActionBars().setGlobalActionHandler(
			ActionFactory.UNDO.getId(), undoAction);
		getEditorSite().getActionBars().setGlobalActionHandler(
			ActionFactory.REDO.getId(), redoAction);

		ResourceBundle bundle = Platform.getResourceBundle(FrameworkUtil.getBundle(getClass()));
		FindReplaceAction findReplaceAction = new FindReplaceAction(bundle, "find_replace_action_", this); //$NON-NLS-1$
		findReplaceAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_FIND_AND_REPLACE);
		getEditorSite().getActionBars().setGlobalActionHandler(ActionFactory.FIND.getId(), findReplaceAction);
		tabChangedListener = findReplaceAction::update;
		getComponent().addTabChangedListener(tabChangedListener);
	}

	@Override
	public void dispose() {
		if (undoAction != null) {
			undoAction.dispose();
		}

		if (redoAction != null) {
			redoAction.dispose();
		}

		ApplicationModelEditor component = getComponent();
		if (component != null && tabChangedListener != null) {
			component.removeTabChangedListener(tabChangedListener);
		}

		super.dispose();
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		ApplicationModelEditor component = getComponent();
		if (component != null) {
			T adapted = Adapters.adapt(component, adapter);
			if (adapted != null) {
				return adapted;
			}
		}
		return super.getAdapter(adapter);
	}
}
