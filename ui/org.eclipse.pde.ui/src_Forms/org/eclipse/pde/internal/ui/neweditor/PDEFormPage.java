/*
 * Created on Jan 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor;

import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.forms.ManagedForm;
import org.eclipse.ui.forms.editor.*;
import org.eclipse.ui.forms.widgets.Form;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public abstract class PDEFormPage extends FormPage {
	/**
	 * @param editor
	 * @param id
	 * @param title
	 */
	public PDEFormPage(FormEditor editor, String id, String title) {
		super(editor, id, title);
	}
	
	protected void createFormContent(ManagedForm managedForm) {
		Form form = managedForm.getForm();
		form.setBackgroundImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_FORM_BANNER));
	}
	public PDEFormEditor getPDEEditor() {
		return (PDEFormEditor)getEditor();
	}
}