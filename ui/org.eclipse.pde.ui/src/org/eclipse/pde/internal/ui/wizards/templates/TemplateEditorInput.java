/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.wizards.templates;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.part.FileEditorInput;

public class TemplateEditorInput extends FileEditorInput {
	private String firstPageId;

	/**
	 * Constructor for TemplateEditorInput.
	 * @param file
	 */
	public TemplateEditorInput(IFile file, String firstPageId) {
		super(file);
		this.firstPageId = firstPageId;
	}
	
	public String getFirstPageId() {
		return firstPageId;
	}
}
