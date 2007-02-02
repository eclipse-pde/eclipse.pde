/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.site;

import java.util.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

public class FilteringState extends PDEState {
	SortedSet allPlugins;

	public void setFilter(SortedSet filter) {
		allPlugins = filter;
	}

	public boolean addBundleDescription(BundleDescription toAdd) {
		if (allPlugins == null) {
			return super.addBundleDescription(toAdd);
		}

		SortedSet includedMatches = allPlugins.subSet(new ReachablePlugin(toAdd.getSymbolicName(), ReachablePlugin.WIDEST_RANGE), new ReachablePlugin(toAdd.getSymbolicName(), new VersionRange(Version.emptyVersion, true, Version.emptyVersion, true)));
		for (Iterator iterator = includedMatches.iterator(); iterator.hasNext();) {
			ReachablePlugin constraint = (ReachablePlugin) iterator.next();
			if (constraint.getRange().isIncluded(toAdd.getVersion()))
				return super.addBundleDescription(toAdd);
		}
		return false;
	}
}
