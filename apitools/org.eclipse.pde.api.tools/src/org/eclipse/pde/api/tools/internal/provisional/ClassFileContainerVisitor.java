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



/**
 * Visits class files in a class file container.
 * 
 * @since 1.0.0
 */
public class ClassFileContainerVisitor {	

	/**
	 * Visits a component in the container and returns whether class files
	 * in the component should be visited. This method is only called when
	 * the class file container being visited is contained in an API component.
	 * <p>
	 * The default implementation does nothing and returns <code>true</code>.
	 * Subclasses may re-implement.
	 * </p>
	 * @param component API component being visited 
	 * @return whether class files in the component should be visited
	 */
	public boolean visit(IApiComponent component) {
		return true;
	}

	/**
	 * End visiting a component. This method is only called when
	 * the class file container being visited is contained in an API component.
	 * <p>
	 * The default implementation does nothing. Subclasses may re-implement.
	 * </p>
	 * @param component API component 
	 */
	public void end(IApiComponent component) {
		// subclasses may re-implement
	}

	/**
	 * Visits a package in the container and returns whether class files
	 * in the package should be visited.
	 * <p>
	 * The default implementation does nothing and returns <code>true</code>.
	 * Subclasses may re-implement.
	 * </p>
	 * @param packageName fully qualified dot separated package name or the empty
	 * 	string for the default package 
	 * @return whether class files in the package should be visited
	 */
	public boolean visitPackage(String packageName) {
		return true;
	}
	
	/**
	 * End visiting a package.
	 * <p>
	 * The default implementation does nothing. Subclasses may re-implement.
	 * </p>
	 * @param packageName fully qualified dot separated package name or the empty
	 * 	string for the default package 
	 */	
	public void endVisitPackage(String packageName) {
		// subclasses may re-implement
	}
	
	/**
	 * Visits a class file from the specified package.
	 * <p>
	 * The default implementation does nothing.
	 * </p>
	 * @param packageName fully qualified dot separated package name or the empty
	 * 	string for the default package 
	 * @param classFile class file
	 */
	public void visit(String packageName, IClassFile classFile) {
		// subclasses may re-implement
	}
	
	/**
	 * End visiting a class file.
	 * <p>
	 * The default implementation does nothing. Subclasses may re-implement.
	 * </p>
	 * @param packageName fully qualified dot separated package name or the empty
	 * 	string for the default package
	 * @param classFile class file
	 */	
	public void end(String packageName, IClassFile classFile) {
		// subclasses may re-implement
	}	
}
