/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.ui.wizards.imports;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.StatusWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

public class FeatureImportWizardFirstPage extends StatusWizardPage {

	private static final String SETTINGS_DROPLOCATION = "droplocation";
	private static final String SETTINGS_DOOTHER = "doother";
	private static final String KEY_TITLE =
		"FeatureImportWizard.FirstPage.title";
	private static final String KEY_DESC = "FeatureImportWizard.FirstPage.desc";
	private static final String KEY_RUNTIME_LOCATION =
		"FeatureImportWizard.FirstPage.runtimeLocation";
	private static final String KEY_OTHER_LOCATION =
		"FeatureImportWizard.FirstPage.otherLocation";
	private static final String KEY_RUNTIME_DESC =
		"FeatureImportWizard.FirstPage.runtimeDesc";
	private static final String KEY_OTHER_DESC =
		"ImportWizard.FirstPage.otherDesc";
	private static final String KEY_OTHER_FOLDER =
		"FeatureImportWizard.FirstPage.otherFolder";
	private static final String KEY_SOURCE_REMINDER =
		"FeatureImportWizard.FirstPage.sourceReminder";
	private static final String KEY_BROWSE =
		"FeatureImportWizard.FirstPage.browse";
	private static final String KEY_FOLDER_TITLE =
		"FeatureImportWizard.messages.folder.title";
	private static final String KEY_FOLDER_MESSAGE =
		"FeatureImportWizard.messages.folder.message";
	private static final String KEY_LOCATION_MISSING =
		"FeatureImportWizard.errors.locationMissing";
	private static final String KEY_BUILD_INVALID =
		"FeatureImportWizard.errors.buildFolderInvalid";
	private static final String KEY_BUILD_MISSING =
		"FeatureImportWizard.errors.buildFolderMissing";

	private Label otherLocationLabel;
	private Button runtimeLocationButton;
	private Button browseButton;
	private Combo dropLocation;
	private IStatus dropLocationStatus;

	public FeatureImportWizardFirstPage() {
		super("FeatureImportWizardPage", true);
		setTitle(PDEPlugin.getResourceString(KEY_TITLE));
		setDescription(PDEPlugin.getResourceString(KEY_DESC));

		dropLocationStatus = createStatus(IStatus.OK, "");
	}

