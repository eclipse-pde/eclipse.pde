/*
 * Created on Jan 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.editor.context;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.ui.*;


public abstract class UTF8InputContext extends InputContext {
	/**
	 * @param editor
	 * @param input
	 */
	public UTF8InputContext(PDEFormEditor editor, IEditorInput input, boolean primary) {
		super(editor, input, primary);
	}
	
	protected String getDefaultCharset() {
		return "UTF-8"; //$NON-NLS-1$
	}
	
}