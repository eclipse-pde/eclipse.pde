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

	public static final String KEY_PLATFORM_HOME =
		"Preferences.TargetPlatformPage.PlatformHome";
	public static final String KEY_PLATFORM_HOME_BUTTON =
		"Preferences.TargetPlatformPage.PlatformHome.Button";
	public static final String KEY_DESCRIPTION =
		"Preferences.TargetPlatformPage.Description";

	public static final String KEY_TARGET_MODE =
		"Preferences.TargetPlatformPage.targetMode";
	public static final String KEY_USE_THIS =
		"Preferences.TargetPlatformPage.useThis";
	public static final String KEY_USE_OTHER =
		"Preferences.TargetPlatformPage.useOther";
	private Button thisRadio;
	private Button otherRadio;
	private Label homeLabel;
	private Text homeText;
	private Button browseButton;
	private ExternalPluginsBlock pluginsBlock;
	private boolean useOther = false;
	private Preferences preferences = null;
	private boolean needsReload = false;
	private String originalText;
	
	/**
	 * MainPreferencePage constructor comment.
	 */
	public TargetPlatformPreferencePage() {
		setDescription(PDEPlugin.getResourceString(KEY_DESCRIPTION));
		preferences = PDECore.getDefault().getPluginPreferences();
		pluginsBlock = new ExternalPluginsBlock(this);
	}
	
	public void dispose() {
		pluginsBlock.dispose();
		super.dispose();
	}

	public Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 9;
		layout.marginWidth = layout.marginHeight = 0;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label label;

		Group group = new Group(container, SWT.NULL);
		group.setText(PDEPlugin.getResourceString(KEY_TARGET_MODE));
		group.setLayout(new GridLayout());
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		thisRadio = new Button(group, SWT.RADIO);
		thisRadio.setText(PDEPlugin.getResourceString(KEY_USE_THIS));

		otherRadio = new Button(group, SWT.RADIO);
		otherRadio.setText(PDEPlugin.getResourceString(KEY_USE_OTHER));
		
		if (PDECore.isDevInstance())
			thisRadio.setEnabled(false);
		
		SelectionListener listener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				modeChanged(otherRadio.getSelection());
			}
		};
		thisRadio.addSelectionListener(listener);
		otherRadio.addSelectionListener(listener);

		Composite home = new Composite(group, SWT.NULL);
		home.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginWidth = 0;
		home.setLayout(layout);
		homeLabel = new Label(home, SWT.NULL);
		homeLabel.setText(PDEPlugin.getResourceString(KEY_PLATFORM_HOME));
		homeText = new Text(home, SWT.BORDER);
		homeText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		browseButton = new Button(home, SWT.PUSH);
		browseButton.setText(PDEPlugin.getResourceString(KEY_PLATFORM_HOME_BUTTON));
		browseButton.setLayoutData(new GridData(GridData.END));
		SWTUtil.setButtonDimensionHint(browseButton);
		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowse();
			}
		});

		Control block = pluginsBlock.createContents(container);
		block.setLayoutData(new GridData(GridData.FILL_VERTICAL | GridData.HORIZONTAL_ALIGN_FILL));
		load();
		homeText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				needsReload = true;
			}
		});
		originalText = homeText.getText();
		Dialog.applyDialogFont(container);
		WorkbenchHelp.setHelp(container, IHelpContextIds.TARGET_PLATFORM_PREFERENCE_PAGE);
		return container;
	}

	String getPlatformPath() {
		if (useOther)
			return homeText.getText();
		else
			return ExternalModelManager.computeDefaultPlatformPath();
	}

	boolean getUseOther() {
		return otherRadio.getSelection();
	}

	private void load() {
		String mode = preferences.getString(ICoreConstants.TARGET_MODE);
		boolean useOther = mode.equals(ICoreConstants.VALUE_USE_OTHER);
		String path = preferences.getString(ICoreConstants.PLATFORM_PATH);
		load(useOther, path);
		pluginsBlock.initialize();
	}

	private void load(boolean useOther, String path) {
		this.useOther = useOther;
		homeText.setText(path);
		thisRadio.setSelection(!useOther);
		otherRadio.setSelection(useOther);
		updateEnableState(useOther);
	}
	
	private void updateEnableState(boolean useOther) {
		homeLabel.setEnabled(useOther);
		homeText.setEnabled(useOther);
		browseButton.setEnabled(useOther);
	}

	private void modeChanged(boolean useOther) {
		updateEnableState(useOther);
		String oldPath = getPlatformPath();
		this.useOther = useOther;
		String newPath = getPlatformPath();
		boolean reloadNeeded = false;
		if (oldPath != null && newPath == null)
			reloadNeeded = true;
		if (oldPath == null && newPath != null)
			reloadNeeded = true;
		if (oldPath.equals(newPath) == false)
			reloadNeeded = true;
		if (reloadNeeded) {
			pluginsBlock.handleReload();
			needsReload = false;
		}
	}
	
	private void handleBrowse() {
		DirectoryDialog dialog = new DirectoryDialog(getShell());
		if (homeText.getText().length()>0)
			dialog.setFilterPath(homeText.getText());
		String newPath = dialog.open();
		if (newPath!=null) 
			homeText.setText(newPath);
	}

	private void loadDefaults() {
		String mode = preferences.getDefaultString(ICoreConstants.TARGET_MODE);
		String path = ExternalModelManager.computeDefaultPlatformPath();
		load(mode.equals(ICoreConstants.VALUE_USE_OTHER), path);
	}

	public void init(IWorkbench workbench) {
	}
	
	public void performDefaults() {
		loadDefaults();
		super.performDefaults();
	}

	public boolean performOk() {
		if (needsReload && getUseOther() && !originalText.equals(homeText.getText())) {
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
			pluginsBlock.handleReload();
		} 
		pluginsBlock.save();
		PDECore.getDefault().getSourceLocationManager().reinitializeClasspathVariables(null);
		return super.performOk();
	}
	 
	public void resetNeedsReload() {
		needsReload = false;
	}
}
