package org.eclipse.pde.internal.ui.editor.toc;

import org.eclipse.pde.internal.ui.editor.PDEFormTextEditorContributor;

public class TocEditorContributor extends PDEFormTextEditorContributor {

	public TocEditorContributor() {
		super("TOC Editor"); //$NON-NLS-1$
	}

	public boolean supportsHyperlinking() {
		return true;
	}
}
