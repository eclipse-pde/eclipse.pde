/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.actions;

import java.io.File;

import org.eclipse.jface.action.Action;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.schema.SchemaEditor;

/**
 * OpenSchemaAction
 *
 */
public class OpenSchemaAction extends Action  {

	// TODO: MP: OpenSchema: HIGH: Look into behaviour to emulate PluginSearchActionGroup
	
	private File fFile;
	
	/**
	 * 
	 */
	public OpenSchemaAction() {
		fFile = null;
		
		initialize();
	}

	/**
	 * 
	 */
	private void initialize() {
		setImageDescriptor(PDEPluginImages.DESC_SCHEMA_OBJ);
		setText(PDEUIMessages.HyperlinkActionOpenSchema);
		setToolTipText(PDEUIMessages.HyperlinkActionOpenSchema);
		setEnabled(false);
	}
	
	/**
	 * @param file
	 */
	public void setFile(File file) {
		fFile = file;
	}
	
	/**
	 * @return
	 */
	public File getFile() {
		return fFile;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		SchemaEditor.openSchema(fFile);
	}

}
