package org.eclipse.pde.internal.editor.component;

import org.eclipse.pde.internal.forms.*;
import org.eclipse.ui.views.contentoutline.*;
import org.eclipse.pde.internal.editor.*;

public class ComponentFormPage extends PDEFormPage {

public ComponentFormPage(PDEMultiPageEditor editor, String title) {
	super(editor, title);
}
public IContentOutlinePage createContentOutlinePage() {
	return new ComponentOutlinePage(this);
}
protected Form createForm() {
	return new ComponentForm(this);;
}
}
