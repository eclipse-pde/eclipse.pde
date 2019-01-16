/*******************************************************************************
 * Copyright (c) 2003, 2017 IBM Corporation and others.
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
 *     Johannes Ahlers <Johannes.Ahlers@gmx.de> - bug 477677
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.imports;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.*;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.core.target.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.project.BundleProjectService;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.preferences.TargetPlatformPreferenceNode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.ScmUrlImportDescription;
import org.eclipse.team.core.Team;
import org.eclipse.team.core.importing.provisional.IBundleImporter;
import org.eclipse.team.ui.IScmUrlImportWizardPage;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Version;

/**
 * The first page of the import plug-ins wizard
 */
@SuppressWarnings("restriction")
public class PluginImportWizardFirstPage extends WizardPage {

	private static final String SETTINGS_IMPORTTYPE = "importType"; //$NON-NLS-1$
	private static final String SETTINGS_FROM = "importFrom"; //$NON-NLS-1$
	private static final String SETTINGS_DROPLOCATION = "droplocation"; //$NON-NLS-1$
	private static final String SETTINGS_SCAN_ALL = "scanAll"; //$NON-NLS-1$
	private static final int FROM_ACTIVE_PLATFORM = 1;
	private static final int FROM_TARGET_DEFINITION = 2;
	private static final int FROM_DIRECTORY = 3;

	private Button importActiveTargetButton;
	private Button browseButton;
	private Button importDirectoryButton;
	private Button importTargetDefinitionButton;
	private Combo targetDefinitionCombo;
	private List<ITargetDefinition> targetDefinitions;
	private Combo importDirectory;
	private Link openTargetPrefsLink;

	private Button importButton;
	private Button scanButton;

	private Button binaryButton;
	private Button binaryWithLinksButton;
	private Button sourceButton;
	private Button repositoryButton;

	public static String TARGET_PLATFORM = "targetPlatform"; //$NON-NLS-1$
	private IPluginModelBase[] models = new IPluginModelBase[0];
	/**
	 * When importing from a directory or target platform, use alternate source locations.
	 */
	private SourceLocationManager alternateSource;
	private PDEState state;
	private boolean canceled = false;

	/**
	 * Models that can be imported from a repository
	 */
	protected Set<Object> repositoryModels = new HashSet<>();

	/**
	 * Maps bundle importers to import instructions
	 */
	private Map<IBundleImporter, ScmUrlImportDescription[]> importerToInstructions = new HashMap<>();

	/**
	 * Map of bundle importer extension id to associated wizard page
	 */
	private Map<String, IScmUrlImportWizardPage> importIdToWizardPage = new HashMap<>();

	/**
	 * Array of next wizard pages (in order)
	 */
	private List<IScmUrlImportWizardPage> nextPages = new ArrayList<>();

	public PluginImportWizardFirstPage(String name) {
		super(name);
		setTitle(PDEUIMessages.ImportWizard_FirstPage_title);
		setMessage(PDEUIMessages.ImportWizard_FirstPage_desc);
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH);
		GridLayout layout = (GridLayout) container.getLayout();
		layout.verticalSpacing = 15;
		container.setLayout(layout);

		createImportFromGroup(container);
		createImportChoicesGroup(container);
		createImportOptionsGroup(container);

