/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
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

	@Override
	protected void processValue(String value) {
		try {
			ManifestElement[] elements = ManifestElement.parseHeader(fName, value);
			for (ManifestElement element : elements) {
				if (element.getValueComponents().length > 1) {
					// if package element has multiple value components, create a new Element to represent each value (bug 160233)
					for (String valueComponent : element.getValueComponents()) {
						PDEManifestElement elem = createElement(element);
						elem.setValueComponents(new String[] {valueComponent});
						addManifestElement(elem, false);
					}
				} else {
					addManifestElement(createElement(element), false);
				}
			}
		} catch (BundleException e) {
		}
	}

}
