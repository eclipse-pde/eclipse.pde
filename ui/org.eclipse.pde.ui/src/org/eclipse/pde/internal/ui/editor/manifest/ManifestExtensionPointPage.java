package org.eclipse.pde.internal.ui.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.ui.views.properties.*;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.pde.internal.ui.editor.*;


public class ManifestExtensionPointPage extends PDEChildFormPage {

public ManifestExtensionPointPage(ManifestFormPage parentPage, String title) {
	super(parentPage, title);
}
protected AbstractSectionForm createForm() {
	return new ExtensionPointForm(this);
}
public IPropertySheetPage createPropertySheetPage() {
	return new ManifestPropertySheet(getEditor());
}
}
