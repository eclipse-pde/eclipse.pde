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

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.model.Messages;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
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
	 * Contains all API elements of this scope
	 */
	ArrayList elements;

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiScope#accept(org.eclipse.pde.api.tools.internal.provisional.model.ApiScopeVisitor)
	 */
	public void accept(ApiScopeVisitor visitor) throws CoreException {
		IApiElement[] elements = getApiElements();
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
				default:
					throw new CoreException(
							new Status(
									IStatus.ERROR,
									ApiPlugin.PLUGIN_ID,
									NLS.bind(
											Messages.ApiScope_0,
											Util.getApiElementType(type))));
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiScope#addElement(org.eclipse.pde.api.tools.internal.provisional.model.IApiElement)
	 */
	public void addElement(IApiElement newelement) {
		if (this.elements == null) {
			this.elements = new ArrayList();
		}
		this.elements.add(newelement);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiScope#encloses(org.eclipse.pde.api.tools.internal.provisional.model.IApiElement)
	 */
	public boolean encloses(IApiElement element) {
		if(element != null) {
			IApiComponent component = element.getApiComponent();
			IApiComponent enclosing = null;
			for(Iterator iter = this.elements.iterator(); iter.hasNext();) {
				enclosing = ((IApiElement)iter.next()).getApiComponent();
				if(component.equals(enclosing)) {
					return true;
				}
			}
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiScope#getApiElement()
	 */
	public IApiElement[] getApiElements() {
		if (this.elements == null || this.elements.size() == 0) {
			return NO_ELEMENTS;
		}
		return (IApiElement[]) this.elements.toArray(new IApiElement[this.elements.size()]);
	}
}
