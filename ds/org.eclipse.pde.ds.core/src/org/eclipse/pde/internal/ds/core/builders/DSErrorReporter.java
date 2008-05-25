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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.builders.CompilerFlags;
import org.eclipse.pde.internal.core.builders.PDEMarkerFactory;
import org.eclipse.pde.internal.core.builders.XMLErrorReporter;
import org.eclipse.pde.internal.ds.core.IDSConstants;
import org.eclipse.pde.internal.ds.core.Messages;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class DSErrorReporter extends XMLErrorReporter {

	public DSErrorReporter(IFile file) {
		super(file);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.builders.XMLErrorReporter#validateContent(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void validateContent(IProgressMonitor monitor) {
		List elements = new ArrayList();
	

		//		validateXMLSyntax();
		
		Element component = getDocumentRoot();
		//		validateRoot(component);
		

		// NodeList implementations = component
		// .getElementsByTagName(IDSConstants.ELEMENT_IMPLEMENTATION);
		// validateImplementation(implementations);
		//		
		// NodeList propertiesElements = component
		// .getElementsByTagName(IDSConstants.ELEMENT_PROPERTIES);
		// validateProperties(propertiesElements);
		//		
		// NodeList propertyElements = component
		// .getElementsByTagName(IDSConstants.ELEMENT_PROPERTY);
		// validateProperty(propertyElements);
		//		
		// NodeList provides = component
		// .getElementsByTagName(IDSConstants.ELEMENT_PROVIDE);
		// validateProvides(provides);
		//		
		// NodeList references = component
		// .getElementsByTagName(IDSConstants.ELEMENT_REFERENCE);
		// validateReferences(references);

		NodeList services = component
				.getElementsByTagName(IDSConstants.ELEMENT_SERVICE);
		validateServices(services);
	
	}

	private void validateServices(NodeList services) {
		// Multiple Elements validation
		if (services.getLength() > 1) {
			Element element = (Element) services.item(1);
			reportMultipleElementsForbidden(element,
					IDSConstants.ELEMENT_SERVICE);
		}
	}

	private void reportMultipleElementsForbidden(Element element,
			String elementType) {
		// FIXME change message
		report(NLS.bind(Messages.DSErrorReporter_multipleElements, element
				.getNodeName()), getLine(element), CompilerFlags.ERROR,
				PDEMarkerFactory.CAT_OTHER);

	}

	private void validateReferences(NodeList references) {
		// TODO Auto-generated method stub

	}

	private void validateProvides(NodeList provides) {
		// TODO Auto-generated method stub

	}

	private void validateProperty(NodeList propertyElements) {
		// TODO Auto-generated method stub

	}

	private void validateProperties(NodeList propertiesElements) {
		// TODO Auto-generated method stub

	}

	private void validateImplementation(NodeList implementations) {
		// TODO Auto-generated method stub

	}

	private void validateRoot(Element component) {
		// TODO Auto-generated method stub

	}

}
