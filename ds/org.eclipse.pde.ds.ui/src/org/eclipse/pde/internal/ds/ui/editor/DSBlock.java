/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira Nóbrega <rafael.oliveira@gmail.com> - bug 223739
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.internal.ds.core.IDSImplementation;
import org.eclipse.pde.internal.ds.core.IDSProperties;
import org.eclipse.pde.internal.ds.core.IDSProperty;
import org.eclipse.pde.internal.ds.core.IDSProvide;
import org.eclipse.pde.internal.ds.core.IDSReference;
import org.eclipse.pde.internal.ds.core.IDSRoot;
import org.eclipse.pde.internal.ds.core.IDSService;
import org.eclipse.pde.internal.ds.ui.editor.details.DSProvideDetails;
import org.eclipse.pde.internal.ds.ui.editor.details.DSServiceDetails;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDEMasterDetailsBlock;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.DetailsPart;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IDetailsPageProvider;
import org.eclipse.ui.forms.IManagedForm;

public class DSBlock extends PDEMasterDetailsBlock implements IModelChangedListener, IDetailsPageProvider {

	private DSMasterTreeSection fMasterSection;
	private DSComponentDetails fComponentDetails;
	private DSImplementationDetails fImplementationDetails;
	private DSPropertyDetails fPropertyDetails;
	private DSPropertiesDetails fPropertiesDetails;
	private DSProvideDetails  fProvideDetails;
	private DSReferenceDetails  fReferenceDetails;
	private DSServiceDetails  fServiceDetails;
	
	public DSBlock(PDEFormPage page) {
		super(page);
	}

	protected PDESection createMasterSection(IManagedForm managedForm,
			Composite parent) {
		fMasterSection = new DSMasterTreeSection(getPage(), parent);
		return fMasterSection;
	}

	protected void registerPages(DetailsPart arg0) {
		// Only static pages to be defined.  Do not cache pages
		detailsPart.setPageLimit(0);

		//Instantiate and regiter Static Pages.
		fImplementationDetails = new DSImplementationDetails(fMasterSection);
		detailsPart.registerPage(DSImplementationDetails.class, fImplementationDetails);

		fComponentDetails= new DSComponentDetails(fMasterSection);
		detailsPart.registerPage(DSComponentDetails.class, fComponentDetails);

		fPropertiesDetails = new DSPropertiesDetails(fMasterSection);
		detailsPart.registerPage(DSPropertiesDetails.class, fPropertiesDetails);

		fPropertyDetails = new DSPropertyDetails(fMasterSection);
		detailsPart.registerPage(DSPropertyDetails.class, fPropertyDetails);

		fProvideDetails = new DSProvideDetails(fMasterSection);
		detailsPart.registerPage(DSProvideDetails.class, fProvideDetails);

		fReferenceDetails = new DSReferenceDetails(fMasterSection);
		detailsPart.registerPage(DSReferenceDetails.class, fReferenceDetails);

		fServiceDetails = new DSServiceDetails(fMasterSection);
		detailsPart.registerPage(DSServiceDetails.class, fServiceDetails);

		// Set this class as the page provider
		detailsPart.setPageProvider(this);
		
	}

	public void modelChanged(IModelChangedEvent event) {
		if (fMasterSection != null) {
			fMasterSection.modelChanged(event);
		}		
	}

	public IDetailsPage getPage(Object arg0) {
		// No dynamic pages.  Static pages already registered
		return null;
	}

	public Object getPageKey(Object object) {
		// Get static page key
		if (object instanceof IDSRoot) {
			return DSComponentDetails.class;
		} else if (object instanceof IDSImplementation) {
			return DSImplementationDetails.class;
		} else if (object instanceof IDSProperties) {
			return DSPropertiesDetails.class;
		} else if (object instanceof IDSProperty) {
			return DSPropertyDetails.class;
		} else if (object instanceof IDSProvide) {
			return DSProvideDetails.class;
		} else if (object instanceof IDSService) {
			return DSServiceDetails.class;
		} else if (object instanceof IDSReference) {
			return DSReferenceDetails.class;
		}
		// Should never reach here
		return object.getClass();
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

	/**
	 * @return
	 */
	public IDSMaster getMasterSection() {
		return fMasterSection;
	}
	
}
