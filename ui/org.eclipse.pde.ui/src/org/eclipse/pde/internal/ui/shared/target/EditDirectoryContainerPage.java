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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.AbstractBundleContainer;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;

/**
 * Wizard page for creating a new directory bundle container.
 * 
 * @see AddBundleContainerWizard
 * @see AddBundleContainerSelectionPage
 * @see ITargetLocation
 */
public class EditDirectoryContainerPage extends WizardPage implements IEditBundleContainerPage {

	/**
	 * How long to wait before validating the directory
	 */
	protected static final int TYPING_DELAY = 200;

	private static ITargetPlatformService fTargetService;
	protected Combo fInstallLocation;
	protected ITargetLocation fContainer;
	private Job fTextChangedJob;

	/**
	 * Dialog settings key for the most recent location
	 */
	private static final String SETTINGS_LOCATION_1 = "location1"; //$NON-NLS-1$

	/**
	 * Dialog settings key for the second most recent location
	 */
	private static final String SETTINGS_LOCATION_2 = "location2"; //$NON-NLS-1$

	/**
	 * Dialog settings key for the third most recent location 
	 */
	private static final String SETTINGS_LOCATION_3 = "location3"; //$NON-NLS-1$

	protected EditDirectoryContainerPage(ITargetLocation container) {
		this();
		fContainer = container;
	}

	protected EditDirectoryContainerPage() {
		super("EditDirectoryContainer"); //$NON-NLS-1$
	}

