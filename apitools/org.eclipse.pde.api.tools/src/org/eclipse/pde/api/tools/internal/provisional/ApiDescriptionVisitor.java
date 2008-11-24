/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.provisional;

import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;

/**
 * A visitor for an API component. Component visitors must subclass
 * this class.
 * <p>
 * Package nodes are visited, followed by its type nodes. Component
 * specific nodes are visited before children nodes. 
 * </p>
 * <p> 
 * Specific visit ordering:
 * <pre>
 * ComponentDescription := [visitElement[PackageDescription] endVisitElement[PackageDescription]]*
 * PackageDescription := [visitElement[TypeDescription] endVisitElement[TypeDescription]]*
 * TypeDescription := [[visitElement[OverrideDescription] endVisitElement[OverrideDescription]]* [visitElement[MemberDescription] endVisitElement[MemberDescription]]*]*
 * MemberDescription := MethodDescription | FieldDescription
 * OverrideDescription := PackageDescription | TypeDescription | MethodDescription | FieldDescription
 * </pre>
 * 
 * MemberDescriptions are visited in the order they are keyed for the backing {@link HashMap}
 * </p>
 * 
 * @since 1.0.0
 */
public abstract class ApiDescriptionVisitor {

	/**
	 * Visits an element in the manifest and returns whether children nodes
	 * in the manifest should be visited.
	 * <p>
	 * The default implementation does nothing and returns <code>true</code>.
	 * Subclasses may re-implement.
	 * </p>
	 * @param element element being visited
	 * @param description description of the element visited 
	 * @return whether child elements should be visited
	 */
	public boolean visitElement(IElementDescriptor element, IApiAnnotations description) {
		return true;
	}
	
	/**
	 * End visiting an element.
	 * <p>
	 * The default implementation does nothing. Subclasses may re-implement.
	 * </p>
	 * @param element element being end-visited
	 * @param description description of the element end-visited
	 */
	public void endVisitElement(IElementDescriptor element, IApiAnnotations description) {
		// subclasses may re-implement
	}
}
