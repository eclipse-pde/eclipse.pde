/*
 * Created on Jan 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin;

import java.io.*;
import java.util.ArrayList;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.ui.editor.SystemFileEditorInput;
import org.eclipse.pde.internal.ui.model.bundle.*;
import org.eclipse.pde.internal.ui.neweditor.PDEFormEditor;
import org.eclipse.pde.internal.ui.neweditor.context.UTF8InputContext;
import org.eclipse.text.edits.*;
import org.eclipse.ui.*;

public class BundleInputContext extends UTF8InputContext {
	public static final String CONTEXT_ID="bundle-context";
	/**
	 * @param editor
	 * @param input
	 */
	public BundleInputContext(PDEFormEditor editor, IEditorInput input, boolean primary) {
		super(editor, input, primary);
		create();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.InputContext#createModel(org.eclipse.ui.IEditorInput)
	 */
	protected IBaseModel createModel(IEditorInput input) throws CoreException {
		BundleModel model = null;
		if (input instanceof IStorageEditorInput) {
			boolean isReconciling = input instanceof IFileEditorInput;
			IDocument document = getDocumentProvider().getDocument(input);
			model = new BundleModel(document, isReconciling);
			if (input instanceof IFileEditorInput) {
				IFile file = ((IFileEditorInput)input).getFile();
				model.setUnderlyingResource(file);
				model.setCharset(file.getCharset());
			} else if (input instanceof SystemFileEditorInput){
				File file = (File)((SystemFileEditorInput)input).getAdapter(File.class);
				model.setInstallLocation(file.getParent());
				model.setCharset(getDefaultCharset());
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
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.context.InputContext#addTextEditOperation(java.util.ArrayList, org.eclipse.pde.core.IModelChangedEvent)
	 */
	protected void addTextEditOperation(ArrayList ops, IModelChangedEvent event) {
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.context.InputContext#getMoveOperations()
	 */
	protected TextEdit[] getMoveOperations() {
		return null;
	}
}