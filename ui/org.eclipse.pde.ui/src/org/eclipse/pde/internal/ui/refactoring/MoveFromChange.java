/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.refactoring;

import org.eclipse.core.resources.IFile;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.internal.core.text.bundle.PDEManifestElement;
import org.osgi.framework.BundleException;

public class MoveFromChange extends TextFileChange {
	
	PDEManifestElement fElement;

	public MoveFromChange(String name, IFile file) {
		super(name, file);
	}
	
	public ManifestElement getMovedElement() {
		try {
			String value = fElement.write();
			String name = fElement.getHeader().getName();
			ManifestElement[] elements = ManifestElement.parseHeader(name, value);
			if (elements.length > 0)
				return elements[0];
		} catch (BundleException e) {
		}
		return null;
	}
	
	public String getMovedText() {
		return fElement.write();
	}
	
	public void setMovedElement(PDEManifestElement element) {
		fElement = element;
	}
	
	public String getPackageName() {
		return fElement.getValue();
	}

}
