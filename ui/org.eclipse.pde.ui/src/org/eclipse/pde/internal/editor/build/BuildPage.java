package org.eclipse.pde.internal.editor.build;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.ui.views.contentoutline.*;
import org.eclipse.pde.internal.editor.*;

public class BuildPage extends PDEFormPage {

public BuildPage(PDEMultiPageEditor editor, String title) {
	super(editor, title);
}
public IContentOutlinePage createContentOutlinePage() {
	return new BuildOutlinePage(this);
}
protected AbstractSectionForm createForm() {
	return new BuildForm(this);
}
}
