package org.eclipse.pde.internal.editor.build;

import org.eclipse.pde.internal.forms.*;
import org.eclipse.ui.views.contentoutline.*;
import org.eclipse.pde.internal.editor.*;

public class BuildPage extends PDEFormPage {

public BuildPage(PDEMultiPageEditor editor, String title) {
	super(editor, title);
}
public IContentOutlinePage createContentOutlinePage() {
	return new BuildOutlinePage(this);
}
protected Form createForm() {
	return new BuildForm(this);
}
}
