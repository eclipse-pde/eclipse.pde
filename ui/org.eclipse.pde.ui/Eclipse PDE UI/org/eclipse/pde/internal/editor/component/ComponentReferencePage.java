package org.eclipse.pde.internal.editor.component;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.forms.*;
import org.eclipse.ui.views.contentoutline.*;
import org.eclipse.pde.internal.editor.*;

public class ComponentReferencePage extends PDEChildFormPage {

public ComponentReferencePage(PDEFormPage parent, String title) {
	super(parent, title);
}
protected Form createForm() {
	return new ReferenceForm(this);
}
}
