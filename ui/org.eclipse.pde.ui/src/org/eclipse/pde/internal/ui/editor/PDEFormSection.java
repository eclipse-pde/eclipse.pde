/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.pde.core.*;
import org.eclipse.update.ui.forms.internal.*;

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
	AbstractSectionForm form = formPage.getForm();
	if (form instanceof ScrollableSectionForm) {
		((ScrollableSectionForm)form).updateScrollBars();
	}
}
}
