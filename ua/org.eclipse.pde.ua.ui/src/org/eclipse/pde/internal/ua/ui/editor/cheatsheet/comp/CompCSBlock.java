/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.ui.editor.cheatsheet.comp;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCS;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTask;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskGroup;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.comp.details.CompCSDetails;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.comp.details.CompCSTaskDetails;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.comp.details.CompCSTaskGroupDetails;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDEMasterDetailsBlock;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.DetailsPart;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IDetailsPageProvider;
import org.eclipse.ui.forms.IManagedForm;

/**
 * CompCSBlock
 *
 */
public class CompCSBlock extends PDEMasterDetailsBlock implements IModelChangedListener, IDetailsPageProvider {

	private CompCSMasterTreeSection fMasterSection;

	private CompCSDetails fDetails;

	private CompCSTaskGroupDetails fTaskGroupDetails;

	private CompCSTaskDetails fTaskDetails;

	/**
	 * @param page
	 */
	public CompCSBlock(PDEFormPage page) {
		super(page);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEMasterDetailsBlock#createMasterSection(org.eclipse.ui.forms.IManagedForm, org.eclipse.swt.widgets.Composite)
	 */
	protected PDESection createMasterSection(IManagedForm managedForm, Composite parent) {
		fMasterSection = new CompCSMasterTreeSection(getPage(), parent);
		return fMasterSection;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.MasterDetailsBlock#registerPages(org.eclipse.ui.forms.DetailsPart)
	 */
	protected void registerPages(DetailsPart detailsPart) {
		// Only static pages to be defined.  Do not cache pages
		detailsPart.setPageLimit(0);
		// Register static page:  compositeCheatsheet
		fDetails = new CompCSDetails(fMasterSection);
		detailsPart.registerPage(CompCSDetails.class, fDetails);
		// Register static page:  taskGroup
		fTaskGroupDetails = new CompCSTaskGroupDetails(fMasterSection);
		detailsPart.registerPage(CompCSTaskGroupDetails.class, fTaskGroupDetails);
		// Register static page:  task
		fTaskDetails = new CompCSTaskDetails(fMasterSection);
		detailsPart.registerPage(CompCSTaskDetails.class, fTaskDetails);
		// Set this class as the page provider
		detailsPart.setPageProvider(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IDetailsPageProvider#getPageKey(java.lang.Object)
	 */
	public Object getPageKey(Object object) {
		// Get static page key
		if (object instanceof ICompCS) {
			// Static page:  compositeCheatsheet
			return CompCSDetails.class;
		} else if (object instanceof ICompCSTaskGroup) {
			// Static page:  taskGroup
			return CompCSTaskGroupDetails.class;
		} else if (object instanceof ICompCSTask) {
			// Static page:  task
			return CompCSTaskDetails.class;
		}
		// Should never reach here
		return object.getClass();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IDetailsPageProvider#getPage(java.lang.Object)
	 */
	public IDetailsPage getPage(Object key) {
		// No dynamic pages.  Static pages already registered
		return null;
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
	public CompCSMasterTreeSection getMastersSection() {
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
