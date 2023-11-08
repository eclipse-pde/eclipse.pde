/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 262564
 *     Christoph Läubrich - Bug 576610 - FeatureEditor should support display of non-file-based feature models
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IEditable;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.AbstractModel;
import org.eclipse.pde.internal.core.feature.ExternalFeatureModel;
import org.eclipse.pde.internal.core.feature.WorkspaceFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.context.XMLInputContext;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IURIEditorInput;

/**
 *
 */
public class FeatureInputContext extends XMLInputContext {
	public static final String CONTEXT_ID = "feature-context"; //$NON-NLS-1$

	/**
	 * @param editor
	 * @param input
	 * @param primary
	 */
	public FeatureInputContext(PDEFormEditor editor, IEditorInput input, boolean primary) {
		super(editor, input, primary);
		create();
	}

	@Override
	public String getId() {
		return CONTEXT_ID;
	}

	@Override
	protected IBaseModel createModel(IEditorInput input) throws CoreException {
		if (input instanceof IFileEditorInput)
			return createResourceModel((IFileEditorInput) input);
		if (input instanceof IStorageEditorInput)
			return createStorageModel((IStorageEditorInput) input);
		if (input instanceof IURIEditorInput) {
			return createSystemFileModel((IURIEditorInput) input);
		}
		if (input instanceof FeatureModelEditorInput) {
			IFeatureModel featureModel = input.getAdapter(IFeatureModel.class);
			return featureModel;
		}
		return null;
	}

	private IBaseModel createResourceModel(IFileEditorInput input) {
		IFile file = input.getFile();
		WorkspaceFeatureModel model = new WorkspaceFeatureModel(file);
		model.load();
		return model;
	}

	private IBaseModel createStorageModel(IStorageEditorInput input) throws CoreException {
		InputStream stream = null;
		IStorage storage = input.getStorage();
		try {
			stream = new BufferedInputStream(storage.getContents());
		} catch (CoreException e) {
			PDEPlugin.logException(e);
			return null;
		}
		ExternalFeatureModel model = new ExternalFeatureModel();
		IPath path = input.getStorage().getFullPath();
		model.setInstallLocation(path == null ? "" : path.removeLastSegments(1).toOSString()); //$NON-NLS-1$
		try {
			model.load(stream, false);
		} catch (CoreException e) {
			// Errors in the file
			return null;
		} finally {
			try {
				stream.close();
			} catch (IOException e) {
			}
		}
		return model;
	}

	private IBaseModel createSystemFileModel(IURIEditorInput input) throws CoreException {
		IFileStore store = EFS.getStore(input.getURI());
		ExternalFeatureModel model = new ExternalFeatureModel();
		model.setInstallLocation(store.getParent().toString());
		model.load(store.openInputStream(EFS.CACHE, new NullProgressMonitor()), true);
		return model;
	}

	@Override
	protected void addTextEditOperation(ArrayList<TextEdit> ops, IModelChangedEvent event) {
	}

	@Override
	protected void flushModel(IDocument doc) {
		// if model is dirty, flush its content into
		// the document so that the source editor will
		// pick up the changes.
		if (!(getModel() instanceof IEditable))
			return;
		IEditable editableModel = (IEditable) getModel();
		if (editableModel.isDirty() == false)
			return;
		try (StringWriter swriter = new StringWriter(); PrintWriter writer = new PrintWriter(swriter)) {
			editableModel.save(writer);
			writer.flush();
			String content = swriter.toString();
			content = AbstractModel.fixLineDelimiter(content, (IFile) ((IModel) getModel()).getUnderlyingResource());
			doc.set(content);
		} catch (IOException e) {
			PDEPlugin.logException(e);
		}
	}

	@Override
	protected boolean synchronizeModel(IDocument doc) {
		if (doc == null) {
			return false;
		}
		IFeatureModel model = (IFeatureModel) getModel();
		String text = doc.get();
		try (InputStream stream = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))) {
			model.reload(stream, false);
		} catch (CoreException | IOException e) {
			return false;
		}
		return true;
	}

	@Override
	protected void reorderInsertEdits(ArrayList<TextEdit> ops) {
	}

	@Override
	protected String getPartitionName() {
		return "___feature_partition"; //$NON-NLS-1$
	}
}
