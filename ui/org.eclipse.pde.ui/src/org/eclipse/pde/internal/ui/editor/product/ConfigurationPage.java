package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.*;


public class ConfigurationPage extends PDEFormPage {
	
	public static final String PAGE_ID = "configuration";

	public ConfigurationPage(FormEditor editor) {
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
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth = true;
		layout.marginWidth = 10;
		layout.verticalSpacing = 20;
		layout.horizontalSpacing = 10;
		body.setLayout(layout);

		// sections
		managedForm.addPart(new PluginSection(this, body));	
		managedForm.addPart(new ConfigurationSection(this, body));
	}
	

}
