package org.eclipse.pde.internal.ui.editor.site;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.pde.internal.ui.editor.*;

public class ArchivePage extends PDEChildFormPage {

	public ArchivePage(PDEFormPage parent, String title) {
		super(parent, title);
	}
	protected AbstractSectionForm createForm() {
		return new ArchiveForm(this);
	}
}