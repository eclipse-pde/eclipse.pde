/*******************************************************************************
 * Copyright (c) 2009, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sonatype, Inc. - ongoing development
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.actions.PropertyDialogAction;
import org.eclipse.equinox.internal.p2.ui.dialogs.*;
import org.eclipse.equinox.internal.p2.ui.query.IUViewQueryContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.ui.Policy;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.SameShellProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.target.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.IUBundleContainer;
import org.eclipse.pde.internal.core.target.P2TargetUtils;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

/**
 * Wizard page allowing users to select which IUs they would like to download
 * 
 * @see EditBundleContainerWizard
 * @see AddBundleContainerWizard
 */
public class EditIUContainerPage extends WizardPage implements IEditBundleContainerPage {

	// Status for any errors on the page
	private static final IStatus BAD_IU_SELECTION = new Status(IStatus.ERROR, PDEPlugin.getPluginId(), Messages.EditIUContainerPage_0);
	private IStatus fSelectedIUStatus = Status.OK_STATUS;

	// Dialog settings
	private static final String SETTINGS_GROUP_BY_CATEGORY = "groupByCategory"; //$NON-NLS-1$
	private static final String SETTINGS_SHOW_OLD_VERSIONS = "showVersions"; //$NON-NLS-1$
	private static final String SETTINGS_SELECTED_REPOSITORY = "selectedRepository"; //$NON-NLS-1$

	/**
	 * If the user is only downloading from a specific repository location, we store it here so it can be persisted in the target
	 */
	private URI fRepoLocation;

	/**
	 * If this wizard is editing an existing bundle container instead of creating a new one from scratch it will be stored here
	 */
	private IUBundleContainer fEditContainer;

	/**
	 * Used to provide special attributes/filtering to the available iu group 
	 */
	private IUViewQueryContext fQueryContext;

	/**
	 * The parent target definition
	 */
	private ITargetDefinition fTarget;

	private RepositorySelectionGroup fRepoSelector;
	private AvailableIUGroup fAvailableIUGroup;
	private Label fSelectionCount;
	private Button fPropertiesButton;
	private IAction fPropertyAction;
	private Button fShowCategoriesButton;
	private Button fShowOldVersionsButton;
	private Button fIncludeRequiredButton;
	private Button fAllPlatformsButton;
	private Button fIncludeSourceButton;
	private Button fConfigurePhaseButton;
	private Text fDetailsText;
	private ProvisioningUI profileUI;

	/**
	 * Constructor for creating a new container
	 * @param definition the target definition we are editing
	 */
	protected EditIUContainerPage(ITargetDefinition definition) {
		super("AddP2Container"); //$NON-NLS-1$
		setTitle(Messages.EditIUContainerPage_5);
		setMessage(Messages.EditIUContainerPage_6);
		fTarget = definition;
		ProvisioningSession session;
		try {
			session = new ProvisioningSession(P2TargetUtils.getAgent());
		} catch (CoreException e) {
			PDEPlugin.log(e);
			session = ProvisioningUI.getDefaultUI().getSession();
		}
		profileUI = new ProvisioningUI(session, P2TargetUtils.getProfileId(definition), new Policy());
	}

