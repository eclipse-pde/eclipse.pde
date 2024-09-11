/*******************************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
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
 *     Axel Richard (Obeo) - Bug 41353 - Launch configurations prototypes
 *******************************************************************************/
package org.eclipse.pde.ui.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.SWTFactory;
import org.eclipse.pde.internal.ui.launcher.BlockAdapter;
import org.eclipse.pde.internal.ui.launcher.FeatureBlock;
import org.eclipse.pde.internal.ui.launcher.PluginBlock;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.PlatformUI;

/**
 * A launch configuration tab that displays the different self-hosting modes,
 * and lets the user customize the list of plug-ins to launch with.
 * <p>
 * This class may be instantiated. This class is not intended to be subclassed by clients.
 * </p>
 * @since 3.2
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
public class PluginsTab extends AbstractLauncherTab {

	private final Image fImage;

	private Combo fSelectionCombo;
	private final BlockAdapter fBlock;
	private Combo fDefaultAutoStart;
	private Spinner fDefaultStartLevel;
	private final Listener fListener;
	private boolean fActivated;

	private static final int DEFAULT_SELECTION = 0;
	private static final int PLUGIN_SELECTION = 1;
	private static final int FEATURE_SELECTION = 2;

	class Listener extends SelectionAdapter implements ModifyListener {
		@Override
		public void widgetSelected(SelectionEvent e) {
			int index = fSelectionCombo.getSelectionIndex();
			try {
				fBlock.setActiveBlock(index);
				fBlock.initialize(index == PLUGIN_SELECTION);
			} catch (CoreException ex) {
				PDEPlugin.log(ex);
			}
			updateLaunchConfigurationDialog();
		}

		@Override
		public void modifyText(ModifyEvent e) {
			updateLaunchConfigurationDialog();
		}
	}

	/**
	 * Constructor. Equivalent to PluginsTab(true).
	 *
	 * @see #PluginsTab(boolean)
	 */
	public PluginsTab() {
		super();
		fImage = PDEPluginImages.DESC_PLUGINS_FRAGMENTS.createImage();
		fBlock = new BlockAdapter(new PluginBlock(this), new FeatureBlock(this));
		fListener = new Listener();
	}

	/**
	 * Constructor
	 *
	 * @param showFeatures  a flag indicating if the tab should present the feature-based
	 * self-hosting option.
	 * @deprecated As of 3.6 the feature-based workspace launch option is no longer available, so there is no need to set this flag
	 */
	@Deprecated
	public PluginsTab(boolean showFeatures) {
		this();
	}

	@Override
	public void dispose() {
		fBlock.dispose();
		fImage.dispose();
		super.dispose();
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_HORIZONTAL);

		Composite buttonComp = SWTFactory.createComposite(composite, 6, 1, GridData.FILL_HORIZONTAL, 0, 0);

		SWTFactory.createLabel(buttonComp, PDEUIMessages.PluginsTab_launchWith, 1);

		fSelectionCombo = SWTFactory.createCombo(buttonComp, SWT.READ_ONLY | SWT.BORDER, 1, GridData.HORIZONTAL_ALIGN_BEGINNING, new String[] {PDEUIMessages.PluginsTab_allPlugins, PDEUIMessages.PluginsTab_selectedPlugins, PDEUIMessages.PluginsTab_customFeatureMode});
		fSelectionCombo.select(DEFAULT_SELECTION);
		fSelectionCombo.addSelectionListener(fListener);

		Label label = SWTFactory.createLabel(buttonComp, PDEUIMessages.EquinoxPluginsTab_defaultStart, 1);
		GridData gd = new GridData();
		gd.horizontalIndent = 20;
		label.setLayoutData(gd);

		fDefaultStartLevel = new Spinner(buttonComp, SWT.BORDER);
		fDefaultStartLevel.setMinimum(1);
		fDefaultStartLevel.addModifyListener(fListener);

		label = SWTFactory.createLabel(buttonComp, PDEUIMessages.EquinoxPluginsTab_defaultAuto, 1);
		gd = new GridData();
		gd.horizontalIndent = 20;
		label.setLayoutData(gd);

		fDefaultAutoStart = SWTFactory.createCombo(buttonComp, SWT.BORDER | SWT.READ_ONLY, 1, GridData.HORIZONTAL_ALIGN_BEGINNING, new String[] {Boolean.toString(true), Boolean.toString(false)});
		fDefaultAutoStart.select(0);
		fDefaultAutoStart.addSelectionListener(fListener);

		Label separator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fBlock.createControl(composite, 7, 10);

		setControl(composite);
		Dialog.applyDialogFont(composite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.LAUNCHER_ADVANCED);
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		// Long-running initialization happens on first activation of this tab
	}

	@Override
	public void activated(ILaunchConfigurationWorkingCopy configuration) {
		if (fActivated) {
			// Since this method can be expensive, only activate this tab once.
			return;
		}

		try {
			int index = DEFAULT_SELECTION;
			if (configuration.getAttribute(IPDELauncherConstants.USE_CUSTOM_FEATURES, false)) {
				index = FEATURE_SELECTION;
			} else if (!configuration.getAttribute(IPDELauncherConstants.USE_DEFAULT, true)) {
				index = PLUGIN_SELECTION;
			}
			fSelectionCombo.select(index);
			fBlock.setActiveBlock(index);
			boolean custom = fSelectionCombo.getSelectionIndex() == PLUGIN_SELECTION;
			fBlock.initializeFrom(configuration, custom);
			boolean auto = configuration.getAttribute(IPDELauncherConstants.DEFAULT_AUTO_START, false);
			fDefaultAutoStart.setText(Boolean.toString(auto));
			int level = configuration.getAttribute(IPDELauncherConstants.DEFAULT_START_LEVEL, 4);
			fDefaultStartLevel.setSelection(level);

			// If everything ran smoothly, this tab is activated
			fActivated = true;
		} catch (CoreException e) {
			PDEPlugin.log(e);
		}
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(IPDELauncherConstants.USE_DEFAULT, true);
		configuration.removeAttribute(IPDELauncherConstants.USE_CUSTOM_FEATURES);
		fBlock.setDefaults(configuration);
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		if (!fActivated) {
			return;
		}
		int index = fSelectionCombo.getSelectionIndex();
		configuration.setAttribute(IPDELauncherConstants.USE_DEFAULT, index == DEFAULT_SELECTION);
		boolean useCustomFeatures = index == FEATURE_SELECTION;
		if (useCustomFeatures) {
			configuration.setAttribute(IPDELauncherConstants.USE_CUSTOM_FEATURES, true);
		} else {
			configuration.removeAttribute(IPDELauncherConstants.USE_CUSTOM_FEATURES);
		}
		fBlock.performApply(configuration);
		// clear default values for auto-start and start-level if default
		String autoText = fDefaultAutoStart.getText();
		if (Boolean.toString(false).equals(autoText)) {
			// clear, this is the default value
			configuration.setAttribute(IPDELauncherConstants.DEFAULT_AUTO_START, (String) null);
		} else {
			// persist non-default setting
			configuration.setAttribute(IPDELauncherConstants.DEFAULT_AUTO_START, true);
		}
		int startLevel = fDefaultStartLevel.getSelection();
		if (4 == startLevel) {
			// clear, this is the default value
			configuration.setAttribute(IPDELauncherConstants.DEFAULT_START_LEVEL, (String) null);
		} else {
			configuration.setAttribute(IPDELauncherConstants.DEFAULT_START_LEVEL, startLevel);
		}
	}

	@Override
	public String getName() {
		return PDEUIMessages.AdvancedLauncherTab_name;
	}

	@Override
	public Image getImage() {
		return fImage;
	}

	@Override
	public void validateTab() {
		setErrorMessage(null);
	}

	@Override
	public String getId() {
		return IPDELauncherConstants.TAB_PLUGINS_ID;
	}

	@Override
	protected void initializeAttributes() {
		super.initializeAttributes();
		getAttributesLabelsForPrototype().put(IPDELauncherConstants.USE_DEFAULT, PDEUIMessages.PluginsTab_AttributeLabel_UseDefault);
		getAttributesLabelsForPrototype().put(IPDELauncherConstants.INCLUDE_OPTIONAL, PDEUIMessages.PluginsTab_AttributeLabel_IncludeOptional);
		getAttributesLabelsForPrototype().put(IPDELauncherConstants.AUTOMATIC_ADD, PDEUIMessages.PluginsTab_AttributeLabel_AutomaticAdd);
		getAttributesLabelsForPrototype().put(IPDELauncherConstants.USE_CUSTOM_FEATURES, PDEUIMessages.PluginsTab_AttributeLabel_UseCustomFeatures);
		getAttributesLabelsForPrototype().put(IPDELauncherConstants.DEFAULT_AUTO_START, PDEUIMessages.PluginsTab_AttributeLabel_DefaultAutoStart);
		getAttributesLabelsForPrototype().put(IPDELauncherConstants.DEFAULT_START_LEVEL, PDEUIMessages.PluginsTab_AttributeLabel_DefaultStartLevel);
		getAttributesLabelsForPrototype().put(IPDELauncherConstants.DESELECTED_WORKSPACE_BUNDLES,
				PDEUIMessages.PluginsTab_AttributeLabel_DeselectedWorkspacePlugins);
		getAttributesLabelsForPrototype().put(IPDELauncherConstants.SELECTED_WORKSPACE_BUNDLES,
				PDEUIMessages.PluginsTab_AttributeLabel_SelectedWorkspacePlugins);
		getAttributesLabelsForPrototype().put(IPDELauncherConstants.SELECTED_TARGET_BUNDLES,
				PDEUIMessages.PluginsTab_AttributeLabel_SelectedTargetPlugins);
		getAttributesLabelsForPrototype().put(IPDELauncherConstants.FEATURE_PLUGIN_RESOLUTION, PDEUIMessages.PluginsTab_AttributeLabel_FeaturePluginResolution);
		getAttributesLabelsForPrototype().put(IPDELauncherConstants.FEATURE_DEFAULT_LOCATION, PDEUIMessages.PluginsTab_AttributeLabel_FeatureDefaultLocation);
		getAttributesLabelsForPrototype().put(IPDELauncherConstants.AUTOMATIC_VALIDATE, PDEUIMessages.PluginsTab_AttributeLabel_AutomaticValidate);
		getAttributesLabelsForPrototype().put(IPDELauncherConstants.SHOW_SELECTED_ONLY, PDEUIMessages.PluginsTab_AttributeLabel_OnlyShowSelected);
		getAttributesLabelsForPrototype().put(IPDELauncherConstants.SELECTED_FEATURES, PDEUIMessages.PluginsTab_AttributeLabel_SelectedFeatures);
		getAttributesLabelsForPrototype().put(IPDELauncherConstants.ADDITIONAL_PLUGINS, PDEUIMessages.PluginsTab_AttributeLabel_AdditionalPlugins);
	}
}