		Dialog.applyDialogFont(container);
		initialize();
		setControl(container);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.PLUGIN_IMPORT_FIRST_PAGE);
	}

	/**
	 * Create the import choices group
	 * @param container
	 */
	private void createImportChoicesGroup(Composite container) {
		Group importChoices = SWTFactory.createGroup(container, PDEUIMessages.ImportWizard_FirstPage_importGroup, 1, 1, GridData.FILL_HORIZONTAL);
		scanButton = SWTFactory.createRadioButton(importChoices, PDEUIMessages.ImportWizard_FirstPage_scanAll);
		importButton = SWTFactory.createRadioButton(importChoices, PDEUIMessages.ImportWizard_FirstPage_importPrereqs);
	}

	/**
	 * Create the import options group
	 * @param container
	 */
	private void createImportOptionsGroup(Composite container) {
		Group options = SWTFactory.createGroup(container, PDEUIMessages.ImportWizard_FirstPage_importAs, 1, 1, GridData.FILL_HORIZONTAL);
		binaryButton = SWTFactory.createRadioButton(options, PDEUIMessages.ImportWizard_FirstPage_binary);
		binaryWithLinksButton = SWTFactory.createRadioButton(options, PDEUIMessages.ImportWizard_FirstPage_binaryLinks);
		sourceButton = SWTFactory.createRadioButton(options, PDEUIMessages.ImportWizard_FirstPage_source);
		repositoryButton = SWTFactory.createRadioButton(options, PDEUIMessages.PluginImportWizardFirstPage_3);
	}

	/**
	 * Initialize the page with previous choices from dialog settings
	 */
	private void initialize() {
		IDialogSettings settings = getDialogSettings();

		ArrayList<String> items = new ArrayList<>();
		for (int i = 0; i < 6; i++) {
			String curr = settings.get(SETTINGS_DROPLOCATION + String.valueOf(i));
			if (curr != null && !items.contains(curr)) {
				items.add(curr);
			}
		}
		importDirectory.setItems(items.toArray(new String[items.size()]));
		refreshTargetDropDown();

		int source = FROM_ACTIVE_PLATFORM;
		try {
			source = settings.getInt(SETTINGS_FROM);
		} catch (NumberFormatException e) {
		}
		importDirectory.select(0);
		targetDefinitionCombo.select(0);
		updateSourceGroup(source);

		int importType = PluginImportOperation.IMPORT_BINARY;
		try {
			importType = settings.getInt(SETTINGS_IMPORTTYPE);
		} catch (NumberFormatException e) {
		}
		if (importType == PluginImportOperation.IMPORT_BINARY) {
			binaryButton.setSelection(true);
		} else if (importType == PluginImportOperation.IMPORT_BINARY_WITH_LINKS) {
			binaryWithLinksButton.setSelection(true);
		} else if (importType == PluginImportOperation.IMPORT_WITH_SOURCE) {
			sourceButton.setSelection(true);
		} else {
			repositoryButton.setSelection(true);
		}

		boolean scan = true;
		if (settings.get(SETTINGS_SCAN_ALL) != null) {
			scan = settings.getBoolean(SETTINGS_SCAN_ALL);
		}
		scanButton.setSelection(scan);
		importButton.setSelection(!scan);

	}

	/**
	 * Updates enabled state of the radio buttons/controls used to select the source
	 * of the import.
	 *
	 * @param source one of the source constants
	 */
	private void updateSourceGroup(int source) {
		importActiveTargetButton.setSelection(source == FROM_ACTIVE_PLATFORM);
		importTargetDefinitionButton.setSelection(source == FROM_TARGET_DEFINITION);
		targetDefinitionCombo.setEnabled(source == FROM_TARGET_DEFINITION);
		importDirectoryButton.setSelection(source == FROM_DIRECTORY);
		importDirectory.setEnabled(source == FROM_DIRECTORY);
		browseButton.setEnabled(source == FROM_DIRECTORY);
		if (source == FROM_ACTIVE_PLATFORM) {
			importDirectory.setText(TargetPlatform.getLocation());
		}
	}

	/**
	 * Loads the target definition drop down with all available targets
	 */
	private void refreshTargetDropDown() {
		ITargetPlatformService service = getTargetPlatformService();
		if (service != null) {
			ITargetHandle[] targets = service.getTargets(null);
			targetDefinitions = new ArrayList<>();
			for (ITargetHandle target : targets) {
				try {
					targetDefinitions.add(target.getTargetDefinition());
				} catch (CoreException e) {
					PDEPlugin.log(e);
				}
			}
			Collections.sort(targetDefinitions, (td1, td2) -> {
				String name1 = td1.getName() == null ? "" : td1.getName(); //$NON-NLS-1$
				String name2 = td2.getName() == null ? "" : td2.getName(); //$NON-NLS-1$
				return name1.compareTo(name2);
			});
			String[] names = new String[targetDefinitions.size()];
			for (int i = 0; i < targetDefinitions.size(); i++) {
				ITargetDefinition currentTarget = targetDefinitions.get(i);
				names[i] = currentTarget.getName();
				if (names[i] == null || names[i].trim().length() == 0) {
					names[i] = currentTarget.getHandle().toString();
				}
			}
			targetDefinitionCombo.setItems(names);
		}
	}

	/**
	 * Returns the target platform service or <code>null</code> if none.
	 *
	 * @return target platform service or <code>null</code>
	 */
	private ITargetPlatformService getTargetPlatformService() {
		ITargetPlatformService service = PDECore.getDefault().acquireService(ITargetPlatformService.class);
		return service;
	}

	/**
	 * Creates the directory group
	 * @param parent
	 */
	private void createImportFromGroup(Composite parent) {
		Group composite = SWTFactory.createGroup(parent, PDEUIMessages.ImportWizard_FirstPage_importFrom, 3, 1, GridData.FILL_HORIZONTAL);

		importActiveTargetButton = SWTFactory.createRadioButton(composite, PDEUIMessages.ImportWizard_FirstPage_target, 1);
		importActiveTargetButton.addSelectionListener(widgetSelectedAdapter(e -> {
			updateSourceGroup(getImportOrigin());
			validateDropLocation();
		}));

		Composite linkComp = SWTFactory.createComposite(composite, 1, 2, GridData.FILL_HORIZONTAL, 0, 0);
		openTargetPrefsLink = new Link(linkComp, SWT.NONE);
		openTargetPrefsLink.setText(PDEUIMessages.ImportWizard_FirstPage_goToTarget);
		openTargetPrefsLink.setLayoutData(new GridData(SWT.END, SWT.BEGINNING, true, false));
		openTargetPrefsLink.addSelectionListener(widgetSelectedAdapter(e -> {
			ITargetDefinition selected = getTargetDefinition();
			ITargetHandle handle = null;
			if (selected != null) {
				handle = selected.getHandle();
			}
			IPreferenceNode targetNode = new TargetPlatformPreferenceNode();
			if (showPreferencePage(targetNode, getShell())) {
				refreshTargetDropDown();
				// reselect same target, if possible
				int index = -1;
				if (handle != null) {
					for (int i = 0; i < targetDefinitions.size(); i++) {
						ITargetHandle h = targetDefinitions.get(i).getHandle();
						if (h.equals(handle)) {
							index = i;
							break;
						}
					}
				}
				if (index == -1 && !targetDefinitions.isEmpty()) {
					index = 0;
				}
				if (index >= 0) {
					targetDefinitionCombo.select(index);
				}
				importDirectory.setText(TargetPlatform.getLocation());
			}
		}));

		importTargetDefinitionButton = SWTFactory.createRadioButton(composite, PDEUIMessages.PluginImportWizardFirstPage_0, 1);
		importTargetDefinitionButton.addSelectionListener(widgetSelectedAdapter(e -> {
			updateSourceGroup(getImportOrigin());
			validateDropLocation();
		}));
		targetDefinitionCombo = SWTFactory.createCombo(composite, SWT.DROP_DOWN | SWT.READ_ONLY, 2, GridData.FILL_HORIZONTAL, null);
		targetDefinitionCombo.addSelectionListener(widgetSelectedAdapter(e -> validateDropLocation()));

		importDirectoryButton = SWTFactory.createRadioButton(composite, PDEUIMessages.ImportWizard_FirstPage_otherFolder, 1);
		importDirectoryButton.addSelectionListener(widgetSelectedAdapter(e -> {
			updateSourceGroup(getImportOrigin());
			validateDropLocation();
		}));

		importDirectory = SWTFactory.createCombo(composite, SWT.DROP_DOWN, 1, GridData.FILL_HORIZONTAL, null);
		importDirectory.addModifyListener(e -> validateDropLocation());

		browseButton = SWTFactory.createPushButton(composite, PDEUIMessages.ImportWizard_FirstPage_browse, null, GridData.HORIZONTAL_ALIGN_FILL);
		browseButton.addSelectionListener(widgetSelectedAdapter(e -> {
			IPath chosen = chooseDropLocation();
			if (chosen != null)
				importDirectory.setText(chosen.toOSString());
		}));
	}

	private boolean showPreferencePage(final IPreferenceNode targetNode, Shell shell) {
		PreferenceManager manager = new PreferenceManager();
		manager.addToRoot(targetNode);
		final PreferenceDialog dialog = new PreferenceDialog(shell, manager);
		final boolean[] result = new boolean[] {false};
		BusyIndicator.showWhile(shell.getDisplay(), () -> {
			dialog.create();
			dialog.setMessage(targetNode.getLabelText());
			if (dialog.open() == Window.OK)
				result[0] = true;
		});
		return result[0];
	}

	/**
	 * @return a chosen path from the directory dialog invoked from browse button
	 */
	private IPath chooseDropLocation() {
		DirectoryDialog dialog = new DirectoryDialog(getShell());
		dialog.setFilterPath(importDirectory.getText());
		dialog.setText(PDEUIMessages.ImportWizard_messages_folder_title);
		dialog.setMessage(PDEUIMessages.ImportWizard_messages_folder_message);
		String res = dialog.open();
		if (res != null) {
			return new Path(res);
		}
		return null;
	}

	/**
	 * @return the selection state of the scan all plug-ins button
	 */
	public boolean getScanAllPlugins() {
		return scanButton.getSelection();
	}

	/**
	 * Returns the type of the import. One of:
	 * <ul>
	 * <li>{@link PluginImportOperation#IMPORT_BINARY}</li>
	 * <li>{@link PluginImportOperation#IMPORT_BINARY_WITH_LINKS}</li>
	 * <li>{@link PluginImportOperation#IMPORT_WITH_SOURCE}</li>
	 * </ul>
	 * @return the type of the import.
	 */
	public int getImportType() {
		if (binaryButton.getSelection()) {
			return PluginImportOperation.IMPORT_BINARY;
		}

		if (binaryWithLinksButton.getSelection()) {
			return PluginImportOperation.IMPORT_BINARY_WITH_LINKS;
		}
		if (repositoryButton.getSelection()) {
			return PluginImportOperation.IMPORT_FROM_REPOSITORY;
		}
		return PluginImportOperation.IMPORT_WITH_SOURCE;
	}

	/**
	 * Returns alternate source locations to use when importing, or <code>null</code>
	 * if default locations are to be used.
	 *
	 * @return alternate source locations or <code>null</code>
	 */
	public SourceLocationManager getAlternateSourceLocations() {
		return alternateSource;
	}

	/**
	 * @return the location specified as the drop location for the target platform
	 */
	public String getDropLocation() {
		return importActiveTargetButton.getSelection() ? TARGET_PLATFORM : importDirectory.getText().trim();
	}

	/**
	 * Store all of the dialog settings for this page
	 */
	public void storeSettings() {
		IDialogSettings settings = getDialogSettings();
		boolean other = !importActiveTargetButton.getSelection();
		if (importDirectory.getText().length() > 0 && other) {
			settings.put(SETTINGS_DROPLOCATION + String.valueOf(0), importDirectory.getText().trim());
			String[] items = importDirectory.getItems();
			int nEntries = Math.min(items.length, 5);
			for (int i = 0; i < nEntries; i++) {
				settings.put(SETTINGS_DROPLOCATION + String.valueOf(i + 1), items[i]);
			}
		}
		settings.put(SETTINGS_FROM, getImportOrigin());
		settings.put(SETTINGS_IMPORTTYPE, getImportType());
		settings.put(SETTINGS_SCAN_ALL, getScanAllPlugins());
	}

	/**
	 * Returns a constant indicating what the import is performed on.
	 *
	 * @return source of import
	 */
	private int getImportOrigin() {
		int source = FROM_ACTIVE_PLATFORM;
		if (importTargetDefinitionButton.getSelection()) {
			source = FROM_TARGET_DEFINITION;
		} else if (importDirectoryButton.getSelection()) {
			source = FROM_DIRECTORY;
		}
		return source;
	}

	@Override
	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}

	/**
	 * Validates the drop location
	 */
	private void validateDropLocation() {
		if (importTargetDefinitionButton.getSelection() && targetDefinitionCombo.getText().length() == 0) {
			setPageComplete(false);
			setErrorMessage(PDEUIMessages.PluginImportWizardFirstPage_2);
			return;
		}
		if (importDirectoryButton.getSelection()) {
			IPath curr = new Path(importDirectory.getText());
			if (curr.segmentCount() == 0 && curr.getDevice() == null) {
				setErrorMessage(PDEUIMessages.ImportWizard_errors_locationMissing);
				setPageComplete(false);
				return;
			}
			if (!Path.ROOT.isValidPath(importDirectory.getText())) {
				setErrorMessage(PDEUIMessages.ImportWizard_errors_buildFolderInvalid);
				setPageComplete(false);
				return;
			}

			if (!curr.toFile().isDirectory()) {
				setErrorMessage(PDEUIMessages.ImportWizard_errors_buildFolderMissing);
				setPageComplete(false);
				return;
			}
		}
		setErrorMessage(null);
		setPageComplete(true);
		setMessage(PDEUIMessages.ImportWizard_FirstPage_desc);
	}

	/**
	 * Resolves the plug-ins at the given the base location. Uses plug-ins directory if present.
	 *
	 * @param location
	 */
	private void resolveArbitraryLocation(String location) {
		ITargetPlatformService service = getTargetPlatformService();
		if (service != null) {
			File plugins = new File(location, "plugins"); //$NON-NLS-1$
			ITargetLocation container = null;
			if (plugins.exists()) {
				container = service.newDirectoryLocation(plugins.getAbsolutePath());
			} else {
				container = service.newDirectoryLocation(location);
			}
			ITargetDefinition target = service.newTarget(); // temporary target
			target.setTargetLocations(new ITargetLocation[] {container});
			resolveTargetDefinition(target, getImportType());
		}
	}

	/**
	 * Resolves the plug-in locations for a target definition.
	 *
	 * @param target target definition
	 * @param type import operation type
	 */
	private void resolveTargetDefinition(final ITargetDefinition target, final int type) {
		IRunnableWithProgress op = monitor -> {
			SubMonitor subMon = SubMonitor.convert(monitor);
			subMon.beginTask(PDEUIMessages.PluginImportWizardFirstPage_1, 100);
			if (!target.isResolved()) {
				target.resolve(subMon.split(50));
			}
			subMon.setWorkRemaining(50);
			if (subMon.isCanceled()) {
				return;
			}
			// We allow importing of bundles that are unchecked in the target definition
			TargetBundle[] allBundles = target.getAllBundles();
			Map<SourceLocationKey, TargetBundle> sourceMap = new HashMap<>();
			List<URI> all = new ArrayList<>();
			for (TargetBundle bundle1 : allBundles) {
				if (bundle1.getStatus().isOK()) {
					URI location = bundle1.getBundleInfo().getLocation();
					if(location != null){
						all.add(location);
					}
					if (bundle1.isSourceBundle()) {
						sourceMap.put(new SourceLocationKey(bundle1.getBundleInfo().getSymbolicName(),
								new Version(bundle1.getBundleInfo().getVersion())), bundle1);
					}
				}
			}
			state = new PDEState(all.toArray(new URI[0]), false, false, subMon.split(30));
			models = state.getTargetModels();
			List<IPluginModelBase> sourceModels = new ArrayList<>();
			List<TargetBundle> sourceBundles = new ArrayList<>();
			for (IPluginModelBase model : models) {
				IPluginBase base = model.getPluginBase();
				TargetBundle bundle2 = sourceMap.get(new SourceLocationKey(base.getId(), new Version(base.getVersion())));
				if (bundle2 != null) {
					sourceModels.add(model);
					sourceBundles.add(bundle2);
				}
			}
			alternateSource = new AlternateSourceLocations(sourceModels.toArray(new IPluginModelBase[sourceModels.size()]), sourceBundles.toArray(new TargetBundle[sourceBundles.size()]));
			try {
				buildImportDescriptions(subMon.split(20), type);
			} catch (CoreException e2) {
				throw new InvocationTargetException(e2);
			}
			canceled = subMon.isCanceled();
		};
		try {
			getContainer().run(true, true, op);
		} catch (InvocationTargetException e) {
			PDEPlugin.log(e);
		} catch (InterruptedException e) {
		}
	}

	private void buildImportDescriptions(IProgressMonitor monitor, int type) throws CoreException {
		// build import instructions
		BundleProjectService service = (BundleProjectService) BundleProjectService.getDefault();
		repositoryModels.clear();
		importerToInstructions.clear();
		nextPages.clear();
		if (type == PluginImportOperation.IMPORT_FROM_REPOSITORY) {
			if (models != null) {
				importerToInstructions = service.getImportDescriptions(models);
				Iterator<Entry<IBundleImporter, ScmUrlImportDescription[]>> iterator = importerToInstructions.entrySet().iterator();
				while (iterator.hasNext()) {
					if (!monitor.isCanceled()) {
						Entry<IBundleImporter, ScmUrlImportDescription[]> entry = iterator.next();
						ScmUrlImportDescription[] descriptions = entry.getValue();
						for (ScmUrlImportDescription desc : descriptions) {
							repositoryModels.add(desc.getProperty(BundleProjectService.PLUGIN));
						}
					}
				}
			}
			if (!monitor.isCanceled()) {
				// contributed wizard pages
				Iterator<Entry<IBundleImporter, ScmUrlImportDescription[]>> iterator = importerToInstructions.entrySet().iterator();
				while (iterator.hasNext()) {
					Entry<IBundleImporter, ScmUrlImportDescription[]> entry = iterator.next();
					final IBundleImporter importer = entry.getKey();
					String importerId = importer.getId();
					IScmUrlImportWizardPage page = importIdToWizardPage.get(importerId);
					if (page == null) {
						page = TeamUI.getPages(importerId)[0];
						if (page != null) {
							importIdToWizardPage.put(importerId, page);
							((Wizard) getWizard()).addPage(page);
						}
					}
					if (page != null) {
						nextPages.add(page);
					}
				}
			}
		}
		if (monitor.isCanceled()) {
			importerToInstructions.clear();
			repositoryModels.clear();
			nextPages.clear();
		}
	}

	/**
	 * Returns whether the contributed pages are complete.
	 *
	 * @return whether the contributed pages are complete
	 */
	boolean arePagesComplete() {
		Iterator<IScmUrlImportWizardPage> iterator = nextPages.iterator();
		while (iterator.hasNext()) {
			IScmUrlImportWizardPage page = iterator.next();
			if (!page.isPageComplete()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Finishes contributed pages.
	 *
	 * @return whether finish was successful
	 */
	boolean finishPages() {
		Iterator<IScmUrlImportWizardPage> iterator = nextPages.iterator();
		while (iterator.hasNext()) {
			IScmUrlImportWizardPage page = iterator.next();
			if (!page.finish()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns a map of importers to their bundle import descriptions to process.
	 *
	 * @return map of bundle import descriptions to process, by importers
	 */
	Map<IBundleImporter, ScmUrlImportDescription[]> getImportDescriptions() {
		Map<IBundleImporter, ScmUrlImportDescription[]> map = new HashMap<>();
		if (getImportType() == PluginImportOperation.IMPORT_FROM_REPOSITORY) {
			IBundleImporter[] importers = Team.getBundleImporters();
			for (IBundleImporter importer : importers) {
				if (importerToInstructions.containsKey(importer)) {
					IScmUrlImportWizardPage page = importIdToWizardPage.get(importer.getId());
					if (page != null && nextPages.contains(page) && page.getSelection() != null) {
						map.put(importer, page.getSelection());
					}
				}
			}
		}
		return map;
	}

	/**
	 * Returns the next page to display or <code>null</code> if none.
	 *
	 * @param page current page
	 * @return next page or <code>null</code>
	 */
	IWizardPage getNextPage(IWizardPage page) {
		if (nextPages.isEmpty()) {
			return null;
		}

		if (page instanceof IScmUrlImportWizardPage) {
			int index = nextPages.indexOf(page);
			if (index >= 0 && index < (nextPages.size() - 1)) {
				IWizardPage nextPage = nextPages.get(index + 1);
				return isPageEmpty(nextPage) ? null : nextPage;
			}
		}

		if (page instanceof PluginImportWizardDetailedPage || page instanceof PluginImportWizardExpressPage) {
			Iterator<IScmUrlImportWizardPage> iter = nextPages.iterator();
			while (iter.hasNext()) {
				IWizardPage nextPage = iter.next();
				if (!isPageEmpty(nextPage))
					return nextPage;
			}
		}
		return null;
	}

	private boolean isPageEmpty(IWizardPage page) {
		if (!(page instanceof IScmUrlImportWizardPage))
			return false;
		ScmUrlImportDescription[] selection = ((IScmUrlImportWizardPage) page).getSelection();
		return selection == null || selection.length == 0;
	}

	/**
	 * Returns the previous page to display or <code>null</code> if none.
	 *
	 * @param page current page
	 * @return previous page or <code>null</code>
	 */
	IWizardPage getPreviousPage(IWizardPage page) {
		if (page instanceof IScmUrlImportWizardPage) {
			int index = nextPages.indexOf(page);
			if (index > 0) {
				return nextPages.get(index - 1);
			}
		}
		return null;
	}

	/**
	 * @return the complete set of {@link IPluginModelBase}s for the given drop location, including disabled bundles
	 */
	public IPluginModelBase[] getModels() {
		switch (getImportOrigin()) {
			case FROM_ACTIVE_PLATFORM :
				ITargetPlatformService service = getTargetPlatformService();
				if (service != null) {
					try {
						ITargetDefinition target = service.getWorkspaceTargetDefinition();
						resolveTargetDefinition(target, getImportType());
					} catch (CoreException e) {
						PDEPlugin.log(e);
					}
				}
				break;
			case FROM_TARGET_DEFINITION :
				resolveTargetDefinition(getTargetDefinition(), getImportType());
				break;
			case FROM_DIRECTORY :
				resolveArbitraryLocation(getDropLocation());
				break;
		}
		return models;
	}

	/**
	 * @return the state the was used to resolve the models, will be <code>null</code> unless
	 * getModels() has been called previously.
	 */
	public PDEState getState() {
		return state;
	}

	/**
	 * Returns the selected target definition or <code>null</code>
	 * if there are none.
	 *
	 * @return selected target definition or <code>null</code>
	 */
	private ITargetDefinition getTargetDefinition() {
		int index = targetDefinitionCombo.getSelectionIndex();
		if (index >= 0 && !targetDefinitions.isEmpty()) {
			return targetDefinitions.get(targetDefinitionCombo.getSelectionIndex());
		}
		return null;
	}

	@Override
	public boolean isCurrentPage() {
		return super.isCurrentPage();
	}

	/**
	 * @return true if the page needs to be refreshed, false otherwise
	 */
	public boolean isRefreshNeeded() {
		if (canceled) {
			canceled = false;
			return true;
		}
		return false;
	}

	/**
	 * Returns an object representing what will be imported.
	 *
	 * @return source of the import
	 */
	public Object getImportSource() {
		switch (getImportOrigin()) {
			case FROM_TARGET_DEFINITION :
				return getTargetDefinition();
			case FROM_ACTIVE_PLATFORM :
			case FROM_DIRECTORY :
			default :
				return getDropLocation();
		}
	}

	/**
	 * Notifies the contributed bundle import pages of the bundles to import.
	 *
	 * @param models the models selected for import
	 */
	public void configureBundleImportPages(IPluginModelBase[] models) {
		// make a set of the models to import for quick lookup
		Set<IPluginModelBase> modelsSet = new HashSet<>();
		for (IPluginModelBase model : models) {
			modelsSet.add(model);
		}
		Map<IBundleImporter, List<ScmUrlImportDescription>> importerToImportees = new HashMap<>();
		Iterator<Entry<IBundleImporter, ScmUrlImportDescription[]>> iterator = importerToInstructions.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<IBundleImporter, ScmUrlImportDescription[]> entry = iterator.next();
			IBundleImporter importer = entry.getKey();
			ScmUrlImportDescription[] descriptions = entry.getValue();
			for (ScmUrlImportDescription desc : descriptions) {
				IPluginModelBase model = (IPluginModelBase) desc.getProperty(BundleProjectService.PLUGIN);
				if (modelsSet.contains(model)) {
					List<ScmUrlImportDescription> importees = importerToImportees.get(importer);
					if (importees == null) {
						importees = new ArrayList<>();
						importerToImportees.put(importer, importees);
					}
					importees.add(desc);
				}
			}
		}

		// First clear the selection for all pages
		Iterator<IScmUrlImportWizardPage> iterator2 = importIdToWizardPage.values().iterator();
		while (iterator2.hasNext())
			iterator2.next().setSelection(new ScmUrlImportDescription[0]);

		Iterator<Entry<IBundleImporter, List<ScmUrlImportDescription>>> iterator3 = importerToImportees.entrySet().iterator();
		while (iterator3.hasNext()) {
			Entry<IBundleImporter, List<ScmUrlImportDescription>> entry = iterator3.next();
			IBundleImporter importer = entry.getKey();
			List<ScmUrlImportDescription> list = entry.getValue();
			ScmUrlImportDescription[] descriptions = list.toArray(new ScmUrlImportDescription[list.size()]);
			IScmUrlImportWizardPage page = importIdToWizardPage.get(importer.getId());
			if (page != null) {
				page.setSelection(descriptions);
			}
		}
	}
}
