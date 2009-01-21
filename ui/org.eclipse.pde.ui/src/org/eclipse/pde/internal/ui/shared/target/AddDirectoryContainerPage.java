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

import java.io.File;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.provisional.IBundleContainer;
import org.eclipse.pde.internal.core.target.provisional.ITargetPlatformService;
import org.eclipse.pde.internal.ui.SWTFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;

/**
 * Wizard page for creating a new directory bundle container.
 * 
 * @see AddBundleContainerWizard
 * @see AddBundleContainerSelectionPage
 * @see IBundleContainer
 */
public class AddDirectoryContainerPage extends WizardPage {

	private static ITargetPlatformService fTargetService;
	protected Text fInstallLocation;
	protected TableViewer fTable;
	protected IBundleContainer fContainer;

	protected AddDirectoryContainerPage(String pageName) {
		super(pageName);
		setTitle(Messages.AddDirectoryContainerPage_0);
		setMessage(Messages.AddDirectoryContainerPage_1);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH);
		createLocationArea(comp);
		createTableArea(comp);
		setControl(comp);
	}

	/**
	 * Creates the area at the top of the page.  Contains an entry form for a location path.
	 * This method may be overridden by subclasses to provide custom widgets
	 * @param parent parent composite
	 */
	protected void createLocationArea(Composite parent) {
		Composite locationComp = SWTFactory.createComposite(parent, 3, 1, GridData.FILL_HORIZONTAL);

		SWTFactory.createLabel(locationComp, Messages.AddDirectoryContainerPage_2, 1);

		fInstallLocation = SWTFactory.createText(locationComp, SWT.BORDER, 1);
		fInstallLocation.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				locationChanged();
				updateTable();
			}
		});

		Button browseButton = SWTFactory.createPushButton(locationComp, Messages.AddDirectoryContainerPage_3, null);
		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(getShell());
				dialog.setFilterPath(fInstallLocation.getText());
				dialog.setText(Messages.AddDirectoryContainerPage_4);
				dialog.setMessage(Messages.AddDirectoryContainerPage_5);
				String result = dialog.open();
				if (result != null)
					fInstallLocation.setText(result);
			}
		});
	}

	/**
	 * Creates the area at the bottom of the page.  Contains a table intended to display
	 * the current resolved bundles.  This class may be overridden by subclasses to 
	 * provide custom widgets.
	 * @param parent parent composite
	 */
	protected void createTableArea(Composite parent) {
		// TODO Find a way to make the table read only (cannot be selected)
		fTable = new TableViewer(parent, SWT.BORDER);
		fTable.setLabelProvider(new BundleInfoLabelProvider());
		fTable.setContentProvider(new ArrayContentProvider());
		fTable.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		updateTable();
	}

	/**
	 * Called whenever the text of the location text box is changed.  Checks if the location
	 * is a valid directory and creates a bundle container if valid.  Subclasses to 
	 */
	protected void locationChanged() {
		fContainer = null;
		if (fInstallLocation.getText().trim().length() > 0) {
			File location = new File(fInstallLocation.getText());
			if (location == null || !location.isDirectory()) {
				setErrorMessage(Messages.AddDirectoryContainerPage_6);
			} else {
				try {
					fContainer = getTargetPlatformService().newDirectoryContainer(fInstallLocation.getText());
					setErrorMessage(null);
				} catch (CoreException ex) {
					setErrorMessage(ex.getMessage());
				}
			}
		} else {
			setErrorMessage(null);
		}
	}

	protected void updateTable() {
		if (fContainer == null) {
			fTable.setInput(new String[] {Messages.AddDirectoryContainerPage_7});
			fTable.getControl().setEnabled(false);
			setPageComplete(false);
		} else {
			try {
				BundleInfo[] bundles = fContainer.resolveBundles(null);
				if (bundles == null || bundles.length == 0) {
					fTable.setInput(new String[] {Messages.AddDirectoryContainerPage_7});
					fTable.getControl().setEnabled(false);
				} else {
					fTable.getControl().setEnabled(true);
					fTable.setInput(bundles);
				}
				setPageComplete(true);
			} catch (CoreException e) {
				setErrorMessage(e.getMessage());
				setPageComplete(false);
			}
		}
	}

	/**
	 * Gets the target platform service provided by PDE Core
	 * @return the target platform service
	 * @throws CoreException if unable to acquire the service
	 */
	protected static ITargetPlatformService getTargetPlatformService() throws CoreException {
		if (fTargetService == null) {
			fTargetService = (ITargetPlatformService) PDECore.getDefault().acquireService(ITargetPlatformService.class.getName());
			if (fTargetService == null) {
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.AddDirectoryContainerPage_9));
			}
		}
		return fTargetService;
	}

	/**
	 * @return the bundle container created from the specified location
	 */
	public IBundleContainer getBundleContainer() {
		return fContainer;
	}

}
