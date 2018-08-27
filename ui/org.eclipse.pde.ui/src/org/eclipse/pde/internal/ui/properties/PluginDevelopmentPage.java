/*******************************************************************************
 *  Copyright (c) 2005, 2016 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 507831
 *******************************************************************************/
package org.eclipse.pde.internal.ui.properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyPage;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * Top level PDE property page for projects.
 */
public class PluginDevelopmentPage extends PropertyPage {

	private Button fExtensionButton;
	private Button fEquinoxButton;

	public PluginDevelopmentPage() {
		noDefaultAndApplyButton();
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		Group group = new Group(composite, SWT.NONE);
		group.setText(PDEUIMessages.PluginDevelopmentPage_presentation);
		group.setLayout(new GridLayout());
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fExtensionButton = new Button(group, SWT.CHECK);
		fExtensionButton.setText(PDEUIMessages.PluginDevelopmentPage_extensions);

		fEquinoxButton = new Button(group, SWT.CHECK);
		fEquinoxButton.setText(PDEUIMessages.PluginDevelopmentPage_equinox);

		initialize();
		Dialog.applyDialogFont(composite);
		return composite;
	}

	private void initialize() {
		Preferences pref = getPreferences(getElement().getAdapter(IProject.class));
		if (pref != null) {
			fExtensionButton.setSelection(pref.getBoolean(ICoreConstants.EXTENSIONS_PROPERTY, true));
			fEquinoxButton.setSelection(pref.getBoolean(ICoreConstants.EQUINOX_PROPERTY, true));
		}
	}

	/**
	 * Returns the preference settings scoped to the given project if available
	 * @param project project to get preferences for
	 * @return preferences for the project or <code>null</code>
	 */
	private Preferences getPreferences(IProject project) {
		return new ProjectScope(project).getNode(PDECore.PLUGIN_ID);
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.PLUGIN_DEVELOPMENT_PROPERTY_PAGE);
	}

	@Override
	public boolean performOk() {
		Preferences pref = getPreferences(getElement().getAdapter(IProject.class));
		if (pref != null) {
			if (!fExtensionButton.getSelection())
				pref.putBoolean(ICoreConstants.EXTENSIONS_PROPERTY, false);
			else
				pref.remove(ICoreConstants.EXTENSIONS_PROPERTY);

			if (!fEquinoxButton.getSelection())
				pref.putBoolean(ICoreConstants.EQUINOX_PROPERTY, false);
			else
				pref.remove(ICoreConstants.EQUINOX_PROPERTY);

			try {
				pref.flush();
			} catch (BackingStoreException e) {
				PDEPlugin.logException(e);
			}
		}
		return super.performOk();
	}

}
