/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rafael Oliveira NÃ³brega <rafael.oliveira@gmail.com> - bug 223738
 *******************************************************************************/
package org.eclipse.pde.internal.ds.core.text;

import java.util.ArrayList;

import org.eclipse.pde.internal.ds.core.IDSComponent;
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
		super(model, ELEMENT_ROOT);
		setInTheModel(true);

		// set default values
		this.setEnabled(true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.core.text.plugin.PluginDocumentNode#isRoot()
	 */
	public boolean isRoot() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSObject#getType()
	 */
	public int getType() {
		return TYPE_ROOT;
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
		setBooleanAttributeValue(ATTRIBUTE_ENABLED, bool);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSRoot#getEnabled()
	 */
	public boolean getEnabled() {
		return getBooleanAttributeValue(ATTRIBUTE_ENABLED, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSRoot#setFactory(java.lang.String)
	 */
	public void setFactory(String factory) {
		setXMLAttribute(ATTRIBUTE_FACTORY, factory);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSComponent#getFactory()
	 */
	public String getFactory() {
		return getXMLAttributeValue(ATTRIBUTE_FACTORY);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSComponent#setImmediate(boolean)
	 */
	public void setImmediate(boolean bool) {
		setBooleanAttributeValue(ATTRIBUTE_IMMEDIATE, bool);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSRoot#getImmediate()
	 */
	public boolean getImmediate() {
		return getBooleanAttributeValue(ATTRIBUTE_IMMEDIATE, false);
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

}
