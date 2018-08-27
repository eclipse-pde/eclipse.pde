/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rafael Oliveira Nobrega <rafael.oliveira@gmail.com> - bug 223738
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ds.core.text;
import java.util.ArrayList;

import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.ds.core.IDSComponent;
import org.eclipse.pde.internal.ds.core.IDSConstants;
import org.eclipse.pde.internal.ds.core.IDSImplementation;
import org.eclipse.pde.internal.ds.core.IDSProperties;
import org.eclipse.pde.internal.ds.core.IDSProperty;
import org.eclipse.pde.internal.ds.core.IDSReference;
import org.eclipse.pde.internal.ds.core.IDSService;


/**
 * Represents the root "component" entry in a DS xml file. There may be only one
 * root node in the file and all other nodes must be inside the root. The
 * structure of component XML grammar is:
 *
 * <component> ::= <implementation> <properties> * <service> ? <reference> *
 *
 * @since 3.4
 * @see DSObject
 * @see DSModel
 * @see DSDocumentFactory
 */
public class DSComponent extends DSObject implements IDSComponent {

	private static final long serialVersionUID = 1L;

	public DSComponent(DSModel model) {
		super(model, ELEMENT_COMPONENT);
		setAttributeName(IDSConstants.ELEMENT_COMPONENT);
		setNamespace(IDSConstants.NAMESPACE); // hard code namespace to be 1.1
		setNamespacePrefix("scr"); //$NON-NLS-1$
		setInTheModel(true);
	}

	@Override
	public boolean isRoot() {
		return true;
	}

	@Override
	public int getType() {
		return TYPE_COMPONENT;
	}

	@Override
	public boolean canBeParent() {
		return true;
	}

	@Override
	public String getName() {
		return this.getAttributeName();
	}

	@Override
	public boolean canAddChild(int objectType) {
		return objectType == TYPE_IMPLEMENTATION
				|| objectType == TYPE_PROPERTIES || objectType == TYPE_PROPERTY
				|| objectType == TYPE_SERVICE || objectType == TYPE_REFERENCE;
	}

	@Override
	public void setAttributeName(String name) {
		setXMLAttribute(ATTRIBUTE_COMPONENT_NAME, name);
	}

	@Override
	public String getAttributeName() {
		return getXMLAttributeValue(ATTRIBUTE_COMPONENT_NAME);
	}

	@Override
	public void setEnabled(boolean bool) {
		setBooleanAttributeValue(ATTRIBUTE_COMPONENT_ENABLED, bool);
	}

	@Override
	public boolean getEnabled() {
		return getBooleanAttributeValue(ATTRIBUTE_COMPONENT_ENABLED, true);
	}

	@Override
	public void setFactory(String factory) {
		setXMLAttribute(ATTRIBUTE_COMPONENT_FACTORY, factory);
	}

	@Override
	public String getFactory() {
		return getXMLAttributeValue(ATTRIBUTE_COMPONENT_FACTORY);
	}

	@Override
	public void setImmediate(boolean bool) {
		setBooleanAttributeValue(ATTRIBUTE_COMPONENT_IMMEDIATE, bool);
	}

	@Override
	public boolean getImmediate() {
		return getBooleanAttributeValue(ATTRIBUTE_COMPONENT_IMMEDIATE, false);
	}

	@Override
	public IDSImplementation getImplementation() {
		ArrayList<IDocumentElementNode> childNodesList = getChildNodesList(IDSImplementation.class, true);
		if (childNodesList.isEmpty()) {
			return null;
		}
		return (IDSImplementation) childNodesList.get(0);
	}

	@Override
	public IDSProperties[] getPropertiesElements() {
		ArrayList<IDocumentElementNode> childNodesList = getChildNodesList(IDSProperties.class, true);
		IDSProperties[] array = new IDSProperties[childNodesList.size()];
		for (int i = 0; i < childNodesList.size(); i++) {
			array[i] = (IDSProperties) childNodesList.get(i);
		}
		return array;
	}

