/*
 * Created on Feb 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor;

import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.pde.internal.ui.newparts.*;
import org.eclipse.ui.forms.events.HyperlinkEvent;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class EditorEntryAdapter implements IFormEntryListener {
	protected PDEFormEditor editor;
	/**
	 * 
	 */
	public EditorEntryAdapter(PDEFormEditor editor) {
		this.editor = editor;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.newparts.IFormEntryListener#textDirty(org.eclipse.pde.internal.ui.newparts.FormEntry)
	 */
	public void textDirty(FormEntry entry) {
		editor.fireDirtyStateChanged();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.newparts.IFormEntryListener#textValueChanged(org.eclipse.pde.internal.ui.newparts.FormEntry)
	 */
	public void textValueChanged(FormEntry entry) {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.newparts.IFormEntryListener#browseButtonSelected(org.eclipse.pde.internal.ui.newparts.FormEntry)
	 */
	public void browseButtonSelected(FormEntry entry) {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.events.HyperlinkListener#linkEntered(org.eclipse.ui.forms.events.HyperlinkEvent)
	 */
	public void linkEntered(HyperlinkEvent e) {
		IStatusLineManager mng = editor.getEditorSite().getActionBars().getStatusLineManager();
		mng.setMessage(e.getLabel());
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.events.HyperlinkListener#linkExited(org.eclipse.ui.forms.events.HyperlinkEvent)
	 */
	public void linkExited(HyperlinkEvent e) {
		IStatusLineManager mng = editor.getEditorSite().getActionBars().getStatusLineManager();
		mng.setMessage(null);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.events.HyperlinkListener#linkActivated(org.eclipse.ui.forms.events.HyperlinkEvent)
	 */
	public void linkActivated(HyperlinkEvent e) {
	}
}