/*
 * Created on Jan 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor;

import org.eclipse.pde.internal.ui.editor.text.*;
import org.eclipse.pde.internal.ui.neweditor.text.*;
import org.eclipse.ui.texteditor.*;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class XMLSourcePage extends PDESourcePage {
	protected IColorManager colorManager;
	/**
	 * @param editor
	 * @param id
	 * @param title
	 */
	public XMLSourcePage(PDEFormEditor editor, String id, String title) {
		super(editor, id, title);
		setSourceViewerConfiguration(createXMLConfiguration());
		setRangeIndicator(new DefaultRangeIndicator());
	}

	protected XMLSourceViewerConfiguration createXMLConfiguration() {
		if (colorManager != null)
			colorManager.dispose();
		colorManager = new ColorManager();
		return new XMLSourceViewerConfiguration(this, colorManager);
	}
	
	protected IContentOutlinePage createOutlinePage() {
		return null;
	}

	public void dispose() {
		colorManager.dispose();
		super.dispose();
	}
}
