/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rafael Oliveira Nobrega <rafael.oliveira@gmail.com> - bug 223738
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ds.core.text;
import java.util.ArrayList;

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

	public boolean isRoot() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSObject#getType()
	 */
	public int getType() {
		return TYPE_COMPONENT;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSObject#canBeParent()
	 */
	public boolean canBeParent() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSObject#getName()
	 */
	public String getName() {
		return this.getAttributeName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.DSObject#canAddChild(int)
	 */
	public boolean canAddChild(int objectType) {
		return objectType == TYPE_IMPLEMENTATION
				|| objectType == TYPE_PROPERTIES || objectType == TYPE_PROPERTY
				|| objectType == TYPE_SERVICE || objectType == TYPE_REFERENCE;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSComponent#setAttributeName(java.lang.String)
	 */
	public void setAttributeName(String name) {
		setXMLAttribute(ATTRIBUTE_COMPONENT_NAME, name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSComponent#getAttributeName()
	 */
	public String getAttributeName() {
		return getXMLAttributeValue(ATTRIBUTE_COMPONENT_NAME);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSComponent#setEnabled(boolean)
	 */
	public void setEnabled(boolean bool) {
		setBooleanAttributeValue(ATTRIBUTE_COMPONENT_ENABLED, bool);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSRoot#getEnabled()
	 */
	public boolean getEnabled() {
		return getBooleanAttributeValue(ATTRIBUTE_COMPONENT_ENABLED, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSRoot#setFactory(java.lang.String)
	 */
	public void setFactory(String factory) {
		setXMLAttribute(ATTRIBUTE_COMPONENT_FACTORY, factory);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSComponent#getFactory()
	 */
	public String getFactory() {
		return getXMLAttributeValue(ATTRIBUTE_COMPONENT_FACTORY);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSComponent#setImmediate(boolean)
	 */
	public void setImmediate(boolean bool) {
		setBooleanAttributeValue(ATTRIBUTE_COMPONENT_IMMEDIATE, bool);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSRoot#getImmediate()
	 */
	public boolean getImmediate() {
		return getBooleanAttributeValue(ATTRIBUTE_COMPONENT_IMMEDIATE, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSRoot#getImplemention()
	 */
	public IDSImplementation getImplementation() {
		ArrayList childNodesList = getChildNodesList(IDSImplementation.class,
				true);
		if (childNodesList.size() == 0) {
			return null;
		}
		return (IDSImplementation) childNodesList.get(0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSRoot#getPropertiesElements()
	 */
	public IDSProperties[] getPropertiesElements() {
		ArrayList childNodesList = getChildNodesList(IDSProperties.class, true);
		IDSProperties[] array = new IDSProperties[childNodesList.size()];
		for (int i = 0; i < childNodesList.size(); i++) {
			array[i] = (IDSProperties) childNodesList.get(i);
		}
		return array;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSRoot#getPropertyElements()
	 */
	public IDSProperty[] getPropertyElements() {
		ArrayList childNodesList = getChildNodesList(IDSProperty.class, true);
		IDSProperty[] array = new IDSProperty[childNodesList.size()];
		for (int i = 0; i < childNodesList.size(); i++) {
			array[i] = (IDSProperty) childNodesList.get(i);
		}
		return array;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSRoot#getReferences()
	 */
	public IDSReference[] getReferences() {
		ArrayList childNodesList = getChildNodesList(IDSReference.class, true);
		IDSReference[] array = new IDSReference[childNodesList.size()];
		for (int i = 0; i < childNodesList.size(); i++) {
			array[i] = (IDSReference) childNodesList.get(i);
		}
		return array;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSRoot#getService()
	 */
	public IDSService getService() {
		ArrayList childNodesList = getChildNodesList(IDSService.class, true);
		if (childNodesList.size() == 0) {
			return null;
		}
		return (IDSService) childNodesList.get(0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSRoot#addPropertiesElement(org.eclipse.pde.internal.ds.core.IDSProperties)
	 */
	public void addPropertiesElement(IDSProperties properties) {
		this.addChildNode(properties, true);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSRoot#addPropertyElement(org.eclipse.pde.internal.ds.core.IDSProperty)
	 */
	public void addPropertyElement(IDSProperty property) {
		this.addChildNode(property, true);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSRoot#addReference(org.eclipse.pde.internal.ds.core.IDSReference)
	 */
	public void addReference(IDSReference reference) {
		this.addChildNode(reference, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSRoot#removePropertiesElement(org.eclipse.pde.internal.ds.core.IDSProperties)
	 */
	public void removePropertiesElement(IDSProperties properties) {
		this.removeChildNode(properties, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSComponent#removePropertyElement(org.eclipse.pde.internal.ds.core.IDSProperty)
	 */
	public void removePropertyElement(IDSProperty property) {
		this.removeChildNode(property, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSComponent#removeReference(org.eclipse.pde.internal.ds.core.IDSReference)
	 */
	public void removeReference(IDSReference reference) {
		this.removeChildNode(reference, true);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSComponent#setImplementation(org.eclipse.pde.internal.ds.core.IDSImplementation)
	 */
	public void setImplementation(IDSImplementation implementation) {
		if (this.getImplementation() == null) {
			this.addChildNode(implementation, true);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSComponent#setService(org.eclipse.pde.internal.ds.core.IDSService)
	 */
	public void setService(IDSService service) {
		if (this.getService() == null) {
			this.addChildNode(service, true);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSComponent#removeService(org.eclipse.pde.internal.ds.core.IDSService)
	 */
	public void removeService(IDSService service) {
		this.removeChildNode(service, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.IDSObject#getAttributesNames()
	 */
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

	public String getConfigurationPolicy() {
		return getXMLAttributeValue(ATTRIBUTE_COMPONENT_CONFIGURATION_POLICY);
	}

	public void setConfigurationPolicy(String policy) {
		setXMLAttribute(ATTRIBUTE_COMPONENT_CONFIGURATION_POLICY, policy);
	}

	public String getActivateMethod() {
		return getXMLAttributeValue(ATTRIBUTE_COMPONENT_ACTIVATE);
	}

	public String getDeactivateMethod() {
		return getXMLAttributeValue(ATTRIBUTE_COMPONENT_DEACTIVATE);
	}

	public void setActivateMethod(String name) {
		setXMLAttribute(ATTRIBUTE_COMPONENT_ACTIVATE, name);
	}

	public void setDeactivateMethod(String name) {
		setXMLAttribute(ATTRIBUTE_COMPONENT_DEACTIVATE, name);
	}

	public String getModifiedMethod() {
		return getXMLAttributeValue(ATTRIBUTE_COMPONENT_MODIFIED);
	}

	public void setModifiedeMethod(String name) {
		setXMLAttribute(ATTRIBUTE_COMPONENT_MODIFIED, name);
	}

}
