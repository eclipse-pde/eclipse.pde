/*******************************************************************************
 *  Copyright (c) 2003, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 507831
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import java.io.File;
import java.nio.charset.Charset;
import java.util.*;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.core.text.AbstractEditingModel;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.core.text.plugin.*;
import org.eclipse.pde.internal.ui.editor.JarEntryEditorInput;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.context.XMLInputContext;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.*;

public class PluginInputContext extends XMLInputContext {
	public static final String CONTEXT_ID = "plugin-context"; //$NON-NLS-1$
	private boolean fIsFragment;

	public PluginInputContext(PDEFormEditor editor, IEditorInput input, boolean primary, boolean isFragment) {
		super(editor, input, primary);
		fIsFragment = isFragment;
		create();
	}

	@Override
	protected IBaseModel createModel(IEditorInput input) throws CoreException {
		PluginModelBase model = null;
		boolean isReconciling = input instanceof IFileEditorInput;
		IDocument document = getDocumentProvider().getDocument(input);
		if (fIsFragment) {
			model = new FragmentModel(document, isReconciling);
		} else {
			model = new PluginModel(document, isReconciling);
		}
		if (input instanceof IFileEditorInput) {
			IFile file = ((IFileEditorInput) input).getFile();
			model.setUnderlyingResource(file);
			model.setCharset(Charset.forName(file.getCharset()));
		} else if (input instanceof IURIEditorInput) {
			IFileStore store = EFS.getStore(((IURIEditorInput) input).getURI());
			model.setInstallLocation(store.getParent().toString());
			model.setCharset(getDefaultCharset());
		} else if (input instanceof JarEntryEditorInput) {
			File file = ((JarEntryEditorInput) input).getAdapter(File.class);
			model.setInstallLocation(file.toString());
			model.setCharset(getDefaultCharset());
		} else {
			model.setCharset(getDefaultCharset());
		}
		model.load();
		return model;
	}

	@Override
	public String getId() {
		return CONTEXT_ID;
	}

	public boolean isFragment() {
		return fIsFragment;
	}

	@Override
	protected void reorderInsertEdits(ArrayList<TextEdit> ops) {
		HashMap<Object, TextEdit> map = getOperationTable();
		Iterator<Object> iter = map.keySet().iterator();
		TextEdit runtimeInsert = null;
		TextEdit requiresInsert = null;
		ArrayList<TextEdit> extensionPointInserts = new ArrayList<>();
		ArrayList<TextEdit> extensionInserts = new ArrayList<>();

		while (iter.hasNext()) {
			Object object = iter.next();
			if (object instanceof IDocumentElementNode) {
				IDocumentElementNode node = (IDocumentElementNode) object;
				if (node.getParentNode() instanceof PluginBaseNode) {
					TextEdit edit = map.get(node);
					if (edit instanceof InsertEdit) {
						if (node.getXMLTagName().equals("runtime")) { //$NON-NLS-1$
							runtimeInsert = edit;
						} else if (node.getXMLTagName().equals("requires")) { //$NON-NLS-1$
							requiresInsert = edit;
						} else if (node.getXMLTagName().equals("extension")) { //$NON-NLS-1$
							extensionInserts.add(edit);
						} else if (node.getXMLTagName().equals("extension-point")) { //$NON-NLS-1$
							extensionPointInserts.add(edit);
						}
					}
				}
			}
		}

		for (int i = 0; i < ops.size(); i++) {
			TextEdit edit = ops.get(i);
			if (edit instanceof InsertEdit) {
				if (extensionPointInserts.contains(edit)) {
					ops.remove(edit);
					ops.add(0, edit);
				}
			}
		}

		if (requiresInsert != null) {
			ops.remove(requiresInsert);
			ops.add(0, requiresInsert);
		}

		if (runtimeInsert != null) {
			ops.remove(runtimeInsert);
			ops.add(0, runtimeInsert);
		}
	}

	@Override
	public void doRevert() {
		fEditOperations.clear();
		fOperationTable.clear();
		fMoveOperations.clear();
		AbstractEditingModel model = (AbstractEditingModel) getModel();
		model.reconciled(model.getDocument());
	}

	@Override
	protected String getPartitionName() {
		return "___plugin_partition"; //$NON-NLS-1$
	}
}
