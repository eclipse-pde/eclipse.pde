/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;
import java.util.ArrayList;

import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;

public class DependenciesPage extends PDEFormPage {
	
	public static final String PAGE_ID = "dependencies"; //$NON-NLS-1$
	
	public DependenciesPage(FormEditor editor) {
		super(editor, PAGE_ID, PDEUIMessages.DependenciesPage_tabName);  
	}
	
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		boolean isBundle = isBundle();
		ScrolledForm form = managedForm.getForm();
		form.setText(PDEUIMessages.DependenciesPage_title); 
		Composite body = form.getBody();
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 10;
		layout.verticalSpacing = 20;
		layout.horizontalSpacing = 10;
		layout.makeColumnsEqualWidth = isBundle;
		body.setLayout(layout);
		
		FormToolkit toolkit = managedForm.getToolkit();
		Composite left = toolkit.createComposite(body);
		left.setLayout(new GridLayout());
		left.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Composite right = toolkit.createComposite(body);
		right.setLayout(new GridLayout());
		right.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		managedForm.addPart(new RequiresSection(this, left, getRequiredSectionLabels()));		
		if (isBundle)
			managedForm.addPart(new ImportPackageSection(this, right));
		else
			managedForm.addPart(new MatchSection(this, right, true));
		
		DependencyAnalysisSection section;
		if (isBundle)
			section = new DependencyAnalysisSection(this, left, Section.COMPACT);
		else
			section = new DependencyAnalysisSection(this, right, Section.EXPANDED);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 150;
		section.getSection().setLayoutData(gd);	
		
		if (isBundle  && getModel().isEditable())
			managedForm.addPart(new SecondaryBundlesSection(this, right));
		
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.MANIFEST_PLUGIN_DEPENDENCIES);
	}
	
	private boolean isBundle() {
		return getPDEEditor().getContextManager().findContext(BundleInputContext.CONTEXT_ID) != null;
	}
    
    private String[] getRequiredSectionLabels() {
        ArrayList labels = new ArrayList();
        labels.add(PDEUIMessages.RequiresSection_add);
        labels.add(PDEUIMessages.RequiresSection_delete);
        labels.add(PDEUIMessages.RequiresSection_up);
        labels.add(PDEUIMessages.RequiresSection_down);
        if (isBundle())
            labels.add(PDEUIMessages.DependenciesPage_properties);
        return (String[])labels.toArray(new String[labels.size()]);
    }

}
