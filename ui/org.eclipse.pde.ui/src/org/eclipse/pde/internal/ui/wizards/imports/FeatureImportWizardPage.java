/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487943
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 526283
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.imports;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.feature.ExternalFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.SWTFactory;
import org.eclipse.pde.internal.ui.shared.CachedCheckboxTreeViewer;
import org.eclipse.pde.internal.ui.shared.FilteredCheckboxTree;
import org.eclipse.pde.internal.ui.shared.target.Messages;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.ui.progress.UIJob;

/**
 * Wizard page used when importing features.  Allows the user to choose a location (Default or a folder) to load features from
 * then select one or more features to import into the workspace.
 *
 * @see FeatureImportWizard
 */
public class FeatureImportWizardPage extends WizardPage {

	private static final String SETTINGS_DROPLOCATION = "droplocation"; //$NON-NLS-1$
	private static final String SETTINGS_DOOTHER = "doother"; //$NON-NLS-1$
	private static final String SETTINGS_NOT_BINARY = "notbinary"; //$NON-NLS-1$

	/**
	 * How long to wait before validating the directory
	 */
	protected static final int TYPING_DELAY = 300;

	private Label fOtherLocationLabel;
	private Button fRuntimeLocationButton;
	private Button fBrowseButton;
	private Combo fDropLocation;
	private boolean fSelfSetLocation;
	private String fCurrDropLocation;
	private Job fTextChangedJob;
	private CachedCheckboxTreeViewer fFeatureViewer;
	private Label fCounterLabel;
	private Button fSelectAllButton;
	private Button fDeselectAllButton;

	private IFeatureModel[] fModels;

	private Button fBinaryButton;

	class ContentProvider implements IStructuredContentProvider {
		@Override
		public Object[] getElements(Object parent) {
			if (fModels != null)
				return fModels;
			return new Object[0];
		}
	}

	private class LocationChangedJob extends UIJob {
		public LocationChangedJob(Display jobDisplay, String name) {
			super(jobDisplay, name);
		}

		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}

			validateDropLocation();
			if (!fRuntimeLocationButton.getSelection()) {
				String newLoc = fDropLocation.getText();
				if (getMessageType() != WARNING && !newLoc.equals(fCurrDropLocation) && !fSelfSetLocation) {
					handleReload();
				}
				fCurrDropLocation = newLoc;
			}

