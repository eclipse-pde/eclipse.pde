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
package org.eclipse.pde.internal.ui.editor.site;

import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.ui.views.contentoutline.*;
import org.eclipse.update.ui.forms.internal.*;

public class FeaturesPage extends PDEFormPage {

public FeaturesPage(PDEMultiPageEditor editor, String title) {
	super(editor, title);
}
public IContentOutlinePage createContentOutlinePage() {
	return null;
}
protected AbstractSectionForm createForm() {
	return new FeaturesForm(this);
}
}
