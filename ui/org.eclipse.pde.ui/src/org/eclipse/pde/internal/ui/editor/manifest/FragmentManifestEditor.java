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

public class FragmentManifestEditor extends ManifestEditor {

public FragmentManifestEditor() {
	super();
}

/*
protected void createPages() {
	firstPageId = OVERVIEW_PAGE;
	formWorkbook.setFirstPageSelected(false);
	ManifestFormPage formPage =
		new ManifestFormPage(this, PDEPlugin.getResourceString(KEY_OVERVIEW));
	addPage(OVERVIEW_PAGE, formPage);
	addPage(
		RUNTIME_PAGE,
		new ManifestRuntimePage(formPage, PDEPlugin.getResourceString(KEY_RUNTIME)));
	addPage(
		EXTENSIONS_PAGE,
		new ManifestExtensionsPage(
			formPage,
			PDEPlugin.getResourceString(KEY_EXTENSIONS)));
	addPage(
		EXTENSION_POINT_PAGE,
		new ManifestExtensionPointPage(
			formPage,
			PDEPlugin.getResourceString(KEY_EXTENSION_POINTS)));
	addPage(SOURCE_PAGE, new ManifestSourcePage(this));
}
*/
public boolean isFragmentEditor() {
	return true;
}
}
