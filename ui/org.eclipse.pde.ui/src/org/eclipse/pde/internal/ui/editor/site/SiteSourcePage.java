package org.eclipse.pde.internal.ui.editor.site;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.text.*;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

public class SiteSourcePage extends PDESourcePage {
	IColorManager colorManager = new ColorManager();

public SiteSourcePage(PDEMultiPageEditor editor) {
	super(editor);
	setSourceViewerConfiguration(new XMLConfiguration(colorManager));
}
public IContentOutlinePage createContentOutlinePage() {
	return new SiteSourceOutlinePage(getEditor().getEditorInput(), getDocumentProvider(), this);
}
public void dispose() {
	colorManager.dispose();
	super.dispose();
}
}
