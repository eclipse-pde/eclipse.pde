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
package org.eclipse.pde.internal.ui.editor.build;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.*;
import org.eclipse.update.ui.forms.internal.*;

public class BuildForm extends ScrollableSectionForm {
	public static final String FORM_TITLE = "BuildEditor.Form.title";

	private BuildPage page;
	private BuildClasspathSection classpathSection;
	private BuildContentsSection srcSection;
	private BuildContentsSection binSection;
	private RuntimeInfoSection runtimeSection;
	
	private Button customButton;
	public BuildForm(BuildPage page) {
		this.page = page;
		setScrollable(true);
		setVerticalFit(true);
	}

	protected void createFormClient(Composite parent) {
		FormWidgetFactory factory = getFactory();
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 10;
		layout.horizontalSpacing = 15;
		layout.verticalSpacing = 10;
		layout.makeColumnsEqualWidth = true;
		parent.setLayout(layout);


		
		customButton =
			factory.createButton(
				parent,
				getCustomText(),
				SWT.CHECK);
		customButton.setAlignment(SWT.LEFT);
		GridData gd = new GridData (GridData.FILL_HORIZONTAL);
		gd.horizontalSpan =1;
		customButton.setLayoutData(gd);
		
		Label label = new Label(parent, SWT.NULL);
		gd = new GridData (GridData.FILL_HORIZONTAL);
		gd.horizontalSpan =1;
		label.setBackground(parent.getBackground());
		label.setLayoutData(gd);
		
		customButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean isCustom = customButton.getSelection();
				IBuildEntry customEntry = getCustomBuildEntry();
				setCustomEntryValue(customEntry, isCustom);
				handleCustomCheckState(isCustom);
			}
		});
		
		runtimeSection = new RuntimeInfoSection(page);
		Control control = runtimeSection.createControl(parent, factory);
		gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		control.setLayoutData(gd);
		runtimeSection.setSectionControl(control);
		
		binSection = new BinSection(page);
		control = binSection.createControl(parent, factory);
		gd = new GridData(GridData.FILL_BOTH);
		control.setLayoutData(gd);
		binSection.setSectionControl(control);

		srcSection = new SrcSection(page);
		control = srcSection.createControl(parent, factory);
		gd = new GridData(GridData.FILL_BOTH);
		control.setLayoutData(gd);
		srcSection.setSectionControl(control);

		classpathSection = new BuildClasspathSection(page);
		control = classpathSection.createControl(parent, factory);
		gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan=2;
		gd.widthHint = 100;
		gd.heightHint = 100;
		control.setLayoutData(gd);
		classpathSection.setSectionControl(control);
		
		registerSection(runtimeSection);
		registerSection(srcSection);
		registerSection(binSection);
		registerSection(classpathSection);

		handleCustomCheckState(getCustomSelection());
		WorkbenchHelp.setHelp(parent, IHelpContextIds.BUILD_PAGE);
	}

	private IBuildEntry getCustomBuildEntry(){
		IBuildModel buildModel = (IBuildModel) page.getModel();
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
	
	public void dispose() {
		if (runtimeSection!=null){
			unregisterSection(runtimeSection);
			runtimeSection.dispose();
		}
		if (srcSection!=null){
			unregisterSection(srcSection);
			srcSection.dispose();
		}
		if (binSection!=null){
			unregisterSection(binSection);
			binSection.dispose();
		}
		if (classpathSection!=null){
			unregisterSection(classpathSection);
			classpathSection.dispose();
		}
	}


	private boolean getCustomSelection(){
		IBuildModel model = (IBuildModel)page.getModel();
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
	
	public void initialize(Object modelObject) {
		IBuildModel model = (IBuildModel) modelObject;
		super.initialize(model);
		setHeadingText(getText());
		((Composite) getControl()).layout(true);
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
	
	private String getText() {
		return "Build Configuration";
		/*IBuildModel buildModel = (IBuildModel)page.getModel();
		IProject project = buildModel.getUnderlyingResource().getProject();
		IModel model = PDECore.getDefault().getWorkspaceModelManager().getWorkspaceModel(project);
		String label = "";
		if (model instanceof IFeatureModel) {
			label = ((IFeatureModel)model).getFeature().getLabel();
			if (label == null || label.trim().length() == 0)
			label = ((IFeatureModel)model).getFeature().getId();
		} else {
			label = ((IPluginModelBase)model).getPluginBase().getName();
			if (label == null || label.trim().length() == 0)
			label = ((IPluginModelBase)model).getPluginBase().getId();
		}
		
		return label;*/
	}
	
	private String getCustomText() {
		IBuildModel buildModel = (IBuildModel)page.getModel();
		IProject project = buildModel.getUnderlyingResource().getProject();
		IModel model = PDECore.getDefault().getWorkspaceModelManager().getWorkspaceModel(project);
		if (model instanceof IFeatureModel)
			return PDEPlugin.getResourceString("BuildPropertiesEditor.Custom.feature");
		if (model instanceof IPluginModel)
			return PDEPlugin.getResourceString("BuildPropertiesEditor.Custom.plugin");
		return PDEPlugin.getResourceString("BuildPropertiesEditor.Custom.fragment");
	}

}
