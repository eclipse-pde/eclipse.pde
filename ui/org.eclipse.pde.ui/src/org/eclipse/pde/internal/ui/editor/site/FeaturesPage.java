/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.site;
import org.eclipse.pde.internal.core.isite.ISiteModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.swt.layout.*;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.*;
/**
 * 
 * Features page.
 */
public class FeaturesPage extends PDEFormPage {
	public static final String PAGE_ID = "features"; //$NON-NLS-1$
	private FeatureSection featureSection;
	private CategorySection categorySection;
	
	public FeaturesPage(PDEFormEditor editor) {
		super(editor, PAGE_ID, PDEPlugin.getResourceString("SiteEditor.page1")); //$NON-NLS-1$
	}
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = mform.getForm();
		GridLayout layout = new GridLayout();
		form.getBody().setLayout(layout);
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth = true;
		layout.horizontalSpacing = 12;
		layout.marginWidth = 10;
		featureSection = new FeatureSection(this, form.getBody());
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 250;
		featureSection.getSection().setLayoutData(gd);
		categorySection = new CategorySection(this, form.getBody());
		gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 250;
		categorySection.getSection().setLayoutData(gd);
		
		mform.addPart(featureSection);
		mform.addPart(categorySection);
		//WorkbenchHelp.setHelp(form.getBody(),
		// IHelpContextIds.MANIFEST_SITE_OVERVIEW);
		ISiteModel model = (ISiteModel) getModel();
		IEditorInput input = getEditor().getEditorInput();
		String name = input.getName();
		form.setText(model.getResourceString(name));
	}
}
