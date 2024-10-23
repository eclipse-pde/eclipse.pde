/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *     EclipseSource Corporation - ongoing enhancements
 *     Simon Scholz <simon.scholz@vogella.com> - Bug 449348
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 434428
 *******************************************************************************/
package org.eclipse.pde.internal.ui.dialogs;

import java.util.Comparator;
import java.util.HashMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageHeader;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageObject;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;
import org.osgi.framework.Constants;
import org.osgi.framework.VersionRange;

public class PluginSelectionDialog extends FilteredItemsSelectionDialog {

	private static final String DIALOG_SETTINGS = "org.eclipse.pde.ui.dialogs.PluginSelectionDialog"; //$NON-NLS-1$
	private final IPluginModelBase[] fModels;
	String originalPattern;

	private class PluginSearchItemsFilter extends ItemsFilter {

		public PluginSearchItemsFilter() {
			super();
			originalPattern = patternMatcher.getPattern();
			String pattern = originalPattern;
			if (pattern.indexOf('*') != 0 && pattern.indexOf('?') != 0 && pattern.indexOf('.') != 0) {
				pattern = "*" + pattern; //$NON-NLS-1$
				patternMatcher.setPattern(pattern);
			}
		}

		@Override
		public boolean isConsistentItem(Object item) {
			return true;
		}

		@Override
		public boolean matchItem(Object item) {
			String id = null;
			if (item instanceof IPluginModelBase model) {
				id = model.getPluginBase().getId();
			}

			return (matches(id));
		}
	}

	private class PluginSearchComparator implements Comparator<Object> {

		@Override
		public int compare(Object o1, Object o2) {
			int id1 = getId(o1);
			int id2 = getId(o2);

			if (id1 != id2) {
				return id1 - id2;
			}
			return compareSimilarObjects(o1, o2);
		}

		private int getId(Object element) {
			if (element instanceof IPluginModelBase) {
				return 100;
			}
			return 0;
		}

		private int compareSimilarObjects(Object o1, Object o2) {
			if (o1 instanceof IPluginModelBase ipmb1 && o2 instanceof IPluginModelBase) {
				IPluginModelBase ipmb2 = (IPluginModelBase) o2;
				return comparePlugins(ipmb1.getPluginBase(), ipmb2.getPluginBase());
			}
			return 0;
		}

		private int comparePlugins(IPluginBase ipmb1, IPluginBase ipmb2) {
			if (ipmb1.getId().startsWith(originalPattern) && !ipmb2.getId().startsWith(originalPattern)) {
				return -1;
			}
			if (!ipmb1.getId().startsWith(originalPattern) && ipmb2.getId().startsWith(originalPattern)) {
				return 1;
			}
			return ipmb1.getId().compareTo(ipmb2.getId());
		}

	}

	public PluginSelectionDialog(Shell parentShell, boolean includeFragments, boolean multipleSelection) {
		this(parentShell, getElements(includeFragments), multipleSelection);
	}

	public PluginSelectionDialog(Shell parentShell, IPluginModelBase[] models, boolean multipleSelection) {
		super(parentShell, multipleSelection);
		fModels = models;
		setTitle(PDEUIMessages.PluginSelectionDialog_title);
		setMessage(PDEUIMessages.PluginSelectionDialog_message);
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		setListLabelProvider(PDEPlugin.getDefault().getLabelProvider());
	}