			return Status.OK_STATUS;
		}
	}

	public FeatureImportWizardPage() {
		super("FeatureImportWizardPage"); //$NON-NLS-1$
		setTitle(PDEUIMessages.FeatureImportWizard_FirstPage_title);
		setDescription(PDEUIMessages.FeatureImportWizard_FirstPage_desc);
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	@Override
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));

		fRuntimeLocationButton = new Button(composite, SWT.CHECK);
		fillHorizontal(fRuntimeLocationButton, 1, false);
		fRuntimeLocationButton.setText(PDEUIMessages.FeatureImportWizard_FirstPage_runtimeLocation);

		Composite otherLocationComposite = SWTFactory.createComposite(composite, 3, 1, GridData.FILL_HORIZONTAL, 0, 0);

		fOtherLocationLabel = new Label(otherLocationComposite, SWT.NULL);
		fOtherLocationLabel.setText(PDEUIMessages.FeatureImportWizard_FirstPage_otherFolder);

		fDropLocation = new Combo(otherLocationComposite, SWT.DROP_DOWN);
		fillHorizontal(fDropLocation, 1, true);

		fBrowseButton = new Button(otherLocationComposite, SWT.PUSH);
		fBrowseButton.setText(PDEUIMessages.FeatureImportWizard_FirstPage_browse);
		fBrowseButton.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(fBrowseButton);

		createFeatureTable(composite);

		Composite underTableComp = SWTFactory.createComposite(composite, 2, 1, GridData.FILL_HORIZONTAL, 0, 0);

		fCounterLabel = SWTFactory.createLabel(underTableComp, "", 1); //$NON-NLS-1$
		fCounterLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false));
		Composite buttonComp = SWTFactory.createComposite(underTableComp, 2, 1, SWT.NONE, 0, 0);
		buttonComp.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));

		fSelectAllButton = SWTFactory.createPushButton(buttonComp, PDEUIMessages.WizardCheckboxTablePart_selectAll, null);
		fSelectAllButton.addSelectionListener(widgetSelectedAdapter(e -> {
			fFeatureViewer.setAllChecked(true);
			updateCounter();
			dialogChanged();
		}));
		fDeselectAllButton = SWTFactory.createPushButton(buttonComp, PDEUIMessages.WizardCheckboxTablePart_deselectAll, null);
		fDeselectAllButton.addSelectionListener(widgetSelectedAdapter(e -> {
			fFeatureViewer.setAllChecked(false);
			updateCounter();
			dialogChanged();
		}));

		fBinaryButton = SWTFactory.createCheckButton(composite, PDEUIMessages.FeatureImportWizard_FirstPage_binaryImport, null, true, 1);

		initializeFields(getDialogSettings());
		hookListeners();

		setControl(composite);
		updateCounter();
		dialogChanged();
		Dialog.applyDialogFont(composite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.FEATURE_IMPORT_FIRST_PAGE);
	}

	private void hookListeners() {
		fRuntimeLocationButton.addSelectionListener(widgetSelectedAdapter(e -> {
			setOtherEnabled(!fRuntimeLocationButton.getSelection());
			setLocation(fRuntimeLocationButton.getSelection() ? TargetPlatform.getLocation() : fCurrDropLocation);
			handleReload();
		}));

		fDropLocation.addModifyListener(e -> {
			// If the text is a combo item, immediately try to resolve,
			// otherwise wait in case they type more
			boolean isItem = false;
			String[] items = fDropLocation.getItems();
			for (String item : items) {
				if (fDropLocation.getText().equals(item)) {
					isItem = true;
					break;
				}
			}
			locationChanged(isItem ? 0 : TYPING_DELAY);
		});

		fBrowseButton.addSelectionListener(widgetSelectedAdapter(e -> {
			IPath chosen = chooseDropLocation();
			if (chosen != null) {
				setLocation(chosen.toOSString());
				handleReload();
			}
		}));
	}

	/**
	 * Called whenever the location or another aspect of the container has changed
	 * in the UI.  Will schedule a UIJob to verify and resolve the container
	 * reporting any problems to the user.  If a previous job is running or sleeping
	 * it will be cancelled.
	 *
	 * @param delay a delay to add to the job scheduling
	 */
	protected void locationChanged(long delay) {
		if (fTextChangedJob == null) {
			fTextChangedJob = new LocationChangedJob(getShell().getDisplay(), Messages.EditDirectoryContainerPage_3);
		} else {
			fTextChangedJob.cancel();
		}
		fTextChangedJob.schedule(delay);
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

			ArrayList<String> items = new ArrayList<>();
			for (int i = 0; i < 6; i++) {
				String curr = initialSettings.get(SETTINGS_DROPLOCATION + String.valueOf(i));
				if (curr != null && !items.contains(curr))
					items.add(curr);
			}
			if (items.isEmpty())
				items.add(""); //$NON-NLS-1$
			dropItems = items.toArray(new String[items.size()]);
		}
		fDropLocation.setItems(dropItems);
		fRuntimeLocationButton.setSelection(!doOther);
		setOtherEnabled(doOther);
		fCurrDropLocation = doOther ? dropItems[0] : TargetPlatform.getLocation();
		setLocation(fCurrDropLocation);
		fBinaryButton.setSelection(binary);

		validateDropLocation();

		handleReload();
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
	}

	public boolean isBinary() {
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

	private void handleReload() {
		IFeatureModel[] models = getModels();
		fFeatureViewer.setInput(models);
		if (models != null) {
			if (!fRuntimeLocationButton.getSelection()) {
				String currItem = fDropLocation.getText();
				if (models.length > 0 && fDropLocation.indexOf(currItem) == -1) {
					fDropLocation.add(currItem, 0);
					if (fDropLocation.getItemCount() > 6)
						fDropLocation.remove(6);
					storeSettings(false);
				}

				// When the models are loaded, the entire contents of the combo get selected.  This can easily lead to overwriting the contents so we instead select the end.
				fDropLocation.setSelection(new Point(fDropLocation.getText().length(), fDropLocation.getText().length()));

			}
			fFeatureViewer.setCheckedElements(models);

		}
		updateCounter();
		dialogChanged();
	}

	public void createFeatureTable(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		container.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.verticalIndent = 5;
		gd.heightHint = gd.widthHint = 300;
		container.setLayoutData(gd);

		PatternFilter filter = new PatternFilter();
		filter.setIncludeLeadingWildcard(true);
		FilteredCheckboxTree tree = new FilteredCheckboxTree(container, null, SWT.NONE, filter);
		fFeatureViewer = tree.getCheckboxTreeViewer();
		fFeatureViewer.setContentProvider(new ITreeContentProvider() {

			@Override
			public Object[] getElements(Object inputElement) {
				if (inputElement instanceof Object[]) {
					return (Object[]) inputElement;
				}
				return new Object[0];
			}

			@Override
			public boolean hasChildren(Object element) {
				return false;
			}

			@Override
			public Object getParent(Object element) {
				return null;
			}

			@Override
			public Object[] getChildren(Object parentElement) {
				return new Object[0];
			}
		});
		fFeatureViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		fFeatureViewer.setComparator(ListUtil.FEATURE_COMPARATOR);
		fFeatureViewer.addCheckStateListener(event -> {
			updateCounter();
			dialogChanged();
		});
		fFeatureViewer.addDoubleClickListener(event -> {
			IStructuredSelection selection = fFeatureViewer.getStructuredSelection();
			if (!selection.isEmpty()) {
				Object selected = selection.getFirstElement();
				fFeatureViewer.setChecked(selected, !fFeatureViewer.getChecked(selected));
				updateCounter();
				dialogChanged();
			}
		});

	}

	private void updateCounter() {
		int total = 0;
		if (fModels != null) {
			total = fModels.length;
		}
		int checked = fFeatureViewer.getCheckedLeafCount();
		fCounterLabel.setText(NLS.bind(PDEUIMessages.WizardCheckboxTablePart_counter, Integer.toString(checked),
				Integer.toString(total)));
	}

	@Override
	public void dispose() {
		super.dispose();
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}

	public IFeatureModel[] getModels() {
		final IPath home = getDropLocation();
		final boolean useRuntimeLocation = fRuntimeLocationButton.getSelection() || TargetPlatform.getLocation().equals(fDropLocation.getText().trim());
		IRunnableWithProgress runnable = monitor -> {
			monitor.beginTask(PDEUIMessages.FeatureImportWizard_messages_updating, IProgressMonitor.UNKNOWN);
			ArrayList<IFeatureModel> result = new ArrayList<>();
			if (useRuntimeLocation) {
				IFeatureModel[] allModels = PDECore.getDefault().getFeatureModelManager().getModels();
				for (IFeatureModel model : allModels)
					if (model.getUnderlyingResource() == null)
						result.add(model);
			} else {
				MultiStatus errors = doLoadFeatures(result, createPath(home));
				if (errors != null && errors.getChildren().length > 0)
					PDEPlugin.log(errors);
			}
			fModels = result.toArray(new IFeatureModel[result.size()]);
			monitor.done();
		};

		IProgressService pservice = PlatformUI.getWorkbench().getProgressService();
		try {
			pservice.busyCursorWhile(runnable);
		} catch (InvocationTargetException | InterruptedException e) {
		}
		return fModels;
	}

	private File createPath(IPath dropLocation) {
		File featuresDir = new File(dropLocation.toFile(), ICoreConstants.FEATURE_FOLDER_NAME);
		if (featuresDir.exists())
			return featuresDir;
		return null;
	}

	private MultiStatus doLoadFeatures(ArrayList<IFeatureModel> result, File path) {
		if (path == null)
			return null;
		File[] dirs = path.listFiles();
		if (dirs == null)
			return null;
		ArrayList<IStatus> resultStatus = new ArrayList<>();
		for (File dir : dirs) {
			if (dir.isDirectory()) {
				File manifest = new File(dir, ICoreConstants.FEATURE_FILENAME_DESCRIPTOR);
				if (manifest.exists()) {
					IStatus status = doLoadFeature(dir, manifest, result);
					if (status != null)
						resultStatus.add(status);
				}
			}
		}
		return new MultiStatus(IPDEUIConstants.PLUGIN_ID, IStatus.OK, resultStatus.toArray(new IStatus[resultStatus.size()]), PDEUIMessages.FeatureImportWizard_DetailedPage_problemsLoading, null);
	}

	private IStatus doLoadFeature(File dir, File manifest, ArrayList<IFeatureModel> result) {
		ExternalFeatureModel model = new ExternalFeatureModel();
		model.setInstallLocation(dir.getAbsolutePath());
		IStatus status = null;

		try (InputStream stream = new BufferedInputStream(new FileInputStream(manifest))) {

			model.load(stream, false);
			if (!model.isValid()) {
				status = Status.warning(NLS.bind(PDEUIMessages.FeatureImportWizardPage_importHasInvalid, dir));
			}
		} catch (Exception e) {
			// Errors in the file
			status = Status.error(e.getMessage(), e);
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
		if (fFeatureViewer != null && (fModels == null || fModels.length == 0)) {
			message = PDEUIMessages.FeatureImportWizard_messages_noFeatures;
		}

		setMessage(message, IMessageProvider.INFORMATION);
		setPageComplete(fFeatureViewer.getCheckedLeafCount() > 0);
	}

	@Override
	public boolean isPageComplete() {
		return fFeatureViewer.getCheckedLeafCount() > 0;
	}

	private void setLocation(String location) {
		fSelfSetLocation = true;
		fDropLocation.setText(location);
		fSelfSetLocation = false;
	}

}
