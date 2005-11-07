/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.IPreferenceConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.text.ColorManager;
import org.eclipse.pde.internal.ui.editor.text.IColorManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class EditorPreferencePage
	extends PreferencePage
	implements IWorkbenchPreferencePage, IPreferenceConstants {
		
	private XMLSyntaxColorTab fXMLTab;
	//private ManifestSyntaxColorTab fManifestTab;
	private IColorManager fColorManager;

	public EditorPreferencePage() {
		setDescription(PDEUIMessages.EditorPreferencePage_colorSettings);
		fColorManager = new ColorManager();
	}

	public boolean performOk() {
		fXMLTab.performOk();
		//fManifestTab.performOk();
		PDEPlugin.getDefault().savePluginPreferences();
		return super.performOk();
	}
	
	public void dispose() {
		fColorManager.dispose();
		fXMLTab.dispose();
		//fManifestTab.dispose();
		super.dispose();
	}
	
	protected void performDefaults() {
		fXMLTab.performDefaults();
		//fManifestTab.performDefaults();
		super.performDefaults();
	}
	
	public void init(IWorkbench workbench) {
	}
	
	protected Control createContents(Composite parent) {
		final Link link = new Link(parent, SWT.NONE);
		final String target = "org.eclipse.ui.preferencePages.GeneralTextEditor"; //$NON-NLS-1$
		link.setText(PDEUIMessages.EditorPreferencePage_link);
		link.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				PreferencesUtil.createPreferenceDialogOn(link.getShell(), target, null, null);
			}
		});
		
		fXMLTab = new XMLSyntaxColorTab(fColorManager);
		fXMLTab.createContents(parent);
		
		/*TabFolder folder = new TabFolder(parent, SWT.NONE);
		folder.setLayout(new TabFolderLayout());	
		folder.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		createXMLTab(folder);
		createManifestTab(folder);*/
		
		Dialog.applyDialogFont(getControl());
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.EDITOR_PREFERENCE_PAGE);
		
		return parent;
	}
	
	/*private void createXMLTab(TabFolder folder) {
		fXMLTab = new XMLSyntaxColorTab(fColorManager);
		TabItem item = new TabItem(folder, SWT.NONE);
		item.setText("&XML Highlighting");
		item.setControl(fXMLTab.createContents(folder));
	}*/
	
	/*private void createManifestTab(TabFolder folder) {
		fManifestTab = new ManifestSyntaxColorTab(fColorManager);
		TabItem item = new TabItem(folder, SWT.NONE);
		item.setText("&Manifest Highlighting");
		item.setControl(fManifestTab.createContents(folder));				
	}*/
	
}
