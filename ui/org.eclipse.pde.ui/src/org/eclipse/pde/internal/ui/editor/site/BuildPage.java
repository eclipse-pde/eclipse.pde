package org.eclipse.pde.internal.ui.editor.site;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.pde.internal.ui.editor.*;

public class BuildPage extends PDEChildFormPage {

	public BuildPage(PDEFormPage parent, String title) {
		super(parent, title);
	}
	protected AbstractSectionForm createForm() {
		return new BuildForm(this);
	}
}