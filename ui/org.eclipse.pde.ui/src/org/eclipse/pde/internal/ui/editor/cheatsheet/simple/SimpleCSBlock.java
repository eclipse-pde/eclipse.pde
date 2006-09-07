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

import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSCommand;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSDescription;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSIntro;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSOnCompletion;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItem;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDEMasterDetailsBlock;
import org.eclipse.pde.internal.ui.editor.PDESection;
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

	// TODO: MP: Update name to master section ?
	private SimpleCSElementSection fMasterSection;
	// TODO: MP: Create a new interface 
	private SimpleCSAbstractDetails fCurrentDetailsSection;
	
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
		fMasterSection = new SimpleCSElementSection(getPage(), parent);
		return fMasterSection;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.MasterDetailsBlock#registerPages(org.eclipse.ui.forms.DetailsPart)
	 */
	protected void registerPages(DetailsPart detailsPart) {
		// We Need to store current page in this object		
		detailsPart.setPageLimit(0); 
		detailsPart.setPageProvider(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IDetailsPageProvider#getPage(java.lang.Object)
	 */
	public IDetailsPage getPage(Object key) {

		if (key instanceof ISimpleCSItem) {
			fCurrentDetailsSection = new SimpleCSItemDetails((ISimpleCSItem)key, fMasterSection);
		} else if (key instanceof ISimpleCSSubItem) {
			fCurrentDetailsSection = new SimpleCSSubItemDetails((ISimpleCSSubItem)key, fMasterSection);
		} else if (key instanceof ISimpleCS) {
			fCurrentDetailsSection = new SimpleCSDetails((ISimpleCS)key, fMasterSection);
		} else if (key instanceof ISimpleCSDescription) {
			fCurrentDetailsSection = new SimpleCSDescriptionDetails((ISimpleCSDescription)key, fMasterSection);
		} else if (key instanceof ISimpleCSIntro) {
			fCurrentDetailsSection = new SimpleCSIntroDetails((ISimpleCSIntro)key, fMasterSection);
		} else if (key instanceof ISimpleCSCommand) {
			fCurrentDetailsSection = new SimpleCSCommandDetails((ISimpleCSCommand)key, fMasterSection);
		} else if (key instanceof ISimpleCSOnCompletion) {
			fCurrentDetailsSection = new SimpleCSOnCompletionDetails((ISimpleCSOnCompletion)key, fMasterSection);
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
			fMasterSection.handleModelChanged(event);
		}
		// Inform the details section
		if (fCurrentDetailsSection != null) {
			fCurrentDetailsSection.modelChanged(event);
		}
	}
	
	/**
	 * @return
	 */
	public SimpleCSElementSection getMastersSection() {
		return fMasterSection;
	}
	
	/**
	 * @return
	 */
	public SimpleCSAbstractDetails getCurrentDetailsSection() {
		// TODO: MP: Should use inteface instead of abstract class
		// Method not used at the moment
		return fCurrentDetailsSection;
	}
	
}
