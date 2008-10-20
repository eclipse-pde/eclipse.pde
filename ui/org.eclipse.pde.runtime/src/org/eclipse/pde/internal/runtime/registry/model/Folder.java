/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.registry.model;

public class Folder {

	public static final int F_EXTENSIONS = 1;
	public static final int F_EXTENSION_POINTS = 2;
	public static final int F_IMPORTS = 3;
	public static final int F_LIBRARIES = 4;
	public static final int F_REGISTERED_SERVICES = 5;
	public static final int F_SERVICES_IN_USE = 6;

	private int id;
	private Bundle parent;

	public Folder(int id, Bundle parent) {
		this.id = id;
		this.parent = parent;
	}

	public int getId() {
		return id;
	}

	public ModelObject getParent() {
		return parent;
	}

	public ModelObject[] getChildren() {
		switch (id) {
			case F_EXTENSION_POINTS :
				return parent.getExtensionPoints();
			case F_EXTENSIONS :
				return parent.getExtensions();
			case F_IMPORTS :
				return parent.getImports();
			case F_LIBRARIES :
				return parent.getLibraries();
			case F_REGISTERED_SERVICES :
				return parent.getRegisteredServices();
			case F_SERVICES_IN_USE :
				return parent.getServicesInUse();
		}

		return null;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + ((parent == null) ? 0 : parent.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		return ((obj instanceof Folder) && (((Folder) obj).id == id) && (((Folder) obj).parent.equals(parent)));
	}
}
