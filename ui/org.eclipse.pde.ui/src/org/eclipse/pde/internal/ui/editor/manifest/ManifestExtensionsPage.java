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

import org.eclipse.ui.views.properties.*;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.pde.internal.ui.editor.*;


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
public void openNewExtensionWizard() {
	((ExtensionsForm)getForm()).openNewExtensionWizard();
}
}
