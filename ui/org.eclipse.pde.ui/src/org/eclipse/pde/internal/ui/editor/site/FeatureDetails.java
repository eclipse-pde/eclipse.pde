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
import org.eclipse.pde.internal.ui.editor.PDEDetails;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IFormPart;

public class FeatureDetails extends PDEDetails {
	private FeatureDetailsSection featureDetailsSection;

	private PortabilitySection portabilitySection;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	public void createContents(Composite parent) {
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 30;
		parent.setLayout(layout);
		featureDetailsSection = new FeatureDetailsSection(getPage(), parent);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		featureDetailsSection.getSection().setLayoutData(gd);
		portabilitySection = new PortabilitySection(getPage(), parent);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		portabilitySection.getSection().setLayoutData(gd);

		featureDetailsSection.initialize();
		portabilitySection.initialize();
		getManagedForm().addPart(featureDetailsSection);
		getManagedForm().addPart(portabilitySection);
		//markDetailsPart(featureDetailsSection.getSection().getClient());
	}

	public void dispose() {
		featureDetailsSection.dispose();
		portabilitySection.dispose();
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
	 * @see org.eclipse.ui.forms.AbstractFormPart#isDirty()
	 */
	public boolean isDirty() {
		if (featureDetailsSection.isDirty() || portabilitySection.isDirty()) {
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
		if (featureDetailsSection.isStale() || portabilitySection.isStale())
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
	 * @see org.eclipse.ui.forms.IDetailsPage#inputChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void selectionChanged(IFormPart masterPart, ISelection selection) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#setFocus()
	 */
	public void setFocus() {
		featureDetailsSection.setFocus();
	}
}
