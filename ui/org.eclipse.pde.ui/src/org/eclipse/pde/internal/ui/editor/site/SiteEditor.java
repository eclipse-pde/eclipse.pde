package org.eclipse.pde.internal.ui.editor.site;

import org.eclipse.pde.internal.ui.editor.text.ColorManager;
import org.eclipse.ui.editors.text.TextEditor;

public class SiteEditor extends TextEditor {

	private ColorManager colorManager;

	public SiteEditor() {
		super();
		colorManager = new ColorManager();
		setSourceViewerConfiguration(new XMLConfiguration(colorManager));
		setDocumentProvider(new XMLDocumentProvider());
	}
	public void dispose() {
		colorManager.dispose();
		super.dispose();
	}

}
