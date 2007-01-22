/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.views.dependencies;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;

public class CallersContentProvider extends DependenciesViewPageContentProvider {
	public CallersContentProvider(DependenciesView view) {
		super(view);
	}

	/**
	 * @param id
	 * @return Set of IPluginBase
	 */
	protected Set findReferences(String id) {
		IPluginModelBase[] models = PluginRegistry.getAllModels();
		Set l = new HashSet(models.length);
		for (int i = 0; i < models.length; i++) {
			IPluginModelBase candidate = models[i];
			IPluginBase candidateBase = candidate.getPluginBase(false);
			if (candidateBase == null) {
				continue;
			}
			// refs by require
			IPluginImport[] imports = candidateBase.getImports();
			for (int m = 0; m < imports.length; m++) {
				String candidateId = imports[m].getId();
				if (id.equals(candidateId)) {
					l.add(candidateBase);
				}
			}
			// ref of plugin by fragment
			if (candidateBase instanceof IFragment) {
				String candidateId = ((IFragment) candidateBase).getPluginId();
				if (id.equals(candidateId)) {
					l.add(candidateBase);
				}
			}
		}
		return l;
	}

}
