/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.feature.ExternalFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.target.provisional.IBundleContainer;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

/**
 * Wizard page for creating one or more feature bundle containers.
 * 
 * @see AddBundleContainerWizard
 * @see AddBundleContainerSelectionPage
 * @see IBundleContainer
 */
public class AddFeatureContainerPage extends AddDirectoryContainerPage {

	private Button fIncludeVersionButton;

	protected AddFeatureContainerPage(String pageName) {
		super(pageName);
		setTitle(Messages.AddFeatureContainerPage_0);
		setMessage(Messages.AddFeatureContainerPage_1);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.shared.target.AddDirectoryContainerPage#createTableArea(org.eclipse.swt.widgets.Composite)
	 */
	protected void createTableArea(Composite parent) {
		fTable = CheckboxTableViewer.newCheckList(parent, SWT.BORDER);
		// Connect the label provider
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		fTable.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		fTable.setContentProvider(new ArrayContentProvider());
		fTable.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		((CheckboxTableViewer) fTable).addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				boolean checked = ((CheckboxTableViewer) fTable).getCheckedElements().length > 0;
				setPageComplete(checked);
				if (checked) {
					setErrorMessage(null);
				} else {
					setErrorMessage(Messages.AddFeatureContainerPage_2);
				}

			}
		});
		updateTable();

		fIncludeVersionButton = SWTFactory.createCheckButton(parent, Messages.AddFeatureContainerPage_3, null, false, 1);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.DialogPage#dispose()
	 */
	public void dispose() {
		// Disconnect the label provider so it can be disposed
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.shared.target.AddDirectoryContainerPage#locationChanged()
	 */
	protected void locationChanged() {
		// Do nothing, work is done in update table
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.shared.target.AddDirectoryContainerPage#updateTable()
	 */
	protected void updateTable() {
		if (fInstallLocation.getText().trim().length() > 0) {
			File location = new File(fInstallLocation.getText());
			if (location == null || !location.isDirectory()) {
				fTable.setInput(new String[] {Messages.AddFeatureContainerPage_4});
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
					fTable.setInput(result.toArray(new IFeatureModel[result.size()]));
					fTable.getControl().setEnabled(true);
				} else {
					fTable.setInput(new String[] {Messages.AddFeatureContainerPage_4});
					fTable.getControl().setEnabled(false);
					setPageComplete(false);
				}
			}
		} else {
			fTable.setInput(new String[] {Messages.AddFeatureContainerPage_4});
			fTable.getControl().setEnabled(false);
			setPageComplete(false);
			setErrorMessage(null);
		}

	}

	/**
	 * @return array of containers created by this wizard
	 * @throws CoreException if there was a problem acquiring the target service
	 */
	public IBundleContainer[] getContainers() throws CoreException {
		Object[] elements = ((CheckboxTableViewer) fTable).getCheckedElements();
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

				containers.add(getTargetPlatformService().newFeatureContainer(location.getPath(), ((IFeatureModel) elements[i]).getFeature().getId(), version));
			}
		}
		if (containers.size() == 0) {
			return null;
		}
		return (IBundleContainer[]) containers.toArray(new IBundleContainer[containers.size()]);
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
				File manifest = new File(dir, "feature.xml"); //$NON-NLS-1$
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