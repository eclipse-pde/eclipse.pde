/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.internal.ui.views.dependencies;

import java.util.*;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.osgi.framework.Constants;

public class CalleesListContentProvider extends CalleesContentProvider implements IStructuredContentProvider {

	boolean fShowOptional;

	public CalleesListContentProvider(DependenciesView view) {
		super(view);
	}

	public void setShowOptional(boolean showOptional) {
		fShowOptional = showOptional;
	}

	public boolean getShowOptional() {
		return fShowOptional;
	}

	/**
	 * @see IStructuredContentProvider#getElements(Object)
	 */
	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof IPluginModelBase) {
			Map<String, Object> elements = new LinkedHashMap<>();
			Set<Object> candidates = new LinkedHashSet<>();
			candidates.addAll(Arrays.asList(findCallees(((IPluginModelBase) inputElement))));

			while (!candidates.isEmpty()) {
				Set<Object> newCandidates = new HashSet<>();
				for (Iterator<Object> it = candidates.iterator(); it.hasNext();) {
					Object candidate = it.next();
					BundleDescription desc = null;
					it.remove();
					if (candidate instanceof BundleSpecification) {
						if (!fShowOptional && ((BundleSpecification) candidate).isOptional())
							continue;
						desc = (BundleDescription) ((BundleSpecification) candidate).getSupplier();
						// include unresolved require-bundles
						if (desc == null)
							elements.put(((BundleSpecification) candidate).getName(), candidate);
					} else if (candidate instanceof BundleDescription) {
						desc = (BundleDescription) candidate;
					} else if (candidate instanceof ImportPackageSpecification) {
						if (!fShowOptional && Constants.RESOLUTION_OPTIONAL.equals(((ImportPackageSpecification) candidate).getDirective(Constants.RESOLUTION_DIRECTIVE)))
							continue;
						desc = ((ExportPackageDescription) (((ImportPackageSpecification) candidate).getSupplier())).getExporter();
					}
					if (desc == null)
						continue;
					IPluginModelBase callee = PluginRegistry.findModel(desc.getSymbolicName());
					if (!elements.containsKey(desc.getSymbolicName())) {
						elements.put(desc.getSymbolicName(), candidate);
						if (callee != null) {
							newCandidates.addAll(Arrays.asList(findCallees(desc)));
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
