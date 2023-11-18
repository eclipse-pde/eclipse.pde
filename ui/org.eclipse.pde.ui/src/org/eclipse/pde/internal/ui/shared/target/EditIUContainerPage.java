/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
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
 *     Sonatype, Inc. - ongoing development
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.dialogs.AvailableIUGroup;
import org.eclipse.equinox.internal.p2.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.equinox.internal.p2.ui.dialogs.RepositorySelectionGroup;
import org.eclipse.equinox.internal.p2.ui.model.AvailableIUElement;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.ui.Policy;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.window.SameShellProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.IUBundleContainer;
import org.eclipse.pde.internal.core.target.P2TargetUtils;
import org.eclipse.pde.internal.core.util.VersionUtil;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.SWTFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Version;

/**
 * Wizard page allowing users to select which IUs they would like to download
 *
 * @see EditBundleContainerWizard
 * @see AddBundleContainerWizard
 */
public class EditIUContainerPage extends WizardPage implements IEditBundleContainerPage {

	// Status for any errors on the page
	private static final IStatus BAD_IU_SELECTION = Status.error(Messages.EditIUContainerPage_0);
	private IStatus fSelectedIUStatus = Status.OK_STATUS;

	// Dialog settings
	private static final String SETTINGS_GROUP_BY_CATEGORY = "groupByCategory"; //$NON-NLS-1$
	private static final String SETTINGS_SHOW_OLD_VERSIONS = "showVersions"; //$NON-NLS-1$
	private static final String SETTINGS_SELECTED_REPOSITORY = "selectedRepository"; //$NON-NLS-1$

	// Refresh settings
	private static final int REFRESH_INTERVAL = 4000;
	private static final int REFRESH_TRIES = 10;

	private static final String EMPTY_VERSION = Version.emptyVersion.toString();
	private static final String LATEST_LABEL = Messages.EditIUContainerPage_Latest_Label;

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
	@SuppressWarnings("restriction")
	private org.eclipse.equinox.internal.p2.ui.query.IUViewQueryContext fQueryContext;

	/**
	 * The parent target definition
	 */
	private final ITargetDefinition fTarget;

	private RepositorySelectionGroup fRepoSelector;
	private AvailableIUGroup fAvailableIUGroup;
	private Map<IInstallableUnit, String> versionSpecifications = new HashMap<>();
	private Label fSelectionCount;
	private Button fPropertiesButton;
	private IAction fPropertyAction;
	private Button fShowCategoriesButton;
	private Button fShowOldVersionsButton;
	private Button fIncludeRequiredButton;
	private Button fAllPlatformsButton;
	private Button fIncludeSourceButton;
	private Button fConfigurePhaseButton;
	private Button fFollowRepositoryReferencesButton;
	private Text fDetailsText;
	private final ProvisioningUI profileUI;
	private Thread refreshThread;

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
	 */
	protected EditIUContainerPage(IUBundleContainer container, ITargetDefinition definition) {
		this(definition);
		setTitle(Messages.EditIUContainerPage_7);
		setMessage(Messages.EditIUContainerPage_6);
		fEditContainer = container;
	}

