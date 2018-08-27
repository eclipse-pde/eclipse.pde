/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.provisional.model;

import org.eclipse.core.runtime.CoreException;

/**
 * Visits {@link IApiElement}s in an {@link IApiScope}
 *
 * @since 1.0.0
 */
public class ApiScopeVisitor {
	/**
	 * End visiting an {@link IApiComponent}.
	 * <p>
	 * The default implementation does nothing. Subclasses may re-implement.
	 * </p>
	 *
	 * @param component
	 */
	public void endVisit(IApiComponent component) {
		// subclasses may re-implement
	}

	/**
	 * End visiting an {@link IApiBaseline}.
	 * <p>
	 * The default implementation does nothing. Subclasses may re-implement.
	 * </p>
	 *
	 * @param baseline
	 */
	public void endVisit(IApiBaseline baseline) {
		// subclasses may re-implement
	}

	/**
	 * End visiting an {@link IApiTypeContainer}.
	 * <p>
	 * The default implementation does nothing. Subclasses may re-implement.
	 * </p>
	 *
	 * @param container
	 */
	public void endVisit(IApiTypeContainer container) {
		// subclasses may re-implement
	}

	/**
	 * End visiting an {@link IApiTypeRoot}.
	 * <p>
	 * The default implementation does nothing. Subclasses may re-implement.
	 * </p>
	 *
	 * @param typeRoot
	 */
	public void endVisit(IApiTypeRoot typeRoot) {
		// subclasses may re-implement
	}

	/**
	 * Visits a component in the scope and returns whether class files in the
	 * component should be visited. This method is only called when the class
	 * file container being visited is contained in an API component.
	 * <p>
	 * The default implementation does nothing and returns <code>true</code>.
	 * Subclasses may re-implement.
	 * </p>
	 *
	 * @param component API component being visited
	 * @return whether class files in the component should be visited
	 * @throws CoreException if an exception occurs during the visit
	 */
	public boolean visit(IApiComponent component) throws CoreException {
		return true;
	}

	/**
	 * Visits a baseline in the scope and returns whether components in the
	 * baseline should be visited.
	 * <p>
	 * The default implementation does nothing and returns <code>true</code>.
	 * Subclasses may re-implement.
	 * </p>
	 *
	 * @param baseline API baseline being visited
	 * @return whether API baseline's components should be visited
	 * @throws CoreException if an exception occurs during the visit
	 */
	public boolean visit(IApiBaseline baseline) throws CoreException {
		return true;
	}

	/**
	 * Visits an API type container in the scope and returns whether types in
	 * the container should be visited.
	 * <p>
	 * The default implementation does nothing and returns <code>true</code>.
	 * Subclasses may re-implement.
	 * </p>
	 *
	 * @param container the given API type container being visited
	 * @return whether types in the container should be visited
	 * @throws CoreException if an exception occurs during the visit
	 */
	public boolean visit(IApiTypeContainer container) throws CoreException {
		return true;
	}

	/**
	 * Visits an API type root in the scope.
	 * <p>
	 * The default implementation does nothing and returns <code>true</code>.
	 * Subclasses may re-implement.
	 * </p>
	 *
	 * @param typeRoot the given API type root being visited
	 * @return whether types in the container should be visited
	 * @throws CoreException if an exception occurs during the visit
	 */
	public void visit(IApiTypeRoot typeRoot) throws CoreException {
		//
	}
}
