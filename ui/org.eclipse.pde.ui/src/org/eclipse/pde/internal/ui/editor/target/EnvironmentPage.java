package org.eclipse.pde.internal.ui.editor.target;

import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class EnvironmentPage extends PDEFormPage {
	
	public static final String PAGE_ID = "environment"; //$NON-NLS-1$

	public EnvironmentPage(FormEditor editor) {
		super(editor, PAGE_ID, PDEUIMessages.EnvironmentPage_title); 
	}
	
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		form.setText(PDEUIMessages.EnvironmentPage_title);
		FormToolkit toolkit = managedForm.getToolkit();
		fillBody(managedForm, toolkit);
	}
	
	private void fillBody(IManagedForm managedForm, FormToolkit toolkit) {
		Composite body = managedForm.getForm().getBody();
		GridLayout layout = new GridLayout();
		layout.marginBottom = 10;
		layout.marginTop = 5;
		layout.marginLeft = 10;
		layout.marginRight = 10;
		layout.verticalSpacing = 15;
		layout.horizontalSpacing = 10;
		layout.numColumns = 2;
		body.setLayout(layout);
		
		managedForm.addPart(new EnvironmentSection(this, body));
		managedForm.addPart(new JRESection(this, body));
		managedForm.addPart(new ArgumentsSection(this, body));
		managedForm.addPart(new ImplicitDependenciesSection(this, body));
	}
	
}
