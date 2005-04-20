/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.text.*;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.texteditor.DefaultRangeIndicator;

public abstract class XMLSourcePage extends PDESourcePage {
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
		colorManager = ColorManager.getDefault();
		return new XMLSourceViewerConfiguration(this, colorManager);
	}
	
	public void dispose() {
		super.dispose();
		colorManager.dispose();
	}
	
	public boolean canLeaveThePage() {
		boolean cleanModel = getInputContext().isModelCorrect();
		if (!cleanModel) {
			Display.getCurrent().beep();
			String title = getEditor().getSite().getRegisteredName();
			MessageDialog.openError(
				PDEPlugin.getActiveWorkbenchShell(),
				title,
				PDEUIMessages.SourcePage_errorMessage);
		}
		return cleanModel;
	}

	protected boolean affectsTextPresentation(PropertyChangeEvent event){
		String property = event.getProperty();
		return property.equals(IPDEColorConstants.P_DEFAULT) 
		|| property.equals(IPDEColorConstants.P_PROC_INSTR) 
		|| property.equals(IPDEColorConstants.P_STRING) 
		|| property.equals(IPDEColorConstants.P_TAG) 
		|| property.equals(IPDEColorConstants.P_XML_COMMENT);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#handlePreferenceStoreChanged(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {
		XMLSourceViewerConfiguration sourceViewerConfiguration= (XMLSourceViewerConfiguration)getSourceViewerConfiguration();
		if (affectsTextPresentation(event)) {
			sourceViewerConfiguration.adaptToPreferenceChange(event);
			setSourceViewerConfiguration(sourceViewerConfiguration);
		}
							
		super.handlePreferenceStoreChanged(event);
	}

}
