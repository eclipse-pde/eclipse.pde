/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.model.plugin;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.model.*;
import org.xml.sax.SAXException;

public abstract class AbstractPluginDocumentHandler extends DocumentHandler {
	
	private PluginModelBase fModel;
	private String fSchemaVersion;

	/**
	 * @param model
	 */
	public AbstractPluginDocumentHandler(PluginModelBase model) {
		fModel = model;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.plugin.DocumentHandler#getDocument()
	 */
	protected IDocument getDocument() {
		return fModel.getDocument();
	}
	
	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#endDocument()
	 */
	public void endDocument() throws SAXException {
		IPluginBase pluginBase = fModel.getPluginBase();
		try {
			if (pluginBase != null)
				pluginBase.setSchemaVersion(fSchemaVersion);
		} catch (CoreException e) {
		}
	}
	

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#processingInstruction(java.lang.String, java.lang.String)
	 */
	public void processingInstruction(String target, String data)
			throws SAXException {
		if ("eclipse".equals(target)) { //$NON-NLS-1$
			fSchemaVersion = "3.0"; //$NON-NLS-1$
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.DocumentHandler#startDocument()
	 */
	public void startDocument() throws SAXException {
		super.startDocument();
		fSchemaVersion = null;
	}
	protected PluginModelBase getModel() {
		return fModel;
	}
}
