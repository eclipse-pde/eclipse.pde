/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.neweditor.build;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.internal.build.IXMLConstants;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.neweditor.PDEFormPage;
import org.eclipse.pde.internal.ui.neweditor.context.InputContext;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.help.WorkbenchHelp;

public class BuildPage extends PDEFormPage {
	public static final String FORM_TITLE = "BuildEditor.Form.title";
	public static final String PAGE_ID = "build";
	private BuildClasspathSection classpathSection;
	private BuildContentsSection srcSection;
	private BuildContentsSection binSection;
	private RuntimeInfoSection runtimeSection;
	
	private Button customButton;
	
	public BuildPage(FormEditor editor) {
		super(editor, PAGE_ID, "Build Configuration");
	}

	protected void createFormContent(IManagedForm mform) {
		super.createFormContent(mform);
		FormToolkit toolkit = mform.getToolkit();
		GridLayout layout = new GridLayout();
		ScrolledForm form = mform.getForm();
		form.setText(PDEPlugin.getResourceString(FORM_TITLE));
		layout.numColumns = 2;
		layout.marginWidth = 10;
		layout.horizontalSpacing = 15;
		layout.verticalSpacing = 10;
		layout.makeColumnsEqualWidth = true;
		form.getBody().setLayout(layout);

		customButton =
			toolkit.createButton(
				form.getBody(),
				getCustomText(),
				SWT.CHECK);
		customButton.setAlignment(SWT.LEFT);
		GridData gd = new GridData (GridData.FILL_HORIZONTAL);
		gd.horizontalSpan =1;
		customButton.setLayoutData(gd);
		
		Label label = toolkit.createLabel(form.getBody(), null);
		gd = new GridData (GridData.FILL_HORIZONTAL);
		gd.horizontalSpan =1;
		label.setLayoutData(gd);
		
		customButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean isCustom = customButton.getSelection();
				IBuildEntry customEntry = getCustomBuildEntry();
				setCustomEntryValue(customEntry, isCustom);
				handleCustomCheckState(isCustom);
			}
		});
		
		runtimeSection = new RuntimeInfoSection(this, form.getBody());
		gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		runtimeSection.getSection().setLayoutData(gd);
		
		binSection = new BinSection(this, form.getBody());
		gd = new GridData(GridData.FILL_BOTH);
		binSection.getSection().setLayoutData(gd);

		srcSection = new SrcSection(this, form.getBody());
		gd = new GridData(GridData.FILL_BOTH);
		srcSection.getSection().setLayoutData(gd);

		classpathSection = new BuildClasspathSection(this, form.getBody());
		gd = new GridData(GridData.FILL_HORIZONTAL|GridData.VERTICAL_ALIGN_BEGINNING);
		gd.horizontalSpan=2;
		//gd.widthHint = 100;
		//gd.heightHint = 100;
		classpathSection.getSection().setLayoutData(gd);
		
		mform.addPart(runtimeSection);
		mform.addPart(srcSection);
		mform.addPart(binSection);
		mform.addPart(classpathSection);

		handleCustomCheckState(getCustomSelection());
		WorkbenchHelp.setHelp(form, IHelpContextIds.BUILD_PAGE);
	}
	
	private IBuildModel getBuildModel() {
		InputContext context = getPDEEditor().getContextManager()
				.findContext(BuildInputContext.CONTEXT_ID);
		return (IBuildModel) context.getModel();
	}

	private IBuildEntry getCustomBuildEntry(){
		IBuildModel buildModel = getBuildModel();
		IBuildEntry customEntry =
			buildModel.getBuild().getEntry(IXMLConstants.PROPERTY_CUSTOM);
			
		if (customEntry!=null)
			return customEntry;
							
		try {
			customEntry =
				buildModel.getFactory().createEntry(IXMLConstants.PROPERTY_CUSTOM);
			buildModel.getBuild().add(customEntry);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		return customEntry;
	}
	
	private boolean getCustomSelection(){
		IBuildModel model = getBuildModel();
		IBuild build = model.getBuild();
		IBuildEntry customEntry = build.getEntry(IXMLConstants.PROPERTY_CUSTOM);
		if (customEntry ==null || customEntry.getTokens().length ==0)
			return false;
		return customEntry.getTokens()[0].equals("true"); 
	}
	
	private void handleCustomCheckState(boolean isCustom){
		if (isCustom) 
			disableAllSections();
		else 
			enableAllSections();
	}
	
	public void disableAllSections(){
		customButton.setSelection(true);
		runtimeSection.disableSection();
		binSection.disableSection();
		srcSection.disableSection();
		classpathSection.disableSection();
	}
	
	public void enableAllSections(){
		customButton.setSelection(false);
		runtimeSection.enableSection();
		binSection.enableSection();
		srcSection.enableSection();
		classpathSection.enableSection();
	}

	private void setCustomEntryValue(IBuildEntry customEntry, boolean isCustom){
		String[] tokens = customEntry.getTokens();
		try {
			if (tokens.length != 0) {
				for (int i = 0; i < tokens.length; i++)
					customEntry.removeToken(tokens[i]);
			}
			customEntry.addToken(isCustom ? "true" : "false");
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}	
	}
	
	public void setFocus() {
	}
	
	private String getCustomText() {
		return "Custom Build";
		/*
		IBuildModel buildModel = getBuildModel();
		IProject project = buildModel.getUnderlyingResource().getProject();
		IModel model = PDECore.getDefault().getWorkspaceModelManager().getWorkspaceModel(project);
		if (model instanceof IFeatureModel)
			return PDEPlugin.getResourceString("BuildPropertiesEditor.Custom.feature");
		if (model instanceof IPluginModel)
			return PDEPlugin.getResourceString("BuildPropertiesEditor.Custom.plugin");
		return PDEPlugin.getResourceString("BuildPropertiesEditor.Custom.fragment");
		*/
	}

}
