/*
 * Created on Jan 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin;

import java.io.File;

import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.ui.editor.SystemFileEditorInput;
import org.eclipse.pde.internal.ui.neweditor.PDEFormEditor;
import org.eclipse.pde.internal.ui.neweditor.context.XMLInputContext;
import org.eclipse.ui.*;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class PluginInputContext extends XMLInputContext {
	public static final String CONTEXT_ID="plugin-context";
	private boolean fragment;
	/**
	 * @param editor
	 * @param input
	 */
	public PluginInputContext(PDEFormEditor editor, IEditorInput input, boolean primary, boolean fragment) {
		super(editor, input, primary);
		this.fragment = fragment;
		create();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.InputContext#createModel(org.eclipse.ui.IEditorInput)
	 */
	protected IModel createModel(IEditorInput input) {
		boolean hasBundle = getEditor().getContextManager().hasContext(BundleInputContext.CONTEXT_ID);
		if (input instanceof IFileEditorInput)
			return createResourceModel((IFileEditorInput)input, hasBundle);
		if (input instanceof SystemFileEditorInput)
			return createExternalModel((SystemFileEditorInput)input, hasBundle);
		if (input instanceof IStorageEditorInput)
			return createStorageModel((IStorageEditorInput)input, hasBundle);
		return null;
	}
	private IModel createResourceModel(IFileEditorInput input, boolean hasBundle) {
		if (hasBundle) {
			WorkspaceExtensionsModel wmodel = new WorkspaceExtensionsModel(input.getFile());
			wmodel.load();
			return wmodel;
		}
		else {
			WorkspacePluginModelBase model;
			if (fragment)
				model = new WorkspaceFragmentModel(input.getFile());
			else
				model = new WorkspacePluginModel(input.getFile());
			model.load();
			return model;
		}
	}
	private IModel createExternalModel(SystemFileEditorInput input, boolean hasBundle) {
		ExternalPluginModelBase model;
		File file = (File)input.getAdapter(File.class);
		String location = file.getParentFile().getPath();
		if (fragment)
			model = new ExternalFragmentModel();
		else
			model = new ExternalPluginModel();
		model.setInstallLocation(location);
		model.load();
		return model;
	}
	private IModel createStorageModel(IStorageEditorInput input, boolean hasBundle) {
		return null;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.InputContext#getId()
	 */
	public String getId() {
		return CONTEXT_ID;
	}
	public boolean isFragment() {
		return fragment;
	}
}