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
import org.eclipse.pde.internal.core.ibundle.IBundle;

public class ImportPackageHeader extends BasePackageHeader {

	private static final long serialVersionUID = 1L;

	public ImportPackageHeader(String name, String value, IBundle bundle, String lineDelimiter) {
		super(name, value, bundle, lineDelimiter);
	}

	protected PDEManifestElement createElement(ManifestElement element) {
		return new ImportPackageObject(this, element, getVersionAttribute());
	}

	public ImportPackageObject getPackage(String packageName) {
		return (fElementMap == null) ? null : (ImportPackageObject) fElementMap.get(packageName);
	}

	public ImportPackageObject[] getPackages() {
		PDEManifestElement[] elements = getElements();
		ImportPackageObject[] result = new ImportPackageObject[elements.length];
		System.arraycopy(elements, 0, result, 0, elements.length);
		return result;
	}

	public ImportPackageObject addPackage(String id) {
		ImportPackageObject obj = new ImportPackageObject(this, id, null, getVersionAttribute());
		addManifestElement(obj);
		return obj;
	}

}
