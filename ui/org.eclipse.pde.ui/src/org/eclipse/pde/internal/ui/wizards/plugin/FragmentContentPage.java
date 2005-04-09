/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.wizards.plugin;

import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.*;

public class FragmentContentPage extends ContentPage {

	private Text fPluginIdText;
	private Text fPluginVersion;
	private Combo fMatchCombo;

	protected ModifyListener listener = new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			validatePage();
		}
	};
	
	public FragmentContentPage(String pageName, IProjectProvider provider,
			NewProjectCreationPage page, AbstractFieldData data) {
		super(pageName, provider, page, data);
		setTitle(PDEUIMessages.ContentPage_ftitle); //$NON-NLS-1$
		setDescription(PDEUIMessages.ContentPage_fdesc); //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.plugin.ContentPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 15;
		container.setLayout(layout);
		
		createFragmentPropertiesGroup(container);
		createParentPluginGroup(container);

		setControl(container);
		WorkbenchHelp.setHelp(getControl(), IHelpContextIds.NEW_FRAGMENT_REQUIRED_DATA);
	}

	public void createFragmentPropertiesGroup(Composite container) {
		Group propertiesGroup = new Group(container, SWT.NONE);
		propertiesGroup.setLayout(new GridLayout(2, false));
		propertiesGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		propertiesGroup.setText(PDEUIMessages.ContentPage_fGroup); //$NON-NLS-1$

		Label label = new Label(propertiesGroup, SWT.NONE);
		label.setText(PDEUIMessages.ContentPage_fid); //$NON-NLS-1$
		fIdText = createText(propertiesGroup, propertiesListener);

		label = new Label(propertiesGroup, SWT.NONE);
		label.setText(PDEUIMessages.ContentPage_fversion); //$NON-NLS-1$
		fVersionText = createText(propertiesGroup, propertiesListener);

		label = new Label(propertiesGroup, SWT.NONE);
		label.setText(PDEUIMessages.ContentPage_fname); //$NON-NLS-1$
		fNameText = createText(propertiesGroup, propertiesListener);

		label = new Label(propertiesGroup, SWT.NONE);
		label.setText(PDEUIMessages.ContentPage_fprovider); //$NON-NLS-1$
		fProviderText = createText(propertiesGroup, propertiesListener);

		fLibraryLabel = new Label(propertiesGroup, SWT.NONE);
		fLibraryLabel.setText(PDEUIMessages.ProjectStructurePage_library); //$NON-NLS-1$
		fLibraryText = createText(propertiesGroup, propertiesListener);
	}

	private void createParentPluginGroup(Composite container) {
		Group parentGroup = new Group(container, SWT.NONE);
		parentGroup.setLayout(new GridLayout(2, false));
		parentGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		parentGroup.setText(PDEUIMessages.ContentPage_parentPluginGroup); //$NON-NLS-1$

		Label label = new Label(parentGroup, SWT.NONE);
		label.setText(PDEUIMessages.FragmentContentPage_pid); //$NON-NLS-1$
		createPluginIdContainer(parentGroup);
		
		label = new Label(parentGroup, SWT.NONE);
		label.setText(PDEUIMessages.FragmentContentPage_pversion); //$NON-NLS-1$
		fPluginVersion = createText(parentGroup, listener);
		
		label = new Label(parentGroup, SWT.NONE);
		label.setText(PDEUIMessages.ContentPage_matchRule); //$NON-NLS-1$
		
		fMatchCombo = new Combo(parentGroup, SWT.READ_ONLY | SWT.BORDER);
		fMatchCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fMatchCombo.setItems(new String[]{"", //$NON-NLS-1$
				PDEUIMessages.ManifestEditor_MatchSection_equivalent,
				PDEUIMessages.ManifestEditor_MatchSection_compatible,
				PDEUIMessages.ManifestEditor_MatchSection_perfect,
				PDEUIMessages.ManifestEditor_MatchSection_greater});
		fMatchCombo.setText(fMatchCombo.getItem(0));
	}

	private void createPluginIdContainer(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = layout.marginWidth = 0;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fPluginIdText = createText(container, listener);

		Button browse = new Button(container, SWT.PUSH);
		browse.setText(PDEUIMessages.ContentPage_browse); //$NON-NLS-1$
		browse.setLayoutData(new GridData());
		browse.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				BusyIndicator.showWhile(fPluginIdText.getDisplay(), new Runnable() {
					public void run() {
						PluginSelectionDialog dialog = new PluginSelectionDialog(
								fPluginIdText.getShell(), false, false);
						dialog.create();
						if (dialog.open() == PluginSelectionDialog.OK) {
							IPluginModel model = (IPluginModel) dialog.getFirstResult();
							IPlugin plugin = model.getPlugin();
							fPluginIdText.setText(plugin.getId());
							fPluginVersion.setText(plugin.getVersion());
						}
					}
				});
			}
		});
		SWTUtil.setButtonDimensionHint(browse);
	}

	public void updateData() {
		super.updateData();
		((FragmentFieldData) fData).setPluginId(fPluginIdText.getText().trim());
		((FragmentFieldData) fData).setPluginVersion(fPluginVersion.getText().trim());
		((FragmentFieldData) fData).setMatch(fMatchCombo.getSelectionIndex());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.plugin.ContentPage#validatePage()
	 */
	protected void validatePage() {
		String errorMessage = validateProperties();
		if (errorMessage == null) {
			String pluginID = fPluginIdText.getText().trim();
			if (pluginID.length() == 0) {
				errorMessage = PDEUIMessages.ContentPage_nopid; //$NON-NLS-1$
			} else if (PDECore.getDefault().getModelManager().findEntry(pluginID) == null) {
				errorMessage = PDEUIMessages.ContentPage_pluginNotFound; //$NON-NLS-1$
			} else if (fPluginVersion.getText().trim().length() == 0) {
				errorMessage = PDEUIMessages.ContentPage_nopversion; //$NON-NLS-1$
			} else if (!isVersionValid(fPluginVersion.getText().trim())) {
				errorMessage = PDEUIMessages.ContentPage_badpversion; //$NON-NLS-1$
			}
		}
		if (fInitialized)
			setErrorMessage(errorMessage);
		setPageComplete(errorMessage == null);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.plugin.ContentPage#getNameFieldQualifier()
	 */
	protected String getNameFieldQualifier() {
		return PDEUIMessages.ContentPage_fragment; //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.plugin.ContentPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		if (visible)
			fMainPage.updateData();
		super.setVisible(visible);
	}
}
