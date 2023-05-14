/*******************************************************************************
 * Copyright (c) 2009, 2022 IBM Corporation and others.
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
 *     Christoph Läubrich - Bug 577184 - [target] Allow references to other targets inside a target-file
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetHandle;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.RemoteTargetHandle;
import org.eclipse.pde.internal.core.target.TargetReferenceBundleContainer;
import org.eclipse.pde.internal.core.target.WorkspaceFileTargetHandle;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.SWTFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

/**
 * Wizard page for creating a new directory bundle container.
 *
 * @see AddBundleContainerWizard
 * @see AddBundleContainerSelectionPage
 * @see ITargetLocation
 */
public class EditTargetContainerPage extends WizardPage implements IEditBundleContainerPage {

	/**
	 * How long to wait before validating the directory
	 */
	protected static final int TYPING_DELAY = 200;

	protected Combo furiLocation;
	protected TargetReferenceBundleContainer fContainer;

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

	private ITargetDefinition targetDefinition;

	public EditTargetContainerPage(ITargetDefinition targetDefinition, TargetReferenceBundleContainer container) {
		super("EditTargetContainerPage"); //$NON-NLS-1$
		this.targetDefinition = targetDefinition;
		fContainer = container;
	}

	@Override
	public void createControl(Composite parent) {
		setMessage(getDefaultMessage());
		setTitle(getDefaultTitle());
		setPageComplete(false);
		Composite comp = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH, 0, 0);
		createLocationArea(comp);
		setControl(comp);
		initializeInputFields(fContainer);
	}

	/**
	 * @return the default title for this wizard page
	 */
	protected String getDefaultTitle() {
		if (fContainer == null) {
			return Messages.EditTargetContainerPage_Add_Title;
		}
		return Messages.EditTargetContainerPage_Edit_Title;
	}

	/**
	 * @return the default message for this wizard page
	 */
	protected String getDefaultMessage() {
		return Messages.EditTargetContainerPage_Message;
	}

	/**
	 * Creates the area at the top of the page. Contains an entry form for a
	 * location path. This method may be overridden by subclasses to provide
	 * custom widgets
	 *
	 * @param parent
	 *            parent composite
	 */
	protected void createLocationArea(Composite parent) {
		Composite locationComp = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_HORIZONTAL, 0, 0);

		SWTFactory.createLabel(locationComp, Messages.AddDirectoryContainerPage_2, 1);

		furiLocation = SWTFactory.createCombo(locationComp, SWT.BORDER, 1, getLocationComboItems());
		furiLocation.addModifyListener(e -> {
			setPageComplete(validateInput());
		});
		try {
			String location = fContainer != null ? fContainer.getLocation(false) : ""; //$NON-NLS-1$
			furiLocation.setText(location);
		} catch (CoreException e) {
			setErrorMessage(e.getMessage());
		}

		Composite buttonComp = SWTFactory.createComposite(locationComp, 2, 2, GridData.CENTER, 0, 0);
		GridData gd = (GridData) buttonComp.getLayoutData();
		gd.horizontalAlignment = SWT.RIGHT;

		Button variablesButton = SWTFactory.createPushButton(buttonComp, Messages.EditDirectoryContainerPage_1, null);
		variablesButton.addSelectionListener(widgetSelectedAdapter(e -> {
			StringVariableSelectionDialog dialog = new StringVariableSelectionDialog(getShell());
			dialog.open();
			String variable = dialog.getVariableExpression();
			if (variable != null) {
				furiLocation.setText(furiLocation.getText() + variable);
			}
		}));
	}

	/**
	 * Initializes the fields use to describe the container. They should be
	 * filled in using the given container or set to default values if the
	 * container is <code>null</code>.
	 *
	 * @param container
	 *            bundle container being edited, possibly <code>null</code>
	 */
	protected void initializeInputFields(ITargetLocation container) {
		try {
			String currentLocation = fContainer != null ? fContainer.getLocation(false) : ""; //$NON-NLS-1$
			boolean found = false;
			String[] items = furiLocation.getItems();
			for (String item : items) {
				if (item.equals(currentLocation)) {
					found = true;
					break;
				}
			}
			if (!found) {
				furiLocation.add(currentLocation);
			}
			furiLocation.setText(currentLocation);

			setPageComplete(validateInput());
		} catch (CoreException e) {
			PDEPlugin.log(e);
		}
	}

	@Override
	public boolean isPageComplete() {
		return !furiLocation.getText().isBlank();
	}

	/**
	 * @return a list of previous locations from settings plus the default
	 *         location
	 */
	private String[] getLocationComboItems() {
		List<String> previousLocations = new ArrayList<>(4);
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
		ITargetHandle handle = targetDefinition.getHandle();
		if (handle instanceof WorkspaceFileTargetHandle) {
			WorkspaceFileTargetHandle wsHandle = (WorkspaceFileTargetHandle) handle;
			String name = wsHandle.getTargetFile().getProject().getName();
			previousLocations.add(String.format("file:${project_loc:/%s}/", name)); //$NON-NLS-1$

			for (ITargetHandle targetHandle : PDECore.getDefault().acquireService(ITargetPlatformService.class)
					.getTargets(new NullProgressMonitor())) {
				if (!handle.equals(targetHandle) && targetHandle instanceof WorkspaceFileTargetHandle) {
					IFile targetFile = ((WorkspaceFileTargetHandle) targetHandle).getTargetFile();
					String location = String.format("file:${project_loc:/%s}/%s", targetFile.getProject().getName(), //$NON-NLS-1$
							targetFile.getProjectRelativePath());
					if (!previousLocations.contains(location)) {
						if (!alreadyContains(location))
							previousLocations.add(location);
					}
				}
			}

		}
		return previousLocations.toArray(new String[previousLocations.size()]);
	}

	private boolean alreadyContains(String location) {
		boolean alreadyContains = false;
		if (targetDefinition.getTargetLocations() != null) {
			ITargetLocation[] containers = targetDefinition.getTargetLocations();
			for (ITargetLocation targetLoc : containers) {
				String uri = null;
				try {
					uri = targetLoc.getLocation(false);
				} catch (CoreException e) {
				}
				if (uri != null) {
					if (uri.equals(location)) {
						alreadyContains = true;
						break;
					}
				}
			}
		}
		return alreadyContains;
	}

	@Override
	public void storeSettings() {
		String newLocation = furiLocation.getText().trim();

		int length = newLocation.length();
		if (length > 0 && newLocation.charAt(length - 1) == File.separatorChar) {
			newLocation = newLocation.substring(0, length - 1);
		}
		String[] items = furiLocation.getItems();
		for (String item : items) {
			if (item.equals(newLocation)) {
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

	@Override
	public ITargetLocation getBundleContainer() {
		return fContainer;
	}

	/**
	 * Validate the input fields before a container is created/edited. The
	 * page's enablement, message and completion should be updated.
	 *
	 * @return whether the finish button should be enabled and container
	 *         creation should continue
	 */
	protected boolean validateInput() {
		if (furiLocation.isDisposed())
			return false;

		// Check if the text field is blank
		if (furiLocation.getText().trim().length() == 0) {
			setMessage(getDefaultMessage());
			return false;
		}

		// Resolve any variables
		URI location;
		try {
			location = RemoteTargetHandle.getEffectiveUri(furiLocation.getText().trim());
		} catch (CoreException e) {
			setMessage(e.getMessage(), IMessageProvider.WARNING);
			return true;
		} catch (URISyntaxException e) {
			setMessage(e.getMessage(), IMessageProvider.ERROR);
			return false;
		}
		try {
			// and be converted to an URL
			URL url = location.toURL();
			if ("file".equalsIgnoreCase(url.getProtocol())) { //$NON-NLS-1$
				File file = new File(location);
				if (!file.isFile()) {
					setMessage(NLS.bind(Messages.EditTargetContainerPage_Not_A_File, file.getAbsolutePath()),
							IMessageProvider.ERROR);
					return true;
				}
			}
		} catch (MalformedURLException | RuntimeException e) {
			setMessage(e.getMessage(), IMessageProvider.ERROR);
			return false;
		}
		setMessage(getDefaultMessage());
		return true;
	}

}
