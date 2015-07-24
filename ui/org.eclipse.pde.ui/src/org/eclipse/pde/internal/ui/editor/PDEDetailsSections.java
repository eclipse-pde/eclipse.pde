/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IFormPart;

/**
 * Wrapper for PDESections, implemens IDetailsPage for use in MasterDetailsBlock
 */
public abstract class PDEDetailsSections extends PDEDetails {
	private PDESection sections[];

	protected abstract PDESection[] createSections(PDEFormPage page, Composite parent);

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createContents(Composite parent) {
		sections = createSections(getPage(), parent);
		parent.setLayout(FormLayoutFactory.createDetailsGridLayout(false, 1));
		for (int i = 0; i < sections.length; i++) {
			getManagedForm().addPart(sections[i]);
		}
	}

	@Override
	public void dispose() {
		for (int i = 0; i < sections.length; i++) {
			sections[i].dispose();
		}
	}

	@Override
	public void fireSaveNeeded() {
		markDirty();
		getPage().getPDEEditor().fireSaveNeeded(getContextId(), false);
	}

	@Override
	public abstract String getContextId();

	@Override
	public PDEFormPage getPage() {
		return (PDEFormPage) getManagedForm().getContainer();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#isDirty()
	 */
	@Override
	public boolean isDirty() {
		for (int i = 0; i < sections.length; i++) {
			if (sections[i].isDirty()) {
				return true;
			}
		}
		return super.isDirty();
	}

	@Override
	public boolean isEditable() {
		return getPage().getPDEEditor().getAggregateModel().isEditable();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#isStale()
	 */
	@Override
	public boolean isStale() {
		for (int i = 0; i < sections.length; i++) {
			if (sections[i].isStale()) {
				return true;
			}
		}
		return super.isStale();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.IModelChangedListener#modelChanged(org.eclipse.pde.core.IModelChangedEvent)
	 */
	@Override
	public void modelChanged(IModelChangedEvent event) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#inputChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	@Override
	public void selectionChanged(IFormPart masterPart, ISelection selection) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#setFocus()
	 */
	@Override
	public void setFocus() {
		if (sections.length > 0) {
			sections[0].setFocus();
		}
	}
}
