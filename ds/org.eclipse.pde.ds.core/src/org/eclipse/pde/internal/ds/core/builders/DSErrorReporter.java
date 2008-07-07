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
import org.eclipse.jface.text.Document;
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
import org.eclipse.pde.internal.ds.core.IDSProvide;
import org.eclipse.pde.internal.ds.core.IDSReference;
import org.eclipse.pde.internal.ds.core.IDSService;
import org.eclipse.pde.internal.ds.core.Messages;
import org.eclipse.pde.internal.ds.core.text.DSModel;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

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
			Document textDocument = CoreUtility.getTextDocument(fFile
					.getContents());
			IDSModel model = new DSModel(textDocument, false);

			model.load();

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
	
	private void validateBoolean(Element element, Attr attr) {
		String value = attr.getValue();
		if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) //$NON-NLS-1$ //$NON-NLS-2$
			reportIllegalAttributeValue(element, attr);
	}

	private void reportIllegalAttributeValue(Element element, Attr attr) {
		String message = NLS.bind(Messages.DSErrorReporter_attrValue,
				(new String[] { attr.getValue(), attr.getName() }));
		report(message, getLine(element, attr.getName()), CompilerFlags.ERROR,
				DSMarkerFactory.CAT_OTHER);
	}

	private void validateReferences(IDSReference[] references) {
		for (int i = 0; i < references.length; i++) {
			IDSReference reference = references[i];

			// Validate Required Attributes
			if (reference.getName() == null) {
				Element element = (Element) getDocumentRoot()
						.getElementsByTagName(reference.getXMLTagName())
						.item(i);
				reportMissingRequiredAttribute(element,
						IDSConstants.ATTRIBUTE_REFERENCE_NAME,
						CompilerFlags.ERROR);
			}

			if (reference.getReferenceInterface() == null) {
				Element element = (Element) getDocumentRoot()
						.getElementsByTagName(reference.getXMLTagName())
						.item(i);
				reportMissingRequiredAttribute(element,
						IDSConstants.ATTRIBUTE_REFERENCE_INTERFACE,
						CompilerFlags.ERROR);
			}

		}

	}

	private void validateProperties(IDSProperties[] propertiesElements) {
		for (int i = 0; i < propertiesElements.length; i++) {
			IDSProperties properties = propertiesElements[i];

			// Validate Required Attributes
			if (properties.getEntry() == null) {
				Element element = (Element) getDocumentRoot()
						.getElementsByTagName(properties.getXMLTagName()).item(
								i);
				reportMissingRequiredAttribute(element,
						IDSConstants.ATTRIBUTE_PROPERTIES_ENTRY,
						CompilerFlags.ERROR);
			}

		}

	}

	private void validatePropertyElements(IDSProperty[] propertyElements) {
		for (int i = 0; i < propertyElements.length; i++) {
			IDSProperty property = propertyElements[i];
			
			// Validate Required Attributes
			if (property.getName() == null) {
				Element element = (Element) getDocumentRoot()
						.getElementsByTagName(property.getXMLTagName()).item(i);
				reportMissingRequiredAttribute(element,
						IDSConstants.ATTRIBUTE_PROPERTY_NAME,
						CompilerFlags.ERROR);
			}

		}
	}

	private void validateImplementation(IDSImplementation implementation) {
		if (implementation != null) {
			String className = implementation.getClassName();
			if (className == null) {
				// Validate Required Attributes
				Element element = (Element) getDocumentRoot()
						.getElementsByTagName(implementation.getXMLTagName())
						.item(0);
				reportMissingRequiredAttribute(element,
						IDSConstants.ATTRIBUTE_IMPLEMENTATION_CLASS,
						CompilerFlags.ERROR);
			} else {
				// Validate Resource existence - FIXME Not Working yet.
// IWorkspaceRoot myWorkspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
// IProject[] projects = myWorkspaceRoot.getProjects();
// IResource member = null;
// System.out.println(className);
				// for (int i = 0; i < projects.length; i++) {
				// IResource temp = projects[i].findMember(className);
				// if (temp != null) {
				// member = temp;
				// }
				// }
				//				
				// System.out.println(member);
				// if (member == null) {
				// reportResourceNotFound(IDSConstants.ELEMENT_IMPLEMENTATION,
				// IDSConstants.ATTRIBUTE_IMPLEMENTATION_CLASS,
				// className);
				// }
			}
		}

	}

	protected void reportMissingRequiredAttribute(Element element,
			String attName, int severity) {

		String message = NLS.bind(Messages.DSErrorReporter_requiredAttribute,
				(new String[] { attName, element.getNodeName() })); //			
		report(message, getLine(element), severity, DSMarkerFactory.CAT_OTHER);
	}

	private void reportResourceNotFound(String elementConstant,
			String attributeConstant, String resource) {
		Element documentRoot = getDocumentRoot();
		NodeList elementsByTagName = documentRoot
				.getElementsByTagName(elementConstant);
		Element element = (Element) elementsByTagName.item(0);

		String[] binds = new String[] { resource, attributeConstant };

		report(NLS.bind(Messages.DSErrorReporter_cannotResolveResource, binds),
				getLine(element), CompilerFlags.WARNING,
				DSMarkerFactory.CAT_OTHER);
	}

	private void validateComponent(IDSComponent component) {
		if (component != null) {
			// Validate Required Attributes
			if (component.getAttributeName() == null) {
				Element element = (Element) getDocumentRoot()
						.getElementsByTagName(component.getXMLTagName())
						.item(0);
				reportMissingRequiredAttribute(element,
						IDSConstants.ATTRIBUTE_COMPONENT_NAME,
						CompilerFlags.ERROR);

			}
			// Validate Required Children
			if (component.getImplementation() == null) {
				report(NLS.bind(Messages.DSErrorReporter_requiredElement,
						IDSConstants.ELEMENT_IMPLEMENTATION),
						getLine(getDocumentRoot()), CompilerFlags.ERROR,
						DSMarkerFactory.CAT_OTHER);
			}

		}
	}

	private void validateService(IDSService service) {
		if (service != null) {
			
			
			
			validateProvide(service.getProvidedServices());
		}
	}

	private void validateProvide(IDSProvide[] providedServices) {
		for (int i = 0; i < providedServices.length; i++) {
			IDSProvide provide = providedServices[i];

			// Validate Required Attributes
			if (provide.getInterface() == null) {
				Element element = (Element) getDocumentRoot()
						.getElementsByTagName(provide.getXMLTagName()).item(i);
				reportMissingRequiredAttribute(element,
						IDSConstants.ATTRIBUTE_PROVIDE_INTERFACE,
						CompilerFlags.ERROR);
			}
		}
	}

}
