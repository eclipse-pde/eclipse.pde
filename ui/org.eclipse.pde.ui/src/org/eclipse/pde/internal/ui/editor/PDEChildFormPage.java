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
package org.eclipse.pde.internal.ui.editor;

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

public PDEChildFormPage(PDEFormPage parentPage, String title, AbstractSectionForm form) {
	super(parentPage.getEditor(), title, form);
	this.parentPage = parentPage;
}

public IContentOutlinePage createContentOutlinePage() {
	return null;
}
protected abstract AbstractSectionForm createForm();
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