	/*
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Label label;
		GridData gd;
		initializeDialogUnits(parent);

		Composite composite = new Composite(parent, SWT.NONE);

		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		composite.setLayout(layout);

		runtimeLocationButton = new Button(composite, SWT.CHECK);
		fillHorizontal(runtimeLocationButton, 3, false);
		runtimeLocationButton.setText(
			PDEPlugin.getResourceString(KEY_RUNTIME_LOCATION));

		int wizardClientWidth = parent.getSize().x - 2 * layout.marginWidth;
		otherLocationLabel = new Label(composite, SWT.NULL);
		otherLocationLabel.setText(
			PDEPlugin.getResourceString(KEY_OTHER_FOLDER));

		dropLocation = new Combo(composite, SWT.DROP_DOWN);
		fillHorizontal(dropLocation, 1, true);

		browseButton = new Button(composite, SWT.PUSH);
		browseButton.setText(PDEPlugin.getResourceString(KEY_BROWSE));
		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IPath chosen = chooseDropLocation();
				if (chosen != null)
					dropLocation.setText(chosen.toOSString());
			}
		});
		browseButton.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(browseButton);

		initializeFields(getDialogSettings());
		hookListeners();

		setControl(composite);
		Dialog.applyDialogFont(composite);
		//WorkbenchHelp.setHelp(composite, IHelpContextIds.PLUGIN_IMPORT_FIRST_PAGE);
	}

	private Label createMultiLineLabel(
		Composite composite,
		int parentWidth,
		String text,
		int span) {
		Label label = new Label(composite, SWT.WRAP);
		label.setText(text);
		GridData gd = new GridData();
		gd.horizontalSpan = span;
		gd.widthHint = parentWidth;
		label.setLayoutData(gd);
		return label;
	}

	private String getTargetHome() {
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		return preferences.getString(ICoreConstants.PLATFORM_PATH);
	}

	private void hookListeners() {
		runtimeLocationButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setOtherEnabled(!runtimeLocationButton.getSelection());
				updateStatus();
				if (runtimeLocationButton.getSelection()) {
					dropLocation.setText(getTargetHome());
				}
			}
		});

		dropLocation.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateStatus();
			}
		});
		dropLocation.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateStatus();
			}
		});
	}

	private void updateStatus() {
		validateDropLocation();
		updateStatus(dropLocationStatus);
		if (dropLocationStatus.getSeverity() == IStatus.OK
			&& !runtimeLocationButton.getSelection()
			&& !new Path(dropLocation.getText()).equals(
				new Path(getTargetHome()))) {
			updateStatus(
				createStatus(
					IStatus.INFO,
					PDEPlugin.getResourceString(KEY_SOURCE_REMINDER)));
		}
	}

	private GridData fillHorizontal(Control control, int span, boolean grab) {
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = span;
		gd.grabExcessHorizontalSpace = grab;
		control.setLayoutData(gd);
		return gd;
	}

	private boolean showPreferencePage(final IPreferenceNode targetNode) {
		PreferenceManager manager = new PreferenceManager();
		manager.addToRoot(targetNode);
		final PreferenceDialog dialog =
			new PreferenceDialog(getControl().getShell(), manager);
		final boolean[] result = new boolean[] { false };
		BusyIndicator.showWhile(getControl().getDisplay(), new Runnable() {
			public void run() {
				dialog.create();
				dialog.setMessage(targetNode.getLabelText());
				if (dialog.open() == PreferenceDialog.OK)
					result[0] = true;
			}
		});
		return result[0];
	}

	private void initializeFields(IDialogSettings initialSettings) {
		String[] dropItems = new String[0];
		boolean doOther = false;

		if (initialSettings != null) {
			doOther = initialSettings.getBoolean(SETTINGS_DOOTHER);

			ArrayList items = new ArrayList();
			for (int i = 0; i < 6; i++) {
				String curr =
					initialSettings.get(
						SETTINGS_DROPLOCATION + String.valueOf(i));
				if (curr != null && !items.contains(curr)) {
					items.add(curr);
				}
			}
			dropItems = (String[]) items.toArray(new String[items.size()]);
		}
		dropLocation.setItems(dropItems);
		runtimeLocationButton.setSelection(!doOther);
		setOtherEnabled(doOther);
		if (doOther)
			dropLocation.select(0);
		else
			dropLocation.setText(getTargetHome());

		validateDropLocation();
		updateStatus(dropLocationStatus);
	}

	private void setOtherEnabled(boolean enabled) {
		otherLocationLabel.setEnabled(enabled);
		dropLocation.setEnabled(enabled);
		browseButton.setEnabled(enabled);
	}

	public void storeSettings(boolean finishPressed) {
		IDialogSettings settings = getDialogSettings();
		boolean other = !runtimeLocationButton.getSelection();
		if (finishPressed || dropLocation.getText().length() > 0 && other) {
			settings.put(
				SETTINGS_DROPLOCATION + String.valueOf(0),
				dropLocation.getText());
			String[] items = dropLocation.getItems();
			int nEntries = Math.min(items.length, 5);
			for (int i = 0; i < nEntries; i++) {
				settings.put(
					SETTINGS_DROPLOCATION + String.valueOf(i + 1),
					items[i]);
			}
		}
		if (finishPressed) {
			settings.put(SETTINGS_DOOTHER, other);
		}
	}

	/**
	 * Browses for a drop location.
	 */
	private IPath chooseDropLocation() {
		DirectoryDialog dialog = new DirectoryDialog(getShell());
		dialog.setFilterPath(dropLocation.getText());
		dialog.setText(PDEPlugin.getResourceString(KEY_FOLDER_TITLE));
		dialog.setMessage(PDEPlugin.getResourceString(KEY_FOLDER_MESSAGE));
		String res = dialog.open();
		if (res != null) {
			return new Path(res);
		}
		return null;
	}

	private void validateDropLocation() {
		if (isOtherLocation()) {
			IPath curr = getDropLocation();
			if (curr.segmentCount() == 0) {
				dropLocationStatus =
					createStatus(
						IStatus.ERROR,
						PDEPlugin.getResourceString(KEY_LOCATION_MISSING));
				return;
			}
			if (!Path.ROOT.isValidPath(dropLocation.getText())) {
				dropLocationStatus =
					createStatus(
						IStatus.ERROR,
						PDEPlugin.getResourceString(KEY_BUILD_INVALID));
				return;
			}

			File file = curr.toFile();
			if (!file.isDirectory()) {
				dropLocationStatus =
					createStatus(
						IStatus.ERROR,
						PDEPlugin.getResourceString(KEY_BUILD_MISSING));
				return;
			}
		}
		dropLocationStatus = createStatus(IStatus.OK, "");
	}

	/**
	 * Returns the drop location.
	 */
	public IPath getDropLocation() {
		return new Path(dropLocation.getText());
	}

	public boolean isOtherLocation() {
		return !runtimeLocationButton.getSelection();
	}
}