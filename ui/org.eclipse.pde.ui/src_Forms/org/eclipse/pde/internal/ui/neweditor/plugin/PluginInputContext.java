/*
 * Created on Jan 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin;

import java.io.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.ui.model.plugin.*;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.pde.internal.ui.neweditor.context.*;
import org.eclipse.ui.*;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
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
	protected IModel createModel(IEditorInput input) throws CoreException {
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
}