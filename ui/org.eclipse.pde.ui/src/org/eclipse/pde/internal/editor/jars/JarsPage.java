package org.eclipse.pde.internal.editor.jars;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.ui.views.contentoutline.*;
import org.eclipse.pde.internal.editor.*;

public class JarsPage extends PDEFormPage {

public JarsPage(PDEMultiPageEditor editor, String title) {
	super(editor, title);
}
public IContentOutlinePage createContentOutlinePage() {
	return new JarsOutlinePage(this);
}
protected SectionForm createForm() {
	return new JarsForm(this);
}
}
