package org.eclipse.pde.internal.editor.manifest;

import org.eclipse.pde.internal.base.model.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.model.*;
import java.io.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.pde.internal.*;

public class FragmentManifestEditor extends ManifestEditor {

public FragmentManifestEditor() {
	super();
}
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
public boolean isFragmentEditor() {
	return true;
}
}
