/*******************************************************************************
 * Copyright (c) 2003, 2009 IBM Corporation and others.
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
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.importing.BundleImportDescription;
import org.eclipse.pde.core.importing.IBundleImporter;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDEPreferencesManager;
import org.eclipse.pde.internal.core.PDEState;
import org.eclipse.pde.internal.core.SourceLocationKey;
import org.eclipse.pde.internal.core.SourceLocationManager;
import org.eclipse.pde.internal.core.project.BundleProjectService;
import org.eclipse.pde.internal.core.target.provisional.IBundleContainer;
import org.eclipse.pde.internal.core.target.provisional.IResolvedBundle;
import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;
import org.eclipse.pde.internal.core.target.provisional.ITargetHandle;
import org.eclipse.pde.internal.core.target.provisional.ITargetPlatformService;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.SWTFactory;
import org.eclipse.pde.internal.ui.preferences.TargetPlatformPreferenceNode;
import org.eclipse.pde.ui.IBundeImportWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Version;

/**
 * The first page of the import plug-ins wizard
 */
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
	private List targetDefinitions;
	private Combo importDirectory;
	private Button openTargetPrefsButton;

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
	protected Set repositoryModels = new HashSet();

	/**
	 * Maps bundle importers to import instructions
	 */
	private Map importerToInstructions = new HashMap();

	/**
	 * Map of bundle importer extension id to associated wizard page
	 */
	private Map importIdToWizardPage = new HashMap();

	/**
	 * Array of next wizard pages (in order)
	 */
	private List nextPages = new ArrayList();

	public PluginImportWizardFirstPage(String name) {
		super(name);
		setTitle(PDEUIMessages.ImportWizard_FirstPage_title);
		setMessage(PDEUIMessages.ImportWizard_FirstPage_desc);
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
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

		ArrayList items = new ArrayList();
		for (int i = 0; i < 6; i++) {
			String curr = settings.get(SETTINGS_DROPLOCATION + String.valueOf(i));
			if (curr != null && !items.contains(curr)) {
				items.add(curr);
			}
		}
		importDirectory.setItems((String[]) items.toArray(new String[items.size()]));
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
			importDirectory.setText(getTargetHome());
		}
	}

	/**
	 * Loads the target definition drop down with all available targets
	 */
	private void refreshTargetDropDown() {
		ITargetPlatformService service = getTargetPlatformService();
		if (service != null) {
			ITargetHandle[] targets = service.getTargets(null);
			targetDefinitions = new ArrayList();
			for (int i = 0; i < targets.length; i++) {
				try {
					targetDefinitions.add(targets[i].getTargetDefinition());
				} catch (CoreException e) {
					PDEPlugin.log(e);
				}
			}
			Collections.sort(targetDefinitions, new Comparator() {

				public int compare(Object o1, Object o2) {
					ITargetDefinition td1 = (ITargetDefinition) o1;
					ITargetDefinition td2 = (ITargetDefinition) o2;
					String name1 = td1.getName() == null ? "" : td1.getName(); //$NON-NLS-1$
					String name2 = td2.getName() == null ? "" : td2.getName(); //$NON-NLS-1$
					return name1.compareTo(name2);
				}
			});
			String[] names = new String[targetDefinitions.size()];
			for (int i = 0; i < targetDefinitions.size(); i++) {
				ITargetDefinition currentTarget = (ITargetDefinition) targetDefinitions.get(i);
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
		ITargetPlatformService service = (ITargetPlatformService) PDECore.getDefault().acquireService(ITargetPlatformService.class.getName());
		return service;
	}

	/**
	 * Creates the directory group
	 * @param parent
	 */
	private void createImportFromGroup(Composite parent) {
		Group composite = SWTFactory.createGroup(parent, PDEUIMessages.ImportWizard_FirstPage_importFrom, 3, 1, GridData.FILL_HORIZONTAL);

		importActiveTargetButton = SWTFactory.createRadioButton(composite, PDEUIMessages.ImportWizard_FirstPage_target, 3);
		importActiveTargetButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateSourceGroup(getImportOrigin());
				validateDropLocation();
			}
		});

		importTargetDefinitionButton = SWTFactory.createRadioButton(composite, PDEUIMessages.PluginImportWizardFirstPage_0, 1);
		importTargetDefinitionButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateSourceGroup(getImportOrigin());
				validateDropLocation();
			}
		});
		targetDefinitionCombo = SWTFactory.createCombo(composite, SWT.DROP_DOWN | SWT.READ_ONLY, 1, GridData.FILL_HORIZONTAL, null);
		targetDefinitionCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				validateDropLocation();
			}
		});

		openTargetPrefsButton = SWTFactory.createPushButton(composite, PDEUIMessages.ImportWizard_FirstPage_goToTarget, null);
		openTargetPrefsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
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
							ITargetHandle h = ((ITargetDefinition) targetDefinitions.get(i)).getHandle();
							if (h.equals(handle)) {
								index = i;
								break;
							}
						}
					}
					if (index == -1 && targetDefinitions.size() > 0) {
						index = 0;
					}
					if (index >= 0) {
						targetDefinitionCombo.select(index);
					}
					importDirectory.setText(TargetPlatform.getLocation());
				}
			}
		});

		importDirectoryButton = SWTFactory.createRadioButton(composite, PDEUIMessages.ImportWizard_FirstPage_otherFolder, 1);
		importDirectoryButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateSourceGroup(getImportOrigin());
				validateDropLocation();
			}
		});

		importDirectory = SWTFactory.createCombo(composite, SWT.DROP_DOWN, 1, GridData.FILL_HORIZONTAL, null);
		importDirectory.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validateDropLocation();
			}
		});

		browseButton = SWTFactory.createPushButton(composite, PDEUIMessages.ImportWizard_FirstPage_browse, null, GridData.HORIZONTAL_ALIGN_FILL);
		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IPath chosen = chooseDropLocation();
				if (chosen != null)
					importDirectory.setText(chosen.toOSString());
			}
		});
	}

	private boolean showPreferencePage(final IPreferenceNode targetNode, Shell shell) {
		PreferenceManager manager = new PreferenceManager();
		manager.addToRoot(targetNode);
		final PreferenceDialog dialog = new PreferenceDialog(shell, manager);
		final boolean[] result = new boolean[] {false};
		BusyIndicator.showWhile(shell.getDisplay(), new Runnable() {
			public void run() {
				dialog.create();
				dialog.setMessage(targetNode.getLabelText());
				if (dialog.open() == Window.OK)
					result[0] = true;
			}
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
	 * @return the value of the {@link ICoreConstants#PLATFORM_PATH} preference
	 */
	private String getTargetHome() {
		PDEPreferencesManager preferences = PDECore.getDefault().getPreferencesManager();
		return preferences.getString(ICoreConstants.PLATFORM_PATH);
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

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.DialogPage#dispose()
	 */
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
	 * Resolves the target platform
	 * @param type import type
	 */
	private void resolveTargetPlatform(final int type) {
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				models = PluginRegistry.getExternalModels();
				state = PDECore.getDefault().getModelManager().getState();
				alternateSource = null;
				try {
					buildImportDescriptions(monitor, type);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
				monitor.done();
			}
		};
		try {
			getContainer().run(true, false, op);
		} catch (InvocationTargetException e) {
			PDEPlugin.log(e);
		} catch (InterruptedException e) {
		}
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
			IBundleContainer container = null;
			if (plugins.exists()) {
				container = service.newDirectoryContainer(plugins.getAbsolutePath());
			} else {
				container = service.newDirectoryContainer(location);
			}
			ITargetDefinition target = service.newTarget(); // temporary target
			target.setBundleContainers(new IBundleContainer[] {container});
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
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				monitor.beginTask(PDEUIMessages.PluginImportWizardFirstPage_1, 100);
				SubProgressMonitor pm = new SubProgressMonitor(monitor, 50);
				target.resolve(pm);
				pm.done();
				if (monitor.isCanceled()) {
					return;
				}
				IResolvedBundle[] bundles = target.getBundles();
				Map sourceMap = new HashMap();
				URL[] all = new URL[bundles.length];
				for (int i = 0; i < bundles.length; i++) {
					IResolvedBundle bundle = bundles[i];
					try {
						if (bundle.getStatus().isOK()) {
							all[i] = new File(bundle.getBundleInfo().getLocation()).toURL();
							if (bundle.isSourceBundle()) {
								sourceMap.put(new SourceLocationKey(bundle.getBundleInfo().getSymbolicName(), new Version(bundle.getBundleInfo().getVersion())), bundle);
							}
						}
					} catch (MalformedURLException e) {
						setErrorMessage(e.getMessage());
						monitor.setCanceled(true);
						return;
					}
				}
				pm = new SubProgressMonitor(monitor, 50);
				state = new PDEState(all, false, pm);
				models = state.getTargetModels();
				List sourceModels = new ArrayList();
				List sourceBundles = new ArrayList();
				for (int i = 0; i < models.length; i++) {
					IPluginBase base = models[i].getPluginBase();
					IResolvedBundle bundle = (IResolvedBundle) sourceMap.get(new SourceLocationKey(base.getId(), new Version(base.getVersion())));
					if (bundle != null) {
						sourceModels.add(models[i]);
						sourceBundles.add(bundle);
					}
				}
				alternateSource = new AlternateSourceLocations((IPluginModelBase[]) sourceModels.toArray(new IPluginModelBase[sourceModels.size()]), (IResolvedBundle[]) sourceBundles.toArray(new IResolvedBundle[sourceBundles.size()]));
				try {
					buildImportDescriptions(pm, type);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
				pm.done();
				canceled = monitor.isCanceled();
				monitor.done();
			}
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
				Iterator iterator = importerToInstructions.entrySet().iterator();
				while (iterator.hasNext()) {
					if (!monitor.isCanceled()) {
						Entry entry = (Entry) iterator.next();
						BundleImportDescription[] descriptions = (BundleImportDescription[]) entry.getValue();
						for (int i = 0; i < descriptions.length; i++) {
							repositoryModels.add(descriptions[i].getProperty(BundleProjectService.PLUGIN));
						}
					}
				}
			}
			if (!monitor.isCanceled()) {
				// contributed wizard pages
				Iterator iterator = importerToInstructions.entrySet().iterator();
				while (iterator.hasNext()) {
					Entry entry = (Entry) iterator.next();
					IBundleImporter importer = (IBundleImporter) entry.getKey();
					IBundeImportWizardPage page = (IBundeImportWizardPage) importIdToWizardPage.get(importer.getId());
					if (page == null) {
						page = getPage(importer.getId());
						if (page != null) {
							importIdToWizardPage.put(importer.getId(), page);
							((Wizard) getWizard()).addPage(page);
						}
					}
					if (page != null) {
						nextPages.add(page);
						page.setSelection((BundleImportDescription[]) entry.getValue());
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
		Iterator iterator = nextPages.iterator();
		while (iterator.hasNext()) {
			IBundeImportWizardPage page = (IBundeImportWizardPage) iterator.next();
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
		Iterator iterator = nextPages.iterator();
		while (iterator.hasNext()) {
			IBundeImportWizardPage page = (IBundeImportWizardPage) iterator.next();
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
	Map getImportDescriptions() {
		Map map = new HashMap();
		if (getImportType() == PluginImportOperation.IMPORT_FROM_REPOSITORY) {
			IBundleImporter[] importers = ((BundleProjectService) BundleProjectService.getDefault()).getBundleImporters();
			for (int i = 0; i < importers.length; i++) {
				IBundleImporter importer = importers[i];
				if (importerToInstructions.containsKey(importer)) {
					IBundeImportWizardPage page = (IBundeImportWizardPage) importIdToWizardPage.get(importer.getId());
					if (page != null && nextPages.contains(page)) {
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
		if (page instanceof IBundeImportWizardPage) {
			int index = nextPages.indexOf(page);
			if (index >= 0 && index < (nextPages.size() - 2)) {
				return (IWizardPage) nextPages.get(index + 1);
			}
		}
		if (page instanceof PluginImportWizardDetailedPage) {
			return (IWizardPage) nextPages.get(0);
		}
		return null;
	}

	/**
	 * Returns the previous page to display or <code>null</code> if none.
	 * 
	 * @param page current page
	 * @return previous page or <code>null</code>
	 */
	IWizardPage getPreviousPage(IWizardPage page) {
		if (page instanceof IBundeImportWizardPage) {
			int index = nextPages.indexOf(page);
			if (index > 0) {
				return (IWizardPage) nextPages.get(index - 1);
			}
		}
		return null;
	}

	/**
	 * Creates and returns a wizard page associated with the given bundle importer extension identifier
	 * or <code>null</code> of none.
	 * 
	 * @param importerId org.eclipse.pde.core.bundleImporters extension identifier
	 * @return associated bundle import wizard page or <code>null</code>
	 */
	private IBundeImportWizardPage getPage(String importerId) {
		IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(IPDEUIConstants.EXTENSION_POINT_BUNDLE_IMPORT_PAGES);
		if (point != null) {
			IConfigurationElement[] infos = point.getConfigurationElements();
			for (int i = 0; i < infos.length; i++) {
				IConfigurationElement element = infos[i];
				String id = element.getAttribute("bundleImporter"); //$NON-NLS-1$
				if (id != null && importerId.equals(id)) {
					try {
						return (IBundeImportWizardPage) element.createExecutableExtension("class"); //$NON-NLS-1$
					} catch (CoreException e) {
						PDEPlugin.log(e);
					}
				}
			}
		}
		return null;
	}

	/**
	 * @return the complete set of {@link IPluginModelBase}s for the given drop location
	 */
	public IPluginModelBase[] getModels() {
		switch (getImportOrigin()) {
			case FROM_ACTIVE_PLATFORM :
				resolveTargetPlatform(getImportType());
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
		if (index >= 0 && targetDefinitions.size() > 0) {
			return (ITargetDefinition) targetDefinitions.get(targetDefinitionCombo.getSelectionIndex());
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.WizardPage#isCurrentPage()
	 */
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
}