	/**
	 * Constructor for editing an existing container
	 * @param container the container to edit
	 * @param profile profile from the parent target, used to setup the p2 UI
	 */
	protected EditIUContainerPage(IUBundleContainer container, ITargetDefinition definition) {
		this(definition);
		setTitle(Messages.EditIUContainerPage_7);
		setMessage(Messages.EditIUContainerPage_6);
		fEditContainer = container;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.shared.target.IEditTargetLocationPage#getBundleContainer()
	 */
	public ITargetLocation getBundleContainer() {
		ITargetPlatformService service = (ITargetPlatformService) PDECore.getDefault().acquireService(ITargetPlatformService.class.getName());
		if (service == null) {
			PDEPlugin.log(new Status(IStatus.ERROR, PDEPlugin.getPluginId(), Messages.EditIUContainerPage_9));
		}
		int flags = fIncludeRequiredButton.getSelection() ? IUBundleContainer.INCLUDE_REQUIRED : 0;
		flags |= fAllPlatformsButton.getSelection() ? IUBundleContainer.INCLUDE_ALL_ENVIRONMENTS : 0;
		flags |= fIncludeSourceButton.getSelection() ? IUBundleContainer.INCLUDE_SOURCE : 0;
		flags |= fConfigurePhaseButton.getSelection() ? IUBundleContainer.INCLUDE_CONFIGURE_PHASE : 0;
		IUBundleContainer container = (IUBundleContainer) service.newIULocation(fAvailableIUGroup.getCheckedLeafIUs(), fRepoLocation != null ? new URI[] {fRepoLocation} : null, flags);
		return container;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.shared.target.IEditTargetLocationPage#storeSettings()
	 */
	public void storeSettings() {
		IDialogSettings settings = getDialogSettings();
		if (settings != null) {
			settings.put(SETTINGS_GROUP_BY_CATEGORY, fShowCategoriesButton.getSelection());
			settings.put(SETTINGS_SHOW_OLD_VERSIONS, fShowOldVersionsButton.getSelection());
			settings.put(SETTINGS_SELECTED_REPOSITORY, fRepoLocation != null ? fRepoLocation.toString() : null);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite composite = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH, 0, 0);

		createQueryContext();
		createRepositoryComboArea(composite);
		createAvailableIUArea(composite);
		createDetailsArea(composite);
		createCheckboxArea(composite);

		restoreWidgetState();
		setControl(composite);
		setPageComplete(false);
		if (fEditContainer == null) {
			PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.LOCATION_ADD_SITE_WIZARD);
		} else {
			PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.LOCATION_EDIT_SITE_WIZARD);
		}
	}

	/**
	 * Combo at the top of the page allowing the user to select a specific repo or group of repositories
	 * @param parent parent composite
	 */
	private void createRepositoryComboArea(Composite parent) {
		profileUI.getPolicy().setRepositoryPreferencePageId(null);
		fRepoSelector = new RepositorySelectionGroup(profileUI, getContainer(), parent, fQueryContext);
		fRepoSelector.addRepositorySelectionListener(new IRepositorySelectionListener() {
			public void repositorySelectionChanged(int repoChoice, URI repoLocation) {
				fAvailableIUGroup.setRepositoryFilter(repoChoice, repoLocation);
				fRepoLocation = repoChoice == AvailableIUGroup.AVAILABLE_SPECIFIED ? repoLocation : null;
				if (repoChoice == AvailableIUGroup.AVAILABLE_NONE) {
					setDescription(Messages.EditIUContainerPage_10);
				} else {
					setDescription(Messages.EditIUContainerPage_11);
				}
				pageChanged();
			}
		});
	}

