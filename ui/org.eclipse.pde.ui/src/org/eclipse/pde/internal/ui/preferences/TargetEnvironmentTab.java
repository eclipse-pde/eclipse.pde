/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.preferences;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.pde.core.IModelProviderEvent;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.itarget.*;
import org.eclipse.pde.internal.core.util.VMUtil;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.launcher.VMHelper;
import org.eclipse.pde.internal.ui.util.LocaleUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

public class TargetEnvironmentTab {
	private Combo fOSCombo;
	private Combo fWSCombo;
	private Combo fNLCombo;
	private Combo fArchCombo;

	private Preferences preferences;
	private TreeSet fNLChoices;
	private TreeSet fOSChoices;
	private TreeSet fWSChoices;
	private TreeSet fArchChoices;
	private Combo fJRECombo;

	private static boolean LOCALES_INITIALIZED = false;
	private String fDefaultJRE;
	private TargetPlatformPreferencePage fPage;

	public TargetEnvironmentTab(TargetPlatformPreferencePage page) {
		fPage = page;
		preferences = PDECore.getDefault().getPluginPreferences();
	}

	private void initializeChoices() {
		fOSChoices = new TreeSet();
		String[] os = Platform.knownOSValues();
		for (int i = 0; i < os.length; i++)
			fOSChoices.add(os[i]);
		addExtraChoices(fOSChoices, preferences.getString(ICoreConstants.OS_EXTRA));

		fWSChoices = new TreeSet();
		String[] ws = Platform.knownWSValues();
		for (int i = 0; i < ws.length; i++)
			fWSChoices.add(ws[i]);
		addExtraChoices(fWSChoices, preferences.getString(ICoreConstants.WS_EXTRA));

		fArchChoices = new TreeSet();
		String[] arch = Platform.knownOSArchValues();
		for (int i = 0; i < arch.length; i++)
			fArchChoices.add(arch[i]);
		addExtraChoices(fArchChoices, preferences.getString(ICoreConstants.ARCH_EXTRA));

		fNLChoices = new TreeSet();
		if (LOCALES_INITIALIZED) {
			initializeAllLocales();
		} else {
			fNLChoices.add(LocaleUtil.expandLocaleName(preferences.getString(ICoreConstants.NL)));
		}
	}

