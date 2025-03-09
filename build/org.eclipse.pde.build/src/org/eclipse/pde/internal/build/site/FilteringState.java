/*******************************************************************************
 *  Copyright (c) 2007, 2021 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.site;

import java.util.SortedSet;

import org.eclipse.osgi.service.resolver.BundleDescription;

public class FilteringState extends PDEState {
	SortedSet<ReachablePlugin> allPlugins;

	public void setFilter(SortedSet<ReachablePlugin> filter) {
		allPlugins = filter;
	}

	@Override
	public boolean addBundleDescription(BundleDescription toAdd) {
		if (allPlugins == null) {
			return super.addBundleDescription(toAdd);
		}

		SortedSet<ReachablePlugin> includedMatches = allPlugins.subSet(new ReachablePlugin(toAdd.getSymbolicName(), ReachablePlugin.WIDEST_RANGE), new ReachablePlugin(toAdd.getSymbolicName(), ReachablePlugin.NARROWEST_RANGE));
		for (ReachablePlugin constraint : includedMatches) {
			if (constraint.getRange().includes(toAdd.getVersion())) {
				return super.addBundleDescription(toAdd);
			}
		}
		return false;
	}
}
