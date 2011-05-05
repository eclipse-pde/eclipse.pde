/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.preferences;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.text.ColorManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class EditorPreferencePage extends PreferencePage implements IWorkbenchPreferencePage, IPreferenceConstants {

	private XMLSyntaxColorTab fXMLTab;
	private ManifestSyntaxColorTab fManifestTab;
	private ColorManager fColorManager;

	public EditorPreferencePage() {
		setDescription(PDEUIMessages.EditorPreferencePage_colorSettings);
		fColorManager = new ColorManager();
	}

	public boolean performOk() {
		fXMLTab.performOk();
		fManifestTab.performOk();
		PDEPlugin.getDefault().getPreferenceManager().savePluginPreferences();
		return super.performOk();
	}

	public void dispose() {
		fColorManager.disposeColors(false);
		fXMLTab.dispose();
		fManifestTab.dispose();
		super.dispose();
	}

	protected void performDefaults() {
		fXMLTab.performDefaults();
		fManifestTab.performDefaults();
		super.performDefaults();
	}

	public void init(IWorkbench workbench) {
	}

	protected Control createContents(Composite parent) {
		final Link link = new Link(parent, SWT.NONE);
		link.setText(PDEUIMessages.EditorPreferencePage_link);
		link.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if ("org.eclipse.ui.preferencePages.GeneralTextEditor".equals(e.text)) //$NON-NLS-1$
					PreferencesUtil.createPreferenceDialogOn(link.getShell(), e.text, null, null);
				else if ("org.eclipse.ui.preferencePages.ColorsAndFonts".equals(e.text)) //$NON-NLS-1$
					PreferencesUtil.createPreferenceDialogOn(link.getShell(), e.text, null, "selectFont:org.eclipse.jface.textfont"); //$NON-NLS-1$
			}
		});

		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);

		Button foldingButton = new Button(parent, SWT.CHECK | SWT.LEFT | SWT.WRAP);
		foldingButton.setText(PDEUIMessages.EditorPreferencePage_folding);
		foldingButton.setLayoutData(gd);
		foldingButton.setSelection(PDEPlugin.getDefault().getPreferenceStore().getBoolean(IPreferenceConstants.EDITOR_FOLDING_ENABLED));
		foldingButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
				store.setValue(IPreferenceConstants.EDITOR_FOLDING_ENABLED, ((Button) e.getSource()).getSelection());
			}
		});

		TabFolder folder = new TabFolder(parent, SWT.NONE);
		folder.setLayout(new TabFolderLayout());
		folder.setLayoutData(new GridData(GridData.FILL_BOTH));

		createXMLTab(folder);
		createManifestTab(folder);

		Dialog.applyDialogFont(getControl());
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.EDITOR_PREFERENCE_PAGE);

		return parent;
	}

	private void createXMLTab(TabFolder folder) {
		fXMLTab = new XMLSyntaxColorTab(fColorManager);
		TabItem item = new TabItem(folder, SWT.NONE);
		item.setText(PDEUIMessages.EditorPreferencePage_xml);
		item.setControl(fXMLTab.createContents(folder));
	}

	private void createManifestTab(TabFolder folder) {
		fManifestTab = new ManifestSyntaxColorTab(fColorManager);
		TabItem item = new TabItem(folder, SWT.NONE);
		item.setText(PDEUIMessages.EditorPreferencePage_manifest);
		item.setControl(fManifestTab.createContents(folder));
	}

}