	/**
	 * Create the UI area where the user will be able to select which IUs they
	 * would like to download.  There will also be buttons to see properties for
	 * the selection and open the manage sites dialog.
	 * 
	 * @param parent parent composite
	 */
	private void createAvailableIUArea(Composite parent) {
		int filterConstant = AvailableIUGroup.AVAILABLE_NONE;
		if (!profileUI.getPolicy().getRepositoriesVisible())
			filterConstant = AvailableIUGroup.AVAILABLE_ALL;
		fAvailableIUGroup = new AvailableIUGroup(profileUI, parent, parent.getFont(), fQueryContext, null, filterConstant);
		fAvailableIUGroup.getCheckboxTreeViewer().addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				IInstallableUnit[] units = fAvailableIUGroup.getCheckedLeafIUs();
				if (units.length > 0) {
					if (units.length == 1) {
						fSelectionCount.setText(NLS.bind(Messages.EditIUContainerPage_itemSelected, Integer.toString(units.length)));
					} else {
						fSelectionCount.setText(NLS.bind(Messages.EditIUContainerPage_itemsSelected, Integer.toString(units.length)));
					}
					fSelectedIUStatus = Status.OK_STATUS;
				} else {
					fSelectionCount.setText(NLS.bind(Messages.EditIUContainerPage_itemsSelected, Integer.toString(0)));
					fSelectedIUStatus = BAD_IU_SELECTION;
				}
				pageChanged();
			}
		});
		fAvailableIUGroup.getCheckboxTreeViewer().addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateDetails();
				fPropertiesButton.setEnabled(fAvailableIUGroup.getSelectedIUElements().length == 1);
			}
		});

		fAvailableIUGroup.setUseBoldFontForFilteredItems(true);
		GridData data = (GridData) fAvailableIUGroup.getStructuredViewer().getControl().getLayoutData();
		data.heightHint = 200;

		fSelectionCount = SWTFactory.createLabel(parent, NLS.bind(Messages.EditIUContainerPage_itemsSelected, Integer.toString(0)), 1);
	}

	/**
	 * Details area underneath the group that displays more info on the selected IU
	 * @param parent parent composite
	 */
	private void createDetailsArea(Composite parent) {
		Group detailsGroup = SWTFactory.createGroup(parent, Messages.EditIUContainerPage_12, 1, 1, GridData.FILL_HORIZONTAL);

		fDetailsText = SWTFactory.createText(detailsGroup, SWT.WRAP | SWT.READ_ONLY, 1, GridData.FILL_HORIZONTAL);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = 50;
		gd.widthHint = 400;
		fDetailsText.setLayoutData(gd);

		// TODO Use a link instead of a button? To be consistent with the install wizard
		fPropertiesButton = SWTFactory.createPushButton(detailsGroup, Messages.EditIUContainerPage_13, null);
		((GridData) fPropertiesButton.getLayoutData()).horizontalAlignment = SWT.RIGHT;
		fPropertiesButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				fPropertyAction.run();
			}
		});
		fPropertyAction = new PropertyDialogAction(new SameShellProvider(getShell()), fAvailableIUGroup.getStructuredViewer());
		fPropertiesButton.setEnabled(false);
	}

	/**
	 * Checkboxes at the bottom of the page to control the available IU tree viewer
	 * @param parent parent composite
	 */
	private void createCheckboxArea(Composite parent) {
		Composite checkComp = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_HORIZONTAL, 0, 0);
		checkComp.setLayout(new GridLayout(2, true));
		fShowCategoriesButton = SWTFactory.createCheckButton(checkComp, Messages.EditIUContainerPage_14, null, true, 1);
		fShowCategoriesButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateViewContext();
			}
		});
		fShowOldVersionsButton = SWTFactory.createCheckButton(checkComp, Messages.EditIUContainerPage_15, null, true, 1);
		fShowOldVersionsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateViewContext();
			}
		});

		Group slicerGroup = SWTFactory.createGroup(parent, Messages.EditIUContainerPage_1, 1, 1, GridData.FILL_HORIZONTAL);
		SWTFactory.createWrapLabel(slicerGroup, Messages.EditIUContainerPage_2, 1, 400);
		fIncludeRequiredButton = SWTFactory.createCheckButton(slicerGroup, Messages.EditIUContainerPage_3, null, true, 1);
		fIncludeRequiredButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fAllPlatformsButton.setEnabled(!fIncludeRequiredButton.getSelection());
				warnIfGlobalSettingChanged();
			}
		});
		fAllPlatformsButton = SWTFactory.createCheckButton(slicerGroup, Messages.EditIUContainerPage_8, null, false, 1);
		fAllPlatformsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				warnIfGlobalSettingChanged();
			}
		});
		((GridData) fAllPlatformsButton.getLayoutData()).horizontalIndent = 10;
		fIncludeSourceButton = SWTFactory.createCheckButton(slicerGroup, Messages.EditIUContainerPage_16, null, true, 1);
		fIncludeSourceButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				warnIfGlobalSettingChanged();
			}
		});
		fConfigurePhaseButton = SWTFactory.createCheckButton(slicerGroup, Messages.EditIUContainerPage_IncludeConfigurePhase, null, true, 1);
		fConfigurePhaseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				warnIfGlobalSettingChanged();
			}
		});

	}

	private void warnIfGlobalSettingChanged() {
		boolean noChange = true;
		IUBundleContainer iuContainer = null;
		ITargetLocation[] containers = fTarget.getTargetLocations();
		if (containers != null) {
			// Look for a IUBundleContainer to compare against.
			for (int i = 0; i < containers.length; i++) {
				if (containers[i] instanceof IUBundleContainer && containers[i] != fEditContainer) {
					iuContainer = (IUBundleContainer) containers[i];
					break;
				}
			}
			// If there is another IU container then compare against it.  No need to check them all
			// as they will all be set the same within one target.
			if (iuContainer != null) {
				noChange &= fIncludeRequiredButton.getSelection() == iuContainer.getIncludeAllRequired();
				noChange &= fAllPlatformsButton.getSelection() == iuContainer.getIncludeAllEnvironments();
				noChange &= fIncludeSourceButton.getSelection() == iuContainer.getIncludeSource();
				noChange &= fConfigurePhaseButton.getSelection() == iuContainer.getIncludeConfigurePhase();
			}
		}
		if (noChange) {
			setMessage(Messages.EditIUContainerPage_6);
		} else {
			setMessage(Messages.EditIUContainerPage_4, IStatus.WARNING);
		}
	}

	/**
	 * Creates a default query context to setup the available IU Group
	 */
	private void createQueryContext() {
		fQueryContext = ProvUI.getQueryContext(profileUI.getPolicy());
		fQueryContext.setInstalledProfileId(P2TargetUtils.getProfileId(fTarget));
		fQueryContext.showAlreadyInstalled();
	}

	/**
	 * Update the available group and context using the current checkbox state
	 */
	private void updateViewContext() {
		if (fShowCategoriesButton.getSelection()) {
			fQueryContext.setViewType(IUViewQueryContext.AVAILABLE_VIEW_BY_CATEGORY);
		} else {
			fQueryContext.setViewType(IUViewQueryContext.AVAILABLE_VIEW_FLAT);
		}
		fQueryContext.setShowLatestVersionsOnly(fShowOldVersionsButton.getSelection());
		fAvailableIUGroup.updateAvailableViewState();
	}

	/**
	 * Update the details section of the page using the currently selected IU
	 */
	private void updateDetails() {
		IInstallableUnit[] selected = (IInstallableUnit[]) fAvailableIUGroup.getSelectedIUs().toArray(new IInstallableUnit[0]);
		if (selected.length == 1) {
			StringBuffer result = new StringBuffer();
			String description = selected[0].getProperty(IInstallableUnit.PROP_DESCRIPTION, null);
			if (description != null) {
				result.append(description);
			} else {
				String name = selected[0].getProperty(IInstallableUnit.PROP_NAME, null);
				if (name != null)
					result.append(name);
				else
					result.append(selected[0].getId());
				result.append(" "); //$NON-NLS-1$
				result.append(selected[0].getVersion().toString());
			}

			fDetailsText.setText(result.toString());
			return;
		}
		fDetailsText.setText(""); //$NON-NLS-1$
	}

