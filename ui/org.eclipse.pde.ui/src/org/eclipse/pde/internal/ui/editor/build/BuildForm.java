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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.core.plugin.*;
//import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.build.IXMLConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;
import org.eclipse.update.ui.forms.internal.ScrollableSectionForm;

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


		boolean isCustom = getCustomSelection();
		customButton =
			factory.createButton(
				parent,
				getCustomText(),
				SWT.CHECK);
		customButton.setAlignment(SWT.LEFT);
		GridData gd = new GridData (GridData.FILL_HORIZONTAL);
		gd.horizontalSpan =2;
		customButton.setLayoutData(gd);
		customButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean isCustom = customButton.getSelection();
				IBuildModel buildModel = (IBuildModel) page.getModel();
				IBuildEntry customEntry =
					buildModel.getBuild().getEntry(
						IXMLConstants.PROPERTY_CUSTOM);
				try {
					if (customEntry == null) {
						customEntry =
							buildModel.getFactory().createEntry(
								IXMLConstants.PROPERTY_CUSTOM);
						buildModel.getBuild().add(customEntry);
					}
					String[] tokens = customEntry.getTokens();
					if (tokens.length != 0) {
						for (int i = 0; i < tokens.length; i++)
							customEntry.removeToken(tokens[i]);
					}
					customEntry.addToken(isCustom ? "true" : "false");
					if (isCustom) {
						disableAllSections();
					} else {
						enableAllSections();
					}
				} catch (CoreException e1) {
					PDEPlugin.logException(e1);
				}
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
		gd.heightHint = 150;
		control.setLayoutData(gd);
		binSection.setSectionControl(control);

		srcSection = new SrcSection(page);
		control = srcSection.createControl(parent, factory);
		gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 150;
		control.setLayoutData(gd);
		srcSection.setSectionControl(control);

		classpathSection = new BuildClasspathSection(page);
		control = classpathSection.createControl(parent, factory);
		gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		control.setLayoutData(gd);
		classpathSection.setSectionControl(control);

		registerSection(runtimeSection);
		registerSection(srcSection);
		registerSection(binSection);
		registerSection(classpathSection);

		if (isCustom)
			disableAllSections();
		WorkbenchHelp.setHelp(parent, IHelpContextIds.BUILD_PAGE);
	}

	public void dispose() {
		unregisterSection(runtimeSection);
		unregisterSection(srcSection);
		unregisterSection(binSection);
		unregisterSection(classpathSection);
		binSection.dispose();
		srcSection.dispose();
		runtimeSection.dispose();
		classpathSection.dispose();
	}


	private boolean getCustomSelection(){
		IBuildModel model = (IBuildModel)page.getModel();
		IBuild build = model.getBuild();
		IBuildEntry customEntry = build.getEntry(IXMLConstants.PROPERTY_CUSTOM);
		if (customEntry ==null || customEntry.getTokens().length ==0)
			return false;
		return customEntry.getTokens()[0].equals("true"); 
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
