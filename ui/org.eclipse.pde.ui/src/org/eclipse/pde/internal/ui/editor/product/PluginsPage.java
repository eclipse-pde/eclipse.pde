package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.*;


public class PluginsPage extends PDEFormPage {
	
	public static final String PAGE_ID = "configuration";

	public PluginsPage(FormEditor editor) {
		super(editor, PAGE_ID, "Configuration");
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#createFormContent(org.eclipse.ui.forms.IManagedForm)
	 */
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		form.setText("Configuration"); 
		fillBody(managedForm, toolkit);
		managedForm.refresh();
	}

	private void fillBody(IManagedForm managedForm, FormToolkit toolkit) {
		Composite body = managedForm.getForm().getBody();
		TableWrapLayout layout = new TableWrapLayout();
		layout.bottomMargin = 10;
		layout.topMargin = 5;
		layout.leftMargin = 10;
		layout.rightMargin = 10;
		layout.numColumns = 2;
		layout.verticalSpacing = 30;
		layout.horizontalSpacing = 10;
		layout.makeColumnsEqualWidth = true;
		body.setLayout(layout);

		// sections
		managedForm.addPart(new PluginSection(this, body));	
		managedForm.addPart(new ExportSection(this, body));
	}
	

}
