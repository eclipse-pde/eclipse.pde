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

package org.eclipse.pde.internal.ui.wizards.plugin;

import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.*;

/**
 * @author cgwong
 */
public class FragmentContentPage extends ContentPage {

	public FragmentContentPage(String pageName, IProjectProvider provider,
			NewProjectCreationPage page, AbstractFieldData data) {
		super(pageName, provider, page, data, true);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.plugin.ContentPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(), IHelpContextIds.NEW_FRAGMENT_REQUIRED_DATA);
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.wizards.plugin.ContentPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createPropertyControls(Composite container) {
		Group propertiesGroup = new Group(container, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 5;
		propertiesGroup.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		propertiesGroup.setLayoutData(gd);
		propertiesGroup.setText(PDEPlugin.getResourceString("ContentPage.fGroup")); //$NON-NLS-1$

		Label label = new Label(propertiesGroup, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("ContentPage.fid")); //$NON-NLS-1$
		fIdText = createText(propertiesGroup, propertiesListener);

		label = new Label(propertiesGroup, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("ContentPage.fversion")); //$NON-NLS-1$
		fVersionText = createText(propertiesGroup, propertiesListener);

		label = new Label(propertiesGroup, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("ContentPage.fname")); //$NON-NLS-1$
		fNameText = createText(propertiesGroup, propertiesListener);

		label = new Label(propertiesGroup, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("ContentPage.fprovider")); //$NON-NLS-1$
		fProviderText = createText(propertiesGroup, propertiesListener);

		fLibraryLabel = new Label(propertiesGroup, SWT.NONE);
		fLibraryLabel
				.setText(PDEPlugin.getResourceString("ProjectStructurePage.library")); //$NON-NLS-1$
		fLibraryText = createText(propertiesGroup, propertiesListener);
		addFragmentSpecificControls(container);
	}

	/**
	 * @param container
	 */
	private void addFragmentSpecificControls(Composite container) {
		Group parentGroup = new Group(container, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 5;
		parentGroup.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		parentGroup.setLayoutData(gd);
		parentGroup.setText(PDEPlugin.getResourceString("ContentPage.parentPluginGroup")); //$NON-NLS-1$

		Label label = new Label(parentGroup, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("ContentPage.pid")); //$NON-NLS-1$
		createPluginIdContainer(parentGroup);
		label = new Label(parentGroup, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("ContentPage.pversion")); //$NON-NLS-1$
		fPluginVersion = createText(parentGroup, listener);
		label = new Label(parentGroup, SWT.NONE);
		label.setText(PDEPlugin.getResourceString(PDEPlugin
				.getResourceString("ContentPage.matchRule"))); //$NON-NLS-1$
		fMatchCombo = new Combo(parentGroup, SWT.READ_ONLY | SWT.BORDER);
		fMatchCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fMatchCombo.setItems(new String[]{"", //$NON-NLS-1$
				PDEPlugin.getResourceString(KEY_MATCH_EQUIVALENT),
				PDEPlugin.getResourceString(KEY_MATCH_COMPATIBLE),
				PDEPlugin.getResourceString(KEY_MATCH_PERFECT),
				PDEPlugin.getResourceString(KEY_MATCH_GREATER)});
		fMatchCombo.setText(fMatchCombo.getItem(0));
	}

	private void createPluginIdContainer(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = layout.marginWidth = 0;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fPluginIdText = createText(container, listener);

		Button browse = new Button(container, SWT.PUSH);
		browse.setText(PDEPlugin.getResourceString("ContentPage.browse")); //$NON-NLS-1$
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
}
