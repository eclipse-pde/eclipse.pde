package org.eclipse.pde.internal.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.schema.*;
import org.eclipse.swt.graphics.*;
import java.util.*;
import org.eclipse.ui.views.properties.*;
import org.eclipse.core.runtime.*;
import java.io.*;
import org.eclipse.core.resources.*;
import org.eclipse.ui.views.contentoutline.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.part.*;
import org.eclipse.swt.*;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.pde.internal.*;


public class ManifestExtensionsPage extends PDEChildFormPage {

public ManifestExtensionsPage(ManifestFormPage parentPage, String title) {
	super(parentPage, title);
}
protected AbstractSectionForm createForm() {
	return new ExtensionsForm(this);
}
public IPropertySheetPage createPropertySheetPage() {
	return new ExtensionsPropertySheet(getEditor());
}
}
