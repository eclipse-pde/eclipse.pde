package org.eclipse.pde.internal.editor.jars;

import org.eclipse.pde.internal.forms.*;
import org.eclipse.ui.views.contentoutline.*;
import org.eclipse.pde.internal.editor.*;

public class JarsPage extends PDEFormPage {

public JarsPage(PDEMultiPageEditor editor, String title) {
	super(editor, title);
}
public IContentOutlinePage createContentOutlinePage() {
	return new JarsOutlinePage(this);
}
protected Form createForm() {
	return new JarsForm(this);;
}
}
