/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.pde.internal.ui.editor.PDEFormEditorContributor;
import org.eclipse.pde.internal.ui.wizards.site.OpenProjectWizardAction;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.ui.IEditorPart;

public class FeatureEditorContributor extends PDEFormEditorContributor {
	private EditorBuildFeatureAction fBuildAction;

	private SynchronizeVersionsAction fSynchronizeAction;

	private OpenProjectWizardAction fNewSiteAction;

	public FeatureEditorContributor() {
		super("&Feature"); //$NON-NLS-1$
	}

	public void contextMenuAboutToShow(IMenuManager mng) {
		super.contextMenuAboutToShow(mng);
		mng.add(new Separator());
		mng.add(fSynchronizeAction);
		// mng.add(fNewSiteAction);
		mng.add(fBuildAction);
	}

	public Action getBuildAction() {
		return fBuildAction;
	}

	public Action getSynchronizeAction() {
		return fSynchronizeAction;
	}

	public Action getNewSiteAction() {
		return fNewSiteAction;
	}

	protected void makeActions() {
		super.makeActions();
		fBuildAction = new EditorBuildFeatureAction();
		fSynchronizeAction = new SynchronizeVersionsAction();
		fNewSiteAction = new OpenProjectWizardAction();
	}

	public void setActiveEditor(IEditorPart targetEditor) {
		super.setActiveEditor(targetEditor);
		if (targetEditor instanceof FeatureEditor) {
			fBuildAction.setActiveEditor((FeatureEditor) targetEditor);
			fSynchronizeAction.setActiveEditor((FeatureEditor) targetEditor);
		}
	}

	protected boolean hasKnownTypes(Clipboard clipboard) {
		return true;
	}
}
