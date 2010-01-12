/*******************************************************************************
 * Copyright (c) 2008, 2009 Code 9 Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *     Rafael Oliveira Nóbrega <rafael.oliveira@gmail.com> - bug 230232, 249254
 *******************************************************************************/
package org.eclipse.pde.internal.ds.core.builders;

import java.lang.reflect.Constructor;
import java.util.Hashtable;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.text.Document;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
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
import org.osgi.framework.InvalidSyntaxException;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class DSErrorReporter extends XMLErrorReporter {
	public static final int ERROR = 0;
	public static final int WARNING = 1;
	public static final int IGNORE = 2;

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

			validateComponentElement(component);
			validateImplementationElement(component.getImplementation());
			validateServiceElement(component.getService());
			validatePropertyElements(component.getPropertyElements());
			validatePropertiesElements(component.getPropertiesElements());
			validateReferenceElements(component.getReferences());

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
		if (attr == null || attr.getValue() == null || attr.getName() == null)
			return;
		String message = NLS.bind(Messages.DSErrorReporter_attrValue, attr
				.getValue(), attr.getName());
		report(message, getLine(element, attr.getName()), ERROR,
				DSMarkerFactory.CAT_OTHER);
	}

	private void validateReferenceElements(IDSReference[] references) {
		Hashtable referencedNames = new Hashtable();
		for (int i = 0; i < references.length; i++) {
			IDSReference reference = references[i];
			Element element = (Element) getElements(reference).item(i);

			// Validate Required Attributes
			if (reference.getReferenceInterface() == null) {
				reportMissingRequiredAttribute(element,
						IDSConstants.ATTRIBUTE_REFERENCE_INTERFACE, ERROR);
			} else {
				// Validate Resource Existence
				validateJavaElement(reference.getReferenceInterface(),
						IDSConstants.ELEMENT_REFERENCE,
						IDSConstants.ATTRIBUTE_REFERENCE_INTERFACE, i);
			}

			// Validate Allowed Values
			validateReferenceCardinality(element);
			// Validate Allowed Values
			validateReferencePolicy(element);

			// Validate duplicated names
			validateReferenceElementNames(referencedNames, element);

			// Validate target
			validateTargetAttribute(element);

		}

	}

	private void validateTargetAttribute(Element element) {
		Attr attr = element
				.getAttributeNode(IDSConstants.ATTRIBUTE_REFERENCE_TARGET);
		if (attr != null) {
			String value = attr.getValue();
			try {
				Activator.getDefault().getBundle().getBundleContext()
						.createFilter(value);
			} catch (InvalidSyntaxException ise) {
				reportInvalidTarget(element, value);
			}
		}
	}

	private void reportInvalidTarget(Element element, String target) {
		String name = element
				.getAttribute(IDSConstants.ATTRIBUTE_REFERENCE_NAME);
		String message = NLS.bind(Messages.DSErrorReporter_invalidTarget, name,
				target);
		report(message, getLine(element), ERROR, DSMarkerFactory.CAT_OTHER);
	}

	private void validateReferenceElementNames(Hashtable referencedNames,
			Element element) {
		String name = element
				.getAttribute(IDSConstants.ATTRIBUTE_REFERENCE_NAME);
		if (name != null && referencedNames.containsKey(name)) {
			reportDuplicateReferenceElementName(element, name);
		} else {
			referencedNames.put(name, name);
		}
	}

	private void reportDuplicateReferenceElementName(Element element,
			String name) {
		Attr attr = element
				.getAttributeNode(IDSConstants.ATTRIBUTE_REFERENCE_NAME);
		if (attr == null || attr.getValue() == null || attr.getName() == null)
			return;
		String message = NLS.bind(
				Messages.DSErrorReporter_duplicateReferenceName, name);
		report(message,
				getLine(element, IDSConstants.ATTRIBUTE_REFERENCE_NAME), ERROR,
				DSMarkerFactory.CAT_OTHER);
	}

	private void validateReferencePolicy(Element element) {
		String attribute = element
				.getAttribute(IDSConstants.ATTRIBUTE_REFERENCE_POLICY);
		String allowedValues[] = new String[] {
				IDSConstants.VALUE_REFERENCE_POLICY_DYNAMIC,
				IDSConstants.VALUE_REFERENCE_POLICY_STATIC };

		if (attribute != null && attribute.length() > 0) {
			for (int i = 0; i < allowedValues.length; i++) {
				if (allowedValues[i].equalsIgnoreCase(attribute)) {
					return;
				}
			}
			reportIllegalPolicy(element, attribute);
		}

	}

	private void reportIllegalPolicy(Element element, String policy) {
		String name = element
				.getAttribute(IDSConstants.ATTRIBUTE_REFERENCE_NAME);
		if (name == null)
			name = element
					.getAttribute(IDSConstants.ATTRIBUTE_REFERENCE_INTERFACE);
		String message = NLS.bind(Messages.DSErrorReporter_invalidPolicyValue,
				name, policy);
		Attr attr = element
				.getAttributeNode(IDSConstants.ATTRIBUTE_REFERENCE_POLICY);
		report(message, getLine(element, attr.getName()), ERROR,
				DSMarkerFactory.CAT_OTHER);
	}

	private void validateReferenceCardinality(Element element) {
		String cardinality = element
				.getAttribute(IDSConstants.ATTRIBUTE_REFERENCE_CARDINALITY);
		String allowedValues[] = new String[] {
				IDSConstants.VALUE_REFERENCE_CARDINALITY_ONE_N,
				IDSConstants.VALUE_REFERENCE_CARDINALITY_ONE_ONE,
				IDSConstants.VALUE_REFERENCE_CARDINALITY_ZERO_N,
				IDSConstants.VALUE_REFERENCE_CARDINALITY_ZERO_ONE };

		if (cardinality != null) {
			for (int i = 0; i < allowedValues.length; i++) {
				if (allowedValues[i].equalsIgnoreCase(cardinality)) {
					return;
				}
			}
			reportIllegalCardinality(element, cardinality);
		}

	}

	private void reportIllegalCardinality(Element element, String cardinality) {
		String name = element
				.getAttribute(IDSConstants.ATTRIBUTE_REFERENCE_NAME);
		// if we don't have a name, use the interface
		if (name == null) {
			name = element
					.getAttribute(IDSConstants.ATTRIBUTE_REFERENCE_INTERFACE);
		}
		String message = NLS.bind(
				Messages.DSErrorReporter_invalidCardinalityValue, name,
				cardinality);
		Attr attr = element
				.getAttributeNode(IDSConstants.ATTRIBUTE_REFERENCE_CARDINALITY);
		if (attr == null || attr.getValue() == null || attr.getName() == null)
			return;
		report(message, getLine(element, attr.getName()), ERROR,
				DSMarkerFactory.CAT_OTHER);
	}

	private void validatePropertiesElements(IDSProperties[] propertiesElements) {
		for (int i = 0; i < propertiesElements.length; i++) {
			IDSProperties properties = propertiesElements[i];
			Element element = (Element) getElements(properties).item(i);

			// Validate Required Attributes
			if (properties.getEntry() == null
					|| properties.getEntry().length() == 0) {
				reportMissingRequiredAttribute(element,
						IDSConstants.ATTRIBUTE_PROPERTIES_ENTRY, ERROR);
			} else {
				if (!fProject.exists(new Path((properties.getEntry())))) {
					report(NLS
.bind(
							Messages.DSErrorReporter_cannotFindProperties,
							properties.getEntry()), getLine(element), WARNING,
							DSMarkerFactory.CAT_OTHER);
				}
			}

		}

	}

	private void validatePropertyElements(IDSProperty[] propertyElements) {
		for (int i = 0; i < propertyElements.length; i++) {
			IDSProperty property = propertyElements[i];
			Element element = (Element) getElements(property).item(i);

			// Validate Required Attributes
			String name = property.getName();
			if (name == null || name.length() == 0) {
				reportMissingRequiredAttribute(element,
						IDSConstants.ATTRIBUTE_PROPERTY_NAME, ERROR);
			}
			// Validate Allowed Values
			validatePropertyTypes(element);

			// Validate Value Attribute and Body Values
			validatePropertyAttrValueAndBody(element, property);

			// Validate Type Specific Values
			validatePropertyTypesValues(element, property);

		}
	}

	private void validatePropertyTypesValues(Element element,
			IDSProperty property) {
		String type = property.getPropertyType();
		String value = property.getPropertyValue();
		String body = property.getPropertyElemBody();
		if (value != null && value.length() > 0) {
			validatePropertySpecificTypeValue(type, value, element);
		} else {
			if (body != null && body.length() > 0) {
				validatePropertySpecificTypeBody(type, body, element);
			}
		}

	}

	private void validatePropertySpecificTypeBody(String type, String body,
			Element element) {
		StringTokenizer st = new StringTokenizer(body, "\n"); //$NON-NLS-1$
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			token = token.trim();
			if (token.length() > 0)
				validatePropertySpecificTypeValue(type, token, element);
		}
	}

	private void validatePropertySpecificTypeValue(String type, String value,
			Element element) {

		if (type == null) { // if null, we assume string
			type = IDSConstants.VALUE_PROPERTY_TYPE_STRING;
		}
		// Validate Double, Long, Float, Integer, Byte, Short and
		// String
		if (!type.equals(IDSConstants.VALUE_PROPERTY_TYPE_CHAR)
				&& !type.equals(IDSConstants.VALUE_PROPERTY_TYPE_BOOLEAN)) {
			try {
				Class forName = Class.forName("java.lang." + type); //$NON-NLS-1$
				Constructor[] constructors = forName.getConstructors();
				for (int i = 0; i < constructors.length; i++) {
					Constructor constructor = constructors[i];
					Class[] parameterTypes = constructor.getParameterTypes();
					if (parameterTypes.length == 1) {
						if (parameterTypes[0].equals(Class
								.forName("java.lang.String"))) { //$NON-NLS-1$
							constructor.newInstance(new Object[] { value });

						}
					}

				}

			} catch (Exception e) {
				reportPropertyTypeCastException(element, value, type);
			}
		} else {
			// Validate Booleans
			if (type.equals(IDSConstants.VALUE_PROPERTY_TYPE_BOOLEAN)) {
				if (!value.equals(IDSConstants.VALUE_FALSE)
						&& !value.equals(IDSConstants.VALUE_TRUE)) {
					reportPropertyTypeCastException(element, value, type);
				}
			} else {
				// Validate Chars
				if (type.equals(IDSConstants.VALUE_PROPERTY_TYPE_CHAR)) {
					if (value.length() > 1) {
						reportPropertyTypeCastException(element, value, type);
					}
				}
			}
		}
	}

	private void reportPropertyTypeCastException(Element element, String value,
			String type) {
		String message = NLS.bind(
				Messages.DSErrorReporter_propertyTypeCastException,
				new String[] { value, type });
		report(message, getLine(element), WARNING, DSMarkerFactory.CAT_OTHER);
	}

	/**
	 * Validates if a property elements defines a single value and multiple
	 * values at the same time.
	 * 
	 * @param element
	 * @param property
	 */
	private void validatePropertyAttrValueAndBody(Element element,
			IDSProperty property) {
		if (property.getPropertyValue() != null) {
			if (property.getPropertyElemBody() != null
					&& !property.getPropertyElemBody().equals("")) { //$NON-NLS-1$
				String propertyName = property.getPropertyName();
				reportSingleAndMultiplePropertyValues(element, propertyName,
						property.getPropertyValue());
			}
			String propertyType = property.getPropertyType();
			if (propertyType == null
					|| propertyType
							.equals(IDSConstants.VALUE_PROPERTY_TYPE_STRING))
				return; // It's OK for a property of type "String" to have a
			// value of "".
			if (property.getPropertyValue().equals("")) { //$NON-NLS-1$
				String propertyName = property.getPropertyName();
				reportEmptyPropertyValue(element, propertyName);
			}
		} else {
			if (property.getPropertyElemBody() == null
					|| property.getPropertyElemBody().equals("")) { //$NON-NLS-1$
				String propertyName = property.getPropertyName();
				reportEmptyPropertyValue(element, propertyName);
			}
		}
	}

	private void reportEmptyPropertyValue(Element element, String propertyName) {
		String message = NLS.bind(Messages.DSErrorReporter_emptyPropertyValue,
				propertyName);
		report(message, getLine(element), WARNING, DSMarkerFactory.CAT_OTHER);

	}

	private void reportSingleAndMultiplePropertyValues(Element element,
			String propertyName, String value) {
		String message = NLS.bind(
				Messages.DSErrorReporter_singleAndMultipleAttrValue,
				propertyName, value);
		report(message, getLine(element), WARNING, DSMarkerFactory.CAT_OTHER);
	}

	private void validatePropertyTypes(Element element) {
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

	private void validateImplementationElement(IDSImplementation implementation) {
		if (implementation != null) {
			String className = implementation.getClassName();
			Element element = (Element) getElements(implementation).item(0);

			if (className == null) {
				// Validate Required Attributes
				reportMissingRequiredAttribute(element,
						IDSConstants.ATTRIBUTE_IMPLEMENTATION_CLASS, ERROR);
			} else {
				// validate Resource Existence
				validateJavaElement(className,
						IDSConstants.ELEMENT_IMPLEMENTATION,
						IDSConstants.ATTRIBUTE_IMPLEMENTATION_CLASS, 0);

				// validate Class Default Constructor
				// validateClassDefaultConstructor(element, className);

			}
		}

	}

	// private void validateClassDefaultConstructor(Element element,
	// String className) {
	// try {
	// Class.forName(className);
	// } catch (ClassNotFoundException e) {
	// reportDefaultConstructorNotDefined(element, className);
	// }
	// }
	//
	// private void reportDefaultConstructorNotDefined(Element element,
	// String className) {
	// String message = NLS.bind(
	// Messages.DSErrorReporter_requiredDefaultConstructor,
	// (new String[] { className }));
	// report(message, getLine(element), WARNING, DSMarkerFactory.CAT_OTHER);
	// }

	private void validateJavaElement(String fullyQualifiedName,
			String elementName, String attrName, int index) {
		try {
			if (fProject.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject jp = JavaCore.create(fProject);
				if (!DSJavaHelper.isOnClasspath(fullyQualifiedName, (jp))) {
					reportJavaTypeNotFound(elementName, attrName,
							fullyQualifiedName, index);
				}
			}
		} catch (CoreException e) {
		}
	}

	private void reportMissingRequiredAttribute(Element element,
			String attName, int severity) {
		String message = NLS.bind(Messages.DSErrorReporter_requiredAttribute,
				attName, element.getNodeName());
		report(message, getLine(element), severity, DSMarkerFactory.CAT_OTHER);
	}

	/**
	 * 
	 * @param elementConstant
	 *            element name
	 * @param attributeConstant
	 *            attribute name
	 * @param resource
	 *            resource qualified name
	 * @param index
	 *            used to select an element among many from the same type
	 */
	private void reportJavaTypeNotFound(String elementConstant,
			String attributeConstant, String resource, int index) {
		Element documentRoot = getDocumentRoot();
		NodeList elementsByTagName = documentRoot
				.getElementsByTagName(elementConstant);
		Element element = (Element) elementsByTagName.item(index);
		report(NLS.bind(Messages.DSErrorReporter_cannotFindJavaType, resource,
				attributeConstant), getLine(element), WARNING,
				DSMarkerFactory.CAT_OTHER);
	}

	private void validateComponentElement(IDSComponent component) {
		if (component != null) {
			Element element = getDocumentRoot();
			// Validate Required Children
			if (component.getImplementation() == null) {
				report(NLS.bind(Messages.DSErrorReporter_requiredElement,
						IDSConstants.ELEMENT_IMPLEMENTATION),
						getLine(getDocumentRoot()), ERROR,
						DSMarkerFactory.CAT_OTHER);
			}

			// validate boolean values
			validateBoolean(
					element,
					element
							.getAttributeNode(IDSConstants.ATTRIBUTE_COMPONENT_IMMEDIATE));
			validateBoolean(element, element
					.getAttributeNode(IDSConstants.ATTRIBUTE_COMPONENT_ENABLED));

			// validate non-empty values
			validateEmpty(element, element
					.getAttributeNode(IDSConstants.ATTRIBUTE_COMPONENT_FACTORY));

			validateEmpty(element, element
					.getAttributeNode(IDSConstants.ATTRIBUTE_COMPONENT_NAME));

			// validate immediate values
			validateImmediateAttribute(element, component);

			validateConfigurationPolicyAttribute(element, component);

		}
	}

	private void validateConfigurationPolicyAttribute(Element element,
			IDSComponent component) {

		String modified = component.getModifiedMethod();
		String policy = component.getConfigurationPolicy();
		if (policy != null && policy.length() > 0) {
			if (policy
					.equalsIgnoreCase(IDSConstants.VALUE_CONFIGURATION_POLICY_IGNORE)) {
				if (modified != null && modified.length() > 0) {
					report(
							Messages.DSErrorReporter_invalidConfigurationPolicyValue,
							getLine(element), WARNING,
							DSMarkerFactory.CAT_OTHER);
				}
			}
		}
	}

	private void validateImmediateAttribute(Element element,
			IDSComponent component) {
		boolean isService = false;
		boolean isFactory = component.getFactory() != null;
		boolean isImmediate = component.getImmediate();

		if (component.getService() != null) {
			IDSProvide[] providedServices = component.getService()
					.getProvidedServices();
			if (providedServices != null && providedServices.length > 0) {
				isService = true;
			}
		}
		if (!isService && !isFactory && !isImmediate
				&& component
						.getXMLAttributeValue(IDSConstants.ATTRIBUTE_COMPONENT_IMMEDIATE) != null) {
			reportInvalidImmediate(element);
		}

		if (isFactory && isImmediate) {
			reportInvalidImmediateFactory(element);
		}

	}

	private void reportInvalidImmediateFactory(Element element) {
		report(Messages.DSErrorReporter_invalidImmediateValueFactory,
				getLine(element), WARNING, DSMarkerFactory.CAT_OTHER);

	}

	private void reportInvalidImmediate(Element element) {
		report(Messages.DSErrorReporter_invalidImmediateValue,
				getLine(element), WARNING, DSMarkerFactory.CAT_OTHER);
	}

	private void validateEmpty(Element element, Attr attr) {
		if (attr != null) {
			String value = attr.getValue();
			if (value.equalsIgnoreCase("")) //$NON-NLS-1$
				reportIllegalEmptyAttributeValue(element, attr);
		}
	}

	private void reportIllegalEmptyAttributeValue(Element element, Attr attr) {
		if (attr == null || attr.getValue() == null || attr.getName() == null)
			return;
		String message = NLS.bind(Messages.DSErrorReporter_emptyAttrValue, attr
				.getName());
		report(message, getLine(element, attr.getName()), ERROR,
				DSMarkerFactory.CAT_OTHER);
	}

	private void validateServiceElement(IDSService service) {
		if (service != null) {
			Element element = (Element) getElements(service).item(0);

			validateBoolean(element, element
					.getAttributeNode(IDSConstants.ATTRIBUTE_SERVICE_FACTORY));

			validateServiceFactory(element, service);

			IDSProvide[] providedServices = service.getProvidedServices();
			if (providedServices.length == 0) {
				reportEmptyService(element);
			} else {
				validateProvideElement(providedServices);
			}
		}
	}

	private void reportEmptyService(Element element) {
		report(Messages.DSErrorReporter_illegalEmptyService, getLine(element),
				ERROR, DSMarkerFactory.CAT_OTHER);
	}

	private void validateServiceFactory(Element element, IDSService service) {
		IDSComponent component = service.getComponent();
		boolean isFactory = component.getFactory() != null;
		boolean isImmediate = component.getImmediate();

		if (isFactory) {
			if (service.getServiceFactory()) {
				reportIllegalServiceFactory(element);
			}
		}

		if (isImmediate) {
			if (service.getServiceFactory()) {
				reportIllegalServiceFactory_Immediate(element);
			}
		}
	}

	private void reportIllegalServiceFactory_Immediate(Element element) {
		report(Messages.DSErrorReporter_illegalServiceFactory_Immediate,
				getLine(element), ERROR, DSMarkerFactory.CAT_OTHER);


	}

	private void reportIllegalServiceFactory(Element element) {
		report(Messages.DSErrorReporter_illegalServiceFactory,
				getLine(element), ERROR, DSMarkerFactory.CAT_OTHER);
	}

	private void validateProvideElement(IDSProvide[] providedServices) {
		Hashtable providedInterfaces = new Hashtable();

		for (int i = 0; i < providedServices.length; i++) {
			IDSProvide provide = providedServices[i];

			Element element = (Element) getElements(provide).item(i);

			// Validate Required Attributes
			if (provide.getInterface() == null) {
				reportMissingRequiredAttribute(element,
						IDSConstants.ATTRIBUTE_PROVIDE_INTERFACE, ERROR);
			} else {
				validateJavaElement(provide.getInterface(),
						IDSConstants.ELEMENT_PROVIDE,
						IDSConstants.ATTRIBUTE_PROVIDE_INTERFACE, i);

				// validate if implementation class implements services
				// interfaces
				// validateClassInstanceofProvidedInterface(element, provide);

				// validate duplicate interfaces
				validateDuplicateInterface(providedInterfaces, provide, element);
			}
		}
	}

	private void validateDuplicateInterface(Hashtable providedInterfaces,
			IDSProvide provide, Element element) {
		String interface1 = provide.getInterface();
		if (providedInterfaces.get(interface1) != null) {
			String message = NLS.bind(
					Messages.DSErrorReporter_duplicatedInterface, interface1);
			report(message, getLine(element), WARNING,
					DSMarkerFactory.CAT_OTHER);
		} else {
			providedInterfaces.put(interface1, interface1);
		}
	}

	// TODO this is a hack
	private NodeList getElements(IDocumentElementNode node) {
		String name = node.getXMLTagName();
		String prefix = node.getNamespacePrefix();
		if (prefix != null && prefix.length() > 0) {
			name = prefix + ":" + name; //$NON-NLS-1$
		}
		return getDocumentRoot().getElementsByTagName(name);
	}
}
