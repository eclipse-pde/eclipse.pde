/*******************************************************************************
 *  Copyright (c) 2003, 2019 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487943
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.imports;

import java.util.ArrayList;
import java.util.Set;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.pde.core.IModelProviderEvent;
import org.eclipse.pde.core.IModelProviderListener;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.ClasspathUtilCore;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.bundle.BundlePluginModel;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.osgi.framework.Version;

public abstract class BaseImportWizardSecondPage extends WizardPage implements IModelProviderListener {

	protected static final String SETTINGS_ADD_FRAGMENTS = "addFragments"; //$NON-NLS-1$
	protected static final String SETTINGS_AUTOBUILD = "autobuild"; //$NON-NLS-1$

	protected PluginImportWizardFirstPage fPage1;
	protected IPluginModelBase[] fModels = new IPluginModelBase[0];
	private Object fImportSource;
	private int fImportType;
	protected Button fAddFragmentsButton;
	private Button fAutoBuildButton;
	protected TableViewer fImportListViewer;
	private boolean fRefreshNeeded = true;

	class ContentProvider implements IStructuredContentProvider {
		@Override
		public Object[] getElements(Object element) {
			return new Object[0];
		}
	}

	public BaseImportWizardSecondPage(String pageName, PluginImportWizardFirstPage page) {
		super(pageName);
		fPage1 = page;
		PDECore.getDefault().getModelManager().getExternalModelManager().addModelProviderListener(this);
	}

	protected Composite createImportList(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		Label label = new Label(container, SWT.NONE);
		label.setText(PDEUIMessages.ImportWizard_DetailedPage_importList);

		Table table = new Table(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 225;
		table.setLayoutData(gd);

		fImportListViewer = new TableViewer(table);
		fImportListViewer.setLabelProvider(new PluginImportLabelProvider());
		fImportListViewer.setContentProvider(new ContentProvider());
		fImportListViewer.setInput(PDECore.getDefault().getModelManager().getExternalModelManager());
		fImportListViewer.setComparator(ListUtil.PLUGIN_COMPARATOR);
		return container;
	}

	protected void createComputationsOption(Composite parent) {
		fAddFragmentsButton = SWTFactory.createCheckButton(parent, PDEUIMessages.ImportWizard_SecondPage_addFragments, null, true, 1);
		if (getDialogSettings().get(SETTINGS_ADD_FRAGMENTS) != null)
			fAddFragmentsButton.setSelection(getDialogSettings().getBoolean(SETTINGS_ADD_FRAGMENTS));
		else
			fAddFragmentsButton.setSelection(true);

		if (!PDEPlugin.getWorkspace().isAutoBuilding()) {
			fAutoBuildButton = SWTFactory.createCheckButton(parent, PDEUIMessages.BaseImportWizardSecondPage_autobuild, null, getDialogSettings().getBoolean(SETTINGS_AUTOBUILD), 1);
		}
	}

	@Override
	public void dispose() {
		PDECore.getDefault().getModelManager().getExternalModelManager().removeModelProviderListener(this);
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible && isRefreshNeeded()) {
			fModels = fPage1.getModels();
			refreshPage();
		}
	}

	protected abstract void refreshPage();

	protected boolean isRefreshNeeded() {
		if (fRefreshNeeded) {
			fRefreshNeeded = false;
			fImportSource = fPage1.getImportSource();
			fImportType = fPage1.getImportType();
			return true;
		}
		Object currSource = fPage1.getImportSource();
		if (fImportSource == null || !fImportSource.equals(currSource)) {
			fImportSource = fPage1.getImportSource();
			return true;
		}
		// If the import type was changed to/from repository need refresh to filter available models
		int currType = fPage1.getImportType();
		if (currType != fImportType) {
			if (currType == PluginImportOperation.IMPORT_FROM_REPOSITORY || fImportType == PluginImportOperation.IMPORT_FROM_REPOSITORY) {
				fImportType = currType;
				return true;
			}
			fImportType = currType;
		}
		return fPage1.isRefreshNeeded();
	}

	/**
	 * Find the model that best matches the given string id and version.  If there is a
	 * bundle that matches the name and version it returns.  If there is a bundle with the
	 * correct name but no matching version, the highest version available will be returned.
	 * If no match could be found this method returns <code>null</code>
	 *
	 * @param id id of the bundle to find
	 * @param version version of the bundle to find, may be <code>null</code>
	 * @return the best matching bundle or <code>null</code>
	 */
	private IPluginModelBase findModel(String id, String version) {
		// Look for a matching version, if one cannot be found, take the highest version
		IPluginModelBase bestMatch = null;
		for (IPluginModelBase model : fModels) {
			String modelId = model.getPluginBase().getId();
			if (modelId != null && modelId.equals(id)) {
				String modelVersion = model.getPluginBase().getVersion();
				if (modelVersion != null && modelVersion.equals(version)) {
					// Strict version match, return this model
					return model;
				}
				if (bestMatch == null || bestMatch.getPluginBase().getVersion() == null || version == null) {
					// No good match yet, use current model
					bestMatch = model;
				} else {
					// At least one good match, use highest version
					Version bestVersion = Version.parseVersion(bestMatch.getPluginBase().getVersion());
					Version currentVersion = Version.parseVersion(version);
					if (bestVersion.compareTo(currentVersion) < 0) {
						bestMatch = model;
					}
				}
			}
		}
		return bestMatch;
	}

	private IFragmentModel[] findFragments(IPlugin plugin) {
		ArrayList<IPluginModelBase> result = new ArrayList<>();
		for (IPluginModelBase model : fModels) {
			if (model instanceof IFragmentModel) {
				IFragment fragment = ((IFragmentModel) model).getFragment();
				if (plugin.getId().equalsIgnoreCase(fragment.getPluginId())) {
					result.add(model);
				}
			}
		}
		return result.toArray(new IFragmentModel[result.size()]);
	}

	protected void addPluginAndDependencies(IPluginModelBase model, ArrayList<IPluginModelBase> selected, boolean addFragments) {

		boolean containsVariable = false;
		if (!selected.contains(model)) {
			selected.add(model);
			boolean hasextensibleAPI = ClasspathUtilCore.hasExtensibleAPI(model);
			if (!addFragments && !hasextensibleAPI && model instanceof IPluginModel) {
				IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
				for (IPluginLibrary library : libraries) {
					if (ClasspathUtilCore.containsVariables(library.getName())) {
						containsVariable = true;
						break;
					}
				}
			}
			addDependencies(model, selected, addFragments || containsVariable || hasextensibleAPI);
		}
	}

	protected void addUnresolvedImportPackagesModels(IPluginModelBase model, ArrayList<IPluginModelBase> result, boolean addFragments) {
		ISharedPluginModel sharedModel = model.getPluginBase().getModel();
		if (sharedModel instanceof BundlePluginModel) {
			BundlePluginModel bundleModel = (BundlePluginModel) sharedModel;
			ImportPackageSpecification[] importPackages = bundleModel.getBundleDescription().getImportPackages();
			for (ImportPackageSpecification importPackageSpecification : importPackages) {
				IPluginModelBase found = findModel(importPackageSpecification.getName(), null);
				for (IPluginModelBase mod : fModels) {
					ExportPackageDescription[] exportPackages = mod.getBundleDescription().getExportPackages();
					for (ExportPackageDescription exp : exportPackages) {
						if (exp.getName().equals(importPackageSpecification.getName())) {
							found = mod;
							break;
						}
					}
				}
				 if (found != null) {
					addPluginAndDependencies(found, result, addFragments);
				 }
			}
		}
	}

	protected void addDependencies(IPluginModelBase model, ArrayList<IPluginModelBase> selected, boolean addFragments) {

		IPluginImport[] required = model.getPluginBase().getImports();
		if (required.length > 0) {
			for (IPluginImport pluginImport : required) {
				IPluginModelBase found = findModel(pluginImport.getId(), pluginImport.getVersion());
				if (found != null) {
					addPluginAndDependencies(found, selected, addFragments);
				}
			}
		}

		if (addFragments) {
			if (model instanceof IPluginModel) {
				IFragmentModel[] fragments = findFragments(((IPluginModel) model).getPlugin());
				for (IFragmentModel fragment : fragments) {
					addPluginAndDependencies(fragment, selected, addFragments);
				}
			} else {
				IFragment fragment = ((IFragmentModel) model).getFragment();
				IPluginModelBase found = findModel(fragment.getPluginId(), fragment.getVersion());
				if (found != null) {
					addPluginAndDependencies(found, selected, addFragments);
				}
			}
		}
	}

	public IPluginModelBase[] getModelsToImport() {
		TableItem[] items = fImportListViewer.getTable().getItems();
		ArrayList<Object> result = new ArrayList<>();
		for (TableItem item : items) {
			result.add(item.getData());
		}
		return result.toArray(new IPluginModelBase[result.size()]);
	}

	public void storeSettings() {
		IDialogSettings settings = getDialogSettings();
		settings.put(SETTINGS_ADD_FRAGMENTS, fAddFragmentsButton.getSelection());
		if (fAutoBuildButton != null)
			settings.put(SETTINGS_AUTOBUILD, fAutoBuildButton.getSelection());
	}

	@Override
	public void modelsChanged(IModelProviderEvent event) {
		fRefreshNeeded = true;
	}

	public boolean forceAutoBuild() {
		return fAutoBuildButton != null && getDialogSettings().getBoolean(SETTINGS_AUTOBUILD);
	}

	/**
	 * Adds a warning if importing from a repository and not all bundles are available in
	 * a repository.
	 * <p>
	 * The detail page no longer needs this as its available list is filtered, but on the express page
	 * we may end up with plug-ins from the workspace that require plug-ins without repo info.  In that
	 * case the bundles will still be added to the right column but won't be available on the next page
	 * (CVS page).
	 * </p>
	 */
	protected void checkRepositoryAvailability() {
		PluginImportWizardFirstPage page = (PluginImportWizardFirstPage) getPreviousPage();
		if (page.getImportType() == PluginImportOperation.IMPORT_FROM_REPOSITORY) {
			if (getMessageType() != ERROR && getMessageType() != WARNING) {
				IPluginModelBase[] selected = getModelsToImport();
				Set<?> available = page.repositoryModels;
				for (int i = 0; i < selected.length; i++) {
					if (!available.contains(selected[i])) {
						setMessage(PDEUIMessages.BaseImportWizardSecondPage_0, WARNING);
						return;
					}
				}
			}
		}
	}

}
