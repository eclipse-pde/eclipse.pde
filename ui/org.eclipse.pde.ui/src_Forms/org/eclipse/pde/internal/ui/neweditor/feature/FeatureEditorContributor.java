/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.neweditor.feature;
import org.eclipse.jface.action.*;
import org.eclipse.pde.internal.ui.neweditor.PDEFormEditorContributor;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.ui.IEditorPart;
public class FeatureEditorContributor extends PDEFormEditorContributor {
	private EditorBuildFeatureAction buildAction;
	private SynchronizeVersionsAction synchronizeAction;
	public FeatureEditorContributor() {
		super("&Feature");
	}
	public void contextMenuAboutToShow(IMenuManager mng) {
		super.contextMenuAboutToShow(mng);
		mng.add(new Separator());
		mng.add(synchronizeAction);
		mng.add(buildAction);
	}
	public Action getBuildAction() {
		return buildAction;
	}
	public Action getSynchronizeAction() {
		return synchronizeAction;
	}
	protected void makeActions() {
		super.makeActions();
		buildAction = new EditorBuildFeatureAction();
		synchronizeAction = new SynchronizeVersionsAction();
	}
	public void setActiveEditor(IEditorPart targetEditor) {
		super.setActiveEditor(targetEditor);
		buildAction.setActiveEditor((FeatureEditor) targetEditor);
		synchronizeAction.setActiveEditor((FeatureEditor) targetEditor);
	}
	protected boolean hasKnownTypes(Clipboard clipboard) {
		return true;
	}
}