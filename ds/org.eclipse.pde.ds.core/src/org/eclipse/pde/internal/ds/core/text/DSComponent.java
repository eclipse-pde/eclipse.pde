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

import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.ds.core.IDSComponent;
import org.eclipse.pde.internal.ds.core.IDSImplementation;
import org.eclipse.pde.internal.ds.core.IDSObject;
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
	 * @see org.eclipse.pde.internal.core.toc.TocObject#getType()
	 */
	public int getType() {
		return TYPE_ROOT;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.core.text.ctxhelp.CtxHelpObject#canBeParent()
	 */
	public boolean canBeParent() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.core.text.ctxhelp.CtxHelpObject#getName()
	 */
	public String getName() {
		return this.getAttributeName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.core.text.ctxhelp.CtxHelpObject#canAddChild(org.eclipse.pde.internal.core.text.ctxhelp.CtxHelpObject)
	 */
	public boolean canAddChild(int objectType) {
		return objectType == TYPE_IMPLEMENTATION
				|| objectType == TYPE_PROPERTIES || objectType == TYPE_PROPERTY
				|| objectType == TYPE_SERVICE || objectType == TYPE_REFERENCE;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.core.text.ctxhelp.CtxHelpObject#canAddSibling(int)
	 */
	public boolean canAddSibling(int objectType) {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSRoot#setAttributeName(java.lang.String)
	 */
	public void setAttributeName(String name) {
		setXMLAttribute(ATTRIBUTE_COMPONENT_NAME, name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSRoot#getAttributeName()
	 */
	public String getAttributeName() {
		return getXMLAttributeValue(ATTRIBUTE_COMPONENT_NAME);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSRoot#setEnabled(boolean)
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
	 * @see org.eclipse.pde.internal.ds.core.text.IDSRoot#getFactory()
	 */
	public String getFactory() {
		return getXMLAttributeValue(ATTRIBUTE_FACTORY);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSRoot#setImmediate(boolean)
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

	public void addService(IDSService service) {
		addChildNode((IDocumentElementNode) service, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#removeSubItem(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItemObject)
	 */
	public void removeChild(IDSObject subitem) {
		removeChildNode((IDocumentElementNode) subitem, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#moveItem(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem,
	 *      int)
	 */
	public void moveItem(IDSObject item, int newRelativeIndex) {
		moveChildNode((IDocumentElementNode) item, newRelativeIndex, true);
	}

	public IDSImplementation[] getImplementations() {
		ArrayList childNodesList = getChildNodesList(IDSImplementation.class,
				true);
		IDSImplementation[] array = new IDSImplementation[childNodesList.size()];
		for (int i = 0; i < childNodesList.size(); i++) {
			array[i] = (IDSImplementation) childNodesList.get(i);
		}
		return array;
	}

	public IDSProperties[] getPropertiesElements() {
		ArrayList childNodesList = getChildNodesList(IDSProperties.class, true);
		IDSProperties[] array = new IDSProperties[childNodesList.size()];
		for (int i = 0; i < childNodesList.size(); i++) {
			array[i] = (IDSProperties) childNodesList.get(i);
		}
		return array;
	}

	public IDSProperty[] getPropertyElements() {
		ArrayList childNodesList = getChildNodesList(IDSProperty.class, true);
		IDSProperty[] array = new IDSProperty[childNodesList.size()];
		for (int i = 0; i < childNodesList.size(); i++) {
			array[i] = (IDSProperty) childNodesList.get(i);
		}
		return array;
	}

	public IDSReference[] getReferences() {
		ArrayList childNodesList = getChildNodesList(IDSReference.class, true);
		IDSReference[] array = new IDSReference[childNodesList.size()];
		for (int i = 0; i < childNodesList.size(); i++) {
			array[i] = (IDSReference) childNodesList.get(i);
		}
		return array;
	}

	public IDSService[] getServices() {
		ArrayList childNodesList = getChildNodesList(IDSService.class, true);
		IDSService[] services = new IDSService[childNodesList.size()];
		for (int i = 0; i < childNodesList.size(); i++) {
			services[i] = (IDSService) childNodesList.get(i);
		}
		return services;
	}

}
