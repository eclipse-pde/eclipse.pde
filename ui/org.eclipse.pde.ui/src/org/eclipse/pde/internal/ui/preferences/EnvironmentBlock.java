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
package org.eclipse.pde.internal.ui.preferences;

import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jdt.launching.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.launcher.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.WorkbenchHelp;

public class EnvironmentBlock implements IEnvironmentVariables {
	public static final String KEY_OS = "Preferences.TargetEnvironmentPage.os"; //$NON-NLS-1$
	public static final String KEY_WS = "Preferences.TargetEnvironmentPage.ws"; //$NON-NLS-1$
	public static final String KEY_NL = "Preferences.TargetEnvironmentPage.nl"; //$NON-NLS-1$
	public static final String KEY_ARCH = "Preferences.TargetEnvironmentPage.arch"; //$NON-NLS-1$
		
	private Combo fOSCombo;
	private Combo fWSCombo;
	private Combo fNLCombo;
	private Combo fArchCombo;
	
	private Preferences preferences;
	private TreeSet fNLChoices;
	private TreeSet fOSChoices;
	private TreeSet fWSChoices;
	private TreeSet fArchChoices;
	private Button fDefaultJREButton;
	private Combo fJRECombo;
	private Button fThisJREButton;

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
		layout.verticalSpacing = 20;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		createTargetEnvironmentGroup(container);
		createJREGroup(container);
		
		Dialog.applyDialogFont(container);
		WorkbenchHelp.setHelp(container, IHelpContextIds.TARGET_ENVIRONMENT_PREFERENCE_PAGE);
		return container;
	}
	
	private void createJREGroup(Composite container) {
		Group group = new Group(container, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setText(PDEPlugin.getResourceString("EnvironmentBlock.jreGroup")); //$NON-NLS-1$
		
		fDefaultJREButton = new Button(group, SWT.RADIO);
		fDefaultJREButton.setText(PDEPlugin.getResourceString("EnvironmentBlock.defaultJRE") + LauncherUtils.getDefaultVMInstallName() + "."); //$NON-NLS-1$ //$NON-NLS-2$
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		fDefaultJREButton.setLayoutData(gd);
		boolean usedefault = preferences.getBoolean(ICoreConstants.USE_DEFAULT_JRE);
		fDefaultJREButton.setSelection(usedefault);
		
		fThisJREButton = new Button(group, SWT.RADIO);
		fThisJREButton.setText(PDEPlugin.getResourceString("EnvironmentBlock.thisJRE")); //$NON-NLS-1$
		fThisJREButton.setSelection(!usedefault);
		
		fJRECombo = new Combo(group, SWT.SINGLE|SWT.READ_ONLY);
		fJRECombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fJRECombo.setItems(LauncherUtils.getVMInstallNames());
		
		String vm = preferences.getString(ICoreConstants.TARGET_JRE);
		if (vm.length() == 0 || fJRECombo.indexOf(vm) == -1)
			fJRECombo.setText(LauncherUtils.getDefaultVMInstallName());
		else 
			fJRECombo.setText(vm);
		fJRECombo.setEnabled(!usedefault);
		
		fThisJREButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fJRECombo.setEnabled(fThisJREButton.getSelection());
			}
		});
		
		Label label = new Label(group, SWT.WRAP);
		label.setText(PDEPlugin.getResourceString("EnvironmentBlock.jreNote")); //$NON-NLS-1$
		gd = new GridData();
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
		group.setText(PDEPlugin.getResourceString("EnvironmentBlock.targetEnv")); //$NON-NLS-1$
		
		initializeChoices();
		
		Label label = new Label(group, SWT.NULL);
		label.setText(PDEPlugin.getResourceString(KEY_OS));
		
		fOSCombo = new Combo(group, SWT.SINGLE | SWT.BORDER);
		fOSCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fOSCombo.setItems((String[])fOSChoices.toArray(new String[fOSChoices.size()]));
		
		label = new Label(group, SWT.NULL);
		label.setText(PDEPlugin.getResourceString(KEY_WS));
		
		fWSCombo = new Combo(group, SWT.SINGLE | SWT.BORDER);
		fWSCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fWSCombo.setItems((String[])fWSChoices.toArray(new String[fWSChoices.size()]));
		
		label = new Label(group, SWT.NULL);
		label.setText(PDEPlugin.getResourceString(KEY_ARCH));
		
		fArchCombo = new Combo(group, SWT.SINGLE | SWT.BORDER);
		fArchCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fArchCombo.setItems((String[])fArchChoices.toArray(new String[fArchChoices.size()]));
		
		label = new Label(group, SWT.NULL);
		label.setText(PDEPlugin.getResourceString(KEY_NL));
		
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
		fDefaultJREButton.setSelection(true);
		fThisJREButton.setSelection(false);
		fJRECombo.setEnabled(false);
		fJRECombo.setText(LauncherUtils.getDefaultVMInstallName());
	}

	public boolean performOk() {
		applyTargetEnvironmentGroup();
		applyJREGroup();
		return true;
	}
	
	private void applyJREGroup() {
		boolean useDefault = fDefaultJREButton.getSelection();
		preferences.setValue(ICoreConstants.USE_DEFAULT_JRE, useDefault);
		if (useDefault)
			preferences.setValue(ICoreConstants.TARGET_JRE, LauncherUtils.getDefaultVMInstallName());
		else {
			preferences.setValue(ICoreConstants.TARGET_JRE, fJRECombo.getText());
			try {
				JavaRuntime.setDefaultVMInstall(LauncherUtils.getVMInstall(fJRECombo.getText()), null);
			} catch (CoreException e) {
			}
		}
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
			result[i] = locale.toString() + " - " + locale.getDisplayName(); //$NON-NLS-1$
		}
		return result;
	}
}
