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
package org.eclipse.pde.internal.runtime.registry;

import org.osgi.framework.ServiceReference;

public class ServiceReferenceAdapter extends ParentAdapter {

	public ServiceReferenceAdapter(ServiceReference object) {
		super(object);
	}

	protected Object[] createChildren() {
		// TODO pluggable support for different services
		return null;
	}

	public boolean equals(Object obj) {
		// imitate ServiceReference behavior, that multiple ServiceReference instances are equal
		return (obj instanceof ServiceReferenceAdapter) ? getObject().equals(((ServiceReferenceAdapter) obj).getObject()) : false;
	}

	public int hashCode() {
		// imitate ServiceReference behavior, that multiple ServiceReference instances return the same hashCode
		return getObject().hashCode();
	}
}
