/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;


public class PDEStateHelper {

	/**
	 * Returns the bundles that export packages being imported via the Import-Package header
	 * by resolved fragments of a given bundle
	 * 
	 * @param root the bundle
	 * @return
	 * 			an array of bundles supplying packages to resolved fragments of the given bundle
	 */
	public static BundleDescription[] getImportedByFragments(BundleDescription root) {
		BundleDescription[] fragments = root.getFragments();
		List list = new ArrayList();
		for (int i = 0; i < fragments.length; i++) {
			if (fragments[i].isResolved()) {
				BundleDescription[] toAdd = getImportedBundles(fragments[i]);
				for (int j = 0; j < toAdd.length; j++) {
					if (!list.contains(toAdd[j]))
						list.add(toAdd[j]);
				}
			}
		}
		BundleDescription[] result = new BundleDescription[list.size()];
		return (BundleDescription[]) list.toArray(result);
	}
	
	/**
	 * Returns the bundles that export packages imported by the given bundle
	 * via the Import-Package header
	 * 
	 * @param root the given bundle
	 * 
	 * @return an array of bundles that export packages being imported by the given bundle
	 */
	public static BundleDescription[] getImportedBundles(BundleDescription root) {
		if (root == null)
			return new BundleDescription[0];
		ExportPackageDescription[] packages = root.getResolvedImports();
		ArrayList resolvedImports = new ArrayList(packages.length);
		for (int i = 0; i < packages.length; i++)
			if (!root.getLocation().equals(packages[i].getExporter().getLocation())
					&& !resolvedImports.contains(packages[i].getExporter()))
				resolvedImports.add(packages[i].getExporter());
		return (BundleDescription[]) resolvedImports.toArray(new BundleDescription[resolvedImports.size()]);
	}

	public static IPluginExtensionPoint findExtensionPoint(String fullID) {
		if (fullID == null || fullID.length() == 0)
			return null;
		// separate plugin ID first
		int lastDot = fullID.lastIndexOf('.');
		if (lastDot == -1)
			return null;
		String pluginID = fullID.substring(0, lastDot);
		IPluginModelBase model = PluginRegistry.findModel(pluginID);
		if (model == null)
			return PDEManager.findExtensionPoint(fullID);
		String pointID = fullID.substring(lastDot + 1);
		IPluginExtensionPoint[] points = model.getPluginBase().getExtensionPoints();
		for (int i = 0; i < points.length; i++) {
			IPluginExtensionPoint point = points[i];
			if (point.getId().equals(pointID))
				return point;
		}
		IFragmentModel[] fragments = PDEManager.findFragmentsFor(model);
		for (int i = 0; i < fragments.length; i++) {
			points = fragments[i].getPluginBase().getExtensionPoints();
			for (int j = 0; j < points.length; j++)
				if (points[j].getId().equals(pointID))
					return points[j];
		}
		return PDEManager.findExtensionPoint(fullID);
	}
	
}
