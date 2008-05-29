/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rafael Oliveira Nóbrega <rafael.oliveira@gmail.com> - bug 230232
 *******************************************************************************/
package org.eclipse.pde.internal.ds.core.builders;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.ds.core.Activator;
import org.eclipse.pde.internal.ds.core.IDSComponent;
import org.eclipse.pde.internal.ds.core.IDSImplementation;
import org.eclipse.pde.internal.ds.core.IDSModel;
import org.eclipse.pde.internal.ds.core.IDSProperties;
import org.eclipse.pde.internal.ds.core.IDSProperty;
import org.eclipse.pde.internal.ds.core.IDSReference;
import org.eclipse.pde.internal.ds.core.IDSService;
import org.eclipse.pde.internal.ds.core.text.DSModel;

public class DSErrorReporter extends XMLErrorReporter {

	public DSErrorReporter(IFile file) {
		super(file);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.builders.XMLErrorReporter#validateContent(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void validateContent(IProgressMonitor monitor) {
	
		try {
			IDSModel model = new DSModel(CoreUtility.getTextDocument(fFile
					.getContents()), false);
			model.setUnderlyingResource(fFile);	
			
			IDSComponent component = model.getDSComponent();
			
			validateComponent(component);
			validateService(component.getService());
			validateImplementation(component.getImplementation());
			validatePropertyElements(component.getPropertyElements());
			validateProperties(component.getPropertiesElements());
			validateReferences(component.getReferences());
			
		} catch (CoreException e) {
			Activator.log(e);
		}	
	
	}

	private void validateReferences(IDSReference[] references) {
		// TODO Auto-generated method stub

	}

	private void validateProperties(IDSProperties[] propertiesElements) {
		// TODO Auto-generated method stub

	}

	private void validatePropertyElements(IDSProperty[] propertyElements) {
		// TODO Auto-generated method stub

	}

	private void validateImplementation(IDSImplementation implementation) {
		// TODO Auto-generated method stub

	}

	private void validateComponent(IDSComponent component) {
		// TODO Auto-generated method stub

	}

	private void validateService(IDSService service) {
		// TODO
	}


}
