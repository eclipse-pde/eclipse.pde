package org.eclipse.pde.internal.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.ui.views.contentoutline.*;
import org.eclipse.pde.internal.editor.*;

public class FeatureReferencePage extends PDEChildFormPage {

public FeatureReferencePage(PDEFormPage parent, String title) {
	super(parent, title);
}
protected AbstractSectionForm createForm() {
	return new ReferenceForm(this);
}
}
