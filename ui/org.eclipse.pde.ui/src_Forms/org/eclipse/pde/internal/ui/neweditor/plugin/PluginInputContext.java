/*
 * Created on Jan 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin;

import java.io.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.ui.model.*;
import org.eclipse.pde.internal.ui.model.plugin.*;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.pde.internal.ui.neweditor.context.*;
import org.eclipse.text.edits.*;
import org.eclipse.ui.*;

public class PluginInputContext extends XMLInputContext {
	public static final String CONTEXT_ID = "plugin-context";
	private boolean fIsFragment;
	/**
	 * @param editor
	 * @param input
	 */
	public PluginInputContext(PDEFormEditor editor, IEditorInput input, boolean primary, boolean isFragment) {
		super(editor, input, primary);
		fIsFragment = isFragment;
		create();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.InputContext#createModel(org.eclipse.ui.IEditorInput)
	 */
	protected IBaseModel createModel(IEditorInput input) throws CoreException {
		//boolean hasBundle = getEditor().getContextManager().hasContext(BundleInputContext.CONTEXT_ID);
		PluginModelBase model = null;
		if (input instanceof IStorageEditorInput) {
			boolean isReconciling = input instanceof IFileEditorInput;
			IDocument document = getDocumentProvider().getDocument(input);
			if (fIsFragment) {
				model = new FragmentModel(document, isReconciling);
			} else {
				model = new PluginModel(document, isReconciling);
			}
			if (input instanceof IFileEditorInput) {
				model.setUnderlyingResource(((IFileEditorInput)input).getFile());
			} else {
				model.setInstallLocation(((File)input.getAdapter(File.class)).getParent());
			}
			model.load();
		}
		return model;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.InputContext#getId()
	 */
	public String getId() {
		return CONTEXT_ID;
	}
	public boolean isFragment() {
		return fIsFragment;
	}
	
	protected void reorderInsertEdits(ArrayList ops) {
		HashMap map = getOperationTable();
		Iterator iter = map.keySet().iterator();
		TextEdit runtimeInsert = null;
		TextEdit requiresInsert = null;
		ArrayList extensionPointInserts = new ArrayList();
		ArrayList extensionInserts = new ArrayList();
		
		while (iter.hasNext()) {
			Object object = iter.next();
			if (object instanceof IDocumentNode) {
				IDocumentNode node = (IDocumentNode)object;
				if (node.getParentNode() instanceof PluginBaseNode) {
					TextEdit edit = (TextEdit)map.get(node);
					if (edit instanceof InsertEdit) {
						if (node.getXMLTagName().equals("runtime")) {
							runtimeInsert = edit;
						} else if (node.getXMLTagName().equals("requires")) {
							requiresInsert = edit;
						} else if (node.getXMLTagName().equals("extension")) {
							extensionInserts.add(edit);
						} else if (node.getXMLTagName().equals("extension-point")) {
							extensionPointInserts.add(edit);
						}
					}
				}
			}
		}
		
		if (runtimeInsert != null) {
			ops.remove(runtimeInsert);
			ops.add(runtimeInsert);
		}
		
		if (requiresInsert != null) {
			ops.remove(requiresInsert);
			ops.add(requiresInsert);
		}
		
		for (int i = 0; i < extensionPointInserts.size(); i++) {
			InsertEdit edit = (InsertEdit)extensionPointInserts.get(i);
			ops.remove(edit);
			ops.add(edit);
		}
		for (int i = 0; i < extensionInserts.size(); i++) {
			InsertEdit edit = (InsertEdit)extensionInserts.get(i);
			ops.remove(edit);
			ops.add(edit);
		}
	}
	
}