	@Override
	public ITargetLocation getBundleContainer() {
		ITargetPlatformService service = PDECore.getDefault().acquireService(ITargetPlatformService.class);
		if (service == null) {
			PDEPlugin.log(Status.error(Messages.EditIUContainerPage_9));
		}
		int flags = fIncludeRequiredButton.getSelection() ? IUBundleContainer.INCLUDE_REQUIRED : 0;
		flags |= fAllPlatformsButton.getSelection() ? IUBundleContainer.INCLUDE_ALL_ENVIRONMENTS : 0;
		flags |= fIncludeSourceButton.getSelection() ? IUBundleContainer.INCLUDE_SOURCE : 0;
		flags |= fConfigurePhaseButton.getSelection() ? IUBundleContainer.INCLUDE_CONFIGURE_PHASE : 0;
		flags |= fFollowRepositoryReferencesButton.getSelection() ? IUBundleContainer.FOLLOW_REPOSITORY_REFERENCES : 0;

		IInstallableUnit[] selectedIUs = fAvailableIUGroup.getCheckedLeafIUs();
		URI[] repos = fRepoLocation != null ? new URI[] { fRepoLocation } : null;
		versionSpecifications.values().removeIf(String::isBlank);
		if (!versionSpecifications.isEmpty()) {
			List<String> ids = new ArrayList<>(selectedIUs.length);
			List<String> versions = new ArrayList<>(selectedIUs.length);
			for (IInstallableUnit iu : selectedIUs) {
				ids.add(iu.getId());
				String version = versionSpecifications.get(iu);
				if (version == null || version.isBlank()) {
					version = iu.getVersion().toString();
				}
				versions.add(version);
			}
			return service.newIULocation(ids.toArray(String[]::new), versions.toArray(String[]::new), repos, flags);
		}
		return service.newIULocation(selectedIUs, repos, flags);
	}

	@Override
	public void storeSettings() {
		IDialogSettings settings = getDialogSettings();
		if (settings != null) {
			settings.put(SETTINGS_GROUP_BY_CATEGORY, fShowCategoriesButton.getSelection());
			settings.put(SETTINGS_SHOW_OLD_VERSIONS, fShowOldVersionsButton.getSelection());
			settings.put(SETTINGS_SELECTED_REPOSITORY, fRepoLocation != null ? fRepoLocation.toString() : null);
		}
	}

	@Override
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
	private void createRepositoryComboArea(final Composite parent) {
		profileUI.getPolicy().setRepositoryPreferencePageId(null);
		fRepoSelector = new RepositorySelectionGroup(profileUI, getContainer(), parent, fQueryContext);
		fRepoSelector.addRepositorySelectionListener((repoChoice, repoLocation) -> {
			fAvailableIUGroup.setRepositoryFilter(repoChoice, repoLocation);
			fRepoLocation = repoChoice == AvailableIUGroup.AVAILABLE_SPECIFIED ? repoLocation : null;
			if (repoChoice == AvailableIUGroup.AVAILABLE_NONE) {
				setDescription(Messages.EditIUContainerPage_10);
			} else {
				setDescription(Messages.EditIUContainerPage_11);
				refreshAvailableIUArea(parent);
			}
			pageChanged();
		});
	}

