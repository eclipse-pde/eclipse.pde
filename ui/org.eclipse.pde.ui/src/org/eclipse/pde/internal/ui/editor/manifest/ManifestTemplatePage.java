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
package org.eclipse.pde.internal.ui.editor.manifest;

import org.eclipse.core.resources.IFile;
import org.eclipse.pde.internal.ui.editor.PDEChildFormPage;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.update.ui.forms.internal.AbstractSectionForm;

public class ManifestTemplatePage extends PDEChildFormPage {
	private IFile templateFile;

	public ManifestTemplatePage(
		ManifestFormPage parentPage,
		String title,
		IFile templateFile) {
		super(parentPage, title);
		this.templateFile = templateFile;
	}

	public IContentOutlinePage createContentOutlinePage() {
		return null;
	}

	public IFile getTemplateFile() {
		return templateFile;
	}

	protected AbstractSectionForm createForm() {
		return new TemplateForm(this);
	}
}
