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

import org.eclipse.pde.internal.core.ibundle.IBundle;

public class BundleClasspathHeader extends CompositeManifestHeader {

	private static final long serialVersionUID = 1L;

	public BundleClasspathHeader(String name, String value, IBundle bundle, String lineDelimiter) {
		super(name, value, bundle, lineDelimiter);
	}

	public void addLibrary(String name) {
		addManifestElement(name);
	}

	/**
	 * @param name
	 * @param index
	 */
	public void addLibrary(String name, int index) {
		addManifestElement(name, index);
	}

	public void removeLibrary(String name) {
		removeManifestElement(name);
	}

}
