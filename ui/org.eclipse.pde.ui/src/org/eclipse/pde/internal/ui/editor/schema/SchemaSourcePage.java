package org.eclipse.pde.internal.ui.editor.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.action.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.text.*;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

public class SchemaSourcePage extends PDESourcePage implements IPDEEditorPage {
	public static final String SCHEMA_TYPE = "__extension_point_schema";
	private IColorManager colorManager = new ColorManager();

	public SchemaSourcePage(SchemaEditor editor) {
		super(editor);
		setSourceViewerConfiguration(new XMLConfiguration(colorManager));
	}
	public IContentOutlinePage createContentOutlinePage() {
		return new SchemaSourceOutlinePage(
			getEditorInput(),
			getDocumentProvider(),
			this);
	}
	public void dispose() {
		super.dispose();
		colorManager.dispose();
	}
	
	protected void editorContextMenuAboutToShow(IMenuManager menu) {
		super.editorContextMenuAboutToShow(menu);
		menu.add(new Separator());
		SchemaEditorContributor contributor = (SchemaEditorContributor)getEditor().getContributor();
		menu.add(contributor.getPreviewAction());
	}
}
