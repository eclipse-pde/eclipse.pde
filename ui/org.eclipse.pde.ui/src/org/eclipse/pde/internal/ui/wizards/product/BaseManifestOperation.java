/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.product;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModel;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.IModelTextChangeListener;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.bundle.BundleSymbolicNameHeader;
import org.eclipse.pde.internal.core.text.bundle.BundleTextChangeListener;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.osgi.framework.Constants;

public abstract class BaseManifestOperation implements IRunnableWithProgress {
	
	private Shell fShell;
	protected String fPluginId;

	public BaseManifestOperation(Shell shell, String pluginId) {
		fShell = shell;
		fPluginId = pluginId;
	}
	
	protected Shell getShell() {
		return fShell;
	}

	protected void updateSingleton(IProgressMonitor monitor) throws CoreException, MalformedTreeException, BadLocationException {
		IPluginModelBase plugin = PDECore.getDefault().getModelManager().findModel(fPluginId);
		if (plugin instanceof IBundlePluginModel) {
			IFile file = (IFile)plugin.getUnderlyingResource();
			IStatus status = PDEPlugin.getWorkspace().validateEdit(new IFile[] { file }, fShell);
			if (status.getSeverity() != IStatus.OK)
				throw new CoreException(new Status(IStatus.ERROR, "org.eclipse.pde.ui", IStatus.ERROR, NLS.bind(PDEUIMessages.ProductDefinitionOperation_readOnly, fPluginId), null)); //$NON-NLS-1$ 

			try {
				ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
				manager.connect(file.getFullPath(), null);
				ITextFileBuffer buffer = manager.getTextFileBuffer(file.getFullPath());
				IDocument document = buffer.getDocument();		
				BundleModel model = new BundleModel(document, false);
				model.load();
				if (model.isLoaded()) {
					IModelTextChangeListener listener = new BundleTextChangeListener(document);
					model.addModelChangedListener(listener);
					Bundle bundle = (Bundle)model.getBundle();
					IManifestHeader header = bundle.getManifestHeader(Constants.BUNDLE_SYMBOLICNAME);
					if (header instanceof BundleSymbolicNameHeader) {
						BundleSymbolicNameHeader symbolic = (BundleSymbolicNameHeader)header;
						if (!symbolic.isSingleton())
							symbolic.setSingleton(true);
					}
					
					TextEdit[] edits = listener.getTextOperations();
					if (edits.length > 0) {
						MultiTextEdit multi = new MultiTextEdit();
						multi.addChildren(edits);
						multi.apply(document);
						buffer.commit(monitor, true);
					}
				}
			} catch (CoreException e) {
				PDEPlugin.log(e);
			} catch (MalformedTreeException e) {
				PDEPlugin.log(e);
			} catch (BadLocationException e) {
				PDEPlugin.log(e);
			} finally {
				try {
					FileBuffers.getTextFileBufferManager().disconnect(file.getFullPath(), null);
				} catch (CoreException e) {
					PDEPlugin.log(e);
				}
			}
		}
	}
}
