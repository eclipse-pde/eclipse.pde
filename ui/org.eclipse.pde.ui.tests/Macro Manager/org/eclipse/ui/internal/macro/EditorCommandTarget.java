/*
 * Created on Dec 2, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.eclipse.ui.internal.macro;

import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.*;
import org.eclipse.ui.IEditorPart;

/**
 * @author dejan
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class EditorCommandTarget extends CommandTarget {
	/**
	 * @param widget
	 * @param context
	 */
	public EditorCommandTarget(Widget widget, IEditorPart editor) {
		super(widget, editor);
	}
	
	public IEditorPart getEditor() {
		return (IEditorPart)getContext();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.macro.CommandTarget#ensureVisible()
	 */
	public void ensureVisible() {
		IEditorPart editor = getEditor();
		IWorkbenchPage page = editor.getEditorSite().getPage();
		page.activate(editor);
	}
}