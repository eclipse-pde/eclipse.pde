package org.eclipse.pde.internal.ui.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.resources.*;
import org.eclipse.ui.views.contentoutline.*;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.jface.action.*;


public class ManifestTemplatePage extends PDEChildFormPage {
	private IFile templateFile;

public ManifestTemplatePage(ManifestFormPage parentPage, String title, IFile templateFile) {
	super(parentPage, title);
	this.templateFile = templateFile;
}

public IContentOutlinePage createContentOutlinePage() {
	return null;
}

public IFile getTemplateFile() {
	return templateFile;
}

protected AbstractSectionForm createForm() {
	return new TemplateForm(this);
}

public boolean contextMenuAboutToShow(IMenuManager manager) {
	return ((DependenciesForm)getForm()).fillContextMenu(manager);
}
}
