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
package org.eclipse.pde.internal.ui.refactoring;

import org.eclipse.core.resources.IFile;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.internal.core.text.bundle.PDEManifestElement;
import org.osgi.framework.BundleException;

public class MoveFromChange extends TextFileChange {

	PDEManifestElement[] fElements;

	public MoveFromChange(String name, IFile file) {
		super(name, file);
	}

	public ManifestElement[] getMovedElements() {
		ManifestElement[] result = new ManifestElement[fElements.length];
		try {
			for (int i = 0; i < fElements.length; i++) {
				String value = fElements[i].write();
				String name = fElements[i].getHeader().getName();
				result[i] = ManifestElement.parseHeader(name, value)[0];
			}
		} catch (BundleException e) {
		}
		return result;
	}

	public String getMovedText(int index) {
		return fElements[index].write();
	}

	public void setMovedElements(PDEManifestElement[] elements) {
		fElements = elements;
	}

	public String getPackageName(int index) {
		return fElements[index].getValue();
	}

}
