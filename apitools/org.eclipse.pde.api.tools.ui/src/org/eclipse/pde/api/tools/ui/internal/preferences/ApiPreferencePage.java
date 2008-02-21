/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class ApiPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	public ApiPreferencePage() {
		super("API"); //$NON-NLS-1$
		noDefaultAndApplyButton();
	}

	protected Control createContents(Composite parent) {
		final Link errorWarningsLink = new Link(parent, SWT.NONE);
		final String errorWarningsTarget = "org.eclipse.pde.api.tools.ui.prefpages.errorswarnings"; //$NON-NLS-1$
		errorWarningsLink.setText(PreferenceMessages.ApiPreferencePage_0);
		errorWarningsLink.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				PreferencesUtil.createPreferenceDialogOn(errorWarningsLink.getShell(), errorWarningsTarget, null, null);
			}
		});
		
		final Link profilesLink = new Link(parent, SWT.NONE);
		final String profilesTarget = "org.eclipse.pde.api.tools.ui.prefpages.profiles"; //$NON-NLS-1$
		profilesLink.setText(PreferenceMessages.ApiPreferencePage_1);
		profilesLink.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				PreferencesUtil.createPreferenceDialogOn(profilesLink.getShell(), profilesTarget, null, null);
			}
		});

		return parent;
	}

	public void init(IWorkbench workbench) {} // nothing to do

}
