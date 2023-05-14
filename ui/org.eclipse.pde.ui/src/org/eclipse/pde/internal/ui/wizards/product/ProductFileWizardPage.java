/*******************************************************************************
 * Copyright (c) 2005, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 507831
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.product;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.wizards.PDEWizardNewFileCreationPage;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.pde.ui.launcher.EclipseLaunchShortcut;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.PlatformUI;

public class ProductFileWizardPage extends PDEWizardNewFileCreationPage {

	public final static int USE_DEFAULT = 0;
	public final static int USE_PRODUCT = 1;
	public final static int USE_LAUNCH_CONFIG = 2;

	private static final String F_FILE_EXTENSION = "product"; //$NON-NLS-1$

	private Button fBasicButton;
	private Button fProductButton;
	private Combo fProductCombo;
	private Button fLaunchConfigButton;
	private Combo fLaunchConfigCombo;
	private Group fGroup;

	private IPluginModelBase fModel;

	public ProductFileWizardPage(String pageName, IStructuredSelection selection) {
		super(pageName, selection);
		setDescription(PDEUIMessages.ProductFileWizadPage_title);
		setTitle(PDEUIMessages.NewProductFileWizard_title);
		// Force the file extension to be 'product'
		setFileExtension(F_FILE_EXTENSION);

		initializeModel(selection);
	}

	private void initializeModel(IStructuredSelection selection) {
		Object selected = selection.getFirstElement();
		if (selected instanceof IAdaptable) {
			IResource resource = ((IAdaptable) selected).getAdapter(IResource.class);
			if (resource != null) {
				IProject project = resource.getProject();
				fModel = PluginRegistry.findModel(project);
			}
		}
	}

	@Override
	protected void createAdvancedControls(Composite parent) {
		fGroup = new Group(parent, SWT.NONE);
		fGroup.setText(PDEUIMessages.ProductFileWizadPage_groupTitle);
		fGroup.setLayout(new GridLayout(2, false));
		fGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fBasicButton = new Button(fGroup, SWT.RADIO);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		fBasicButton.setLayoutData(gd);
		fBasicButton.setText(PDEUIMessages.ProductFileWizadPage_basic);

		fProductButton = new Button(fGroup, SWT.RADIO);
		fProductButton.setText(PDEUIMessages.ProductFileWizadPage_existingProduct);
		fProductButton.addSelectionListener(widgetSelectedAdapter(e -> fProductCombo.setEnabled(fProductButton.getSelection())));

		fProductCombo = new Combo(fGroup, SWT.SINGLE | SWT.READ_ONLY);
		fProductCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fProductCombo.setItems(TargetPlatform.getProducts());

		fLaunchConfigButton = new Button(fGroup, SWT.RADIO);
		fLaunchConfigButton.setText(PDEUIMessages.ProductFileWizadPage_existingLaunchConfig);
		fLaunchConfigButton.addSelectionListener(widgetSelectedAdapter(e -> fLaunchConfigCombo.setEnabled(fLaunchConfigButton.getSelection())));

		fLaunchConfigCombo = new Combo(fGroup, SWT.SINGLE | SWT.READ_ONLY);
		fLaunchConfigCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fLaunchConfigCombo.setItems(getLaunchConfigurations());

		initializeState();
	}

	private void initializeState() {
		fLaunchConfigCombo.setEnabled(false);
		if (fLaunchConfigCombo.getItemCount() > 0)
			fLaunchConfigCombo.setText(fLaunchConfigCombo.getItem(0));

		if (fModel != null && fModel.getPluginBase().getId() != null) {
			IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
			for (IPluginExtension extension : extensions) {
				String point = extension.getPoint();
				if ("org.eclipse.core.runtime.products".equals(point)) { //$NON-NLS-1$
					String id = extension.getId();
					if (id != null) {
						String full = fModel.getPluginBase().getId() + "." + id; //$NON-NLS-1$
						if (fProductCombo.indexOf(full) != -1) {
							fProductCombo.setText(full);
							fProductButton.setSelection(true);
							return;
						}
					}
				}
			}
		}

		fBasicButton.setSelection(true);

		fProductCombo.setEnabled(false);
		if (fProductCombo.getItemCount() > 0)
			fProductCombo.setText(fProductCombo.getItem(0));

	}

	private String[] getLaunchConfigurations() {
		ArrayList<String> list = new ArrayList<>();
		try {
			ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
			ILaunchConfigurationType type = manager.getLaunchConfigurationType(EclipseLaunchShortcut.CONFIGURATION_TYPE);
			ILaunchConfiguration[] configs = manager.getLaunchConfigurations(type);
			for (int i = 0; i < configs.length; i++) {
				if (!DebugUITools.isPrivate(configs[i]))
					list.add(configs[i].getName());
			}
			// add osgi launch configs to the list
			type = manager.getLaunchConfigurationType(IPDELauncherConstants.OSGI_CONFIGURATION_TYPE);
			configs = manager.getLaunchConfigurations(type);
			for (int i = 0; i < configs.length; i++) {
				if (!DebugUITools.isPrivate(configs[i]))
					list.add(configs[i].getName());
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		return list.toArray(new String[list.size()]);
	}

	public ILaunchConfiguration getSelectedLaunchConfiguration() {
		if (!fLaunchConfigButton.getSelection())
			return null;

		String configName = fLaunchConfigCombo.getText();
		try {
			ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
			ILaunchConfigurationType type = manager.getLaunchConfigurationType(EclipseLaunchShortcut.CONFIGURATION_TYPE);
			ILaunchConfigurationType type2 = manager.getLaunchConfigurationType(IPDELauncherConstants.OSGI_CONFIGURATION_TYPE);
			ILaunchConfiguration[] configs = manager.getLaunchConfigurations(type);
			ILaunchConfiguration[] configs2 = manager.getLaunchConfigurations(type2);
			ILaunchConfiguration[] configurations = new ILaunchConfiguration[configs.length + configs2.length];
			System.arraycopy(configs, 0, configurations, 0, configs.length);
			System.arraycopy(configs2, 0, configurations, configs.length, configs2.length);
			for (int i = 0; i < configurations.length; i++) {
				if (configurations[i].getName().equals(configName) && !DebugUITools.isPrivate(configurations[i]))
					return configurations[i];
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		return null;
	}

	public String getSelectedProduct() {
		return fProductButton.getSelection() ? fProductCombo.getText() : null;
	}

	public int getInitializationOption() {
		if (fBasicButton.getSelection())
			return USE_DEFAULT;
		if (fProductButton.getSelection())
			return USE_PRODUCT;
		return USE_LAUNCH_CONFIG;
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		Dialog.applyDialogFont(fGroup);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.PRODUCT_FILE_PAGE);
	}

}
