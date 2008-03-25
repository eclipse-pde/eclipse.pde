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

import org.eclipse.pde.internal.core.text.DocumentNodeFactory;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.core.text.IDocumentNodeFactory;
import org.eclipse.pde.internal.ds.core.IDSConstants;

/**
 * Handles the creation of document nodes representing the types of elements that
 * can exist in a declarative services xml file.
 * 
 * @since 3.4
 * @see DSModel
 * @see DSDocumentHandler
 */
public class DSDocumentFactory extends DocumentNodeFactory implements IDocumentNodeFactory {
	private DSModel fModel;

	public DSDocumentFactory(DSModel model) {
		fModel = model;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentNodeFactory#createDocumentNode(java.lang.String, org.eclipse.pde.internal.core.text.IDocumentElementNode)
	 */
	public IDocumentElementNode createDocumentNode(String name, IDocumentElementNode parent) {
		if (isRoot(name)) { // Root
			return createRoot();
		}
		if (isImplementation(name)){ 
			return createImplementation();
		}
		if (isProperties(name)){
			return createProperties();
		}
		if(isProperty(name)){
			return createProperty();
		}
		if (isService(name)) { 
			return createService();
		}
		if (isReference(name)){
			return createReference();
		}
		if(isProvide(name)){
			return createProvide();
		}
		
		return super.createDocumentNode(name, parent);
	}

	public DSProvide createProvide() {
		return new DSProvide(fModel);
	}

	private DSProperty createProperty() {
		return new DSProperty(fModel);
	}

	public DSReference createReference() {
		return new DSReference(fModel);
	}

	public DSService createService() {
		return new DSService(fModel);
	}

	public DSProperties createProperties() {
		return new DSProperties(fModel);
	}

	public DSImplementation createImplementation() {
		return new DSImplementation(fModel);
	}
	
	public DSRoot createRoot() {
		return new DSRoot(fModel);
	}


	private boolean isReference(String name) {
		return name.equals(IDSConstants.ELEMENT_REFERENCE);
	}

	private boolean isService(String name) {
		return name.equals(IDSConstants.ELEMENT_SERVICE);
	}

	private boolean isProperties(String name) {
		return name.equals(IDSConstants.ELEMENT_PROPERTIES);
	}

	private boolean isImplementation(String name) {
		return name.equals(IDSConstants.ELEMENT_IMPLEMENTATION);
	}

	private boolean isRoot(String name) {
		return name.equals(IDSConstants.ELEMENT_ROOT); 
	}

	private boolean isProperty(String name) {
		return name.equals(IDSConstants.ELEMENT_PROPERTY);
	}

	private boolean isProvide(String name) {
		return name.equals(IDSConstants.ELEMENT_PROVIDE);
	}

}
