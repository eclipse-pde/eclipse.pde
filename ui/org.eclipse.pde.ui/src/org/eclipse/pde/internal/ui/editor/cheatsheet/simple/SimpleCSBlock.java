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

import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSCommand;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSDescription;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSIntro;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem;
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
		IDetailsPageProvider {

	private SimpleCSElementSection fSection;
	
	private SimpleCSAbstractDetails fCurrentSection;
	
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
		fSection = new SimpleCSElementSection(getPage(), parent);
		return fSection;
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
			fCurrentSection = new SimpleCSItemDetails((ISimpleCSItem)key, fSection);
		} else if (key instanceof ISimpleCSSubItem) {
			fCurrentSection = new SimpleCSSubItemDetails((ISimpleCSSubItem)key, fSection);
		} else if (key instanceof ISimpleCS) {
			fCurrentSection = new SimpleCSDetails((ISimpleCS)key, fSection);
		} else if (key instanceof ISimpleCSDescription) {
			fCurrentSection = new SimpleCSDescriptionDetails((ISimpleCSDescription)key, fSection);
		} else if (key instanceof ISimpleCSIntro) {
			fCurrentSection = new SimpleCSIntroDetails((ISimpleCSIntro)key, fSection);
		} else if (key instanceof ISimpleCSCommand) {
			fCurrentSection = new SimpleCSCommandDetails((ISimpleCSCommand)key, fSection);
		} else {
			fCurrentSection = null;
		}
		
		return fCurrentSection;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IDetailsPageProvider#getPageKey(java.lang.Object)
	 */
	public Object getPageKey(Object object) {
		return object;
	}

}
