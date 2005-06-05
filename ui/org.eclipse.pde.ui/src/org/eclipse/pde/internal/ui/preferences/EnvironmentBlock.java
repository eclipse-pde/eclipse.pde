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
package org.eclipse.pde.internal.ui.preferences;

import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.IEnvironmentVariables;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.launcher.LauncherUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;

public class EnvironmentBlock implements IEnvironmentVariables {
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

	public EnvironmentBlock() {
		preferences = PDECore.getDefault().getPluginPreferences();
	}
	
	private void initializeChoices() {
		fOSChoices = new TreeSet();
		String[] os = Platform.knownOSValues();
		for (int i = 0; i < os.length; i++)
			fOSChoices.add(os[i]);
		addExtraChoices(fOSChoices, preferences.getString(OS_EXTRA));
		
		fWSChoices = new TreeSet();
		String[] ws = Platform.knownWSValues();
		for (int i = 0; i < ws.length; i++)
			fWSChoices.add(ws[i]);
		addExtraChoices(fWSChoices, preferences.getString(WS_EXTRA));
		
		fArchChoices = new TreeSet();
		String[] arch = Platform.knownOSArchValues();
		for (int i = 0; i < arch.length; i++)
			fArchChoices.add(arch[i]);
		addExtraChoices(fArchChoices, preferences.getString(ARCH_EXTRA));
		
		fNLChoices = new TreeSet();
		if (LOCALES_INITIALIZED) {
			initializeAllLocales();
		} else {
			fNLChoices.add(expandLocaleName(preferences.getString(NL)));
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
		fNLCombo.setItems((String[])fNLChoices.toArray(new String[fNLChoices.size()]));
		fNLCombo.setText(current);
	}
	
	private void initializeAllLocales() {
		String[] nl = getLocales();
		for (int i = 0; i < nl.length; i++)
			fNLChoices.add(nl[i]);
		addExtraChoices(fNLChoices, preferences.getString(NL_EXTRA));	
	}
	
	private void addExtraChoices(Set set, String preference) {
		StringTokenizer tokenizer = new StringTokenizer(preference);
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
		label.setText(PDEUIMessages.EnvironmentBlock_jreGroup);  //$NON-NLS-1$
		
		fJRECombo = new Combo(group, SWT.SINGLE|SWT.READ_ONLY);
		fJRECombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fJRECombo.setItems(LauncherUtils.getVMInstallNames());
		fDefaultJRE = LauncherUtils.getDefaultVMInstallName();
		fJRECombo.setText(fDefaultJRE);
		
		label = new Label(group, SWT.WRAP);
		label.setText(PDEUIMessages.EnvironmentBlock_jreNote); //$NON-NLS-1$
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
		group.setText(PDEUIMessages.EnvironmentBlock_targetEnv); //$NON-NLS-1$
		
		initializeChoices();
		
		Label label = new Label(group, SWT.NULL);
		label.setText(PDEUIMessages.Preferences_TargetEnvironmentPage_os);
		
		fOSCombo = new Combo(group, SWT.SINGLE | SWT.BORDER);
		fOSCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fOSCombo.setItems((String[])fOSChoices.toArray(new String[fOSChoices.size()]));
		
		label = new Label(group, SWT.NULL);
		label.setText(PDEUIMessages.Preferences_TargetEnvironmentPage_ws);
		
		fWSCombo = new Combo(group, SWT.SINGLE | SWT.BORDER);
		fWSCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fWSCombo.setItems((String[])fWSChoices.toArray(new String[fWSChoices.size()]));
		
		label = new Label(group, SWT.NULL);
		label.setText(PDEUIMessages.Preferences_TargetEnvironmentPage_arch);
		
		fArchCombo = new Combo(group, SWT.SINGLE | SWT.BORDER);
		fArchCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fArchCombo.setItems((String[])fArchChoices.toArray(new String[fArchChoices.size()]));
		
		label = new Label(group, SWT.NULL);
		label.setText(PDEUIMessages.Preferences_TargetEnvironmentPage_nl);
		
		fNLCombo = new Combo(group, SWT.SINGLE | SWT.BORDER);
		fNLCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fNLCombo.setItems((String[])fNLChoices.toArray(new String[fNLChoices.size()]));
				
		fOSCombo.setText(preferences.getString(OS));
		fWSCombo.setText(preferences.getString(WS));
		fNLCombo.setText(expandLocaleName(preferences.getString(NL)));
		fArchCombo.setText(preferences.getString(ARCH));		
	}
	
	/**
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		fOSCombo.setText(preferences.getDefaultString(OS));
		fWSCombo.setText(preferences.getDefaultString(WS));
		fNLCombo.setText(expandLocaleName(preferences.getDefaultString(NL)));
		fArchCombo.setText(preferences.getDefaultString(ARCH));
		fJRECombo.setText(LauncherUtils.getDefaultVMInstallName());
	}

	public boolean performOk() {
		applySelfHostingMode();
		applyTargetEnvironmentGroup();
		applyJREGroup();
		return true;
	}
	
	private void applyJREGroup() {
		try {
			if (!fDefaultJRE.equals(LauncherUtils.getDefaultVMInstallName()))
				return;
			
			if (!LauncherUtils.getDefaultVMInstallName().equals(fJRECombo.getText()))
				JavaRuntime.setDefaultVMInstall(LauncherUtils.getVMInstall(fJRECombo.getText()), null);
		} catch (CoreException e) {
		}
	}
	
	private void applySelfHostingMode() {
		preferences.setValue(ICoreConstants.STRICT_MODE, false);
	}
	
	private void applyTargetEnvironmentGroup() {
		String os = fOSCombo.getText().trim();
		if (os.length() > 0) {
			if (!fOSChoices.contains(os)) {
				String value = preferences.getString(OS_EXTRA);
				value = (value.length() > 0) ? value + "," + os : os; //$NON-NLS-1$
				preferences.setValue(OS_EXTRA, value);
			}
			preferences.setValue(OS, os);
		}
		
		String ws = fWSCombo.getText().trim();
		if (ws.length() > 0) {
			if (!fWSChoices.contains(ws)) {
				String value = preferences.getString(WS_EXTRA);
				value = (value.length() > 0) ? value + "," + ws : ws; //$NON-NLS-1$
				preferences.setValue(WS_EXTRA, value);
			}
			preferences.setValue(WS, ws);
		}
		
		String arch = fArchCombo.getText().trim();
		if (arch.length() > 0) {
			if (!fArchChoices.contains(arch)) {
				String value = preferences.getString(ARCH_EXTRA);
				value = (value.length() > 0) ? value + "," + arch : arch; //$NON-NLS-1$
				preferences.setValue(ARCH_EXTRA, value);
			}
			preferences.setValue(ARCH, arch);
		}
		
		String locale = fNLCombo.getText().trim();
		if (locale.length() > 0) {
			if (!fNLChoices.contains(locale)) {
				String value = preferences.getString(NL_EXTRA);
				value = (value.length() > 0) ? value + "," + locale : locale; //$NON-NLS-1$
				preferences.setValue(NL_EXTRA, value);
			}			
			int dash = locale.indexOf("-"); //$NON-NLS-1$
			if (dash != -1)
				locale = locale.substring(0, dash);
			locale = locale.trim();
			preferences.setValue(NL, locale);
		}
		PDECore.getDefault().savePluginPreferences();
	}
		
	
	private String expandLocaleName(String name) {
		String language = ""; //$NON-NLS-1$
		String country = ""; //$NON-NLS-1$
		String variant = ""; //$NON-NLS-1$
		
		StringTokenizer tokenizer = new StringTokenizer(name, "_"); //$NON-NLS-1$
		if (tokenizer.hasMoreTokens())
			language = tokenizer.nextToken();
		if (tokenizer.hasMoreTokens())
			country = tokenizer.nextToken();
		if (tokenizer.hasMoreTokens())
			variant = tokenizer.nextToken();
			
		Locale locale = new Locale(language, country, variant);
		return locale.toString() + " - " + locale.getDisplayName(); //$NON-NLS-1$
	}

	private static String[] getLocales() {
		Locale[] locales = Locale.getAvailableLocales();
		String[] result = new String[locales.length];
		for (int i = 0; i < locales.length; i++) {
			Locale locale = locales[i];
			StringBuffer buffer = new StringBuffer();
			buffer.append(locale.toString());
			buffer.append(" - "); //$NON-NLS-1$
			buffer.append(locale.getDisplayName());
			result[i] = buffer.toString();
		}
		return result;
	}
}
