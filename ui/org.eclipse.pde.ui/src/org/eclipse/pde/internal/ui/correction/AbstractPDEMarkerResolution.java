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
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.text.AbstractEditingModel;
import org.eclipse.pde.internal.core.text.IModelTextChangeListener;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.EditorUtilities;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.build.BuildInputContext;
import org.eclipse.pde.internal.ui.editor.build.BuildSourcePage;
import org.eclipse.swt.graphics.Image;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.forms.editor.IFormPage;

public abstract class AbstractPDEMarkerResolution implements IMarkerResolution2 {

	public static final int CREATE_TYPE = 1;
	public static final int RENAME_TYPE = 2;
	public static final int REMOVE_TYPE = 3;
	
	protected int fType;
	private PDEFormEditor fOpenEditor;

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
		IBaseModel model = findModelFromEditor(marker);
		
		if (model != null) {
			// directly modify model from open editor and save
			createChange(model);
			fOpenEditor.doSave(null);
			fOpenEditor = null;
		} else {
			// create text edits and apply them to textbuffer
			IResource res = marker.getResource();
			try {
				ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
				manager.connect(res.getFullPath(), null);
				ITextFileBuffer buffer = manager.getTextFileBuffer(res.getFullPath());
				if (buffer.isDirty())
					buffer.commit(null, true);
				IDocument document = buffer.getDocument();	
				AbstractEditingModel editModel = createModel(document);
				editModel.setUnderlyingResource(res);
				editModel.load();
				if (editModel.isLoaded()) {
					IModelTextChangeListener listener = createListener(document);
					editModel.addModelChangedListener(listener);
					createChange(editModel);
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
	
	private IBaseModel findModelFromEditor(IMarker marker) {
		IResource res = marker.getResource();
		IProject project = res.getProject();
		String name = res.getName();
		if (name.equals("plugin.xml") || name.equals("fragment.xml")) {
			fOpenEditor = EditorUtilities.getOpenManifestEditor(project);
			if (fOpenEditor != null) {
				IBaseModel model = fOpenEditor.getAggregateModel();
				if (model instanceof IBundlePluginModelBase)
					model = ((IBundlePluginModelBase)model).getExtensionsModel();
				if (model instanceof AbstractEditingModel)
					return model;
			}
		} else if (name.equals("build.properties")) {
			fOpenEditor = EditorUtilities.getOpenBuildPropertiesEditor(project);
			if (fOpenEditor != null) {
				IBaseModel model = fOpenEditor.getAggregateModel();
				if (model instanceof AbstractEditingModel)
					return model;
			} else {
				fOpenEditor = EditorUtilities.getOpenManifestEditor(project);
				if (fOpenEditor == null)
					return null;
				IFormPage page = fOpenEditor.findPage(BuildInputContext.CONTEXT_ID);
				if (page instanceof BuildSourcePage) {
					IBaseModel model = ((BuildSourcePage)page).getInputContext().getModel();
					if (model instanceof AbstractEditingModel)
						return model;
				}
			}
		} else if (name.equals("MANIFEST.MF")) {
			fOpenEditor = EditorUtilities.getOpenManifestEditor(project);
			IBaseModel model = fOpenEditor.getAggregateModel();
			if (model instanceof IBundlePluginModel) {
				if (this instanceof AbstractXMLMarkerResolution)
					return model;
				model = ((IBundlePluginModel)model).getBundleModel();
				if (model instanceof AbstractEditingModel)
					return model;
			}
		}
		return null;
	}
	
	protected abstract void createChange(IBaseModel model);
	
	protected abstract AbstractEditingModel createModel(IDocument doc);
	
	protected abstract IModelTextChangeListener createListener(IDocument doc);
}
