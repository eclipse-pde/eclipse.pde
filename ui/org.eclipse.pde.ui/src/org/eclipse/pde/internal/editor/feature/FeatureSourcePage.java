package org.eclipse.pde.internal.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.ui.views.contentoutline.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.pde.internal.editor.text.*;

public class FeatureSourcePage extends PDESourcePage {
	IColorManager colorManager = new ColorManager();

public FeatureSourcePage(PDEMultiPageEditor editor) {
	super(editor);
	setSourceViewerConfiguration(new XMLConfiguration(colorManager));
}
public IContentOutlinePage createContentOutlinePage() {
	return new FeatureSourceOutlinePage(getEditor().getEditorInput(), getDocumentProvider(), this);
}
public void dispose() {
	colorManager.dispose();
	super.dispose();
}
}
