package org.eclipse.pde.internal.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.ui.*;
import org.eclipse.jface.action.*;
import org.eclipse.pde.internal.editor.*;


public class FeatureEditorContributor extends PDEEditorContributor {
	private EditorBuildFeatureAction buildAction;
	private SynchronizeVersionsAction synchronizeAction;

public FeatureEditorContributor() {
	super("&Component");
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
}
