/*
 * Created on Jan 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin;

import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.forms.ManagedForm;
import org.eclipse.ui.forms.editor.*;
import org.eclipse.ui.forms.widgets.*;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class DependenciesPage extends FormPage {
	/**
	 * @param editor
	 * @param id
	 * @param title
	 */
	public DependenciesPage(FormEditor editor) {
		super(editor, "dependencies", "Dependencies");
	}
	
	protected void createFormContent(ManagedForm managedForm) {
		Form form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		form.setText("Dependencies");
		form.setBackgroundImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_FORM_BANNER));
	}
}