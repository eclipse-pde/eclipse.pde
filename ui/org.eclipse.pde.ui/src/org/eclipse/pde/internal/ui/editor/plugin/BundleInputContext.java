/*******************************************************************************
 * Copyright (c) 2003, 2022 IBM Corporation and others.
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
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 507831
 *     Christoph Läubrich - Issue #74
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.AbstractEditingModel;
import org.eclipse.pde.internal.core.text.IDocumentKey;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.bundle.ManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.PDEManifestElement;
import org.eclipse.pde.internal.core.text.bundle.PackageFriend;
import org.eclipse.pde.internal.core.util.ManifestUtils;
import org.eclipse.pde.internal.core.util.PropertiesUtil;
import org.eclipse.pde.internal.ui.editor.JarEntryEditorInput;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.context.ManifestDocumentSetupParticipant;
import org.eclipse.pde.internal.ui.editor.context.UTF8InputContext;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.osgi.framework.Constants;

public class BundleInputContext extends UTF8InputContext {
	public static final String CONTEXT_ID = "bundle-context"; //$NON-NLS-1$

	private final HashMap<IDocumentKey, TextEdit> fOperationTable = new HashMap<>();

	/**
	 * @param editor
	 * @param input
	 */
	public BundleInputContext(PDEFormEditor editor, IEditorInput input, boolean primary) {
		super(editor, input, primary);
		create();
	}

	@Override
	protected IBaseModel createModel(IEditorInput input) throws CoreException {
		boolean isReconciling = input instanceof IFileEditorInput;
		IDocument document = getDocumentProvider().getDocument(input);
		BundleModel model = new BundleModel(document, isReconciling);
		if (input instanceof IFileEditorInput) {
			IFile file = ((IFileEditorInput) input).getFile();
			model.setUnderlyingResource(file);
			model.setCharset(Charset.forName(file.getCharset()));
		} else if (input instanceof IURIEditorInput) {
			IFileStore store = EFS.getStore(((IURIEditorInput) input).getURI());
			model.setInstallLocation(store.getParent().getParent().toString());
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
	public BundleModel getModel() {
		return (BundleModel) super.getModel();
	}

	@Override
	public String getId() {
		return CONTEXT_ID;
	}

	@Override
	protected void addTextEditOperation(ArrayList<TextEdit> ops, IModelChangedEvent event) {
		Object[] objects = event.getChangedObjects();
		if (objects != null) {
			for (Object object : objects) {
				if (object instanceof PDEManifestElement)
					object = ((PDEManifestElement) object).getHeader();
				else if (object instanceof PackageFriend)
					object = ((PackageFriend) object).getHeader();

				if (object instanceof ManifestHeader header) {
					TextEdit op = fOperationTable.get(header);
					if (op != null) {
						fOperationTable.remove(header);
						ops.remove(op);
					}
					String value = header.getValue();
					if (value == null || value.trim().length() == 0) {
						deleteKey(header, ops);
					} else {
						modifyKey(header, ops);
					}
				}
			}
		}
	}

	protected TextEdit[] getMoveOperations() {
		return new TextEdit[0];
	}

	private void insertKey(ManifestHeader key, ArrayList<TextEdit> ops) {
		IDocument doc = getDocumentProvider().getDocument(getInput());
		String write = key.write();
		int offset = getInsertOffset(key, doc);
		InsertEdit op = new InsertEdit(offset, write);
		fOperationTable.put(key, op);
		ops.add(op);
	}

	private int getInsertOffset(ManifestHeader key, IDocument doc) {
		IBundle bundle = key.getBundle();
		List<IManifestHeader> manifestHeaders = bundle.getManifestHeaders().values().stream()
				.filter(header -> {
					if (ManifestUtils.MANIFEST_VERSION.equalsIgnoreCase(header.getKey())
							|| Constants.BUNDLE_MANIFESTVERSION.equalsIgnoreCase(header.getKey())) {
						// leave special headers alone!
						return false;
					}
					return true;
				}).sorted(Comparator.comparing(IManifestHeader::getKey, String.CASE_INSENSITIVE_ORDER))
				.collect(Collectors.toList());

		int indexOf = manifestHeaders.indexOf(key);
		if (indexOf > -1) {
			if (indexOf == 0) {
				// insert after the Bundle-ManifestVersion if given...
				IManifestHeader header = bundle.getManifestHeader(Constants.BUNDLE_MANIFESTVERSION);
				if (header != null && header.getOffset() > -1) {
					return header.getOffset() + header.getLength();
				}
			} else {
				// insert after the the header before our position
				IManifestHeader header = manifestHeaders.get(indexOf - 1);
				return header.getOffset() + header.getLength();
			}
		}
		return PropertiesUtil.getInsertOffset(doc);
	}

	private void deleteKey(IDocumentKey key, ArrayList<TextEdit> ops) {
		if (key.getOffset() > 0) {
			TextEdit op = new DeleteEdit(key.getOffset(), key.getLength());
			fOperationTable.put(key, op);
			ops.add(op);
		}
	}

	private void modifyKey(ManifestHeader key, ArrayList<TextEdit> ops) {
		if (key.getOffset() == -1) {
			insertKey(key, ops);
		} else {
			TextEdit op = new ReplaceEdit(key.getOffset(), key.getLength(), key.write());
			fOperationTable.put(key, op);
			ops.add(op);
		}
	}

	@Override
	public void doRevert() {
		fEditOperations.clear();
		fOperationTable.clear();
		AbstractEditingModel model = getModel();
		model.reconciled(model.getDocument());
	}

	@Override
	protected String getPartitionName() {
		return "___bundle_partition"; //$NON-NLS-1$
	}

	@Override
	protected IDocumentSetupParticipant getDocumentSetupParticipant() {
		return new ManifestDocumentSetupParticipant();
	}
}
