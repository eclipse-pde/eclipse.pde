/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.bnd;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.context.IInputContextListener;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.editor.plugin.BundleInputContext;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorInput;

public class BndInputContext extends InputContext implements IInputContextListener, IModelChangedListener {
	public static final String CONTEXT_ID = "bnd-context"; //$NON-NLS-1$

	public BndInputContext(PDEFormEditor editor, IEditorInput input, boolean primary) {
		super(editor, input, primary);
		create();
	}

	@Override
	protected Charset getDefaultCharset() {
		return StandardCharsets.UTF_8;
	}

	@Override
	protected IBaseModel createModel(IEditorInput input) throws CoreException {
		IDocument document = getDocumentProvider().getDocument(input);
		BndModel model = new BndModel(document);
		model.load();
		return model;
	}

	@Override
	public BndModel getModel() {
		return (BndModel) super.getModel();
	}

	@Override
	public String getId() {
		return CONTEXT_ID;
	}

	@Override
	protected void addTextEditOperation(ArrayList<TextEdit> ops, IModelChangedEvent event) {
	}

	@Override
	public void doRevert() {
	}

	@Override
	protected String getPartitionName() {
		return "___bnd_partition"; //$NON-NLS-1$
	}

	@Override
	public void contextAdded(InputContext context) {
		if (context instanceof BundleInputContext bundleContext) {
			bundleContext.getModel().addModelChangedListener(this);
		}
	}

	@Override
	public void contextRemoved(InputContext context) {
		if (context instanceof BundleInputContext bundleContext) {
			bundleContext.getModel().removeModelChangedListener(this);
		}
	}

	@Override
	public void monitoredFileAdded(IFile monitoredFile) {

	}

	@Override
	public boolean monitoredFileRemoved(IFile monitoredFile) {
		return false;
	}

	@Override
	public void modelChanged(IModelChangedEvent event) {
		String changedProperty = event.getChangedProperty();
		Object newValue = event.getNewValue();
		BndModel model = getModel();
		try {
			// first sync editor with document
			model.load();
			// update the value
			model.genericSet(changedProperty, newValue);
			// update the document
			model.saveChanges();
		} catch (Exception e) {
			// can't sync with bnd file then
		}
	}
}
