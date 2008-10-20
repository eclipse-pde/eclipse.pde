/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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

	public BundlePrerequisite(RegistryModel model, String name, String version, boolean isExported) {
		super(model);
		this.name = name;
		this.version = version;
		this.isExported = isExported;
	}

	public boolean isExported() {
		return isExported;
	}

	public String getName() {
		if (version != null) {
			if (Character.isDigit(version.charAt(0)))
				version = '(' + version + ')';
			return name + ' ' + version;
		}

		return name;
	}
}