	@Override
	public IDSProperty[] getPropertyElements() {
		ArrayList<IDocumentElementNode> childNodesList = getChildNodesList(IDSProperty.class, true);
		IDSProperty[] array = new IDSProperty[childNodesList.size()];
		for (int i = 0; i < childNodesList.size(); i++) {
			array[i] = (IDSProperty) childNodesList.get(i);
		}
		return array;
	}

	@Override
	public IDSReference[] getReferences() {
		ArrayList<IDocumentElementNode> childNodesList = getChildNodesList(IDSReference.class, true);
		IDSReference[] array = new IDSReference[childNodesList.size()];
		for (int i = 0; i < childNodesList.size(); i++) {
			array[i] = (IDSReference) childNodesList.get(i);
		}
		return array;
	}

	@Override
	public IDSService getService() {
		ArrayList<IDocumentElementNode> childNodesList = getChildNodesList(IDSService.class, true);
		if (childNodesList.isEmpty()) {
			return null;
		}
		return (IDSService) childNodesList.get(0);
	}

	@Override
	public void addPropertiesElement(IDSProperties properties) {
		this.addChildNode(properties, true);

	}

	@Override
	public void addPropertyElement(IDSProperty property) {
		this.addChildNode(property, true);

	}

	@Override
	public void addReference(IDSReference reference) {
		this.addChildNode(reference, true);
	}

	@Override
	public void removePropertiesElement(IDSProperties properties) {
		this.removeChildNode(properties, true);
	}

	@Override
	public void removePropertyElement(IDSProperty property) {
		this.removeChildNode(property, true);
	}

	@Override
	public void removeReference(IDSReference reference) {
		this.removeChildNode(reference, true);

	}

	@Override
	public void setImplementation(IDSImplementation implementation) {
		if (this.getImplementation() == null) {
			this.addChildNode(implementation, true);
		}
	}

	@Override
	public void setService(IDSService service) {
		if (this.getService() == null) {
			this.addChildNode(service, true);
		}
	}

	@Override
	public void removeService(IDSService service) {
		this.removeChildNode(service, true);
	}

	@Override
	public String[] getAttributesNames() {
		return new String[] { IDSConstants.ATTRIBUTE_COMPONENT_ENABLED,
				IDSConstants.ATTRIBUTE_COMPONENT_FACTORY,
				IDSConstants.ATTRIBUTE_COMPONENT_IMMEDIATE,
				IDSConstants.ATTRIBUTE_COMPONENT_NAME,
				IDSConstants.ATTRIBUTE_COMPONENT_CONFIGURATION_POLICY,
				IDSConstants.ATTRIBUTE_COMPONENT_ACTIVATE,
				IDSConstants.ATTRIBUTE_COMPONENT_DEACTIVATE,
				IDSConstants.ATTRIBUTE_COMPONENT_MODIFIED };
	}

	@Override
	public String getConfigurationPolicy() {
		return getXMLAttributeValue(ATTRIBUTE_COMPONENT_CONFIGURATION_POLICY);
	}

	@Override
	public void setConfigurationPolicy(String policy) {
		setXMLAttribute(ATTRIBUTE_COMPONENT_CONFIGURATION_POLICY, policy);
	}

	@Override
	public String getActivateMethod() {
		return getXMLAttributeValue(ATTRIBUTE_COMPONENT_ACTIVATE);
	}

	@Override
	public String getDeactivateMethod() {
		return getXMLAttributeValue(ATTRIBUTE_COMPONENT_DEACTIVATE);
	}

	@Override
	public void setActivateMethod(String name) {
		setXMLAttribute(ATTRIBUTE_COMPONENT_ACTIVATE, name);
	}

	@Override
	public void setDeactivateMethod(String name) {
		setXMLAttribute(ATTRIBUTE_COMPONENT_DEACTIVATE, name);
	}

	@Override
	public String getModifiedMethod() {
		return getXMLAttributeValue(ATTRIBUTE_COMPONENT_MODIFIED);
	}

	@Override
	public void setModifiedeMethod(String name) {
		setXMLAttribute(ATTRIBUTE_COMPONENT_MODIFIED, name);
	}

}
