/*
 * Created on Jan 26, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor;

import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.*;
import org.eclipse.ui.ide.IGotoMarker;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class PDESourcePage extends TextEditor implements IFormPage, IGotoMarker {
	private FormEditor editor;
	private int index;
	private String id;
	private String title;
	/**
	 * 
	 */
	public PDESourcePage(FormEditor editor, String id, String title) {
		this.id = id;
		this.title = title;
		initialize(editor);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.IFormPage#initialize(org.eclipse.ui.forms.editor.FormEditor)
	 */
	public void initialize(FormEditor editor) {
		this.editor = editor;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.IFormPage#getEditor()
	 */
	public FormEditor getEditor() {
		return editor;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.IFormPage#getManagedForm()
	 */
	public IManagedForm getManagedForm() {
		// not a form page
		return null;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.IFormPage#setActive(boolean)
	 */
	public void setActive(boolean active) {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.IFormPage#isActive()
	 */
	public boolean isActive() {
		return this.equals(editor.getActivePageInstance());
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.IFormPage#getPartControl()
	 */
	public Control getPartControl() {
		// TODO Auto-generated method stub
		return null;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.IFormPage#getId()
	 */
	public String getId() {
		return id;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.IFormPage#getIndex()
	 */
	public int getIndex() {
		return index;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.IFormPage#setIndex(int)
	 */
	public void setIndex(int index) {
		this.index = index;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.IFormPage#isSource()
	 */
	public boolean isSource() {
		return true;
	}
}
