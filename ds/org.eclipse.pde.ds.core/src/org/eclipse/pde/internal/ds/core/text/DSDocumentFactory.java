/*******************************************************************************
 * Copyright (c) 2008, 2015 IBM Corporation and others.
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
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ds.core.text;

import org.eclipse.pde.internal.core.text.DocumentNodeFactory;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.core.text.IDocumentNodeFactory;
import org.eclipse.pde.internal.ds.core.IDSComponent;
import org.eclipse.pde.internal.ds.core.IDSConstants;
import org.eclipse.pde.internal.ds.core.IDSDocumentFactory;
import org.eclipse.pde.internal.ds.core.IDSImplementation;
import org.eclipse.pde.internal.ds.core.IDSProperties;
import org.eclipse.pde.internal.ds.core.IDSProperty;
import org.eclipse.pde.internal.ds.core.IDSProvide;
import org.eclipse.pde.internal.ds.core.IDSReference;
import org.eclipse.pde.internal.ds.core.IDSService;

/**
 * Handles the creation of document nodes representing the types of elements
 * that can exist in a declarative services xml file.
 *
 * @since 3.4
 * @see DSModel
 * @see DSDocumentHandler
 */
public class DSDocumentFactory extends DocumentNodeFactory implements
		IDocumentNodeFactory, IDSDocumentFactory {
	private DSModel fModel;

	public DSDocumentFactory(DSModel model) {
		fModel = model;
	}

	@Override
	public IDocumentElementNode createDocumentNode(String name,
			IDocumentElementNode parent) {

		// TODO this logic needs to be pushed up somehow... we shouldn't care
		// about the namespace prefix
		int index = name.indexOf(":"); //$NON-NLS-1$
		if (index != -1) {
			name = name.substring(index + 1);
		}

		if (isRoot(name)) { // Root
			return (IDocumentElementNode) createComponent();
		}

		if (parent.getXMLTagName().equals(IDSConstants.ELEMENT_COMPONENT)) {
			if (isImplementation(name)) {
				return (IDocumentElementNode) createImplementation();
			}
			if (isProperties(name)) {
				return (IDocumentElementNode) createProperties();
			}
			if (isProperty(name)) {
				return (IDocumentElementNode) createProperty();
			}
			if (isService(name)) {
				return (IDocumentElementNode) createService();
			}
			if (isReference(name)) {
				return (IDocumentElementNode) createReference();
			}
		}

		if (parent.getXMLTagName().equals(IDSConstants.ELEMENT_SERVICE)) {
			if (isProvide(name)) {
				return (IDocumentElementNode) createProvide();
			}
		}

		return super.createDocumentNode(name, parent);
	}

	@Override
	public IDSProvide createProvide() {
		return new DSProvide(fModel);
	}

	@Override
	public IDSProperty createProperty() {
		return new DSProperty(fModel);
	}

	@Override
	public IDSReference createReference() {
		return new DSReference(fModel);
	}

	@Override
	public IDSService createService() {
		return new DSService(fModel);
	}

	@Override
	public IDSProperties createProperties() {
		return new DSProperties(fModel);
	}

	@Override
	public IDSImplementation createImplementation() {
		return new DSImplementation(fModel);
	}

	@Override
	public IDSComponent createComponent() {
		return new DSComponent(fModel);
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
		return name.equals(IDSConstants.ELEMENT_COMPONENT);
	}

	private boolean isProperty(String name) {
		return name.equals(IDSConstants.ELEMENT_PROPERTY);
	}

	private boolean isProvide(String name) {
		return name.equals(IDSConstants.ELEMENT_PROVIDE);
	}

}
