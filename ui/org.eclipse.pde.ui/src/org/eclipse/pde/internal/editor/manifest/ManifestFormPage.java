package org.eclipse.pde.internal.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.editor.*;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.update.ui.forms.internal.SectionForm;

public class ManifestFormPage extends PDEFormPage {


public ManifestFormPage(ManifestEditor editor, String title) {
	super(editor, title);
}

public IContentOutlinePage createContentOutlinePage() {
	return new ManifestFormOutlinePage(this);
}
protected SectionForm createForm() {
	return new ManifestForm(this);
}
}
