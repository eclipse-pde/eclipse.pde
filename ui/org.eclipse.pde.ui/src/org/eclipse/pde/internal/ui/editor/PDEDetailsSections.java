/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IFormPart;

/**
 * Wrapper for PDESections, implemens IDetailsPage for use in MasterDetailsBlock
 */
public abstract class PDEDetailsSections extends PDEDetails {
	private PDESection sections[];

	protected abstract PDESection[] createSections(PDEFormPage page, Composite parent);

	@Override
	public void createContents(Composite parent) {
		sections = createSections(getPage(), parent);
		parent.setLayout(FormLayoutFactory.createDetailsGridLayout(false, 1));
		for (PDESection section : sections) {
			getManagedForm().addPart(section);
		}
	}

	@Override
	public void dispose() {
		for (PDESection section : sections) {
			section.dispose();
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

	@Override
	public boolean isDirty() {
		for (PDESection section : sections) {
			if (section.isDirty()) {
				return true;
			}
		}
		return super.isDirty();
	}

	@Override
	public boolean isEditable() {
		IBaseModel model = getPage().getPDEEditor().getAggregateModel();
		return model != null && model.isEditable();
	}

	@Override
	public boolean isStale() {
		for (PDESection section : sections) {
			if (section.isStale()) {
				return true;
			}
		}
		return super.isStale();
	}

	@Override
	public void modelChanged(IModelChangedEvent event) {
	}

	@Override
	public void selectionChanged(IFormPart masterPart, ISelection selection) {
	}

	@Override
	public void setFocus() {
		if (sections.length > 0) {
			sections[0].setFocus();
		}
	}
}
