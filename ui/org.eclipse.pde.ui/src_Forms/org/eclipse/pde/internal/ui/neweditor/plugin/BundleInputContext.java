/*
 * Created on Jan 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin;

import java.util.*;

import org.eclipse.pde.core.*;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.core.osgi.bundle.WorkspaceBundleModel;
import org.eclipse.pde.internal.ui.editor.SystemFileEditorInput;
import org.eclipse.pde.internal.ui.neweditor.PDEFormEditor;
import org.eclipse.pde.internal.ui.neweditor.context.UTF8InputContext;
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
	protected IModel createModel(IEditorInput input) {
		if (input instanceof IFileEditorInput)
			return createResourceModel((IFileEditorInput)input);
		if (input instanceof SystemFileEditorInput)
			return createExternalModel((SystemFileEditorInput)input);
		if (input instanceof IStorageEditorInput)
			return createStorageModel((IStorageEditorInput)input);
		return null;
	}
	private IModel createResourceModel(IFileEditorInput input) {
		WorkspaceBundleModel model = new WorkspaceBundleModel(input.getFile());
		model.load();
		return model;
	}
	private IModel createExternalModel(SystemFileEditorInput input) {
		return null;
	}
	
	private IModel createStorageModel(IStorageEditorInput input) {
		return null;
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
}