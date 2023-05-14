/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corporation and others.
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
 *     Wolfgang Schell <ws@jetztgrad.net> - bug 260055
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.registry.model;

import java.util.Arrays;

import org.osgi.framework.ServiceReference;

public class ServiceName extends ModelObject implements Comparable<ServiceName> {

	private String[] classes;
	private ServiceReference<?> reference;

	public ServiceName(String[] classes, ServiceReference<?> ref) {
		this.classes = classes;
		this.reference = ref;
	}

	public ServiceReference<?> getServiceReference() {
		return this.reference;
	}

	public String[] getClasses() {
		return classes;
	}

	public ModelObject[] getChildren() {
		if (model == null) {
			return new ModelObject[0];
		}
		return model.getServices(classes);
	}

	private static int hashCode(Object[] array) {
		int prime = 31;
		if (array == null)
			return 0;
		int result = 1;
		for (Object o : array) {
			result = prime * result + (o == null ? 0 : o.hashCode());
		}
		return result;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		return prime * ServiceName.hashCode(classes);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		ServiceName other = (ServiceName) obj;
		return Arrays.equals(classes, other.classes);
	}

	@Override
	public int compareTo(ServiceName other) {
		// compare first class
		String myClass = classes[0];
		String otherClass = other.getClasses()[0];

		return myClass.compareTo(otherClass);

	}
}
