package org.eclipse.pde.internal.editor.component;

import org.eclipse.ui.*;
import org.eclipse.jface.action.*;
import org.eclipse.pde.internal.editor.*;


public class ComponentEditorContributor extends PDEEditorContributor {
	private BuildComponentAction buildAction;
	private SynchronizeVersionsAction synchronizeAction;

public ComponentEditorContributor() {
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
	buildAction = new BuildComponentAction();
	synchronizeAction = new SynchronizeVersionsAction();
}
public void setActiveEditor(IEditorPart targetEditor) {
	super.setActiveEditor(targetEditor);
	buildAction.setActiveEditor((ComponentEditor) targetEditor);
	synchronizeAction.setActiveEditor((ComponentEditor) targetEditor);
}
}
