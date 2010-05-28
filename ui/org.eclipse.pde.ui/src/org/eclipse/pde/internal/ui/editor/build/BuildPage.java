/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class BuildPage extends PDEFormPage {
	public static final String PAGE_ID = "build"; //$NON-NLS-1$
	private BuildClasspathSection fClasspathSection;
	private BuildContentsSection fSrcSection;
	private BuildContentsSection fBinSection;
	private RuntimeInfoSection fRuntimeSection;

	private Button fCustomButton;

	public BuildPage(FormEditor editor) {
		super(editor, PAGE_ID, PDEUIMessages.BuildPage_name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#getHelpResource()
	 */
	protected String getHelpResource() {
		return IHelpContextIds.BUILD_PAGE;
	}

	protected void createFormContent(IManagedForm mform) {
		super.createFormContent(mform);
		FormToolkit toolkit = mform.getToolkit();
		ScrolledForm form = mform.getForm();
		form.setImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_BUILD_EXEC));
		form.setText(PDEUIMessages.BuildEditor_BuildPage_title);
		form.getBody().setLayout(FormLayoutFactory.createFormGridLayout(true, 2));

		fCustomButton = toolkit.createButton(form.getBody(), getCustomText(), SWT.CHECK);
		fCustomButton.setAlignment(SWT.LEFT);

		Label label = toolkit.createLabel(form.getBody(), null);
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fCustomButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean isCustom = fCustomButton.getSelection();
				IBuildEntry customEntry = getCustomBuildEntry();
				setCustomEntryValue(customEntry, isCustom);
				handleCustomCheckState(isCustom);
			}
		});

		fRuntimeSection = new RuntimeInfoSection(this, form.getBody());

		fBinSection = new BinSection(this, form.getBody());
		fBinSection.getSection().setLayoutData(new GridData(GridData.FILL_BOTH));

		fSrcSection = new SrcSection(this, form.getBody());
		fSrcSection.getSection().setLayoutData(new GridData(GridData.FILL_BOTH));

		fClasspathSection = new BuildClasspathSection(this, form.getBody());

		mform.addPart(fRuntimeSection);
		mform.addPart(fSrcSection);
		mform.addPart(fBinSection);
		mform.addPart(fClasspathSection);

		handleCustomCheckState(getCustomSelection());
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.BUILD_PAGE);
	}

	private IBuildModel getBuildModel() {
		InputContext context = getPDEEditor().getContextManager().findContext(BuildInputContext.CONTEXT_ID);
		return (IBuildModel) context.getModel();
	}

	private IBuildEntry getCustomBuildEntry() {
		IBuildModel buildModel = getBuildModel();
		IBuildEntry customEntry = buildModel.getBuild().getEntry(IBuildPropertiesConstants.PROPERTY_CUSTOM);

		if (customEntry != null)
			return customEntry;

		try {
			customEntry = buildModel.getFactory().createEntry(IBuildPropertiesConstants.PROPERTY_CUSTOM);
			buildModel.getBuild().add(customEntry);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		return customEntry;
	}

	private boolean getCustomSelection() {
		IBuildModel model = getBuildModel();
		IBuild build = model.getBuild();
		IBuildEntry customEntry = build.getEntry(IBuildPropertiesConstants.PROPERTY_CUSTOM);
		if (customEntry == null || customEntry.getTokens().length == 0)
			return false;
		return customEntry.getTokens()[0].equals("true"); //$NON-NLS-1$
	}

	private void handleCustomCheckState(boolean isCustom) {
		fCustomButton.setSelection(isCustom);
		enableAllSections(!isCustom);
	}

	public void enableAllSections(boolean enable) {
		fRuntimeSection.enableSection(enable);
		fBinSection.enableSection(enable);
		fSrcSection.enableSection(enable);
		fClasspathSection.enableSection(enable);
	}

	private void setCustomEntryValue(IBuildEntry customEntry, boolean isCustom) {
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