	public EditDirectoryContainerPage(ITargetLocation container, String name) {
		super(name);
		fContainer = container;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		setMessage(getDefaultMessage());
		setTitle(getDefaultTitle());
		setPageComplete(false);
		Composite comp = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH, 0, 0);
		createLocationArea(comp);
		setControl(comp);
		initializeInputFields(fContainer);
		if ("EditDirectoryContainer".equalsIgnoreCase(getName())) { //$NON-NLS-1$
			if (fContainer == null) {
				PlatformUI.getWorkbench().getHelpSystem().setHelp(comp, IHelpContextIds.LOCATION_ADD_DIRECTORY_WIZARD);
			} else {
				PlatformUI.getWorkbench().getHelpSystem().setHelp(comp, IHelpContextIds.LOCATION_EDIT_DIRECTORY_WIZARD);
			}
		}
	}

	/**
	 * @return the default title for this wizard page
	 */
	protected String getDefaultTitle() {
		if (fContainer == null) {
			return Messages.AddDirectoryContainerPage_0;
		}
		return Messages.EditDirectoryContainerPage_0;
	}

	/**
	 * @return the default message for this wizard page
	 */
	protected String getDefaultMessage() {
		return Messages.AddDirectoryContainerPage_1;
	}

	/**
	 * Creates the area at the top of the page.  Contains an entry form for a location path.
	 * This method may be overridden by subclasses to provide custom widgets
	 * @param parent parent composite
	 */
	protected void createLocationArea(Composite parent) {
		Composite locationComp = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_HORIZONTAL, 0, 0);

		SWTFactory.createLabel(locationComp, Messages.AddDirectoryContainerPage_2, 1);

		fInstallLocation = SWTFactory.createCombo(locationComp, SWT.BORDER, 1, getLocationComboItems());
		fInstallLocation.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				// If the text is a combo item, immediately try to resolve, otherwise wait in case they type more
				boolean isItem = false;
				String[] items = fInstallLocation.getItems();
				for (int i = 0; i < items.length; i++) {
					if (fInstallLocation.getText().equals(items[i])) {
						isItem = true;
						break;
					}
				}
				containerChanged(isItem ? 0 : TYPING_DELAY);
			}
		});
		if (fContainer instanceof AbstractBundleContainer) {
			try {
				String location = ((AbstractBundleContainer) fContainer).getLocation(false);
				fInstallLocation.setText(location);
			} catch (CoreException e) {
				setErrorMessage(e.getMessage());
			}

		}

		Composite buttonComp = SWTFactory.createComposite(locationComp, 2, 2, GridData.CENTER, 0, 0);
		GridData gd = (GridData) buttonComp.getLayoutData();
		gd.horizontalAlignment = SWT.RIGHT;

		Button browseButton = SWTFactory.createPushButton(buttonComp, Messages.AddDirectoryContainerPage_3, null);
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

		Button variablesButton = SWTFactory.createPushButton(buttonComp, Messages.EditDirectoryContainerPage_1, null);
		variablesButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				StringVariableSelectionDialog dialog = new StringVariableSelectionDialog(getShell());
				dialog.open();
				String variable = dialog.getVariableExpression();
				if (variable != null) {
					fInstallLocation.setText(fInstallLocation.getText() + variable);
				}
			}
		});
	}

	/**
	 * Initializes the fields use to describe the container.  They should be filled in using
	 * the given container or set to default values if the container is <code>null</code>.
	 * @param container bundle container being edited, possibly <code>null</code>
	 */
	protected void initializeInputFields(ITargetLocation container) {
		if (container instanceof AbstractBundleContainer) {
			try {
				String currentLocation = ((AbstractBundleContainer) container).getLocation(false);
				boolean found = false;
				String[] items = fInstallLocation.getItems();
				for (int i = 0; i < items.length; i++) {
					if (items[i].equals(currentLocation)) {
						found = true;
						break;
					}
				}
				if (!found) {
					fInstallLocation.add(currentLocation);
				}
				fInstallLocation.setText(currentLocation);

				containerChanged(0);
			} catch (CoreException e) {
				PDEPlugin.log(e);
			}
		} else {
			fInstallLocation.setText(""); //$NON-NLS-1$
		}
	}

	/**
	 * @return a list of previous locations from settings plus the default location
	 */
	private String[] getLocationComboItems() {
		List previousLocations = new ArrayList(4);
		IDialogSettings settings = getDialogSettings();
		if (settings != null) {
			String location = settings.get(SETTINGS_LOCATION_1);
			if (location != null) {
				previousLocations.add(location);
			}
			location = settings.get(SETTINGS_LOCATION_2);
			if (location != null) {
				previousLocations.add(location);
			}
			location = settings.get(SETTINGS_LOCATION_3);
			if (location != null) {
				previousLocations.add(location);
			}
		}
		previousLocations.add(getDefaultLocation());
		return (String[]) previousLocations.toArray(new String[previousLocations.size()]);
	}

	/**
	 * @return the default text to add as a combo item to the location combo
	 */
	protected String getDefaultLocation() {
		return "${eclipse_home}"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.shared.target.IEditTargetLocationPage#storeSettings()
	 */
	public void storeSettings() {
		String newLocation = fInstallLocation.getText().trim();

		if (newLocation.charAt(newLocation.length() - 1) == File.separatorChar) {
			newLocation = newLocation.substring(0, newLocation.length() - 1);
		}
		String[] items = fInstallLocation.getItems();
		for (int i = 0; i < items.length; i++) {
			if (items[i].equals(newLocation)) {
				// Already have this location stored
				return;
			}
		}
		IDialogSettings settings = getDialogSettings();
		if (settings != null) {
			String location = settings.get(SETTINGS_LOCATION_2);
			if (location != null) {
				settings.put(SETTINGS_LOCATION_3, location);
			}
			location = settings.get(SETTINGS_LOCATION_1);
			if (location != null) {
				settings.put(SETTINGS_LOCATION_2, location);
			}
			settings.put(SETTINGS_LOCATION_1, newLocation);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.shared.target.IEditTargetLocationPage#getBundleContainer()
	 */
	public ITargetLocation getBundleContainer() {
		return fContainer;
	}

	/**
	 * Called whenever the location or another aspect of the container has changed
	 * in the UI.  Will schedule a UIJob to verify and resolve the container 
	 * reporting any problems to the user.  If a previous job is running or sleeping
	 * it will be cancelled.
	 * 
	 * @param delay a delay to add to the job scheduling
	 */
	protected void containerChanged(long delay) {
		if (fTextChangedJob == null) {
			fTextChangedJob = new CreateContainerJob(getShell().getDisplay(), Messages.EditDirectoryContainerPage_3);
		} else {
			fTextChangedJob.cancel();
		}
		fTextChangedJob.schedule(delay);
	}

	/**
	 * Validate the input fields before a container is created/edited.
	 * The page's enablement, message and completion should be updated.
	 * 
	 * @return whether the finish button should be enabled and container creation should continue
	 */
	protected boolean validateInput() {
		// Check if the text field is blank
		if (fInstallLocation.getText().trim().length() == 0) {
			setMessage(getDefaultMessage());
			return false;
		}

		// Resolve any variables
		String locationString = null;
		try {
			locationString = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(fInstallLocation.getText().trim());
		} catch (CoreException e) {
			setMessage(e.getMessage(), IMessageProvider.WARNING);
			return true;
		}
		File location = new File(locationString);

		// Check if directory exists
		if (!location.isDirectory()) {
			setMessage(Messages.AddDirectoryContainerPage_6, IMessageProvider.WARNING);
		} else {
			setMessage(getDefaultMessage());
		}
		return true;
	}

	/**
	 * Returns a bundle container based on the current inputs to the wizard.
	 * If a previous container is supplied, any relevant information it stores
	 * should be copied to the new container.
	 * 
	 * @param previous previous container to grab information from or <code>null</code> if a new container should be created
	 * @return a new or modified bundle container
	 * @throws CoreException
	 */
	protected ITargetLocation createContainer(ITargetLocation previous) throws CoreException {
		return getTargetPlatformService().newDirectoryLocation(fInstallLocation.getText());
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

	private class CreateContainerJob extends UIJob {
		public CreateContainerJob(Display jobDisplay, String name) {
			super(jobDisplay, name);
		}

		public IStatus runInUIThread(IProgressMonitor monitor) {
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}

			try {
				// Validate the location and any other text fields
				if (validateInput()) {
					// Create a container from the input
					fContainer = createContainer(fContainer);
					setPageComplete(true);
				} else {
					fContainer = null;
					setPageComplete(false);
				}
				return Status.OK_STATUS;
			} catch (CoreException e) {
				fContainer = null;
				setErrorMessage(e.getMessage());
				setPageComplete(false);
				return e.getStatus();
			}
		}
	}

}
