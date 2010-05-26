/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.provisional.model;


/**
 * Visits {@link IApiTypeRoot}s in an {@link IApiTypeContainer}
 * 
 * @since 1.0.0
 */
public abstract class ApiTypeContainerVisitor {	

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
	 * Visits a container and returns whether class files
	 * in the container should be visited.
	 * <p>
	 * The default implementation does nothing and returns <code>true</code>.
	 * Subclasses may re-implement.
	 * </p>
	 * @param container
	 * @return
	 */
	public boolean visit(IApiTypeContainer container) {
		return true;
	}
	
	/**
	 * Ends visiting a container.
	 * <p>
	 * The default implementation does nothing. Subclasses may re-implement.
	 * </p>
	 * @param container
	 */
	public void end(IApiTypeContainer container) {
		//subclasses my re-implement
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
	 * Visits an {@link IApiTypeRoot} from the specified package.
	 * <p>
	 * The default implementation does nothing.
	 * </p>
	 * @param packageName fully qualified dot separated package name or the empty
	 * 	string for the default package 
	 * @param typeroot {@link IApiTypeRoot} to visit
	 */
	public void visit(String packageName, IApiTypeRoot typeroot) {
		// subclasses may re-implement
	}
	
	/**
	 * End visiting an {@link IApiTypeRoot}.
	 * <p>
	 * The default implementation does nothing. Subclasses may re-implement.
	 * </p>
	 * @param packageName fully qualified dot separated package name or the empty
	 * 	string for the default package
	 * @param typeroot {@link IApiTypeRoot} ending visit on
	 */	
	public void end(String packageName, IApiTypeRoot typeroot) {
		// subclasses may re-implement
	}	
}
