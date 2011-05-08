/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Wolfgang Schell <ws@jetztgrad.net> - bug 260055
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.registry.model;

import java.util.Arrays;

public class ServiceName extends ModelObject implements Comparable {

	private String[] classes;

	public ServiceName(String[] classes) {
		this.classes = classes;
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
		for (int index = 0; index < array.length; index++) {
			result = prime * result + (array[index] == null ? 0 : array[index].hashCode());
		}
		return result;
	}

	public int hashCode() {
		final int prime = 31;
		return prime * ServiceName.hashCode(classes);
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		ServiceName other = (ServiceName) obj;
		return Arrays.equals(classes, other.classes);
	}

	public int compareTo(Object obj) {
		if (obj instanceof ServiceName) {
			// compare first class
			ServiceName other = (ServiceName) obj;
			String myClass = classes[0];
			String otherClass = other.getClasses()[0];

			return myClass.compareTo(otherClass);
		}
		return 0;

	}
}
