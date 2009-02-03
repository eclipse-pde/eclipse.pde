/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.provisional.comparator;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.model.ApiScopeVisitor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiScope;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Default implementation of a {@link IApiScope}.
 */
public class ApiScope implements IApiScope {
	private static final IApiElement[] NO_ELEMENTS = new IApiElement[0];

	/**
	 * Constant used for controlling tracing in an API scope
	 */
	private static boolean DEBUG = Util.DEBUG;
	
	/**
	 * Sets the debug status for an API type
	 * @param debug
	 */
	public static void setDebug(boolean debug) {
		DEBUG = debug | Util.DEBUG;
	}
	
	/**
	 * Contains all API elements of this scope
	 */
	Set apiElements;

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiScope#accept(org.eclipse.pde.api.tools.internal.provisional.model.ApiScopeVisitor)
	 */
	public void accept(ApiScopeVisitor visitor) throws CoreException {
		IApiElement[] elements = getApiElement();
		for (int i = 0; i < elements.length; i++) {
			IApiElement apiElement = elements[i];
			int type = apiElement.getType();
			switch(type) {
				case IApiElement.API_TYPE_CONTAINER : {
					IApiTypeContainer container = (IApiTypeContainer) apiElement;
					visitor.visit(container);
					visitor.endVisit(container);
					break;
				}
				case IApiElement.API_TYPE_ROOT : {
					IApiTypeRoot root = (IApiTypeRoot) apiElement;
					visitor.visit(root);
					visitor.endVisit(root);
					break;
				}
				case IApiElement.BASELINE : {
					IApiBaseline baseline = (IApiBaseline) apiElement;
					visitor.visit(baseline);
					visitor.endVisit(baseline);
					break;
				}
				case IApiElement.COMPONENT : {
					IApiComponent component = (IApiComponent) apiElement;
					visitor.visit(component);
					visitor.endVisit(component);
					break;
				}
				default: {
					if(DEBUG) {
						System.out.println("Unable to visit this element type: "+Util.getApiElementType(type)); //$NON-NLS-1$
					}
				}
			}
		}
	}

	/**
	 * Adds a new {@link IApiElement} to the current listing
	 * @param apiElement
	 */
	public void add(IApiElement apiElement) {
		if (this.apiElements == null) {
			this.apiElements = new HashSet();
		}
		this.apiElements.add(apiElement);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiScope#getApiElement()
	 */
	public IApiElement[] getApiElement() {
		if (this.apiElements == null || this.apiElements.size() == 0) return NO_ELEMENTS;
		return (IApiElement[]) this.apiElements.toArray(new IApiElement[this.apiElements.size()]);
	}
}
