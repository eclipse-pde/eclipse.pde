package org.eclipse.pde.internal.ui.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.pde.internal.ui.editor.*;


public class ManifestRuntimePage extends PDEChildFormPage {

public ManifestRuntimePage(ManifestFormPage parentPage, String title) {
	super(parentPage, title);
}
protected AbstractSectionForm createForm() {
	return new RuntimeForm(this);
}
}
