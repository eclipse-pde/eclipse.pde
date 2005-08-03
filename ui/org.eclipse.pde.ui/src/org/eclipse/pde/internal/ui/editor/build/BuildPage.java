/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.build;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class BuildPage extends PDEFormPage {
	public static final String PAGE_ID = "build"; //$NON-NLS-1$
	private BuildClasspathSection classpathSection;
	private BuildContentsSection srcSection;
	private BuildContentsSection binSection;
	private RuntimeInfoSection runtimeSection;
	
	private Button customButton;
	
	public BuildPage(FormEditor editor) {
		super(editor, PAGE_ID, PDEUIMessages.BuildPage_name);  
	}

	protected void createFormContent(IManagedForm mform) {
		super.createFormContent(mform);
		FormToolkit toolkit = mform.getToolkit();
		GridLayout layout = new GridLayout();
		ScrolledForm form = mform.getForm();
		form.setText(PDEUIMessages.BuildEditor_BuildPage_title);
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
		gd = new GridData(GridData.FILL_HORIZONTAL);
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
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.BUILD_PAGE);
	}

	private IBuildModel getBuildModel() {
		InputContext context = getPDEEditor().getContextManager()
				.findContext(BuildInputContext.CONTEXT_ID);
		return (IBuildModel) context.getModel();
	}

	private IBuildEntry getCustomBuildEntry(){
		IBuildModel buildModel = getBuildModel();
		IBuildEntry customEntry =
			buildModel.getBuild().getEntry(IBuildPropertiesConstants.PROPERTY_CUSTOM);
			
		if (customEntry!=null)
			return customEntry;
							
		try {
			customEntry =
				buildModel.getFactory().createEntry(IBuildPropertiesConstants.PROPERTY_CUSTOM);
			buildModel.getBuild().add(customEntry);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		return customEntry;
	}
	
	private boolean getCustomSelection(){
		IBuildModel model = getBuildModel();
		IBuild build = model.getBuild();
		IBuildEntry customEntry = build.getEntry(IBuildPropertiesConstants.PROPERTY_CUSTOM);
		if (customEntry ==null || customEntry.getTokens().length ==0)
			return false;
		return customEntry.getTokens()[0].equals("true");  //$NON-NLS-1$
	}
	
	private void handleCustomCheckState(boolean isCustom){
		customButton.setSelection(isCustom);
		enableAllSections(!isCustom);
	}
	
	public void enableAllSections(boolean enable){
		runtimeSection.enableSection(enable);
		binSection.enableSection(enable);
		srcSection.enableSection(enable);
		classpathSection.enableSection(enable);
	}

	private void setCustomEntryValue(IBuildEntry customEntry, boolean isCustom){
		String[] tokens = customEntry.getTokens();
		try {
			if (tokens.length != 0) {
				for (int i = 0; i < tokens.length; i++)
					customEntry.removeToken(tokens[i]);
			}
			if (isCustom)
				customEntry.addToken("true"); //$NON-NLS-1$
			else
				getBuildModel().getBuild().remove(customEntry);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}	
	}
	
	private String getCustomText() {
		return PDEUIMessages.BuildPage_custom; 
	}

}
