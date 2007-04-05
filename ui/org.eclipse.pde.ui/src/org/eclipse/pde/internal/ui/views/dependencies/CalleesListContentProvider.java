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
package org.eclipse.pde.internal.ui.views.dependencies;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;

public class CalleesListContentProvider extends CalleesContentProvider
		implements IStructuredContentProvider {

	public CalleesListContentProvider(DependenciesView view) {
		super(view);
	}

	/**
	 * @see IStructuredContentProvider#getElements(Object)
	 */
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof IPluginModelBase) {
			Map elements = new HashMap();
			Set candidates = new HashSet();
			candidates.addAll(Arrays.asList(findCallees(((IPluginModelBase)inputElement))));
			
			while (!candidates.isEmpty()) {
				Set newCandidates = new HashSet();
				for (Iterator it = candidates.iterator(); it.hasNext();) {
					Object candidate = it.next();
					BundleDescription desc = null;
					if(candidate instanceof BundleSpecification){
						desc = (BundleDescription)((BundleSpecification)candidate).getSupplier();
						// include unresolved require-bundles
						if (desc == null)
							elements.put(((BundleSpecification)candidate).getName(), candidate);
					} else if (candidate instanceof BundleDescription) {
						desc = (BundleDescription)candidate;
					} else if (candidate instanceof ImportPackageSpecification) {
						desc = ((ExportPackageDescription)(((ImportPackageSpecification)candidate).getSupplier())).getExporter();
					}
					it.remove();
					if (desc == null)
						continue;
					IPluginModelBase callee = PluginRegistry.findModel(desc.getSymbolicName());
					if (!elements.containsKey(desc.getSymbolicName())) {
						elements.put(desc.getSymbolicName(), candidate);
						if (callee != null) {
							newCandidates.addAll(Arrays
									.asList(findCallees(desc)));
						}
					}
				}
				candidates = newCandidates;

			}
			return elements.values().toArray();
		}
		return new Object[0];
	}
}
