/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.site;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.editor.PDEDetails;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;

public class CategoryDetails extends PDEDetails {
	private CategoryDetailsSection categoryDetailsSection;

	public CategoryDetails() {
	}

	public void cancelEdit() {
		categoryDetailsSection.cancelEdit();
		super.cancelEdit();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#commit()
	 */
	public void commit(boolean onSave) {
		categoryDetailsSection.commit(onSave);
		super.commit(onSave);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	public void createContents(Composite parent) {
		GridLayout layout = new GridLayout();
		parent.setLayout(layout);
		categoryDetailsSection = new CategoryDetailsSection(getPage(), parent);
		GridData gd = new GridData(GridData.FILL_BOTH);
		categoryDetailsSection.getSection().setLayoutData(gd);
		markDetailsPart(categoryDetailsSection.getSection().getClient());
		if (isEditable())
			((ISiteModel)getPage().getModel()).addModelChangedListener(this);
	}

	public void dispose() {
		categoryDetailsSection.dispose();
		super.dispose();
	}

	public void fireSaveNeeded() {
		markDirty();
		getPage().getPDEEditor().fireSaveNeeded(getContextId(), false);
	}

	public String getContextId() {
		return SiteInputContext.CONTEXT_ID;
	}

	public PDEFormPage getPage() {
		return (PDEFormPage) getManagedForm().getContainer();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#initialize(org.eclipse.ui.forms.IManagedForm)
	 */
	public void initialize(IManagedForm form) {
		super.initialize(form);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#isDirty()
	 */
	public boolean isDirty() {
		if (categoryDetailsSection.isDirty()) {
			return true;
		}
		return super.isDirty();
	}

	public boolean isEditable() {
		return getPage().getPDEEditor().getAggregateModel().isEditable();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#isStale()
	 */
	public boolean isStale() {
		if (categoryDetailsSection.isStale())
			return true;
		return super.isStale();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.IModelChangedListener#modelChanged(org.eclipse.pde.core.IModelChangedEvent)
	 */
	public void modelChanged(IModelChangedEvent event) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#refresh()
	 */
	public void refresh() {
		categoryDetailsSection.refresh();
		super.refresh();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#inputChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void selectionChanged(IFormPart masterPart, ISelection selection) {
		categoryDetailsSection.selectionChanged(masterPart, selection);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#setFocus()
	 */
	public void setFocus() {
		categoryDetailsSection.setFocus();
	}
}
