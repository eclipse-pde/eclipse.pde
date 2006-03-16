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
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.core.text.AbstractEditingModel;
import org.eclipse.pde.internal.core.text.IModelTextChangeListener;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.build.BuildInputContext;
import org.eclipse.pde.internal.ui.editor.build.BuildSourcePage;
import org.eclipse.swt.graphics.Image;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.forms.editor.IFormPage;

public abstract class AbstractPDEMarkerResolution implements IMarkerResolution2 {

	public static final int CREATE_TYPE = 1;
	public static final int RENAME_TYPE = 2;
	public static final int REMOVE_TYPE = 3;
	private static final int F_BUNDLE_MODEL = 0;
	private static final int F_BUILD_MODEL = 1;
	
	protected int fType;
	private IEditorPart fOpenEditor;

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
		IResource res = marker.getResource();
		String name = res.getName();
		AbstractEditingModel model = null;
		if (name.equals("MANIFEST.MF"))  //$NON-NLS-1$
			model = findModelFromEditor(res.getProject(), F_BUNDLE_MODEL);
		else if (name.equals("build.properties")) //$NON-NLS-1$
			model = findModelFromEditor(res.getProject(), F_BUILD_MODEL);
		
		if (model != null) {
			// directly modify model from open editor and save
			createChange(model, marker);
			if (fOpenEditor != null)
				fOpenEditor.doSave(null);
			fOpenEditor = null;
		} else {
			// create text edits and apply them to textbuffer
			try {
				ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
				manager.connect(res.getFullPath(), null);
				ITextFileBuffer buffer = manager.getTextFileBuffer(res.getFullPath());
				if (buffer.isDirty())
					buffer.commit(null, true);
				IDocument document = buffer.getDocument();	
				model = createModel(document);
				model.setUnderlyingResource(res);
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
					FileBuffers.getTextFileBufferManager().disconnect(res.getFullPath(), null);
				} catch (CoreException e) {
					PDEPlugin.log(e);
				}
			}
		}
	}
	
	private AbstractEditingModel findModelFromEditor(IProject project, int modelType) {
		PDEFormEditor editor = null;
		switch (modelType) {
		case F_BUNDLE_MODEL:
			editor = PDEPlugin.getOpenManifestEditor(project);
			break;
		case F_BUILD_MODEL:
			editor = PDEPlugin.getOpenBuildPropertiesEditor(project);
			if (editor == null) {
				editor = PDEPlugin.getOpenManifestEditor(project);
				IFormPage page = editor.findPage(BuildInputContext.CONTEXT_ID);
				if (page instanceof BuildSourcePage) {
					IBaseModel model = ((BuildSourcePage)page).getInputContext().getModel();
					if (model instanceof AbstractEditingModel) {
						fOpenEditor = editor;
						return (AbstractEditingModel)model;
					}
				}
			}
			break;
		}
		if (editor != null) {
			fOpenEditor = editor;
			IBaseModel model = editor.getAggregateModel();
			if (model instanceof AbstractEditingModel)
				return (AbstractEditingModel)model;
		}
		return null;
	}
	
	protected abstract void createChange(AbstractEditingModel model, IMarker marker);
	
	protected abstract AbstractEditingModel createModel(IDocument doc);
	
	protected abstract IModelTextChangeListener createListener(IDocument doc);
}
