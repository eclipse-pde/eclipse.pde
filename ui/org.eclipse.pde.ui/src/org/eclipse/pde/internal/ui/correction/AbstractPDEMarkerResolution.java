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
import org.eclipse.pde.internal.core.text.AbstractEditingModel;
import org.eclipse.pde.internal.core.text.IModelTextChangeListener;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.graphics.Image;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IMarkerResolution2;

public abstract class AbstractPDEMarkerResolution implements IMarkerResolution2 {

	public static final int CREATE_TYPE = 1;
	public static final int RENAME_TYPE = 2;
	public static final int REMOVE_TYPE = 3;
	
	protected int fType;

	public AbstractPDEMarkerResolution(int type) {
		fType = type;
	}
	
	public Image getImage() {
		return null;
	}

	public int getType() {
		return fType;
	}
	
	public void run(IMarker marker) {
		IResource resource = marker.getResource();
		try {
			ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
			manager.connect(resource.getFullPath(), null);
			ITextFileBuffer buffer = manager.getTextFileBuffer(resource.getFullPath());
			if (buffer.isDirty())
				buffer.commit(null, true);
			IDocument document = buffer.getDocument();		
			AbstractEditingModel model = createModel(document);
			model.setUnderlyingResource(resource);
			model.load();
			if (model.isLoaded()) {
				IModelTextChangeListener listener = createListener(document);
				model.addModelChangedListener(listener);
				createChange(model, marker);
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
	
	protected abstract void createChange(AbstractEditingModel model, IMarker marker);
	
	protected abstract AbstractEditingModel createModel(IDocument doc);
	
	protected abstract IModelTextChangeListener createListener(IDocument doc);
}
