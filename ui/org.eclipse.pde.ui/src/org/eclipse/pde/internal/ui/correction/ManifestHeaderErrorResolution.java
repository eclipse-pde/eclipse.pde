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
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.bundle.BundleTextChangeListener;
import org.eclipse.pde.internal.core.text.bundle.IModelTextChangeListener;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

public abstract class ManifestHeaderErrorResolution extends AbstractPDEMarkerResolution {	
	
	public ManifestHeaderErrorResolution(int type) {
		super(type);
	}

	public void run(IMarker marker) {
		IResource resource = marker.getResource();
		try {
			ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
			manager.connect(resource.getFullPath(), null);
			ITextFileBuffer buffer = manager.getTextFileBuffer(resource.getFullPath());
			IDocument document = buffer.getDocument();		
			BundleModel model = new BundleModel(document, false);
			model.load();
			if (model.isLoaded()) {
				IModelTextChangeListener listener = new BundleTextChangeListener(document);
				model.addModelChangedListener(listener);
				createChange(model);
				TextEdit[] edits = listener.getTextOperations();
				if (edits.length > 0) {
					MultiTextEdit multi = new MultiTextEdit();
					multi.addChildren(edits);
					multi.apply(document);
					buffer.commit(null, true);
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
				FileBuffers.getTextFileBufferManager().disconnect(resource.getFullPath(), null);
			} catch (CoreException e) {
				PDEPlugin.log(e);
			}
		}
	}
	
	protected abstract void createChange(BundleModel model);

}
