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


import java.util.ArrayList;

import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.pde.internal.core.ExternalModelManager;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

public class TargetPlatformPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public static final int PLUGINS_INDEX = 0;
	public static final int ENVIRONMENT_INDEX = 1;
	public static final int SOURCE_INDEX = 2;
	
	private Label fHomeLabel;
	private Combo fHomeText;
	private Button fBrowseButton;
	private ExternalPluginsBlock fPluginsBlock;
	private EnvironmentBlock fEnvironmentBlock;
	private SourceBlock fSourceBlock;
	
	private Preferences fPreferences = null;
	private boolean fNeedsReload = false;
	private String fOriginalText;
	private int fIndex;
	private TabFolder fTabFolder;
	
	/**
	 * MainPreferencePage constructor comment.
	 */
	public TargetPlatformPreferencePage() {
		this(PLUGINS_INDEX);
	}
	
	public TargetPlatformPreferencePage(int index) {
		setDescription(PDEUIMessages.Preferences_TargetPlatformPage_Description); 
		fPreferences = PDECore.getDefault().getPluginPreferences();
		fPluginsBlock = new ExternalPluginsBlock(this);
		fIndex = index;
	}
	
	public void dispose() {
		fPluginsBlock.dispose();
		fSourceBlock.dispose();
		super.dispose();
	}

	public Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginHeight = layout.marginWidth = 0;
		container.setLayout(layout);

		fHomeLabel = new Label(container, SWT.NULL);
		fHomeLabel.setText(PDEUIMessages.Preferences_TargetPlatformPage_PlatformHome); 
		
		fHomeText = new Combo(container, SWT.NONE);
		fHomeText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		ArrayList locations = new ArrayList();
		for (int i = 0; i < 5; i++) {
			String value = fPreferences.getString(ICoreConstants.SAVED_PLATFORM + i);
			if (value.equals(""))  //$NON-NLS-1$
				break;
			locations.add(value);
		}
		String homeLocation = fPreferences.getString(ICoreConstants.PLATFORM_PATH);
		if (!locations.contains(homeLocation))
			locations.add(0, homeLocation);
		fHomeText.setItems((String[])locations.toArray(new String[locations.size()]));
		fHomeText.setText(homeLocation);
		fOriginalText = fHomeText.getText();
		fHomeText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				fNeedsReload = true;
			}
		});
		fHomeText.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fPluginsBlock.handleReload();
				fNeedsReload = false;
			}
		});
		
		fBrowseButton = new Button(container, SWT.PUSH);
		fBrowseButton.setText(PDEUIMessages.Preferences_TargetPlatformPage_PlatformHome_Button); 
		fBrowseButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		SWTUtil.setButtonDimensionHint(fBrowseButton);
		fBrowseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowse();
			}
		});
		
		
		fTabFolder = new TabFolder(container, SWT.NONE);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 3;
		fTabFolder.setLayoutData(gd);
		
		BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
			public void run() {	
			createPluginsTab(fTabFolder);
			createEnvironmentTab(fTabFolder);
			createSourceTab(fTabFolder);
			fTabFolder.setSelection(fIndex);
			}
		});
		
		fTabFolder.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (fTabFolder.getSelectionIndex() == ENVIRONMENT_INDEX) {
					fEnvironmentBlock.updateChoices();
				}
			}
		});
		
		Dialog.applyDialogFont(container);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.TARGET_PLATFORM_PREFERENCE_PAGE);
		return container;
	}
	
	private void createPluginsTab(TabFolder folder) {
		Control block = fPluginsBlock.createContents(folder);
		block.setLayoutData(new GridData(GridData.FILL_BOTH));	
		fPluginsBlock.initialize();

		TabItem tab = new TabItem(folder, SWT.NONE);
		tab.setText(PDEUIMessages.TargetPlatformPreferencePage_pluginsTab); 
		tab.setControl(block);	
	}
	
	private void createEnvironmentTab(TabFolder folder) {
		fEnvironmentBlock = new EnvironmentBlock();
		Control block = fEnvironmentBlock.createContents(folder);
		
		TabItem tab = new TabItem(folder, SWT.NONE);
		tab.setText(PDEUIMessages.TargetPlatformPreferencePage_environmentTab); 
		tab.setControl(block);
	}
	
	private void createSourceTab(TabFolder folder) {
		fSourceBlock = new SourceBlock();
		Control block = fSourceBlock.createContents(folder);
		
		TabItem tab = new TabItem(folder, SWT.NONE);
		tab.setText(PDEUIMessages.TargetPlatformPreferencePage_sourceCode);  
		tab.setControl(block);
	}

	String getPlatformPath() {
		return fHomeText.getText();
	}

	private void handleBrowse() {
		DirectoryDialog dialog = new DirectoryDialog(getShell());
		if (fHomeText.getText().length() > 0)
			dialog.setFilterPath(fHomeText.getText());
		String newPath = dialog.open();
		if (newPath != null
				&& !ExternalModelManager.arePathsEqual(new Path(fHomeText.getText()), new Path(newPath))) {
			if (fHomeText.indexOf(newPath) == -1)
				fHomeText.add(newPath, 0);
			fHomeText.setText(newPath);
			fPluginsBlock.handleReload();
			fNeedsReload = false;
		}
	}

	public void init(IWorkbench workbench) {
	}
	
	public void performDefaults() {
		fHomeText.setText(ExternalModelManager.computeDefaultPlatformPath());
		fPluginsBlock.handleReload();
		fEnvironmentBlock.performDefaults();
		fSourceBlock.performDefaults();
		super.performDefaults();
	}

	public boolean performOk() {
		fEnvironmentBlock.performOk();
		if (fNeedsReload && !ExternalModelManager.arePathsEqual(new Path(fOriginalText), new Path(fHomeText.getText()))) {
			MessageDialog dialog =
				new MessageDialog(
					getShell(),
					PDEUIMessages.Preferences_TargetPlatformPage_title, 
					null,
					PDEUIMessages.Preferences_TargetPlatformPage_question, 
					MessageDialog.QUESTION,
					new String[] {
						IDialogConstants.YES_LABEL,
						IDialogConstants.NO_LABEL},
					1);
			if (dialog.open() == 1) {
				getContainer().updateButtons();
				return false;
			}
			fPluginsBlock.handleReload();
		} 
		fSourceBlock.performOk();
		fPluginsBlock.performOk();
		return super.performOk();
	}
	
	public String[] getPlatformLocations() {
		return fHomeText.getItems();
	}
	 
	public void resetNeedsReload() {
		fNeedsReload = false;
		String location = fHomeText.getText();
		if (fHomeText.indexOf(location) == -1)
			fHomeText.add(location, 0);
	}
	
	public SourceBlock getSourceBlock() {
		return fSourceBlock;
	}
}
