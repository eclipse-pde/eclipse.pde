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

package org.eclipse.pde.internal.ui.editor.manifest;


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.pde.internal.core.plugin.AbstractModelUpdateStrategy;
import org.eclipse.pde.internal.core.plugin.AbstractPluginModelBase;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.widgets.Shell;


public class XMLReconcilingStrategy implements IReconcilingStrategy {

	private ManifestSourcePageNew fPage;
	private IDocument fDocument;

	public XMLReconcilingStrategy(ManifestSourcePageNew page) {
		fPage= page;
	}

	private void saveInternalReconcile() {
		try {
			internalReconcile();
		} catch (Throwable th) {
			PDEPlugin.logException(th);
		}
	}	
	private synchronized void internalReconcile() {
		if (!fPage.isDynamicReconciling() || !fPage.tryGetModelUpdatingTicket())
			return;
		
		if (fDocument == null)
			return;
		
		Shell shell= fPage.getSite().getShell();
		if (shell != null && !shell.isDisposed()) {
			Object modelObject= fPage.getEditor().getModel();
			if (modelObject != null && modelObject instanceof AbstractPluginModelBase) { //only the plugin-model is currently supported (thread safety)
				AbstractPluginModelBase model= (AbstractPluginModelBase) modelObject;
				String text= fDocument.get();
				try {
					InputStream stream= new ByteArrayInputStream(text.getBytes("UTF8"));
					AbstractModelUpdateStrategy updateStrategy= model.getDocumentModel().reconcile(stream, false, false);
					if (updateStrategy != null) {
						shell.getDisplay().syncExec(updateStrategy);
					}
				} catch (UnsupportedEncodingException e) {
					PDEPlugin.logException(e);
				}
			}
		}
	}

	public void reconcile(IRegion partition) {
		saveInternalReconcile();
	}

	public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
		saveInternalReconcile();
	}

	public synchronized void setDocument(IDocument document) {
		fDocument= document;
	}

}
