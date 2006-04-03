/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.imports;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.feature.ExternalFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.WizardCheckboxTablePart;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
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
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.progress.IProgressService;

public class FeatureImportWizardPage extends WizardPage {

	private static final String SETTINGS_DROPLOCATION = "droplocation"; //$NON-NLS-1$
	private static final String SETTINGS_DOOTHER = "doother"; //$NON-NLS-1$
	private static final String SETTINGS_NOT_BINARY = "notbinary"; //$NON-NLS-1$

	private Label fOtherLocationLabel;
	private Button fRuntimeLocationButton;
	private Button fBrowseButton;
	private Combo fDropLocation;
	private boolean fSelfSetLocation;
	private String fCurrDropLocation;

	private CheckboxTableViewer fFeatureViewer;
	private TablePart fTablePart;
	private IFeatureModel[] fModels;
	
	private Button fBinaryButton;

	class ContentProvider extends DefaultContentProvider implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			if (fModels != null)
				return fModels;
			return new Object[0];
		}
	}
	
	class TablePart extends WizardCheckboxTablePart {
		public TablePart() {
			super(null, new String[] {
					PDEUIMessages.FeatureImportWizardPage_reload,
					PDEUIMessages.WizardCheckboxTablePart_selectAll,
					PDEUIMessages.WizardCheckboxTablePart_deselectAll});
			setSelectAllIndex(1);
			setDeselectAllIndex(2);
		}
		public void updateCounter(int count) {
			super.updateCounter(count);
			dialogChanged();
		}
		protected StructuredViewer createStructuredViewer(Composite parent, int style, FormToolkit toolkit) {
			StructuredViewer viewer = super.createStructuredViewer(parent, style, toolkit);
			viewer.setSorter(ListUtil.FEATURE_SORTER);
			return viewer;
		}
		protected void buttonSelected(Button button, int index) {
			if (index == 0)
				loadFeatureTable();
			else
				super.buttonSelected(button, index);
		}
	}
	
	public FeatureImportWizardPage() {
		super("FeatureImportWizardPage"); //$NON-NLS-1$
		setTitle(PDEUIMessages.FeatureImportWizard_FirstPage_title); 
		setDescription(PDEUIMessages.FeatureImportWizard_FirstPage_desc);
		fTablePart = new TablePart(); 
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	/*
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(3, false));

		fRuntimeLocationButton = new Button(composite, SWT.CHECK);
		fillHorizontal(fRuntimeLocationButton, 3, false);
		fRuntimeLocationButton.setText(PDEUIMessages.FeatureImportWizard_FirstPage_runtimeLocation); 
		
		fOtherLocationLabel = new Label(composite, SWT.NULL);
		fOtherLocationLabel.setText(PDEUIMessages.FeatureImportWizard_FirstPage_otherFolder); 

		fDropLocation = new Combo(composite, SWT.DROP_DOWN);
		fillHorizontal(fDropLocation, 1, true);

		fBrowseButton = new Button(composite, SWT.PUSH);
		fBrowseButton.setText(PDEUIMessages.FeatureImportWizard_FirstPage_browse); 
		fBrowseButton.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(fBrowseButton);

		creatFeatureTable(composite);
		
		fBinaryButton = new Button(composite, SWT.CHECK);
		GridData gd = fillHorizontal(fBinaryButton, 3, false);
		gd.verticalIndent = 5;
		fBinaryButton.setText(PDEUIMessages.FeatureImportWizard_FirstPage_binaryImport); 
		
		initializeFields(getDialogSettings());
		hookListeners();

		setControl(composite);
		dialogChanged();
		Dialog.applyDialogFont(composite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.FEATURE_IMPORT_FIRST_PAGE);
	}

	private String getTargetHome() {
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		return preferences.getString(ICoreConstants.PLATFORM_PATH);
	}

	private void hookListeners() {
		fRuntimeLocationButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setOtherEnabled(!fRuntimeLocationButton.getSelection());
				setLocation(fRuntimeLocationButton.getSelection() ? 
						getTargetHome() : fCurrDropLocation);
				loadFeatureTable();
			}
		});
		fDropLocation.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validateDropLocation();
				if (!fRuntimeLocationButton.getSelection()) {
					String newLoc = fDropLocation.getText();
					if (getMessageType() != WARNING &&
							!newLoc.equals(fCurrDropLocation) &&
							!fSelfSetLocation)
						setMessage(PDEUIMessages.FeatureImportWizardPage_reloadLocation, WARNING);
					fCurrDropLocation = newLoc;
				}
			}
		});
		fBrowseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IPath chosen = chooseDropLocation();
				if (chosen != null) {
					setLocation(chosen.toOSString());
					loadFeatureTable();
				}
			}
		});
	}

	private GridData fillHorizontal(Control control, int span, boolean grab) {
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = span;
		gd.grabExcessHorizontalSpace = grab;
		control.setLayoutData(gd);
		return gd;
	}

	private void initializeFields(IDialogSettings initialSettings) {
		String[] dropItems = new String[0];
		boolean doOther = false;
		boolean binary = true;

		if (initialSettings != null) {
			doOther = initialSettings.getBoolean(SETTINGS_DOOTHER);
			binary = !initialSettings.getBoolean(SETTINGS_NOT_BINARY);

			ArrayList items = new ArrayList();
			for (int i = 0; i < 6; i++) {
				String curr = initialSettings.get(SETTINGS_DROPLOCATION + String.valueOf(i));
				if (curr != null && !items.contains(curr))
					items.add(curr);
			}
			if (items.size() == 0)
				items.add(""); //$NON-NLS-1$
			dropItems = (String[]) items.toArray(new String[items.size()]);
		}
		fDropLocation.setItems(dropItems);
		fRuntimeLocationButton.setSelection(!doOther);
		setOtherEnabled(doOther);
		fCurrDropLocation = doOther ? dropItems[0] : getTargetHome();
		setLocation(fCurrDropLocation);
		fBinaryButton.setSelection(binary);

		validateDropLocation();
		
		loadFeatureTable();
	}

	private void setOtherEnabled(boolean enabled) {
		fOtherLocationLabel.setEnabled(enabled);
		fDropLocation.setEnabled(enabled);
		fBrowseButton.setEnabled(enabled);
	}

	public void storeSettings(boolean finishPressed) {
		IDialogSettings settings = getDialogSettings();
		boolean other = !fRuntimeLocationButton.getSelection();
		boolean binary = fBinaryButton.getSelection();
		if (finishPressed || fDropLocation.getText().length() > 0 && other) {
			settings.put(SETTINGS_DROPLOCATION + String.valueOf(0), fDropLocation.getText());
			String[] items = fDropLocation.getItems();
			int nEntries = Math.min(items.length, 5);
			for (int i = 0; i < nEntries; i++)
				settings.put(SETTINGS_DROPLOCATION + String.valueOf(i + 1), items[i]);
		}
		if (finishPressed) {
			settings.put(SETTINGS_DOOTHER, other);
			settings.put(SETTINGS_NOT_BINARY, !binary);
		}
	}

	/**
	 * Browses for a drop location.
	 */
	private IPath chooseDropLocation() {
		DirectoryDialog dialog = new DirectoryDialog(getShell());
		dialog.setFilterPath(fDropLocation.getText());
		dialog.setText(PDEUIMessages.FeatureImportWizard_messages_folder_title); 
		dialog.setMessage(PDEUIMessages.FeatureImportWizard_messages_folder_message); 
		String res = dialog.open();
		return res != null ? new Path(res) : null;
	}

	private void validateDropLocation() {
		String errorMessage = null;
		if (isOtherLocation()) {
			IPath curr = getDropLocation();
			if (curr.segmentCount() == 0)
				errorMessage = PDEUIMessages.FeatureImportWizard_errors_locationMissing; 
			else if (!Path.ROOT.isValidPath(fDropLocation.getText()))
				errorMessage = PDEUIMessages.FeatureImportWizard_errors_buildFolderInvalid; 
			else {
				File file = curr.toFile();
				if (!file.exists() || !file.isDirectory())
					errorMessage = PDEUIMessages.FeatureImportWizard_errors_buildFolderMissing; 
			}
		}
		setErrorMessage(errorMessage);
		setPageComplete(errorMessage == null);
		fTablePart.setButtonEnabled(0, errorMessage == null);
	}
	
	public boolean isBinary(){
		return fBinaryButton.getSelection();
	}

	/**
	 * Returns the drop location.
	 */
	public IPath getDropLocation() {
		return new Path(fDropLocation.getText().trim());
	}

	public boolean isOtherLocation() {
		return !fRuntimeLocationButton.getSelection();
	}

	private void loadFeatureTable() {
		IFeatureModel[] models = getModels();
		fFeatureViewer.setInput(PDEPlugin.getDefault());
		if (models != null) {
			// warning had been issued for unreloaded location
			if (getMessageType() == WARNING)
				setMessage(null, WARNING);
			if (!fRuntimeLocationButton.getSelection()) {	
				String currItem = fDropLocation.getText();
				if (fDropLocation.indexOf(currItem) == -1) {
					fDropLocation.add(currItem, 0);
					if (fDropLocation.getItemCount() > 6)
						fDropLocation.remove(6);
					storeSettings(false);
				}
			}
			fFeatureViewer.setCheckedElements(models);
		}
		fTablePart.updateCounter(models != null ? models.length : 0);
		fTablePart.getTableViewer().refresh();
		
		fTablePart.setButtonEnabled(1, models != null && models.length > 0);
		fTablePart.setButtonEnabled(2, models != null && models.length > 0);
		
		dialogChanged();
	}

	public void creatFeatureTable(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = layout.marginWidth = 0;
		container.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		gd.verticalIndent = 5;
		gd.heightHint = gd.widthHint = 300;
		container.setLayoutData(gd);
		
		fTablePart.createControl(container);
		fFeatureViewer = fTablePart.getTableViewer();
		fFeatureViewer.setContentProvider(new ContentProvider());
		fFeatureViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
	}

	public void dispose() {
		super.dispose();
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}

	public IFeatureModel[] getModels() {
		final IPath home = getDropLocation();
		final boolean useRuntimeLocation = fRuntimeLocationButton.getSelection() ||
			getTargetHome().equals(fDropLocation.getText().trim());
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				monitor.beginTask(PDEUIMessages.FeatureImportWizard_messages_updating, IProgressMonitor.UNKNOWN);
				ArrayList result = new ArrayList();
				if (useRuntimeLocation) {
					IFeatureModel[] allModels = PDECore.getDefault().getFeatureModelManager().getModels();
					for (int i = 0; i < allModels.length; i++)
						if (allModels[i].getUnderlyingResource() == null)
							result.add(allModels[i]);
				} else {
					MultiStatus errors = doLoadFeatures(result,	createPath(home));
					if (errors != null && errors.getChildren().length > 0)
						PDEPlugin.log(errors);
				}
				fModels = (IFeatureModel[])result.toArray(new IFeatureModel[result.size()]);
				monitor.done();
			}
		};
		IProgressService pservice = PlatformUI.getWorkbench().getProgressService();
		try {
			pservice.busyCursorWhile(runnable);
		} catch (InvocationTargetException e) {
		} catch (InterruptedException e) {
		}
		return fModels;
	}

	private File createPath(IPath dropLocation) {
		File featuresDir = new File(dropLocation.toFile(), "features"); //$NON-NLS-1$
		if (featuresDir.exists())
			return featuresDir;
		return null;
	}

	private MultiStatus doLoadFeatures(ArrayList result, File path) {
		if (path == null)
			return null;
		File[] dirs = path.listFiles();
		if (dirs == null)
			return null;
		ArrayList resultStatus = new ArrayList();
		for (int i = 0; i < dirs.length; i++) {
			File dir = dirs[i];
			if (dir.isDirectory()) {
				File manifest = new File(dir, "feature.xml"); //$NON-NLS-1$
				if (manifest.exists()) {
					IStatus status = doLoadFeature(dir, manifest, result);
					if (status != null)
						resultStatus.add(status);
				}
			}
		}
		return new MultiStatus(
				IPDEUIConstants.PLUGIN_ID, IStatus.OK,
				(IStatus[]) resultStatus.toArray(new IStatus[resultStatus.size()]),
				PDEUIMessages.FeatureImportWizard_DetailedPage_problemsLoading, 
				null);
	}

	private IStatus doLoadFeature(File dir, File manifest, ArrayList result) {
		ExternalFeatureModel model = new ExternalFeatureModel();
		model.setInstallLocation(dir.getAbsolutePath());
		IStatus status = null;

		InputStream stream = null;

		try {
			stream = new FileInputStream(manifest);
			model.load(stream, false);
			if(!model.isValid()){
				status = new Status(
						IStatus.WARNING, IPDEUIConstants.PLUGIN_ID, IStatus.OK,
						NLS.bind(PDEUIMessages.FeatureImportWizardPage_importHasInvalid, dir), null);
			}
		} catch (Exception e) {
			// Errors in the file
			status = new Status(IStatus.ERROR, IPDEUIConstants.PLUGIN_ID, IStatus.OK, e.getMessage(), e);
		}
		if (stream != null) {
			try {
				stream.close();
			} catch (IOException e) {
			}
		}
		if (status == null)
			result.add(model);
		return status;
	}

	public IFeatureModel[] getSelectedModels() { 
		Object[] selected = fFeatureViewer.getCheckedElements();
		IFeatureModel[] result = new IFeatureModel[selected.length];
		System.arraycopy(selected, 0, result, 0, selected.length);
		return result;
	}

	private void dialogChanged() {
		String message = null;
		if (fFeatureViewer != null && fFeatureViewer.getTable().getItemCount() == 0)
			message = PDEUIMessages.FeatureImportWizard_messages_noFeatures; 
		
		setMessage(message, IMessageProvider.INFORMATION);
		setPageComplete(fTablePart.getSelectionCount() > 0);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.WizardPage#isPageComplete()
	 */
	public boolean isPageComplete() {
		return fTablePart.getSelectionCount() > 0;
	}
	
	private void setLocation(String location) {
		fSelfSetLocation = true;
		fDropLocation.setText(location);
		fSelfSetLocation = false;
	}
}
