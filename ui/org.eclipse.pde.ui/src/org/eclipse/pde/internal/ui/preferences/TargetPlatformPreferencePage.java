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

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.help.WorkbenchHelp;

/**
 */
public class TargetPlatformPreferencePage
	extends PreferencePage
	implements IWorkbenchPreferencePage {

	private Button fUseThisPlatform;
	private Label fHomeLabel;
	private Combo fHomeText;
	private Button fBrowseButton;
	private ExternalPluginsBlock fPluginsBlock;
	private Preferences fPreferences = null;
	private boolean fNeedsReload = false;
	private String fOriginalText;
	
	/**
	 * MainPreferencePage constructor comment.
	 */
	public TargetPlatformPreferencePage() {
		setDescription(PDEPlugin.getResourceString("Preferences.TargetPlatformPage.Description"));
		fPreferences = PDECore.getDefault().getPluginPreferences();
		fPluginsBlock = new ExternalPluginsBlock(this);
	}
	
	public void dispose() {
		fPluginsBlock.dispose();
		super.dispose();
	}

	public Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginHeight = layout.marginWidth = 0;
		container.setLayout(layout);

		fUseThisPlatform = new Button(container, SWT.CHECK);
		fUseThisPlatform.setText(PDEPlugin.getResourceString("Preferences.TargetPlatformPage.useThis"));
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 3;
		fUseThisPlatform.setLayoutData(gd);

		if (PDECore.inLaunchedInstance())
			fUseThisPlatform.setEnabled(false);
		
		fUseThisPlatform.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean selected = fUseThisPlatform.getSelection();
				updateEnableState(!selected);
				if (selected) {
					String newPath = ExternalModelManager.computeDefaultPlatformPath();
					if (!newPath.equals(fHomeText.getText())) {
						if (fHomeText.indexOf(newPath) == -1)
							fHomeText.add(newPath, 0);
						fHomeText.setText(newPath);
						fPluginsBlock.handleReload();
						fNeedsReload = false;
					}
				}
			}
		});

		fHomeLabel = new Label(container, SWT.NULL);
		fHomeLabel.setText(PDEPlugin.getResourceString("Preferences.TargetPlatformPage.PlatformHome"));
		
		fHomeText = new Combo(container, SWT.NONE);
		fHomeText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		ArrayList locations = new ArrayList();
		for (int i = 0; i < 5; i++) {
			String value = fPreferences.getString(ICoreConstants.SAVED_PLATFORM + i);
			if (value.equals("")) 
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
		fBrowseButton.setText(PDEPlugin.getResourceString("Preferences.TargetPlatformPage.PlatformHome.Button"));
		fBrowseButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		SWTUtil.setButtonDimensionHint(fBrowseButton);
		fBrowseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowse();
			}
		});

		Control block = fPluginsBlock.createContents(container);
		gd = new GridData(GridData.FILL_VERTICAL|GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 3;
		block.setLayoutData(gd);	
		load();
		
		Dialog.applyDialogFont(container);
		WorkbenchHelp.setHelp(container, IHelpContextIds.TARGET_PLATFORM_PREFERENCE_PAGE);
		return container;
	}

	String getPlatformPath() {
		return fHomeText.getText();
	}

	boolean getUseOther() {
		return !fUseThisPlatform.getSelection();
	}

	private void load() {		
		String mode = fPreferences.getString(ICoreConstants.TARGET_MODE);
		boolean useOther = mode.equals(ICoreConstants.VALUE_USE_OTHER);
		fUseThisPlatform.setSelection(!useOther);
		updateEnableState(useOther);
		fPluginsBlock.initialize();
	}

	private void updateEnableState(boolean useOther) {
		fHomeLabel.setEnabled(useOther);
		fHomeText.setEnabled(useOther);
		fBrowseButton.setEnabled(useOther);
	}

	private void handleBrowse() {
		DirectoryDialog dialog = new DirectoryDialog(getShell());
		if (fHomeText.getText().length() > 0)
			dialog.setFilterPath(fHomeText.getText());
		String newPath = dialog.open();
		if (newPath != null && !new Path(fHomeText.getText()).equals(new Path(newPath))) {
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
		fPluginsBlock.handleSelectAll(true);
		super.performDefaults();
	}

	public boolean performOk() {
		if (fNeedsReload && getUseOther() && !new Path(fOriginalText).equals(new Path(fHomeText.getText()))) {
			MessageDialog dialog =
				new MessageDialog(
					getShell(),
					PDEPlugin.getResourceString("Preferences.TargetPlatformPage.title"),
					null,
					PDEPlugin.getResourceString("Preferences.TargetPlatformPage.question"),
					MessageDialog.QUESTION,
					new String[] {
						IDialogConstants.YES_LABEL,
						IDialogConstants.NO_LABEL},
					1);
			if (dialog.open() == 1)
				return false;
			fPluginsBlock.handleReload();
		} 
		fPluginsBlock.save();
		return super.performOk();
	}
	
	public String[] getPlatformLocations() {
		return fHomeText.getItems();
	}
	 
	public void resetNeedsReload() {
		fNeedsReload = false;
	}
}
