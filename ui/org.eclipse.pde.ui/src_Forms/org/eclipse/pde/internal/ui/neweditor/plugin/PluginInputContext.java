/*
 * Created on Jan 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin;

import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.pde.internal.ui.neweditor.PDEFormEditor;
import org.eclipse.ui.IEditorInput;

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
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.InputContext#createModel(org.eclipse.ui.IEditorInput)
	 */
	protected IModel createModel(IEditorInput input) {
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