/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.ApiDescriptionVisitor;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IFieldDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.util.Signatures;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * {@link IApiDescription} visitor that generates XML for the given {@link IApiComponent}.
 *  
 * @since 1.0.0
 */
public class ApiDescriptionXmlCreator extends ApiDescriptionVisitor {
	
	/**
	 * Component element
	 */
	private Element fComponent;
	
	/**
	 * XML doc being generated
	 */
	private Document fDoc;
	
	/**
	 * Current package node being created
	 */
	private Element fPackage;
	
	/**
	 * Visibility modifiers for package being visited
	 */
	private int fPackageVisibility;
	
	/**
	 * The stack of current type node being visited
	 */
	private Stack fTypeStack;

	/**
	 * Set of package names already visited (to avoid re-visiting same package)
	 */
	private Set fVisitedPackages;
	
	/**
	 * Constructs a new visitor for the given component.
	 * 
	 * @param component API component
	 * @throws CoreException if unable to construct the visitor
	 */
	public ApiDescriptionXmlCreator(IApiComponent component) throws CoreException {
		this(component.getName(), component.getSymbolicName());
	}

	/**
	 * Constructs a new visitor for the given component.
	 * 
	 * @param componentName the given component name
	 * @param componentId the given component id
	 * 
	 * @throws CoreException if unable to construct the visitor
	 */
	public ApiDescriptionXmlCreator(String componentName, String componentId) throws CoreException {
		fDoc = Util.newDocument();
		fComponent = fDoc.createElement(IApiXmlConstants.ELEMENT_COMPONENT);
		fComponent.setAttribute(IApiXmlConstants.ATTR_NAME, componentName);
		fComponent.setAttribute(IApiXmlConstants.ATTR_VERSION, IApiXmlConstants.API_DESCRIPTION_CURRENT_VERSION);
		fDoc.appendChild(fComponent);
		Element plugin = fDoc.createElement(IApiXmlConstants.ELEMENT_PLUGIN);
		plugin.setAttribute(IApiXmlConstants.ATTR_ID, componentId);
		fComponent.appendChild(plugin);
		fVisitedPackages = new HashSet();
		fTypeStack = new Stack();
	}

	/**
	 * Annotates the attribute set of the specified {@link Element}
	 * 
	 * @param componentContext component context to which the API applies, or <code>null</code>
	 * @param description the description to annotate from
	 * @param element the element to annotate
	 */
	private void annotateElementAttributes(IApiAnnotations description, Element element) {
		element.setAttribute(IApiXmlConstants.ATTR_RESTRICTIONS, Integer.toString(description.getRestrictions()));
		int visibility = description.getVisibility();
		if (visibility != fPackageVisibility) {
			element.setAttribute(IApiXmlConstants.ATTR_VISIBILITY, Integer.toString(visibility));
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.ApiDescriptionVisitor#endVisitElement(org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor, org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations)
	 */
	public void endVisitElement(IElementDescriptor element, IApiAnnotations description) {
		switch(element.getElementType()) {
			case IElementDescriptor.PACKAGE: {
				// A null package indicates there was an override for the package in a different context.
				// Package rules are stored in the manifest, not the API description file. 
				// No need to add empty packages.
				if (fPackage != null && fPackage.hasChildNodes()) {
					fComponent.appendChild(fPackage);
				}
				fPackage = null;
				break;
			}
			case IElementDescriptor.TYPE: {
				fTypeStack.pop();
				break;
			}
		}
	}
	
	/**
	 * Returns the settings as a UTF-8 string containing XML.
	 * 
	 * @return XML
	 * @throws CoreException if something goes wrong 
	 */
	public String getXML() throws CoreException {
		return Util.serializeDocument(fDoc);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.ApiDescriptionVisitor#visitElement(org.eclipse.pde.api.tools.model.component.IElementDescriptor, java.lang.String, org.eclipse.pde.api.tools.model.IApiAnnotations)
	 */
	public boolean visitElement(IElementDescriptor element, IApiAnnotations description) {
		switch(element.getElementType()) {
			case IElementDescriptor.PACKAGE: {
				IPackageDescriptor pkg = (IPackageDescriptor) element;
				String pkgName = pkg.getName();
				if (fVisitedPackages.add(pkgName)) {
					fPackage = fDoc.createElement(IApiXmlConstants.ELEMENT_PACKAGE);
					fPackage.setAttribute(IApiXmlConstants.ATTR_NAME, pkgName);
					// package visibility settings are stored in MANIFEST.MF, so omit them here.
					// still keep track of the visibility to know if children should override
					fPackageVisibility = description.getVisibility();
					fPackage.setAttribute(IApiXmlConstants.ATTR_VISIBILITY, Integer.toString(fPackageVisibility));
					fVisitedPackages.add(pkgName);
				}
				break;
			}
			case IElementDescriptor.TYPE: {
				IReferenceTypeDescriptor typeDesc = (IReferenceTypeDescriptor) element;
				fTypeStack.push(fDoc.createElement(IApiXmlConstants.ELEMENT_TYPE));
				Element type = (Element) fTypeStack.peek();
				annotateElementAttributes(description, type);
				fPackage.appendChild(type);
				type.setAttribute(IApiXmlConstants.ATTR_NAME, Signatures.getSimpleTypeName(typeDesc.getQualifiedName()));
				break;
			}
			case IElementDescriptor.METHOD: {
				IMethodDescriptor desc = (IMethodDescriptor) element;
				Element method = fDoc.createElement(IApiXmlConstants.ELEMENT_METHOD);
				Element type = (Element) fTypeStack.peek();
				//add standard attributes
				annotateElementAttributes(description, method);
				if (method.hasAttributes()) {
					type.appendChild(method);
					//add specific method attributes
					method.setAttribute(IApiXmlConstants.ATTR_SIGNATURE, desc.getSignature());
					method.setAttribute(IApiXmlConstants.ATTR_NAME, desc.getName());
				}
				break;
			}
			case IElementDescriptor.FIELD: {
				IFieldDescriptor desc = (IFieldDescriptor) element;
				Element field = fDoc.createElement(IApiXmlConstants.ELEMENT_FIELD);
				Element type = (Element) fTypeStack.peek();
				annotateElementAttributes(description, field);
				if (field.hasAttributes()) {
					type.appendChild(field);
					//add standard attributes
					//add specific field attributes
					field.setAttribute(IApiXmlConstants.ATTR_NAME, desc.getName());
				}
				break;
			}
		}
		return true;
	}
}
