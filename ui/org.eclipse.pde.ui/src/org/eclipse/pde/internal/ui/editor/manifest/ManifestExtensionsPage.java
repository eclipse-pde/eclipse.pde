package org.eclipse.pde.internal.ui.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.ui.views.properties.*;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.pde.internal.ui.editor.*;


public class ManifestExtensionsPage extends PDEChildFormPage {

public ManifestExtensionsPage(ManifestFormPage parentPage, String title) {
	super(parentPage, title);
}
protected AbstractSectionForm createForm() {
	return new ExtensionsForm(this);
}
public IPropertySheetPage createPropertySheetPage() {
	return new ExtensionsPropertySheet(getEditor());
}
public void openNewExtensionWizard() {
	((ExtensionsForm)getForm()).openNewExtensionWizard();
}
}
