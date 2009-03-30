/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
	public static final int F_PROPERTIES = 7;
	public static final int F_USING_BUNDLES = 8;
	public static final int F_FRAGMENTS = 9;
	public static final int F_IMPORTED_PACKAGES = 10;
	public static final int F_EXPORTED_PACKAGES = 11;

	private int id;
	private Object parent;

	public Folder(int id, Object parent) {
		this.id = id;
		this.parent = parent;
	}

	public int getId() {
		return id;
	}

	public Object getParent() {
		return parent;
	}

	public ModelObject[] getChildren() {
		switch (id) {
			case F_EXTENSION_POINTS :
				return ((Bundle) parent).getExtensionPoints();
			case F_EXTENSIONS :
				return ((Bundle) parent).getExtensions();
			case F_IMPORTS :
				return ((Bundle) parent).getImports();
			case F_LIBRARIES :
				return ((Bundle) parent).getLibraries();
			case F_REGISTERED_SERVICES :
				return ((Bundle) parent).getRegisteredServices();
			case F_SERVICES_IN_USE :
				return ((Bundle) parent).getServicesInUse();
			case F_PROPERTIES :
				return ((ServiceRegistration) parent).getProperties();
			case F_USING_BUNDLES :
				return ((ServiceRegistration) parent).getUsingBundles();
			case F_FRAGMENTS :
				return ((Bundle) parent).getFragments();
			case F_IMPORTED_PACKAGES :
				return ((Bundle) parent).getImportedPackages();
			case F_EXPORTED_PACKAGES :
				return ((Bundle) parent).getExportedPackages();
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
