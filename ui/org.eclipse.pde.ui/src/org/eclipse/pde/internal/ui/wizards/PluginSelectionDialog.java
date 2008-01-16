/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards;

import java.util.HashMap;
import java.util.HashSet;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageHeader;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageObject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.osgi.framework.Constants;

public class PluginSelectionDialog extends ElementListSelectionDialog {

	public PluginSelectionDialog(Shell parentShell, boolean includeFragments, boolean multipleSelection) {
		this(parentShell, getElements(includeFragments), multipleSelection);
	}

	public PluginSelectionDialog(Shell parentShell, IPluginModelBase[] models, boolean multipleSelection) {
		super(parentShell, PDEPlugin.getDefault().getLabelProvider());
		setTitle(PDEUIMessages.PluginSelectionDialog_title);
		setMessage(PDEUIMessages.PluginSelectionDialog_message);
		setElements(models);
		setMultipleSelection(multipleSelection);
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	public boolean close() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		return super.close();
	}

	private static IPluginModelBase[] getElements(boolean includeFragments) {
		return PluginRegistry.getActiveModels(includeFragments);
	}

	public static HashSet getExistingImports(IPluginModelBase model, boolean includeImportPkg) {
		HashSet existingImports = new HashSet();
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

	private static void addSelfAndDirectImports(HashSet set, IPluginModelBase model) {
		set.add(model.getPluginBase().getId());
		IPluginImport[] imports = model.getPluginBase().getImports();
		for (int i = 0; i < imports.length; i++) {
			String id = imports[i].getId();
			if (set.add(id)) {
				addReexportedImport(set, id);
			}
		}
	}

	private static void addReexportedImport(HashSet set, String id) {
		IPluginModelBase model = PluginRegistry.findModel(id);
		if (model != null) {
			IPluginImport[] imports = model.getPluginBase().getImports();
			for (int i = 0; i < imports.length; i++) {
				if (imports[i].isReexported() && set.add(imports[i].getId())) {
					addReexportedImport(set, imports[i].getId());
				}
			}
		}
	}

	private static void addImportedPackages(IBundlePluginModelBase base, HashSet existingImports) {
		HashMap map = getImportPackages(base);
		if (map == null)
			return;

		ExportPackageDescription exported[] = PDECore.getDefault().getModelManager().getState().getState().getExportedPackages();
		for (int i = 0; i < exported.length; i++) {
			// iterate through all the exported packages
			ImportPackageObject ipo = (ImportPackageObject) map.get(exported[i].getName());
			// if we find an exported package that matches a pkg in the map, then the exported package matches a package on our import-package statement
			if (ipo != null) {
				// check version to make sure we only add bundles from valid packages
				String version = ipo.getVersion();
				if (version != null)
					try {
						if (!new VersionRange(version).isIncluded(exported[i].getVersion()))
							continue;
						// NFE if ImportPackageObject's version is improperly formatted - ignore any matching imported packages since version is invalid
					} catch (NumberFormatException e) {
						continue;
					}
				existingImports.add(exported[i].getSupplier().getSymbolicName());
			}
		}
	}

	// returns null instead of empty map so we know not to iterate through exported packages
	private static HashMap getImportPackages(IBundlePluginModelBase base) {
		IBundleModel bmodel = base.getBundleModel();
		if (bmodel != null) {
			ImportPackageHeader header = (ImportPackageHeader) bmodel.getBundle().getManifestHeader(Constants.IMPORT_PACKAGE);
			if (header != null) {
				// create a map of all the packages we import
				HashMap map = new HashMap();
				ImportPackageObject[] packages = header.getPackages();
				for (int i = 0; i < packages.length; i++)
					map.put(packages[i].getName(), packages[i]);
				return map;
			}
		}
		return null;
	}
}
