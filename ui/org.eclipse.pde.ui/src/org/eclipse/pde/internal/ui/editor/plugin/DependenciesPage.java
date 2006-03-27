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
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

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
		Composite left, right;
		if (isBundle && getModel().isEditable()) {
			FormToolkit toolkit = managedForm.getToolkit();
			left = toolkit.createComposite(body, SWT.NONE);
			left.setLayout(new GridLayout());
			left.setLayoutData(new GridData(GridData.FILL_BOTH));
			right = toolkit.createComposite(body, SWT.NONE);
			right.setLayout(new GridLayout());
			right.setLayoutData(new GridData(GridData.FILL_BOTH));
		} else {
			left = body;
			right = body;
		}
		
		managedForm.addPart(new RequiresSection(this, left, getRequiredSectionLabels()));		
		
		DependencyAnalysisSection section;
		GridData gd = new GridData(GridData.FILL_HORIZONTAL|GridData.VERTICAL_ALIGN_BEGINNING);
		gd.widthHint = 150;
		if (isBundle) {
			managedForm.addPart(new ImportPackageSection(this, right));
			if (getModel().isEditable()) 
				managedForm.addPart(new DependencyManagementSection(this, left));
			else 
				gd.horizontalSpan = 2;
			section = new DependencyAnalysisSection(this, right, ExpandableComposite.COMPACT);
		} else {
			managedForm.addPart(new MatchSection(this, right, true));
			section = new DependencyAnalysisSection(this, right, ExpandableComposite.EXPANDED);
		}
		section.getSection().setLayoutData(gd);
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
