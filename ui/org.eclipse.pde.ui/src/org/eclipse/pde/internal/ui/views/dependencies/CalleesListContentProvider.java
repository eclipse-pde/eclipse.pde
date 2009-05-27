/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
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
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof IPluginModelBase) {
			Map elements = new HashMap();
			Set candidates = new HashSet();
			candidates.addAll(Arrays.asList(findCallees(((IPluginModelBase) inputElement))));

			while (!candidates.isEmpty()) {
				Set newCandidates = new HashSet();
				for (Iterator it = candidates.iterator(); it.hasNext();) {
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
