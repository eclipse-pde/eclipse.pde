/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards;

import java.util.HashSet;

import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ModelEntry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

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
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		return includeFragments ? manager.getPlugins() : manager.getPluginsOnly();
	}
	
	public static HashSet getExistingImports(IPluginBase model) {
		HashSet existingImports = new HashSet();
		if (!PDECore.getDefault().getModelManager().isOSGiRuntime()) {
			existingImports.add("org.eclipse.core.boot"); //$NON-NLS-1$
			existingImports.add("org.eclipse.core.runtime"); //$NON-NLS-1$
		}
		addSelfAndDirectImports(existingImports, model);
		if (model instanceof IFragment) {
			IPlugin parent = getParentPlugin((IFragment) model);
			if (parent != null) {
				addSelfAndDirectImports(existingImports, parent);
			}
		}
		return existingImports;
	}
	
	private static IPlugin getParentPlugin(IFragment fragment) {
		String targetId = fragment.getPluginId();
		String targetVersion = fragment.getPluginVersion();
		int match = fragment.getRule();
		return PDECore.getDefault().findPlugin(targetId, targetVersion, match);
	}

	private static void addSelfAndDirectImports(HashSet set, IPluginBase pluginBase) {
		set.add(pluginBase.getId());
		IPluginImport[] imports = pluginBase.getImports();
		for (int i = 0; i < imports.length; i++) {
			String id = imports[i].getId();
			if (set.add(id)) {
				addReexportedImport(set, id);
			}
		}
	}
	
	private static void addReexportedImport(HashSet set, String id) {
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		ModelEntry entry = manager.findEntry(id);
		if (entry != null) {
			IPluginModelBase model = entry.getActiveModel();
			IPluginImport[] imports = model.getPluginBase().getImports();
			for (int i = 0; i < imports.length; i++) {
				if (imports[i].isReexported() && set.add(imports[i].getId())) {
					addReexportedImport(set, imports[i].getId());
				}
			}
		}
	}
}