	private void refreshAvailableIUArea(final Composite parent) {
		if (fEditContainer == null || fEditContainer.getInstallableUnits().isEmpty()
				|| (refreshThread != null && refreshThread.isAlive())) {
			return;
		}
		refreshThread = new Thread(() -> {
			try {
				final AtomicBoolean loaded = new AtomicBoolean(false);
				int tries = 0;
				while (!loaded.get()) {
					if (REFRESH_TRIES == tries++) {
						throw new InterruptedException("reached maximum number of tries"); //$NON-NLS-1$
					}

					Thread.sleep(REFRESH_INTERVAL);
					// cancel is pressed before next refresh
					if (parent.isDisposed()) {
						break;
					}
					parent.getDisplay().syncExec(() -> {
						final TreeItem[] children = fAvailableIUGroup.getCheckboxTreeViewer().getTree().getItems();
						@SuppressWarnings("restriction")
						final String pendingLabel = org.eclipse.ui.internal.progress.ProgressMessages.PendingUpdateAdapter_PendingLabel;
						if (children.length > 0 && !children[0].getText().equals(pendingLabel)) {
							fAvailableIUGroup.getCheckboxTreeViewer().expandAll();
							setInstallableUnits(fEditContainer);
							fAvailableIUGroup.getCheckboxTreeViewer().collapseAll();
							loaded.set(true);
						}
					});
				}
			} catch (InterruptedException e) {
				PDEPlugin.log(e);
			}
		});
		refreshThread.start();
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
		if (!profileUI.getPolicy().getRepositoriesVisible()) {
			filterConstant = AvailableIUGroup.AVAILABLE_ALL;
		}
		fAvailableIUGroup = new AvailableIUGroup(profileUI, parent, parent.getFont(), fQueryContext, null,
				filterConstant) {
			@Override
			protected StructuredViewer createViewer(Composite parent) {
				ContainerCheckedTreeViewer treeViewer = (ContainerCheckedTreeViewer) super.createViewer(parent);
				TreeViewerColumn column = new TreeViewerColumn(treeViewer, SWT.NONE, getColumnConfig().length);
				column.getColumn().setText(Messages.EditIUContainerPage_VersionSpecification_Label);
				column.getColumn().setWidth(150);
				column.getColumn().setResizable(true);
				CellEditor versionSpecEditor = new TextCellEditor(treeViewer.getTree());
				versionSpecEditor.setValidator(this::validateVersionSpecification);
				column.setEditingSupport(new EditingSupport(treeViewer) {
					@Override
					@SuppressWarnings("restriction")
					protected void setValue(Object element, Object value) {
						if (element instanceof AvailableIUElement iuElement && value instanceof String spec) {
							spec = sanitizeVersionSpecification(spec);
							versionSpecifications.put(iuElement.getIU(), spec);
							treeViewer.update(iuElement, null);
						}
					}

					@Override
					protected Object getValue(Object element) {
						return getVersionSpecification(element);
					}

					@Override
					protected boolean canEdit(Object element) {
						return element instanceof @SuppressWarnings("restriction") AvailableIUElement iuElement
								&& treeViewer.getChecked(iuElement);
					}

					@Override
					protected CellEditor getCellEditor(Object element) {
						return versionSpecEditor;
					}
				});
				column.setLabelProvider(ColumnLabelProvider.createTextProvider(this::getVersionSpecification));
				return treeViewer;
			}

			private static String sanitizeVersionSpecification(String spec) {
				spec = spec.strip();
				return LATEST_LABEL.equals(spec) ? EMPTY_VERSION : spec;
			}

			@SuppressWarnings("restriction")
			private String getVersionSpecification(Object e) {
				String spec = e instanceof AvailableIUElement iu //
						? versionSpecifications.getOrDefault(iu.getIU(), "") //$NON-NLS-1$
						: ""; //$NON-NLS-1$
				return EMPTY_VERSION.equals(spec) ? LATEST_LABEL : spec;
			}

			private String validateVersionSpecification(Object value) {
				if (LATEST_LABEL.equals(value)) {
					return null;
				}
				IStatus result = VersionUtil.validateVersionRange((String) value);
				return result.isOK() ? null : result.getMessage();
			}
		};
		fAvailableIUGroup.getCheckboxTreeViewer().addCheckStateListener(event -> {
			IInstallableUnit[] units = fAvailableIUGroup.getCheckedLeafIUs();
			if (units.length > 0) {
				if (units.length == 1) {
					fSelectionCount.setText(NLS.bind(Messages.EditIUContainerPage_itemSelected, Integer.toString(units.length)));
				} else {
					fSelectionCount.setText(NLS.bind(Messages.EditIUContainerPage_itemsSelected, Integer.toString(units.length)));
				}
				fSelectedIUStatus = Status.OK_STATUS;
			} else {

				final TreeItem[] children = fAvailableIUGroup.getCheckboxTreeViewer().getTree().getItems();
				@SuppressWarnings("restriction")
				final String pendingLabel = org.eclipse.ui.internal.progress.ProgressMessages.PendingUpdateAdapter_PendingLabel;
				if (children.length > 0 && !children[0].getText().equals(pendingLabel)) {
					fSelectionCount.setText(NLS.bind(Messages.EditIUContainerPage_itemsSelected, Integer.toString(0)));
					fSelectedIUStatus = BAD_IU_SELECTION;
				}
			}
			pageChanged();
		});
		fAvailableIUGroup.getCheckboxTreeViewer().addSelectionChangedListener(event -> {
			updateDetails();
			fPropertiesButton.setEnabled(fAvailableIUGroup.getSelectedIUElements().length == 1);
		});

		fAvailableIUGroup.setUseBoldFontForFilteredItems(true);
		GridData data = (GridData) fAvailableIUGroup.getStructuredViewer().getControl().getLayoutData();
		data.heightHint = 200;

		Composite buttonParent = new Composite(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;
		gridLayout.marginWidth = 0;
		gridLayout.horizontalSpacing = 10;
		buttonParent.setLayout(gridLayout);

		GridData gridData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
		buttonParent.setLayoutData(gridData);

		Button selectAll = new Button(buttonParent, SWT.PUSH);
		selectAll.setText(ProvUIMessages.SelectableIUsPage_Select_All);

		selectAll.setLayoutData(new GridData());
		SWTFactory.setButtonDimensionHint(selectAll);
		selectAll.addListener(SWT.Selection, event -> setAllChecked(true));

		Button deselectAll = new Button(buttonParent, SWT.PUSH);
		deselectAll.setText(ProvUIMessages.SelectableIUsPage_Deselect_All);
		deselectAll.setLayoutData(new GridData());
		SWTFactory.setButtonDimensionHint(deselectAll);
		deselectAll.addListener(SWT.Selection, event -> setAllChecked(false));
		fSelectionCount = SWTFactory.createLabel(buttonParent, NLS.bind(Messages.EditIUContainerPage_itemsSelected, Integer.toString(0)), 1);
		GridData labelData = new GridData();
		labelData.widthHint = 200;
		fSelectionCount.setLayoutData(labelData);
	}

	private void setAllChecked(boolean checked) {
		if (checked) {
			TreeItem[] items = fAvailableIUGroup.getCheckboxTreeViewer().getTree().getItems();
			checkAll(checked, items);
			fAvailableIUGroup.setChecked(fAvailableIUGroup.getCheckboxTreeViewer().getCheckedElements());
		} else {
			fAvailableIUGroup.setChecked(new Object[0]);
		}
		updateSelection();
	}

	private void checkAll(boolean checked, TreeItem[] items) {
		for (TreeItem item : items) {
			item.setChecked(checked);
			TreeItem[] children = item.getItems();
			checkAll(checked, children);
		}
	}

	private void updateSelection() {
		int count = fAvailableIUGroup.getCheckedLeafIUs().length;
		setPageComplete(count > 0);
		String message;
		if (count == 0) {
			message = ProvUIMessages.AvailableIUsPage_MultipleSelectionCount;
			fSelectionCount.setText(NLS.bind(message, Integer.toString(count)));
		} else {
			message = count == 1 ? ProvUIMessages.AvailableIUsPage_SingleSelectionCount : ProvUIMessages.AvailableIUsPage_MultipleSelectionCount;
			fSelectionCount.setText(NLS.bind(message, Integer.toString(count)));
		}
	}

	/**
	 * Details area underneath the group that displays more info on the selected IU
	 * @param parent parent composite
	 */
	@SuppressWarnings("restriction")
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
		fPropertiesButton.addSelectionListener(widgetSelectedAdapter(event -> fPropertyAction.run()));
		fPropertyAction = new org.eclipse.equinox.internal.p2.ui.actions.PropertyDialogAction(
				new SameShellProvider(getShell()), fAvailableIUGroup.getStructuredViewer());
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
		fShowCategoriesButton.addSelectionListener(widgetSelectedAdapter(e -> updateViewContext()));
		fShowOldVersionsButton = SWTFactory.createCheckButton(checkComp, Messages.EditIUContainerPage_15, null, true, 1);
		fShowOldVersionsButton.addSelectionListener(widgetSelectedAdapter(e -> updateViewContext()));

		Group slicerGroup = SWTFactory.createGroup(parent, Messages.EditIUContainerPage_1, 1, 1, GridData.FILL_HORIZONTAL);
		SWTFactory.createWrapLabel(slicerGroup, Messages.EditIUContainerPage_2, 1, 400);
		fIncludeRequiredButton = SWTFactory.createCheckButton(slicerGroup, Messages.EditIUContainerPage_3, null, true, 1);
		fIncludeRequiredButton.addSelectionListener(widgetSelectedAdapter(e -> {
			fAllPlatformsButton.setEnabled(!fIncludeRequiredButton.getSelection());
			warnIfGlobalSettingChanged();
		}));
		fAllPlatformsButton = SWTFactory.createCheckButton(slicerGroup, Messages.EditIUContainerPage_8, null, false, 1);
		fAllPlatformsButton.addSelectionListener(widgetSelectedAdapter(e -> warnIfGlobalSettingChanged()));
		((GridData) fAllPlatformsButton.getLayoutData()).horizontalIndent = 10;
		fIncludeSourceButton = SWTFactory.createCheckButton(slicerGroup, Messages.EditIUContainerPage_16, null, true, 1);
		fIncludeSourceButton.addSelectionListener(widgetSelectedAdapter(e -> warnIfGlobalSettingChanged()));
		fConfigurePhaseButton = SWTFactory.createCheckButton(slicerGroup, Messages.EditIUContainerPage_IncludeConfigurePhase, null, true, 1);
		fConfigurePhaseButton.addSelectionListener(widgetSelectedAdapter(e -> warnIfGlobalSettingChanged()));
		fFollowRepositoryReferencesButton = SWTFactory.createCheckButton(slicerGroup, Messages.EditIUContainerPage_17, null, true, 1);
		fFollowRepositoryReferencesButton.addSelectionListener(widgetSelectedAdapter(e -> warnIfGlobalSettingChanged()));

	}

