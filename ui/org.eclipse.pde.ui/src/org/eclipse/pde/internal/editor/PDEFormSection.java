package org.eclipse.pde.internal.editor;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.internal.PDEPlugin;

public abstract class PDEFormSection extends FormSection implements IModelChangedListener {
	private PDEFormPage formPage;

public PDEFormSection(PDEFormPage formPage) {
	this.formPage = formPage;
}
public PDEFormPage getFormPage() {
	return formPage;
}
public void modelChanged(IModelChangedEvent e) {
}

protected void reflow() {
	super.reflow();
	SectionForm form = formPage.getForm();
	if (form instanceof ScrollableSectionForm) {
		((ScrollableSectionForm)form).updateScrollBars();
	}
}
}
