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

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.builders.CompilerFlags;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.ds.core.Activator;
import org.eclipse.pde.internal.ds.core.IDSComponent;
import org.eclipse.pde.internal.ds.core.IDSConstants;
import org.eclipse.pde.internal.ds.core.IDSImplementation;
import org.eclipse.pde.internal.ds.core.IDSModel;
import org.eclipse.pde.internal.ds.core.IDSProperties;
import org.eclipse.pde.internal.ds.core.IDSProperty;
import org.eclipse.pde.internal.ds.core.IDSReference;
import org.eclipse.pde.internal.ds.core.IDSService;
import org.eclipse.pde.internal.ds.core.Messages;
import org.eclipse.pde.internal.ds.core.text.DSModel;
import org.w3c.dom.Element;

public class DSErrorReporter extends XMLErrorReporter {

	public DSErrorReporter(IFile file) {
		super(file);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.builders.XMLErrorReporter#validateContent(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void validateContent(IProgressMonitor monitor) {

		try {
			IDSModel model = new DSModel(CoreUtility.getTextDocument(fFile.getContents()),
					false);
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
		if (implementation != null) {
			String className = implementation.getClassName();
			File f = new File(className);
			if (!f.exists()) {
				reportResourceNotFound(IDSConstants.ELEMENT_IMPLEMENTATION,
						IDSConstants.ATTRIBUTE_CLASS, className);
			}
		}

	}

	private void reportResourceNotFound(String elementConstant,
			String attributeConstant, String resource) {
		Element element = (Element) getDocumentRoot()
				.getElementsByTagName(
						elementConstant).item(0);

		
		String[] binds = new String[] { resource,
				attributeConstant };
		
		report(NLS.bind(Messages.DSErrorReporter_cannotResolveResource,
				binds),
				getLine(element), CompilerFlags.WARNING,
				DSMarkerFactory.CAT_OTHER);
	}

	private void validateComponent(IDSComponent component) {
		if (component != null) {
			if (component.getAttributeName() == null) {
				report(NLS.bind(Messages.DSErrorReporter_requiredAttribute,
						new String[] { IDSConstants.ATTRIBUTE_COMPONENT_NAME,
								IDSConstants.ELEMENT_COMPONENT }),
						getLine(getDocumentRoot()),
						CompilerFlags.ERROR, DSMarkerFactory.CAT_OTHER);
		
			}
			
			if (component.getImplementation() == null) {
				report(NLS.bind(Messages.DSErrorReporter_requiredElement,
						IDSConstants.ELEMENT_IMPLEMENTATION),
						getLine(getDocumentRoot()),
						CompilerFlags.ERROR, DSMarkerFactory.CAT_OTHER);
			}

		}
	}

	private void validateService(IDSService service) {
		// TODO
	}

}
