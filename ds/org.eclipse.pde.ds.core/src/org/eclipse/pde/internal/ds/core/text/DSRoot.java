/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rafael Oliveira Nóbrega <rafael.oliveira@gmail.com> - bug 223738
 *******************************************************************************/
package org.eclipse.pde.internal.ds.core.text;

import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.ds.core.IDSObject;
import org.eclipse.pde.internal.ds.core.IDSRoot;
import org.eclipse.pde.internal.ds.core.IDSService;

/**
 * Represents the root "component" entry in a DS xml file.  There may
 * be only one root node in the file and all other nodes must be inside the root.
 * The structure of component XML grammar is:
 * 
 * <component> ::= <implementation>
 * 				   <properties> *
 * 				   <service> ?
 * 				   <reference> *
 * 
 * @since 3.4
 * @see DSObject
 * @see DSModel
 * @see DSDocumentFactory
 */
public class DSRoot extends DSObject implements IDSRoot {

	private static final long serialVersionUID = 1L;
	

	public DSRoot(DSModel model) {
		super(model, ELEMENT_ROOT);
		setInTheModel(true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.plugin.PluginDocumentNode#isRoot()
	 */
	public boolean isRoot() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#getType()
	 */
	public int getType() {
		return TYPE_ROOT;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.ctxhelp.CtxHelpObject#canBeParent()
	 */
	public boolean canBeParent() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.ctxhelp.CtxHelpObject#getName()
	 */
	public String getName() {
		return ELEMENT_ROOT;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.ctxhelp.CtxHelpObject#canAddChild(org.eclipse.pde.internal.core.text.ctxhelp.CtxHelpObject)
	 */
	public boolean canAddChild(int objectType) {
		return objectType == TYPE_IMPLEMENTATION || objectType == TYPE_PROPERTIES || objectType == TYPE_PROPERTY || objectType == TYPE_SERVICE;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.ctxhelp.CtxHelpObject#canAddSibling(int)
	 */
	public boolean canAddSibling(int objectType) {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ds.core.text.IDSRoot#setAttributeName(java.lang.String)
	 */
	public void setAttributeName(String name){
		setXMLAttribute(ATTRIBUTE_COMPONENT_NAME, name);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ds.core.text.IDSRoot#getAttributeName()
	 */
	public String getAttributeName(){
		return getXMLAttributeValue(ATTRIBUTE_COMPONENT_NAME);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ds.core.text.IDSRoot#setEnabled(boolean)
	 */
	public void setEnabled(boolean bool){
		setBooleanAttributeValue(ATTRIBUTE_ENABLED, bool);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ds.core.text.IDSRoot#getEnabled()
	 */
	public boolean getEnabled(){
		return getBooleanAttributeValue(ATTRIBUTE_ENABLED, true);
	}
	

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ds.core.text.IDSRoot#setFactory(java.lang.String)
	 */
	public void setFactory(String factory){
		setXMLAttribute(ATTRIBUTE_FACTORY, factory);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ds.core.text.IDSRoot#getFactory()
	 */
	public String getFactory(){
		return getXMLAttributeValue(ATTRIBUTE_FACTORY);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ds.core.text.IDSRoot#setImmediate(boolean)
	 */
	public void setImmediate(boolean bool){
		setBooleanAttributeValue(ATTRIBUTE_IMMEDIATE, bool);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ds.core.text.IDSRoot#getImmediate()
	 */
	public boolean getImmediate(){
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

	
}
