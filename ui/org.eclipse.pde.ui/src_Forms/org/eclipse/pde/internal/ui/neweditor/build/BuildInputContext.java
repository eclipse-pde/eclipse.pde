/*
 * Created on Jan 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.build;

import java.io.*;
import java.util.*;
import org.eclipse.jface.text.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.build.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.pde.internal.ui.neweditor.context.*;
import org.eclipse.text.edits.*;
import org.eclipse.ui.*;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class BuildInputContext extends InputContext {
	public static final String CONTEXT_ID = "build-context";
	/**
	 * @param editor
	 * @param input
	 */
	public BuildInputContext(PDEFormEditor editor, IEditorInput input, boolean primary) {
		super(editor, input, primary);
		create();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.context.InputContext#getCharSet()
	 */
	protected String getDefaultCharset() {
		return "ISO-8859-1";
	}

	protected IBaseModel createModel(IEditorInput input) {
		if (input instanceof IFileEditorInput) {
			return createWorkspaceModel((IFileEditorInput)input);
		}
		if (input instanceof SystemFileEditorInput) {
			return createExternalModel((SystemFileEditorInput)input);
			
		}
		if (input instanceof IStorageEditorInput) {
			return createStorageModel((IStorageEditorInput)input);
		}
		return null;
	}
	private IModel createWorkspaceModel(IFileEditorInput input) {
		WorkspaceBuildModel model = new WorkspaceBuildModel(input.getFile());
		model.load();
		return model;
	}
	private IBaseModel createExternalModel(SystemFileEditorInput input) {
		File file = (File)input.getAdapter(File.class);
		String location = file.getParentFile().getPath();
		ExternalBuildModel model = new ExternalBuildModel(location);
		model.load();
		return model;
	}
	private IBaseModel createStorageModel(IStorageEditorInput input) {
		return null;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.InputContext#getId()
	 */
	public String getId() {
		return CONTEXT_ID;
	}

	protected void flushModel(IDocument doc) {
	}
	
	protected boolean synchronizeModel(IDocument doc) {
		return true;
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
