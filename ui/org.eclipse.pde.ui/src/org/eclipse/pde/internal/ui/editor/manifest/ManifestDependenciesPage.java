package org.eclipse.pde.internal.ui.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.ui.views.contentoutline.*;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.jface.action.*;


public class ManifestDependenciesPage extends PDEChildFormPage {

public ManifestDependenciesPage(ManifestFormPage parentPage, String title) {
	super(parentPage, title);
}
public IContentOutlinePage createContentOutlinePage() {
	return null;
/*
	return new ManifestFormOutlinePage(
		getInput(),
		getEditor().getDocumentProvider(),
		form);
*/
}
protected AbstractSectionForm createForm() {
	return new DependenciesForm(this);
}

public boolean contextMenuAboutToShow(IMenuManager manager) {
	return ((DependenciesForm)getForm()).fillContextMenu(manager);
}
}
