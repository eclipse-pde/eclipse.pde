/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.pde.internal.ui.editor.text.*;

public abstract class XMLSourcePage extends PDESourcePage {
	protected IColorManager colorManager;
	
	public XMLSourcePage(PDEMultiPageEditor editor) {
		super(editor);	
		setSourceViewerConfiguration(createXMLConfiguration());
	}

	protected XMLConfiguration createXMLConfiguration() {
		if (colorManager != null)
			colorManager.dispose();
		colorManager = new ColorManager();
		return new XMLConfiguration(colorManager);
	}

	public void dispose() {
		colorManager.dispose();
		super.dispose();
	}
}
