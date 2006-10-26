/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.cheatsheet.comp;

import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCS;
import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSTask;
import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSTaskGroup;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDEMasterDetailsBlock;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.editor.cheatsheet.comp.details.CompCSDetails;
import org.eclipse.pde.internal.ui.editor.cheatsheet.comp.details.CompCSTaskDetails;
import org.eclipse.pde.internal.ui.editor.cheatsheet.comp.details.CompCSTaskGroupDetails;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.DetailsPart;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IDetailsPageProvider;
import org.eclipse.ui.forms.IManagedForm;

/**
 * CompCSBlock
 *
 */
public class CompCSBlock extends PDEMasterDetailsBlock implements
		IModelChangedListener, IDetailsPageProvider {

	private CompCSMasterTreeSection fMasterSection;	
	
	private IDetailsPage fCurrentDetailsSection;	
	
	/**
	 * @param page
	 */
	public CompCSBlock(PDEFormPage page) {
		super(page);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEMasterDetailsBlock#createMasterSection(org.eclipse.ui.forms.IManagedForm, org.eclipse.swt.widgets.Composite)
	 */
	protected PDESection createMasterSection(IManagedForm managedForm,
			Composite parent) {
		fMasterSection = new CompCSMasterTreeSection(getPage(), parent);
		return fMasterSection;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.MasterDetailsBlock#registerPages(org.eclipse.ui.forms.DetailsPart)
	 */
	protected void registerPages(DetailsPart detailsPart) {
		// TODO: MP: HIGH: CompCS: Set limit to 4 and add update methods accordingly to reuse the section
		detailsPart.setPageLimit(0); 
		detailsPart.setPageProvider(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModelChangedListener#modelChanged(org.eclipse.pde.core.IModelChangedEvent)
	 */
	public void modelChanged(IModelChangedEvent event) {
		// Inform the master section
		if (fMasterSection != null) {
			fMasterSection.modelChanged(event);
		}
		// Inform the details section
		// Unnecessary
		//if (fCurrentDetailsSection != null) {
		//	fCurrentDetailsSection.modelChanged(event);
		//}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IDetailsPageProvider#getPage(java.lang.Object)
	 */
	public IDetailsPage getPage(Object key) {

		if (key instanceof ICompCS) {
			fCurrentDetailsSection = new CompCSDetails((ICompCS) key,
					fMasterSection);
		} else if (key instanceof ICompCSTaskGroup) {
			fCurrentDetailsSection = new CompCSTaskGroupDetails(
					(ICompCSTaskGroup) key, fMasterSection);
		} else if (key instanceof ICompCSTask) {
			fCurrentDetailsSection = new CompCSTaskDetails(
					(ICompCSTask) key, fMasterSection);
		} else {
			fCurrentDetailsSection = null;
		}		
		
		return fCurrentDetailsSection;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IDetailsPageProvider#getPageKey(java.lang.Object)
	 */
	public Object getPageKey(Object object) {
		return object;
	}

	/**
	 * @return
	 */
	public CompCSMasterTreeSection getMastersSection() {
		return fMasterSection;
	}
	
}
