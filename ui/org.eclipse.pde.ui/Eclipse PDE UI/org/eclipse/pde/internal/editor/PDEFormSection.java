package org.eclipse.pde.internal.editor;

import org.eclipse.pde.internal.forms.*;
import org.eclipse.pde.internal.base.model.*;

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
}