//	private Link createLink(Composite parent, IAction action, String text) {
//		Link link = new Link(parent, SWT.PUSH);
//		link.setText(text);
//
//		link.addListener(SWT.Selection, new Listener() {
//			public void handleEvent(Event event) {
//				IAction linkAction = getLinkAction(event.widget);
//				if (linkAction != null) {
//					linkAction.runWithEvent(event);
//				}
//			}
//		});
//		link.setToolTipText(action.getToolTipText());
//		link.setData(LINKACTION, action);
//		return link;
//	}

	/**
	 * Checks if the page is complete, updating messages and finish button.
	 */
	void pageChanged() {
		if (fSelectedIUStatus.getSeverity() == IStatus.ERROR) {
			setErrorMessage(fSelectedIUStatus.getMessage());
			setPageComplete(false);
		} else if (fAvailableIUGroup != null && fAvailableIUGroup.getCheckedLeafIUs().length == 0) {
			// On page load and when sites are selected, we might not have an error status, but we want finish to remain disabled
			setPageComplete(false);
		} else {
			setErrorMessage(null);
			setPageComplete(true);
		}
	}

	/**
	 * Restores the state of the wizard from previous invocations
	 */
	private void restoreWidgetState() {
		IDialogSettings settings = getDialogSettings();
		URI uri = null;
		boolean showCategories = fQueryContext.shouldGroupByCategories();
		boolean showOldVersions = fQueryContext.getShowLatestVersionsOnly();

		// Init the checkboxes and repo selector combo
		if (fEditContainer != null) {
			if (fEditContainer.getRepositories() != null) {
				uri = fEditContainer.getRepositories()[0];
			}
		} else if (settings != null) {
			String stringURI = settings.get(SETTINGS_SELECTED_REPOSITORY);
			if (stringURI != null && stringURI.trim().length() > 0) {
				try {
					uri = new URI(stringURI);
				} catch (URISyntaxException e) {
					PDEPlugin.log(e);
				}
			}
		}

		if (settings != null) {
			if (settings.get(SETTINGS_GROUP_BY_CATEGORY) != null) {
				showCategories = settings.getBoolean(SETTINGS_GROUP_BY_CATEGORY);
			}
			if (settings.get(SETTINGS_SHOW_OLD_VERSIONS) != null) {
				showOldVersions = settings.getBoolean(SETTINGS_SHOW_OLD_VERSIONS);
			}
		}

		if (uri != null) {
			fRepoSelector.setRepositorySelection(AvailableIUGroup.AVAILABLE_SPECIFIED, uri);
		} else if (fEditContainer != null) {
			fRepoSelector.setRepositorySelection(AvailableIUGroup.AVAILABLE_ALL, null);
		} else {
			fRepoSelector.setRepositorySelection(AvailableIUGroup.AVAILABLE_NONE, null);
		}

		fShowCategoriesButton.setSelection(showCategories);
		fShowOldVersionsButton.setSelection(showOldVersions);

		if (fEditContainer != null) {
			fIncludeRequiredButton.setSelection(fEditContainer.getIncludeAllRequired());
			fAllPlatformsButton.setSelection(fEditContainer.getIncludeAllEnvironments());
			fIncludeSourceButton.setSelection(fEditContainer.getIncludeSource());
			fConfigurePhaseButton.setSelection(fEditContainer.getIncludeConfigurePhase());
		} else {
			// If we are creating a new container, but there is an existing iu container we should use it's settings (otherwise we overwrite them)
			ITargetLocation[] knownContainers = fTarget.getTargetLocations();
			if (knownContainers != null) {
				for (int i = 0; i < knownContainers.length; i++) {
					if (knownContainers[i] instanceof IUBundleContainer) {
						fIncludeRequiredButton.setSelection(((IUBundleContainer) knownContainers[i]).getIncludeAllRequired());
						fAllPlatformsButton.setSelection(((IUBundleContainer) knownContainers[i]).getIncludeAllEnvironments());
						fIncludeSourceButton.setSelection(((IUBundleContainer) knownContainers[i]).getIncludeSource());
						fConfigurePhaseButton.setSelection(((IUBundleContainer) knownContainers[i]).getIncludeConfigurePhase());
					}
				}
			}
		}

		// If the user can create two containers with different settings for include required we won't resolve correctly
		// If the user has an existing container, don't let them edit the options, bug 275013
		if (fTarget != null) {
			ITargetLocation[] containers = fTarget.getTargetLocations();
			if (containers != null) {
				for (int i = 0; i < containers.length; i++) {
					if (containers[i] instanceof IUBundleContainer && containers[i] != fEditContainer) {
						fIncludeRequiredButton.setSelection(((IUBundleContainer) containers[i]).getIncludeAllRequired());
						fAllPlatformsButton.setSelection(((IUBundleContainer) containers[i]).getIncludeAllEnvironments());
						break;
					}
				}
			}
		}

		fAllPlatformsButton.setEnabled(!fIncludeRequiredButton.getSelection());

		updateViewContext();
		fRepoSelector.getDefaultFocusControl().setFocus();
		updateDetails();

		// If we are editing a bundle check any installable units
		if (fEditContainer != null) {
			try {
				// TODO This code does not do a good job, selecting, revealing, and collapsing all
				// Only able to check items if we don't have categories
				fQueryContext.setViewType(IUViewQueryContext.AVAILABLE_VIEW_FLAT);
				fAvailableIUGroup.updateAvailableViewState();
				fAvailableIUGroup.setChecked(fEditContainer.getInstallableUnits());
				// Make sure view is back in proper state
				updateViewContext();
				IInstallableUnit[] units = fAvailableIUGroup.getCheckedLeafIUs();
				if (units.length > 0) {
					fAvailableIUGroup.getCheckboxTreeViewer().setSelection(new StructuredSelection(units[0]), true);
					if (units.length == 1) {
						fSelectionCount.setText(NLS.bind(Messages.EditIUContainerPage_itemSelected, Integer.toString(units.length)));
					} else {
						fSelectionCount.setText(NLS.bind(Messages.EditIUContainerPage_itemsSelected, Integer.toString(units.length)));
					}
				} else {
					fSelectionCount.setText(NLS.bind(Messages.EditIUContainerPage_itemsSelected, Integer.toString(0)));
				}
				fAvailableIUGroup.getCheckboxTreeViewer().collapseAll();

			} catch (CoreException e) {
				PDEPlugin.log(e);
			}
		}
	}
}
