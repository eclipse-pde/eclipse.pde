package org.eclipse.pde.internal.editor.build;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.ui.views.contentoutline.*;
import org.eclipse.pde.internal.editor.*;

public class BuildSourcePage extends PDESourcePage {

public BuildSourcePage(PDEMultiPageEditor editor) {
	super(editor);
}
public IContentOutlinePage createContentOutlinePage() {
	return new BuildSourceOutlinePage(getEditor().getEditorInput(), getDocumentProvider(), this);
}
}
