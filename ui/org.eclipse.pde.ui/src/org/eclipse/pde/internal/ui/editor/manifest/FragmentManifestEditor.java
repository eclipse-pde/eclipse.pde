package org.eclipse.pde.internal.ui.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

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
