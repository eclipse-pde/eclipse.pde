/*
 * Created on Jan 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.text.*;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.texteditor.DefaultRangeIndicator;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public abstract class XMLSourcePage extends PDESourcePage {
	public static final String ERROR_MESSAGE = "SourcePage.errorMessage";
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
	
	public void dispose() {
		colorManager.dispose();
		super.dispose();
	}
	public boolean canLeaveThePage() {
		boolean cleanModel = getInputContext().isModelCorrect();
		if (!cleanModel) {
			Display.getCurrent().beep();
			String title = getEditor().getSite().getRegisteredName();
			MessageDialog.openError(
				PDEPlugin.getActiveWorkbenchShell(),
				title,
				PDEPlugin.getResourceString(ERROR_MESSAGE));
		}
		return cleanModel;
	}

}
