/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ds.core.text;

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
public class DSRoot extends DSObject {

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
		return objectType == TYPE_IMPLEMENTATION;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.ctxhelp.CtxHelpObject#canAddSibling(int)
	 */
	public boolean canAddSibling(int objectType) {
		return false;
	}
	
	public void setAttributeName(String name){
		setXMLAttribute(ATTRIBUTE_COMPONENT_NAME, name);
	}
	
	public String getAttributeName(){
		return getXMLAttributeValue(ATTRIBUTE_COMPONENT_NAME);
	}
	
	public void setEnabled(boolean bool){
		setBooleanAttributeValue(ATTRIBUTE_ENABLED, bool);
	}
	
	public boolean getEnabled(){
		return getBooleanAttributeValue(ATTRIBUTE_COMPONENT_NAME, true);
	}
	

	public void setFactory(String factory){
		setXMLAttribute(ATTRIBUTE_FACTORY, factory);
	}
	
	public String getFactory(){
		return getXMLAttributeValue(ATTRIBUTE_FACTORY);
	}
	
	public void setImmediate(String factory){
		setXMLAttribute(ATTRIBUTE_IMMEDIATE, factory);
	}
	
	public String getImmediate(){
		return getXMLAttributeValue(ATTRIBUTE_IMMEDIATE);
	}
	
	
	
	
}
