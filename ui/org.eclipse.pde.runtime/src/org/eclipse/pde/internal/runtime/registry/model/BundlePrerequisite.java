/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.registry.model;

public class BundlePrerequisite extends ModelObject {

	private boolean isExported;
	private String name;
	private String version;
	private boolean isPackage;

	public void setPackage(boolean isPackage) {
		this.isPackage = isPackage;
	}

	public boolean isPackage() {
		return isPackage;
	}

	public void setExported(boolean isExported) {
		this.isExported = isExported;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public boolean isExported() {
		return isExported;
	}

	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}
}
