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

package org.eclipse.pde.internal.ui.editor.cheatsheet.simple;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSIntro;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItem;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDEMasterDetailsBlock;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.editor.cheatsheet.ICSMaster;
import org.eclipse.pde.internal.ui.editor.cheatsheet.simple.details.SimpleCSDetails;
import org.eclipse.pde.internal.ui.editor.cheatsheet.simple.details.SimpleCSIntroDetails;
import org.eclipse.pde.internal.ui.editor.cheatsheet.simple.details.SimpleCSItemDetails;
import org.eclipse.pde.internal.ui.editor.cheatsheet.simple.details.SimpleCSSubItemDetails;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.DetailsPart;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IDetailsPageProvider;
import org.eclipse.ui.forms.IManagedForm;

/**
 * SimpleCSBlock
 *
 */
public class SimpleCSBlock extends PDEMasterDetailsBlock implements
		IDetailsPageProvider, IModelChangedListener {

	private SimpleCSMasterTreeSection fMasterSection;

	private IDetailsPage fCurrentDetailsSection;
	
	/**
	 * @param page
	 */
	public SimpleCSBlock(PDEFormPage page) {
		super(page);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEMasterDetailsBlock#createMasterSection(org.eclipse.ui.forms.IManagedForm, org.eclipse.swt.widgets.Composite)
	 */
	protected PDESection createMasterSection(IManagedForm managedForm,
			Composite parent) {
		fMasterSection = new SimpleCSMasterTreeSection(getPage(), parent);
		return fMasterSection;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.MasterDetailsBlock#registerPages(org.eclipse.ui.forms.DetailsPart)
	 */
	protected void registerPages(DetailsPart detailsPart) {
		// TODO: MP: HIGH: SimpleCS: Set limit to 4 and add update methods accordingly to reuse the section
		detailsPart.setPageLimit(0); 
		detailsPart.setPageProvider(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IDetailsPageProvider#getPage(java.lang.Object)
	 */
	public IDetailsPage getPage(Object key) {

		if (key instanceof ISimpleCSItem) {
			fCurrentDetailsSection = new SimpleCSItemDetails(
					(ISimpleCSItem) key, fMasterSection);
		} else if (key instanceof ISimpleCSSubItem) {
			fCurrentDetailsSection = new SimpleCSSubItemDetails(
					(ISimpleCSSubItem) key, fMasterSection);
		} else if (key instanceof ISimpleCS) {
			fCurrentDetailsSection = new SimpleCSDetails((ISimpleCS) key,
					fMasterSection);
		} else if (key instanceof ISimpleCSIntro) {
			fCurrentDetailsSection = new SimpleCSIntroDetails(
					(ISimpleCSIntro) key, fMasterSection);
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
	
	/**
	 * @return
	 */
	public ICSMaster getMastersSection() {
		return fMasterSection;
	}

	/**
	 * @return
	 */
	public ISelection getSelection() {
		if (fMasterSection != null) {
			return fMasterSection.getSelection();
		}
		return null;
	}	
	
}
