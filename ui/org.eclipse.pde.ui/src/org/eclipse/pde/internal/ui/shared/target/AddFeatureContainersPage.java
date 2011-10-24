/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import org.eclipse.pde.core.target.ITargetLocation;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.*;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.feature.ExternalFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

/**
 * Wizard page for creating one or more feature bundle containers.
 * 
 * @see AddBundleContainerWizard
 * @see AddBundleContainerSelectionPage
 * @see ITargetLocation
 */
public class AddFeatureContainersPage extends EditDirectoryContainerPage {

	private Button fSelectAllButton;
	private Button fDeselectAllButton;
	private Button fIncludeVersionButton;
	private CheckboxTableViewer fFeatureTable;

	protected AddFeatureContainersPage() {
		super(null, "AddFeatureContainers"); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.shared.target.EditDirectoryContainerPage#getDefaultTitle()
	 */
	protected String getDefaultTitle() {
		return Messages.AddFeatureContainerPage_0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.shared.target.EditDirectoryContainerPage#getDefaultMessage()
	 */
	protected String getDefaultMessage() {
		return Messages.AddFeatureContainerPage_1;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.shared.target.EditDirectoryContainerPage#createLocationArea(org.eclipse.swt.widgets.Composite)
	 */
	protected void createLocationArea(Composite parent) {
		super.createLocationArea(parent);
		createTableArea(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IHelpContextIds.LOCATION_ADD_FEATURE_WIZARD);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.shared.target.AddDirectoryContainerPage#createTableArea(org.eclipse.swt.widgets.Composite)
	 */
	protected void createTableArea(Composite parent) {
		Composite tableComp = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_BOTH, 0, 0);

		SWTFactory.createLabel(tableComp, Messages.AddFeatureContainersPage_2, 2);

		fFeatureTable = CheckboxTableViewer.newCheckList(tableComp, SWT.BORDER);
		// Connect the label provider
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		fFeatureTable.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		fFeatureTable.setContentProvider(new ArrayContentProvider());
		fFeatureTable.setSorter(new ViewerSorter());

		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.heightHint = 300;
		fFeatureTable.getControl().setLayoutData(data);
		fFeatureTable.getControl().setFont(tableComp.getFont());

		fFeatureTable.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				AddFeatureContainersPage.this.checkStateChanged();
			}
		});
		fFeatureTable.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				if (!event.getSelection().isEmpty()) {
					Object selection = ((IStructuredSelection) event.getSelection()).getFirstElement();
					fFeatureTable.setChecked(selection, !fFeatureTable.getChecked(selection));
					checkStateChanged();
				}
			}
		});

		Composite buttonComp = SWTFactory.createComposite(tableComp, 1, 1, GridData.FILL_VERTICAL, 0, 0);
		fSelectAllButton = SWTFactory.createPushButton(buttonComp, Messages.AddFeatureContainersPage_0, null);
		fSelectAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fFeatureTable.setAllChecked(true);
				checkStateChanged();
			}
		});
		fDeselectAllButton = SWTFactory.createPushButton(buttonComp, Messages.AddFeatureContainersPage_1, null);
		fDeselectAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fFeatureTable.setAllChecked(false);
				checkStateChanged();
			}
		});

		fIncludeVersionButton = SWTFactory.createCheckButton(tableComp, Messages.AddFeatureContainerPage_3, null, false, 2);
	}

	/**
	 * Updates the buttons and messages based on what is checked in the table
	 */
	private void checkStateChanged() {
		if (fFeatureTable.getControl().isEnabled()) {
			int count = fFeatureTable.getCheckedElements().length;
			int total = ((Object[]) fFeatureTable.getInput()).length;
			fSelectAllButton.setEnabled(count != total);
			fDeselectAllButton.setEnabled(count != 0);
			setPageComplete(count > 0);
			setErrorMessage(count > 0 ? null : Messages.AddFeatureContainerPage_2);
		} else {
			fSelectAllButton.setEnabled(false);
			fDeselectAllButton.setEnabled(false);
			setPageComplete(false);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.shared.target.EditDirectoryContainerPage#containerChanged(long)
	 */
	protected void containerChanged(long delay) {
		if (fInstallLocation.getText().trim().length() > 0) {
			try {
				// Resolve any variables
				String locationString = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(fInstallLocation.getText().trim());
				File location = new File(locationString);

				// check if the directory exists
				if (location == null || !location.isDirectory()) {
					fFeatureTable.setInput(new String[] {Messages.AddFeatureContainerPage_4});
					setErrorMessage(Messages.AddDirectoryContainerPage_6);
				} else {
					// The bundle container home must be the directory containing the feature folder, but we should accept either as input
					if (!location.getName().equalsIgnoreCase(ICoreConstants.FEATURE_FOLDER_NAME)) {
						File featureDir = new File(location, ICoreConstants.FEATURE_FOLDER_NAME);
						if (featureDir.isDirectory()) {
							location = featureDir;
						}
					}
					List result = new ArrayList();
					MultiStatus errors = doLoadFeatures(result, location);
					if (errors != null && errors.getChildren().length > 0) {
						setErrorMessage(errors.getChildren()[0].getMessage());
					} else {
						setErrorMessage(null);
					}
					if (result.size() > 0) {
						fFeatureTable.setInput(result.toArray(new IFeatureModel[result.size()]));
						fFeatureTable.getControl().setEnabled(true);
						checkStateChanged();
						setErrorMessage(null);
						storeSettings();
					} else {
						fFeatureTable.setInput(new String[] {Messages.AddFeatureContainerPage_4});
						fFeatureTable.getControl().setEnabled(false);
						checkStateChanged();
					}
				}
			} catch (CoreException e) {
				PDEPlugin.log(e);
				fFeatureTable.setInput(new String[] {Messages.AddFeatureContainerPage_4});
				fFeatureTable.getControl().setEnabled(false);
				checkStateChanged();
				setErrorMessage(e.getMessage());
			}

		} else {
			fFeatureTable.setInput(new String[] {Messages.AddFeatureContainerPage_4});
			fFeatureTable.getControl().setEnabled(false);
			checkStateChanged();
			setPageComplete(false);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.DialogPage#dispose()
	 */
	public void dispose() {
		// Disconnect the label provider so it can be disposed
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}

	/**
	 * This wizard will be returning multiple containers, not just one so we can't just use {@link #getTargetLocation()}
	 * @return array of containers created by this wizard
	 * @throws CoreException if there was a problem acquiring the target service
	 */
	public ITargetLocation[] getBundleContainers() throws CoreException {
		Object[] elements = fFeatureTable.getCheckedElements();
		List containers = new ArrayList(elements.length);
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] instanceof IFeatureModel) {
				String version = null;
				if (fIncludeVersionButton.getSelection()) {
					version = ((IFeatureModel) elements[i]).getFeature().getVersion();
				}
				File location = new File(fInstallLocation.getText());
				// The bundle container home must be the directory containing the feature folder, but we should accept either as input
				if (location.getName().equalsIgnoreCase(ICoreConstants.FEATURE_FOLDER_NAME)) {
					location = location.getParentFile();
				}

				containers.add(getTargetPlatformService().newFeatureLocation(location.getPath(), ((IFeatureModel) elements[i]).getFeature().getId(), version));
			}
		}
		if (containers.size() == 0) {
			return null;
		}
		return (ITargetLocation[]) containers.toArray(new ITargetLocation[containers.size()]);
	}

	/**
	 * Loads the features in a directory
	 * 
	 * @param result list to put generated IFeatureModel objects into
	 * @param path location to search for features
	 * @return multi-status containing any problems that occurred
	 */
	private MultiStatus doLoadFeatures(List result, File path) {
		if (path == null)
			return null;
		File[] dirs = path.listFiles();
		if (dirs == null)
			return null;
		ArrayList resultStatus = new ArrayList();
		for (int i = 0; i < dirs.length; i++) {
			File dir = dirs[i];
			if (dir.isDirectory()) {
				File manifest = new File(dir, ICoreConstants.FEATURE_FILENAME_DESCRIPTOR);
				if (manifest.exists()) {
					IStatus status = doLoadFeature(dir, manifest, result);
					if (status != null)
						resultStatus.add(status);
				}
			}
		}
		return new MultiStatus(IPDEUIConstants.PLUGIN_ID, IStatus.OK, (IStatus[]) resultStatus.toArray(new IStatus[resultStatus.size()]), Messages.FeatureImportWizard_DetailedPage_problemsLoading, null);
	}

	/**
	 * Creates a IFeatureModel representing the feature in the given directory and adds it
	 * to the result list.  
	 * @param dir direcotry where the feature resides
	 * @param manifest manifest file of the feature
	 * @param result list to add the result to
	 * @return status object if there is a problem or <code>null</code>
	 */
	private IStatus doLoadFeature(File dir, File manifest, List result) {
		ExternalFeatureModel model = new ExternalFeatureModel();
		model.setInstallLocation(dir.getAbsolutePath());
		IStatus status = null;

		InputStream stream = null;

		try {
			stream = new BufferedInputStream(new FileInputStream(manifest));
			model.load(stream, false);
			if (!model.isValid()) {
				status = new Status(IStatus.WARNING, IPDEUIConstants.PLUGIN_ID, IStatus.OK, NLS.bind(Messages.FeatureImportWizardPage_importHasInvalid, dir), null);
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

}