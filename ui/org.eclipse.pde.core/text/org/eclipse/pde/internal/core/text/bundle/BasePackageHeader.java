/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text.bundle;

import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.bundle.BundlePluginBase;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public abstract class BasePackageHeader extends CompositeManifestHeader {

	private static final long serialVersionUID = 1L;

	public BasePackageHeader(String name, String value, IBundle bundle, String lineDelimiter) {
		super(name, value, bundle, lineDelimiter, true);
	}

	protected String getVersionAttribute() {
		int manifestVersion = BundlePluginBase.getBundleManifestVersion(getBundle());
		return (manifestVersion < 2) ? ICoreConstants.PACKAGE_SPECIFICATION_VERSION : Constants.VERSION_ATTRIBUTE;
	}

	public void addPackage(PackageObject object) {
		addManifestElement(object);
	}

	public Object removePackage(PackageObject object) {
		return removeManifestElement(object);
	}

	public boolean hasPackage(String packageName) {
		return hasElement(packageName);
	}

	public Object removePackage(String name) {
		return removeManifestElement(name);
	}

	public boolean renamePackage(String oldName, String newName) {
		if (hasPackage(oldName)) {
			PackageObject object = (PackageObject) removeManifestElement(oldName);
			object.setName(newName);
			addManifestElement(object);
			return true;
		}
		return false;
	}

	protected void processValue(String value) {
		try {
			ManifestElement[] elements = ManifestElement.parseHeader(fName, value);
			for (int i = 0; i < elements.length; i++) {
				if (elements[i].getValueComponents().length > 1) {
					// if package element has multiple value components, create a new Element to represent each value (bug 160233)
					String[] values = elements[i].getValueComponents();
					for (int j = 0; j < values.length; j++) {
						PDEManifestElement elem = createElement(elements[i]);
						elem.setValueComponents(new String[] {values[j]});
						addManifestElement(elem, false);
					}
				} else {
					addManifestElement(createElement(elements[i]), false);
				}
			}
		} catch (BundleException e) {
		}
	}

}
