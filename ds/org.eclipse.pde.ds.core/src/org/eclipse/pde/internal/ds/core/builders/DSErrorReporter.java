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
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
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
			validateImplementation(component.getImplementation());
			validateService(component.getService());
			validatePropertyElements(component.getPropertyElements());
			validateProperties(component.getPropertiesElements());
			validateReferences(component.getReferences());

		} catch (CoreException e) {
			Activator.log(e);
		}

	}

	private void validateBoolean(Element element, Attr attr) {
		if (attr != null) {
			String value = attr.getValue();
			if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) //$NON-NLS-1$ //$NON-NLS-2$
				reportIllegalAttributeValue(element, attr);
		}
	}

	private void reportIllegalAttributeValue(Element element, Attr attr) {
		String message = NLS.bind(Messages.DSErrorReporter_attrValue,
				(new String[] { attr.getValue(), attr.getName() }));
		report(message, getLine(element, attr.getName()), CompilerFlags.ERROR,
				DSMarkerFactory.CAT_OTHER);
	}

	public void validateReferences(IDSReference[] references) {
		for (int i = 0; i < references.length; i++) {
			IDSReference reference = references[i];
			Element element = (Element) getDocumentRoot().getElementsByTagName(
					reference.getXMLTagName()).item(i);
			// Validate Required Attributes
			if (reference.getName() == null) {
				reportMissingRequiredAttribute(element,
						IDSConstants.ATTRIBUTE_REFERENCE_NAME,
						CompilerFlags.ERROR);
			}

			// Validate Required Attributes
			if (reference.getReferenceInterface() == null) {
				reportMissingRequiredAttribute(element,
						IDSConstants.ATTRIBUTE_REFERENCE_INTERFACE,
						CompilerFlags.ERROR);
			}else{
				// Validate Resource Existence
				validateResourceExistence(reference.getReferenceInterface(),
						IDSConstants.ELEMENT_REFERENCE,
						IDSConstants.ATTRIBUTE_REFERENCE_INTERFACE, i);
			}

			// Validate Allowed Values
			validateReferenceCardinality(element);
			// Validate Allowed Values
			validateReferencePolicy(element);

		}

	}

	private void validateReferencePolicy(Element element) {
		String attribute = element
				.getAttribute(IDSConstants.ATTRIBUTE_REFERENCE_POLICY);
		String allowedValues[] = new String[] {
				IDSConstants.VALUE_REFERENCE_POLICY_DYNAMIC,
				IDSConstants.VALUE_REFERENCE_POLICY_STATIC };

		if (attribute != null) {
			for (int i = 0; i < allowedValues.length; i++) {
				if (allowedValues[i].equalsIgnoreCase(attribute)) {
					return;
				}
			}
			reportIllegalAttributeValue(element, element
					.getAttributeNode(IDSConstants.ATTRIBUTE_REFERENCE_POLICY));
		}

	}

	private void validateReferenceCardinality(Element element) {
		String attribute = element
				.getAttribute(IDSConstants.ATTRIBUTE_REFERENCE_CARDINALITY);
		String allowedValues[] = new String[] {
				IDSConstants.VALUE_REFERENCE_CARDINALITY_ONE_N,
				IDSConstants.VALUE_REFERENCE_CARDINALITY_ONE_ONE,
				IDSConstants.VALUE_REFERENCE_CARDINALITY_ZERO_N,
				IDSConstants.VALUE_REFERENCE_CARDINALITY_ZERO_ONE };

		if (attribute != null) {
			for (int i = 0; i < allowedValues.length; i++) {
				if (allowedValues[i].equalsIgnoreCase(attribute)) {
					return;
				}
			}
			reportIllegalAttributeValue(
					element,
					element
							.getAttributeNode(IDSConstants.ATTRIBUTE_REFERENCE_CARDINALITY));
		}

	}

	public void validateProperties(IDSProperties[] propertiesElements) {
		for (int i = 0; i < propertiesElements.length; i++) {
			IDSProperties properties = propertiesElements[i];
			Element element = (Element) getDocumentRoot().getElementsByTagName(
					properties.getXMLTagName()).item(i);
			
			// Validate Required Attributes
			if (properties.getEntry() == null) {
				reportMissingRequiredAttribute(element,
						IDSConstants.ATTRIBUTE_PROPERTIES_ENTRY,
						CompilerFlags.ERROR);
			}

		}

	}

	public void validatePropertyElements(IDSProperty[] propertyElements) {
		for (int i = 0; i < propertyElements.length; i++) {
			IDSProperty property = propertyElements[i];
			Element element = (Element) getDocumentRoot().getElementsByTagName(
					property.getXMLTagName()).item(i);

			// Validate Required Attributes
			if (property.getName() == null) {
				reportMissingRequiredAttribute(element,
						IDSConstants.ATTRIBUTE_PROPERTY_NAME,
						CompilerFlags.ERROR);
			}
			// Validate Allowed Values
			validatePropertyTypeValues(element);

		}
	}

	private void validatePropertyTypeValues(Element element) {
		String attribute = element
				.getAttribute(IDSConstants.ATTRIBUTE_PROPERTY_TYPE);
		String allowedValues[] = new String[] {
				IDSConstants.VALUE_PROPERTY_TYPE_BOOLEAN,
				IDSConstants.VALUE_PROPERTY_TYPE_BYTE,
				IDSConstants.VALUE_PROPERTY_TYPE_CHAR,
				IDSConstants.VALUE_PROPERTY_TYPE_DOUBLE,
				IDSConstants.VALUE_PROPERTY_TYPE_FLOAT,
				IDSConstants.VALUE_PROPERTY_TYPE_INTEGER,
				IDSConstants.VALUE_PROPERTY_TYPE_LONG,
				IDSConstants.VALUE_PROPERTY_TYPE_SHORT,
				IDSConstants.VALUE_PROPERTY_TYPE_STRING };

		if (attribute != null) {
			for (int i = 0; i < allowedValues.length; i++) {
				if (allowedValues[i].equalsIgnoreCase(attribute)) {
					return;
				}
			}
			reportIllegalAttributeValue(element, element
					.getAttributeNode(IDSConstants.ATTRIBUTE_PROPERTY_TYPE));
		}

	}

	public void validateImplementation(IDSImplementation implementation) {
		if (implementation != null) {
			String className = implementation.getClassName();
			Element element = (Element) getDocumentRoot().getElementsByTagName(
					implementation.getXMLTagName()).item(0);
			
			if (className == null) {
				// Validate Required Attributes
				reportMissingRequiredAttribute(element,
						IDSConstants.ATTRIBUTE_IMPLEMENTATION_CLASS,
						CompilerFlags.ERROR);
			} else {
				// validate Resource Existence
				validateResourceExistence(className,
						IDSConstants.ELEMENT_IMPLEMENTATION,
						IDSConstants.ATTRIBUTE_IMPLEMENTATION_CLASS, 0);
			}
		}

	}

	private void validateResourceExistence(String fullyQualifiedName,
			String elementName, String attrName, int index) {
		try {
			if (fProject.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject jp = JavaCore.create(fProject);
				if (!DSJavaHelper.isOnClasspath(fullyQualifiedName, (jp))) {
					reportResourceNotFound(elementName, attrName,
							fullyQualifiedName, index);
				}
			}
		} catch (CoreException e) {
		}
	}

	private void reportMissingRequiredAttribute(Element element,
			String attName, int severity) {
		String message = NLS.bind(Messages.DSErrorReporter_requiredAttribute,
				(new String[] { attName, element.getNodeName() }));			
		report(message, getLine(element), severity, DSMarkerFactory.CAT_OTHER);
	}

	private void reportResourceNotFound(String elementConstant,
			String attributeConstant, String resource, int index) {
		Element documentRoot = getDocumentRoot();
		NodeList elementsByTagName = documentRoot
				.getElementsByTagName(elementConstant);
		Element element = (Element) elementsByTagName.item(index);

		String[] binds = new String[] { resource, attributeConstant };

		report(NLS.bind(Messages.DSErrorReporter_cannotResolveResource, binds),
				getLine(element), CompilerFlags.WARNING,
				DSMarkerFactory.CAT_OTHER);
	}

	public void validateComponent(IDSComponent component) {
		if (component != null) {
			Element element = getDocumentRoot();
			// Validate Required Attributes
			if (component.getAttributeName() == null) {
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

			// validate boolean values
			validateBoolean(
					element,
					element
							.getAttributeNode(IDSConstants.ATTRIBUTE_COMPONENT_IMMEDIATE));
			validateBoolean(element, element
					.getAttributeNode(IDSConstants.ATTRIBUTE_COMPONENT_ENABLED));

		}
	}

	public void validateService(IDSService service) {
		if (service != null) {
			Element element = (Element) getDocumentRoot().getElementsByTagName(
					service.getXMLTagName()).item(0);

			validateBoolean(element, element
					.getAttributeNode(IDSConstants.ATTRIBUTE_SERVICE_FACTORY));

			validateProvide(service.getProvidedServices());
		}
	}

	public void validateProvide(IDSProvide[] providedServices) {
		for (int i = 0; i < providedServices.length; i++) {
			IDSProvide provide = providedServices[i];

			Element element = (Element) getDocumentRoot().getElementsByTagName(
					provide.getXMLTagName()).item(i);
			
			// Validate Required Attributes
			if (provide.getInterface() == null) {
				reportMissingRequiredAttribute(element,
						IDSConstants.ATTRIBUTE_PROVIDE_INTERFACE,
						CompilerFlags.ERROR);
			} else {
				validateResourceExistence(provide.getInterface(),
						IDSConstants.ELEMENT_PROVIDE,
						IDSConstants.ATTRIBUTE_PROVIDE_INTERFACE, i);
			}
		}
	}

}
