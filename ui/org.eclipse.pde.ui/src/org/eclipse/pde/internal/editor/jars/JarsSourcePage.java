package org.eclipse.pde.internal.editor.jars;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.ui.views.contentoutline.*;
import org.eclipse.pde.internal.editor.*;

public class JarsSourcePage extends PDESourcePage {

public JarsSourcePage(PDEMultiPageEditor editor) {
	super(editor);
}
public IContentOutlinePage createContentOutlinePage() {
	return new JarsSourceOutlinePage(getEditor().getEditorInput(), getDocumentProvider(), this);
}
}
