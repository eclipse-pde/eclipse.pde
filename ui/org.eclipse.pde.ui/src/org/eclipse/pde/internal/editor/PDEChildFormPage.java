package org.eclipse.pde.internal.editor;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.action.*;
import org.eclipse.core.runtime.*;
import org.eclipse.ui.views.contentoutline.*;
import org.eclipse.update.ui.forms.internal.*;

public abstract class PDEChildFormPage extends PDEFormPage {
	private PDEFormPage parentPage;

public PDEChildFormPage(PDEFormPage parentPage, String title) {
	super(parentPage.getEditor(), title);
	this.parentPage = parentPage;
}
public IContentOutlinePage createContentOutlinePage() {
	return null;
}
protected abstract SectionForm createForm();
public void doSave(IProgressMonitor monitor) {
	getForm().commitChanges(true);
	parentPage.doSave(monitor);
}
public void doSaveAs() {
	parentPage.doSaveAs();
}
public IAction getAction(String id) {
	return parentPage.getAction(id);
}
public IContentOutlinePage getContentOutlinePage() {
	return parentPage.getContentOutlinePage();
}
public Object getModel() {
	return parentPage.getModel();
}
public PDEFormPage getParentPage() {
	return parentPage;
}
public boolean isDirty() {
	return parentPage.isDirty();
}
}
