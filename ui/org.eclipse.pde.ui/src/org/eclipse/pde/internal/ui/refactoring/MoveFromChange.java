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

public class MoveFromChange extends TextFileChange {
	
	ManifestElement fElement;

	public MoveFromChange(String name, IFile file) {
		super(name, file);
	}
	
	public ManifestElement getMovedElement() {
		return fElement;
	}
	
	public void setMovedElement(ManifestElement element) {
		fElement = element;
	}

}