	public PluginSelectionDialog(Shell activeWorkbenchShell, IPluginModelBase[] availablePlugins,
			boolean multipleSelection, IPluginModelBase model) {
		this(activeWorkbenchShell, availablePlugins, multipleSelection);
		PDEPlugin.getDefault().getLabelProvider().setCurrentModel(model);
	}
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, IHelpContextIds.PLUGIN_SELECTION);
	}

	@Override
	public boolean close() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		return super.close();
	}

	private static IPluginModelBase[] getElements(boolean includeFragments) {
		return PluginRegistry.getActiveModels(includeFragments);
	}

	public static HashMap<String, Boolean> getExistingImports(IPluginModelBase model, boolean includeImportPkg) {
		HashMap<String, Boolean> existingImports = new HashMap<>();
		addSelfAndDirectImports(existingImports, model);
		if (model instanceof IFragmentModel) {
			IFragment fragment = ((IFragmentModel) model).getFragment();
			IPluginModelBase host = PluginRegistry.findModel(fragment.getPluginId());
			if (host instanceof IPluginModel) {
				addSelfAndDirectImports(existingImports, host);
			}
		}
		if (includeImportPkg && model instanceof IBundlePluginModelBase) {
			addImportedPackages((IBundlePluginModelBase) model, existingImports);
		}
		return existingImports;
	}

	private static void addSelfAndDirectImports(HashMap<String, Boolean> existingImports, IPluginModelBase model) {
		if (model == null) {
			return;
		}
		existingImports.put(model.getPluginBase().getId(), false);
		IPluginImport[] imports = model.getPluginBase().getImports();
		for (IPluginImport pImport : imports) {
			String id = pImport.getId();
			existingImports.put(id, false);
			addReexportedImport(existingImports, id);

		}
	}

	private static void addReexportedImport(HashMap<String, Boolean> existingImports, String id) {
		IPluginModelBase model = PluginRegistry.findModel(id);
		if (model != null) {
			IPluginImport[] imports = model.getPluginBase().getImports();
			for (IPluginImport pImport : imports) {
				if (pImport.isReexported()) {
					existingImports.put(pImport.getId(), true);
					addReexportedImport(existingImports, pImport.getId());
				}
			}
		}
	}

	private static void addImportedPackages(IBundlePluginModelBase base, HashMap<String, Boolean> existingImports) {
		HashMap<String, ImportPackageObject> map = getImportPackages(base);
		if (map == null) {
			return;
		}

		ExportPackageDescription exported[] = PDECore.getDefault().getModelManager().getState().getState().getExportedPackages();
		for (int i = 0; i < exported.length; i++) {
			// iterate through all the exported packages
			ImportPackageObject ipo = map.get(exported[i].getName());
			// if we find an exported package that matches a pkg in the map, then the exported package matches a package on our import-package statement
			if (ipo != null) {
				// check version to make sure we only add bundles from valid packages
				String version = ipo.getVersion();
				if (version != null) {
					try {
						if (!new VersionRange(version).includes(exported[i].getVersion())) {
							continue;
						}
					} catch (IllegalArgumentException e) {
						// if ImportPackageObject's version is improperly
						// formatted - ignore any matching imported packages
						// since version is invalid
						continue;
					}
				}
				existingImports.put(exported[i].getSupplier().getSymbolicName(), false);
			}
		}
	}

	// returns null instead of empty map so we know not to iterate through exported packages
	private static HashMap<String, ImportPackageObject> getImportPackages(IBundlePluginModelBase base) {
		IBundleModel bmodel = base.getBundleModel();
		if (bmodel != null) {
			ImportPackageHeader header = (ImportPackageHeader) bmodel.getBundle().getManifestHeader(Constants.IMPORT_PACKAGE);
			if (header != null) {
				// create a map of all the packages we import
				HashMap<String, ImportPackageObject> map = new HashMap<>();
				ImportPackageObject[] packages = header.getPackages();
				for (ImportPackageObject importPackage : packages) {
					map.put(importPackage.getName(), importPackage);
				}
				return map;
			}
		}
		return null;
	}

	@Override
	protected Control createExtendedContentArea(Composite parent) {
		return null;
	}

	@Override
	protected ItemsFilter createFilter() {
		return new PluginSearchItemsFilter();
	}

	@Override
	protected void fillContentProvider(AbstractContentProvider contentProvider, ItemsFilter itemsFilter, IProgressMonitor progressMonitor) throws CoreException {
		for (IPluginModelBase fModel : fModels) {
			contentProvider.add(fModel, itemsFilter);
			progressMonitor.worked(1);
		}
		progressMonitor.done();
	}

	@Override
	protected IDialogSettings getDialogSettings() {
		IDialogSettings settings = PDEPlugin.getDefault().getDialogSettings().getSection(DIALOG_SETTINGS);

		if (settings == null) {
			settings = PDEPlugin.getDefault().getDialogSettings().addNewSection(DIALOG_SETTINGS);
		}

		return settings;
	}

	@Override
	public String getElementName(Object item) {
		if (item instanceof IPluginModelBase model) {
			return model.getPluginBase().getId();
		}
		return null;
	}

	@Override
	protected Comparator<?> getItemsComparator() {
		return new PluginSearchComparator();
	}

	@Override
	protected IStatus validateItem(Object item) {
		return Status.OK_STATUS;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, PDEUIMessages.ManifestEditor_addActionText, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected void updateButtonsEnableState(IStatus status) {
		super.updateButtonsEnableState(status);
		Button okButton = getOkButton();
		StructuredSelection currentSelection = super.getSelectedItems();
		HashMap<String, Boolean> existingImports = PluginSelectionDialog
				.getExistingImports(PDEPlugin.getDefault().getLabelProvider().getCurrentPluginModel(), false);
		if (!currentSelection.isEmpty())
			okButton.setEnabled(false);
		for (Object selection : currentSelection) {
			if (selection instanceof IPluginModelBase
					&& !(existingImports.keySet().contains(((IPluginModelBase) selection).getPluginBase().getId()))) {
				okButton.setEnabled(true);
				break;
			}

		}
	}
}