/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
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

import org.eclipse.core.boot.BootLoader;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.help.WorkbenchHelp;

/**
 */
public class TargetEnvironmentPreferencePage
	extends PreferencePage
	implements IWorkbenchPreferencePage, IEnvironmentVariables {
	private static final String KEY_DESCRIPTION =
		"Preferences.TargetEnvironmentPage.Description";
	public static final String KEY_OS = "Preferences.TargetEnvironmentPage.os";
	public static final String KEY_WS = "Preferences.TargetEnvironmentPage.ws";
	public static final String KEY_NL = "Preferences.TargetEnvironmentPage.nl";
	public static final String KEY_ARCH =
		"Preferences.TargetEnvironmentPage.arch";
		
	private Combo os;
	private Combo ws;
	private Combo nl;
	private Combo arch;
	
	private Preferences preferences;

	public TargetEnvironmentPreferencePage() {
		setDescription(PDEPlugin.getResourceString(KEY_DESCRIPTION));
		preferences = PDECore.getDefault().getPluginPreferences();
	}

	/**
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		container.setLayout(layout);
		
		Label label = new Label(container, SWT.NULL);
		label.setText(PDEPlugin.getResourceString(KEY_OS));
		
		os = new Combo(container, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
		os.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		os.setItems(BootLoader.knownOSValues());
		
		label = new Label(container, SWT.NULL);
		label.setText(PDEPlugin.getResourceString(KEY_WS));
		
		ws = new Combo(container, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
		ws.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		ws.setItems(BootLoader.knownWSValues());
		
		label = new Label(container, SWT.NULL);
		label.setText(PDEPlugin.getResourceString(KEY_NL));
		
		nl = new Combo(container, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
		nl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		nl.setItems(getLocales());
				
		label = new Label(container, SWT.NULL);
		label.setText(PDEPlugin.getResourceString(KEY_ARCH));
		
		arch = new Combo(container, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
		arch.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		arch.setItems(BootLoader.knownOSArchValues());
		
		Dialog.applyDialogFont(container);
		
		os.setText(preferences.getString(OS));
		ws.setText(preferences.getString(WS));
		nl.setText(expandLocaleName(preferences.getString(NL)));
		arch.setText(preferences.getString(ARCH));
			
		WorkbenchHelp.setHelp(container, IHelpContextIds.TARGET_ENVIRONMENT_PREFERENCE_PAGE);

		return container;
	}
	
	/**
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		os.setText(preferences.getDefaultString(OS));
		ws.setText(preferences.getDefaultString(WS));
		nl.setText(expandLocaleName(preferences.getDefaultString(NL)));
		arch.setText(preferences.getDefaultString(ARCH));	
	}


	public boolean performOk() {
		preferences.setValue(OS, os.getText().trim());
		preferences.setValue(WS, ws.getText().trim());
		String locale = nl.getText().trim();
		int dash = locale.indexOf("-");
		if (dash != -1)
			locale = locale.substring(0, dash);
		locale = locale.trim();
		preferences.setValue(NL, locale);
		preferences.setValue(ARCH, arch.getText().trim());

		PDEPlugin.getDefault().savePluginPreferences();
		return super.performOk();
	}
	
	/**
	 * Initializes this preference page using the passed desktop.
	 *
	 * @param desktop the current desktop
	 */
	public void init(IWorkbench workbench) {
	}
	
	private String expandLocaleName(String name) {
		String language = "";
		String country = "";
		String variant = "";
		
		StringTokenizer tokenizer = new StringTokenizer(name, "_");
		if (tokenizer.hasMoreTokens())
			language = tokenizer.nextToken();
		if (tokenizer.hasMoreTokens())
			country = tokenizer.nextToken();
		if (tokenizer.hasMoreTokens())
			variant = tokenizer.nextToken();
			
		Locale locale = new Locale(language, country, variant);
		return locale.toString() + " - " + locale.getDisplayName();
	}

	private static String[] getLocales() {
		Locale[] locales = Locale.getAvailableLocales();
		String[] result = new String[locales.length];
		for (int i = 0; i < locales.length; i++) {
			Locale locale = locales[i];
			result[i] = locale.toString() + " - " + locale.getDisplayName();
		}
		return result;
	}
	
}
