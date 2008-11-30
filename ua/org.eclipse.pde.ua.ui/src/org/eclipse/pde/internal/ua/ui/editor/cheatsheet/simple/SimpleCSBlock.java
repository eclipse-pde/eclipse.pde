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

package org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCS;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSIntro;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSItem;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSSubItem;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.ICSMaster;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple.details.SimpleCSDetails;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple.details.SimpleCSIntroDetails;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple.details.SimpleCSItemDetails;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple.details.SimpleCSSubItemDetails;
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
public class SimpleCSBlock extends PDEMasterDetailsBlock implements IDetailsPageProvider, IModelChangedListener {

	private SimpleCSMasterTreeSection fMasterSection;

	private SimpleCSItemDetails fItemDetails;

	private SimpleCSSubItemDetails fSubItemDetails;

	private SimpleCSDetails fCheatSheetDetails;

	private SimpleCSIntroDetails fIntroDetails;

	/**
	 * @param page
	 */
	public SimpleCSBlock(PDEFormPage page) {
		super(page);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEMasterDetailsBlock#createMasterSection(org.eclipse.ui.forms.IManagedForm, org.eclipse.swt.widgets.Composite)
	 */
	protected PDESection createMasterSection(IManagedForm managedForm, Composite parent) {
		fMasterSection = new SimpleCSMasterTreeSection(getPage(), parent);
		return fMasterSection;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.MasterDetailsBlock#registerPages(org.eclipse.ui.forms.DetailsPart)
	 */
	protected void registerPages(DetailsPart detailsPart) {
		// Only static pages to be defined.  Do not cache pages
		detailsPart.setPageLimit(0);
		// Register static page:  item
		fItemDetails = new SimpleCSItemDetails(fMasterSection);
		detailsPart.registerPage(SimpleCSItemDetails.class, fItemDetails);
		// Register static page:  subitem
		fSubItemDetails = new SimpleCSSubItemDetails(fMasterSection);
		detailsPart.registerPage(SimpleCSSubItemDetails.class, fSubItemDetails);
		// Register static page:  cheatsheet
		fCheatSheetDetails = new SimpleCSDetails(fMasterSection);
		detailsPart.registerPage(SimpleCSDetails.class, fCheatSheetDetails);
		// Register static page:  intro
		fIntroDetails = new SimpleCSIntroDetails(fMasterSection);
		detailsPart.registerPage(SimpleCSIntroDetails.class, fIntroDetails);
		// Set this class as the page provider
		detailsPart.setPageProvider(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IDetailsPageProvider#getPageKey(java.lang.Object)
	 */
	public Object getPageKey(Object object) {
		// Get static page key
		if (object instanceof ISimpleCSItem) {
			// Static page:  item
			return SimpleCSItemDetails.class;
		} else if (object instanceof ISimpleCSSubItem) {
			// Static page:  subitem
			return SimpleCSSubItemDetails.class;
		} else if (object instanceof ISimpleCS) {
			// Static page:  cheatsheet
			return SimpleCSDetails.class;
		} else if (object instanceof ISimpleCSIntro) {
			// Static page:  intro
			return SimpleCSIntroDetails.class;
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
