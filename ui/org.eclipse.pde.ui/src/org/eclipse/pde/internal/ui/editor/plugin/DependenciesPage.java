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
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.editor.*;
import org.eclipse.ui.forms.widgets.*;

public class DependenciesPage extends PDEFormPage {
	
	public static final String PAGE_ID = "dependencies"; //$NON-NLS-1$
	
	public DependenciesPage(FormEditor editor) {
		super(editor, PAGE_ID, PDEPlugin.getResourceString("DependenciesPage.tabName"));  //$NON-NLS-1$
	}
	
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		form.setText(PDEPlugin.getResourceString("DependenciesPage.title")); //$NON-NLS-1$
		Composite body = form.getBody();
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth = true;
		layout.marginWidth = 10;
		layout.verticalSpacing = 20;
		layout.horizontalSpacing = 10;
		body.setLayout(layout);
		
		managedForm.addPart(new RequiresSection(this, body));		
		boolean isBundle = isBundle();
		if (isBundle)
			managedForm.addPart(new ImportPackageSection(this, body));
		else
			managedForm.addPart(new MatchSection(this, body, true));
		
		DependencyAnalysisSection section = new DependencyAnalysisSection(this, body, isBundle ? Section.COMPACT : Section.EXPANDED);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL|GridData.VERTICAL_ALIGN_BEGINNING);
		if (isBundle)
			gd.horizontalSpan = 2;
		section.getSection().setLayoutData(gd);
	}
	
	private boolean isBundle() {
		return getPDEEditor().getContextManager().findContext(BundleInputContext.CONTEXT_ID) != null;
	}

}