	private void warnIfGlobalSettingChanged() {
		boolean noChange = true;
		ITargetLocation[] containers = fTarget.getTargetLocations();
		if (containers != null) {
			// Look for a IUBundleContainer to compare against.
			IUBundleContainer iuContainer = Arrays.stream(containers).filter(container -> container != fEditContainer)
					.filter(IUBundleContainer.class::isInstance).map(IUBundleContainer.class::cast) //
					.findFirst().orElse(null);
			// If there is another IU container then compare against it.  No need to check them all
			// as they will all be set the same within one target.
			if (iuContainer != null) {
				noChange &= fIncludeRequiredButton.getSelection() == iuContainer.getIncludeAllRequired();
				noChange &= fAllPlatformsButton.getSelection() == iuContainer.getIncludeAllEnvironments();
				noChange &= fIncludeSourceButton.getSelection() == iuContainer.getIncludeSource();
				noChange &= fConfigurePhaseButton.getSelection() == iuContainer.getIncludeConfigurePhase();
				noChange &= fFollowRepositoryReferencesButton.getSelection() == iuContainer.isFollowRepositoryReferences();
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
	@SuppressWarnings("restriction")
	private void createQueryContext() {
		fQueryContext = ProvUI.getQueryContext(profileUI.getPolicy());
		fQueryContext.setInstalledProfileId(P2TargetUtils.getProfileId(fTarget));
		fQueryContext.setHideAlreadyInstalled(false);
	}

	/**
	 * Update the available group and context using the current checkbox state
	 */
	@SuppressWarnings("restriction")
	private void updateViewContext() {
		if (fShowCategoriesButton.getSelection()) {
			fQueryContext.setViewType(org.eclipse.equinox.internal.p2.ui.query.IUViewQueryContext.AVAILABLE_VIEW_BY_CATEGORY);
		} else {
			fQueryContext.setViewType(org.eclipse.equinox.internal.p2.ui.query.IUViewQueryContext.AVAILABLE_VIEW_FLAT);
		}
		fQueryContext.setShowLatestVersionsOnly(fShowOldVersionsButton.getSelection());
		fAvailableIUGroup.updateAvailableViewState();
		fAvailableIUGroup.getStructuredViewer().refresh();
	}

	/**
	 * Update the details section of the page using the currently selected IU
	 */
	private void updateDetails() {
		IInstallableUnit[] selected = fAvailableIUGroup.getSelectedIUs().toArray(new IInstallableUnit[0]);
		if (selected.length == 1) {
			StringBuilder result = new StringBuilder();
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
	@SuppressWarnings("restriction")
	private void restoreWidgetState() {
		IDialogSettings settings = getDialogSettings();
		URI uri = null;
		boolean showCategories = fQueryContext.shouldGroupByCategories();
		boolean showOldVersions = fQueryContext.getShowLatestVersionsOnly();

		// Init the checkboxes and repo selector combo
		if (fEditContainer != null) {
			List<URI> repositories = fEditContainer.getRepositories();
			if (!repositories.isEmpty()) {
				uri = repositories.get(0);
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
			fFollowRepositoryReferencesButton.setSelection(fEditContainer.isFollowRepositoryReferences());
		} else {
			// If we are creating a new container, but there is an existing iu container we should use it's settings (otherwise we overwrite them)
			ITargetLocation[] knownContainers = fTarget.getTargetLocations();
			if (knownContainers != null) {
				for (ITargetLocation knownContainer : knownContainers) {
					if (knownContainer instanceof IUBundleContainer iuContainer) {
						fIncludeRequiredButton.setSelection(iuContainer.getIncludeAllRequired());
						fAllPlatformsButton.setSelection(iuContainer.getIncludeAllEnvironments());
						fIncludeSourceButton.setSelection(iuContainer.getIncludeSource());
						fConfigurePhaseButton.setSelection(iuContainer.getIncludeConfigurePhase());
						fFollowRepositoryReferencesButton.setSelection(iuContainer.isFollowRepositoryReferences());
					}
				}
			}
		}

		// If the user can create two containers with different settings for include required we won't resolve correctly
		// If the user has an existing container, don't let them edit the options, bug 275013
		if (fTarget != null) {
			ITargetLocation[] containers = fTarget.getTargetLocations();
			if (containers != null) {
				for (ITargetLocation container : containers) {
					if (container instanceof IUBundleContainer iuContainer && iuContainer != fEditContainer) {
						fIncludeRequiredButton.setSelection(iuContainer.getIncludeAllRequired());
						fAllPlatformsButton.setSelection(iuContainer.getIncludeAllEnvironments());
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
			// TODO This code does not do a good job, selecting, revealing, and collapsing all
			// Only able to check items if we don't have categories
			fQueryContext.setViewType(org.eclipse.equinox.internal.p2.ui.query.IUViewQueryContext.AVAILABLE_VIEW_FLAT);
			fAvailableIUGroup.updateAvailableViewState();
			setInstallableUnits(fEditContainer);
			// Make sure view is back in proper state
			updateViewContext();
			IInstallableUnit[] units = fAvailableIUGroup.getCheckedLeafIUs();
			if (units.length > 0) {
				fAvailableIUGroup.getCheckboxTreeViewer().setSelection(new StructuredSelection(units[0]), true);
			}
			String msg = units.length == 1 ? Messages.EditIUContainerPage_itemSelected
					: Messages.EditIUContainerPage_itemsSelected;
			fSelectionCount.setText(NLS.bind(msg, units.length));
			fAvailableIUGroup.getCheckboxTreeViewer().collapseAll();
		}
	}

	private void setInstallableUnits(IUBundleContainer iuContainer) {
		versionSpecifications = new HashMap<>(iuContainer.getInstallableUnitSpecifications());
		fAvailableIUGroup.setChecked(versionSpecifications.keySet().toArray());
	}
}