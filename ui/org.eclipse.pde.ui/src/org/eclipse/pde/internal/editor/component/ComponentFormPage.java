package org.eclipse.pde.internal.editor.component;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.ui.views.contentoutline.*;
import org.eclipse.pde.internal.editor.*;

public class ComponentFormPage extends PDEFormPage {

public ComponentFormPage(PDEMultiPageEditor editor, String title) {
	super(editor, title);
}
public IContentOutlinePage createContentOutlinePage() {
	return new ComponentOutlinePage(this);
}
protected SectionForm createForm() {
	return new ComponentForm(this);
}
}
