package org.eclipse.pde.internal.editor.component;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.ui.views.contentoutline.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.pde.internal.editor.text.*;

public class ComponentSourcePage extends PDESourcePage {
	IColorManager colorManager = new ColorManager();

public ComponentSourcePage(PDEMultiPageEditor editor) {
	super(editor);
	setSourceViewerConfiguration(new XMLConfiguration(colorManager));
}
public IContentOutlinePage createContentOutlinePage() {
	return new ComponentSourceOutlinePage(getEditor().getEditorInput(), getDocumentProvider(), this);
}
public void dispose() {
	colorManager.dispose();
	super.dispose();
}
}