	protected void updateChoices() {
		if (LOCALES_INITIALIZED)
			return;
		final String current = fNLCombo.getText();
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					initializeAllLocales();
					LOCALES_INITIALIZED = true;
				}
			});
		} catch (InvocationTargetException e) {
			PDEPlugin.log(e);
		} catch (InterruptedException e) {
			PDEPlugin.log(e);
		}
		if (!fNLCombo.isDisposed()) {
			fNLCombo.setItems((String[]) fNLChoices.toArray(new String[fNLChoices.size()]));
			fNLCombo.setText(current);
		}
	}

	private void initializeAllLocales() {
		String[] nl = LocaleUtil.getLocales();
		for (int i = 0; i < nl.length; i++)
			fNLChoices.add(nl[i]);
		addExtraChoices(fNLChoices, preferences.getString(ICoreConstants.NL_EXTRA));
	}

	private void addExtraChoices(Set set, String preference) {
		StringTokenizer tokenizer = new StringTokenizer(preference, ","); //$NON-NLS-1$
		while (tokenizer.hasMoreTokens()) {
			set.add(tokenizer.nextToken().trim());
		}
	}

	public Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 15;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		createTargetEnvironmentGroup(container);
		createJREGroup(container);

		Dialog.applyDialogFont(container);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.TARGET_ENVIRONMENT_PREFERENCE_PAGE);
		return container;
	}

	private void createJREGroup(Composite container) {
		Group group = new Group(container, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setText(PDEUIMessages.EnvironmentBlock_jreTitle);

		Label label = new Label(group, SWT.NONE);
		label.setText(PDEUIMessages.EnvironmentBlock_jreGroup);

		fJRECombo = new Combo(group, SWT.SINGLE | SWT.READ_ONLY);
		fJRECombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fJRECombo.setItems(VMUtil.getVMInstallNames());
		fDefaultJRE = VMUtil.getDefaultVMInstallName();
		fJRECombo.setText(fDefaultJRE);

		label = new Label(group, SWT.WRAP);
		label.setText(PDEUIMessages.EnvironmentBlock_jreNote);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		gd.horizontalIndent = 25;
		gd.widthHint = 400;
		label.setLayoutData(gd);
	}

	private void createTargetEnvironmentGroup(Composite container) {
		Group group = new Group(container, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setText(PDEUIMessages.EnvironmentBlock_targetEnv);

		initializeChoices();

		Label label = new Label(group, SWT.NULL);
		label.setText(PDEUIMessages.Preferences_TargetEnvironmentPage_os);

		fOSCombo = new Combo(group, SWT.SINGLE | SWT.BORDER);
		fOSCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fOSCombo.setItems((String[]) fOSChoices.toArray(new String[fOSChoices.size()]));

		label = new Label(group, SWT.NULL);
		label.setText(PDEUIMessages.Preferences_TargetEnvironmentPage_ws);

		fWSCombo = new Combo(group, SWT.SINGLE | SWT.BORDER);
		fWSCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fWSCombo.setItems((String[]) fWSChoices.toArray(new String[fWSChoices.size()]));

		label = new Label(group, SWT.NULL);
		label.setText(PDEUIMessages.Preferences_TargetEnvironmentPage_arch);

		fArchCombo = new Combo(group, SWT.SINGLE | SWT.BORDER);
		fArchCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fArchCombo.setItems((String[]) fArchChoices.toArray(new String[fArchChoices.size()]));

		label = new Label(group, SWT.NULL);
		label.setText(PDEUIMessages.Preferences_TargetEnvironmentPage_nl);

		fNLCombo = new Combo(group, SWT.SINGLE | SWT.BORDER);
		fNLCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fNLCombo.setItems((String[]) fNLChoices.toArray(new String[fNLChoices.size()]));

		fOSCombo.setText(preferences.getString(ICoreConstants.OS));
		fWSCombo.setText(preferences.getString(ICoreConstants.WS));
		fNLCombo.setText(LocaleUtil.expandLocaleName(preferences.getString(ICoreConstants.NL)));
		fArchCombo.setText(preferences.getString(ICoreConstants.ARCH));
	}

	protected void loadTargetProfile(ITarget target) {
		loadTargetProfileEnvironment(target.getEnvironment());
		loadTargetProfileJRE(target.getTargetJREInfo());
	}

	private void loadTargetProfileEnvironment(IEnvironmentInfo info) {
		if (info == null)
			return;
		String os = info.getDisplayOS();
		String ws = info.getDisplayWS();
		String arch = info.getDisplayArch();
		String nl = info.getDisplayNL();
		nl = LocaleUtil.expandLocaleName(nl);

		if (!os.equals("")) { //$NON-NLS-1$
			if (fOSCombo.indexOf(os) == -1)
				fOSCombo.add(os);
			fOSCombo.setText(os);
		}

		if (!ws.equals("")) { //$NON-NLS-1$
			if (fWSCombo.indexOf(ws) == -1)
				fWSCombo.add(ws);
			fWSCombo.setText(ws);
		}

		if (!arch.equals("")) { //$NON-NLS-1$
			if (fArchCombo.indexOf(arch) == -1)
				fArchCombo.add(arch);
			fArchCombo.setText(arch);
		}

		if (!nl.equals("")) { //$NON-NLS-1$
			if (fNLCombo.indexOf(nl) == -1)
				fNLCombo.add(nl);
			fNLCombo.setText(nl);
		}
	}

	private void loadTargetProfileJRE(ITargetJRE info) {
		if (info != null)
			fJRECombo.setText(info.getCompatibleJRE());
	}

	/**
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		fOSCombo.setText(preferences.getDefaultString(ICoreConstants.OS));
		fWSCombo.setText(preferences.getDefaultString(ICoreConstants.WS));
		fNLCombo.setText(LocaleUtil.expandLocaleName(preferences.getDefaultString(ICoreConstants.NL)));
		fArchCombo.setText(preferences.getDefaultString(ICoreConstants.ARCH));
		fJRECombo.setText(VMUtil.getDefaultVMInstallName());
	}

	public boolean performOk() {
		applyTargetEnvironmentGroup();
		applyJREGroup();
		return true;
	}

	private void applyJREGroup() {
		try {
			if (!fDefaultJRE.equals(VMUtil.getDefaultVMInstallName()))
				return;

			if (!VMUtil.getDefaultVMInstallName().equals(fJRECombo.getText()))
				JavaRuntime.setDefaultVMInstall(VMHelper.getVMInstall(fJRECombo.getText()), null);
		} catch (CoreException e) {
		}
	}

	private void applyTargetEnvironmentGroup() {
		String oldOS = preferences.getString(ICoreConstants.OS);
		String oldWS = preferences.getString(ICoreConstants.WS);
		String oldARCH = preferences.getString(ICoreConstants.ARCH);
		String oldNL = preferences.getString(ICoreConstants.NL);
		boolean changed = false;

		String os = fOSCombo.getText().trim();
		if (os.length() > 0) {
			if (!fOSChoices.contains(os)) {
				String value = preferences.getString(ICoreConstants.OS_EXTRA);
				value = (value.length() > 0) ? value + "," + os : os; //$NON-NLS-1$
				preferences.setValue(ICoreConstants.OS_EXTRA, value);
			}
			preferences.setValue(ICoreConstants.OS, os);
			changed |= !(os.equals(oldOS));
		}

		String ws = fWSCombo.getText().trim();
		if (ws.length() > 0) {
			if (!fWSChoices.contains(ws)) {
				String value = preferences.getString(ICoreConstants.WS_EXTRA);
				value = (value.length() > 0) ? value + "," + ws : ws; //$NON-NLS-1$
				preferences.setValue(ICoreConstants.WS_EXTRA, value);
			}
			preferences.setValue(ICoreConstants.WS, ws);
			changed |= !(ws.equals(oldWS));
		}

		String arch = fArchCombo.getText().trim();
		if (arch.length() > 0) {
			if (!fArchChoices.contains(arch)) {
				String value = preferences.getString(ICoreConstants.ARCH_EXTRA);
				value = (value.length() > 0) ? value + "," + arch : arch; //$NON-NLS-1$
				preferences.setValue(ICoreConstants.ARCH_EXTRA, value);
			}
			preferences.setValue(ICoreConstants.ARCH, arch);
			changed |= !(arch.equals(oldARCH));
		}

		String locale = fNLCombo.getText().trim();
		if (locale.length() > 0) {
			if (!fNLChoices.contains(locale)) {
				String value = preferences.getString(ICoreConstants.NL_EXTRA);
				value = (value.length() > 0) ? value + "," + locale : locale; //$NON-NLS-1$
				preferences.setValue(ICoreConstants.NL_EXTRA, value);
			}
			int dash = locale.indexOf("-"); //$NON-NLS-1$
			if (dash != -1)
				locale = locale.substring(0, dash);
			locale = locale.trim();
			preferences.setValue(ICoreConstants.NL, locale);
			changed |= !(locale.equals(oldNL));
		}
		PDECore.getDefault().savePluginPreferences();
		if (changed) {
			updateState();
		}
	}

	private void updateState() {
		PDEState state = fPage.getCurrentState();
		// update the current state with the platform properties of the current environment settings.
		String[] knownExecutionEnvironments = TargetPlatformHelper.getKnownExecutionEnvironments();
		Dictionary[] properties = TargetPlatformHelper.getPlatformProperties(knownExecutionEnvironments, fPage.getCurrentState());
		state.getState().setPlatformProperties(properties);
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		// Resetting the state (manager.getState() != state) refreshes workspace projects automatically.  So if we are not reseting  
		// the state, we need to fire an event to have the PluginModelManager re-resolve the current state with the new platform properties.
		if (manager.getState() == state) {
			manager.modelsChanged(new ModelProviderEvent(properties, IModelProviderEvent.ENVIRONMENT_CHANGED, null, null, null));
		}
	}
}
