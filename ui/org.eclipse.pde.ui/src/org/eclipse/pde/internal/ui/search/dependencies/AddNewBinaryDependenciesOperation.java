/*******************************************************************************
 *  Copyright (c) 2007, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search.dependencies;

import java.util.Map;
import java.util.Set;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class AddNewBinaryDependenciesOperation extends AddNewDependenciesOperation {
	protected IClassFile[] fClassFiles;

	public AddNewBinaryDependenciesOperation(IProject project, IBundlePluginModelBase base) {
		this(project, base, PackageFinder.getClassFiles(project, base));
	}

	public AddNewBinaryDependenciesOperation(IProject project, IBundlePluginModelBase base, IClassFile[] classFiles) {
		super(project, base);
		fClassFiles = classFiles;
	}

	protected void findSecondaryDependencies(String[] secDeps, Set ignorePkgs, Map additionalDeps, IBundle bundle, boolean useRequireBundle, IProgressMonitor monitor) {
		monitor.beginTask(PDEUIMessages.AddNewDependenciesOperation_searchProject, 100);
		Set projectPkgs = PackageFinder.findPackagesInClassFiles(fClassFiles, new SubProgressMonitor(monitor, 75));
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		IProgressMonitor searchMonitor = new SubProgressMonitor(monitor, 25);
		searchMonitor.beginTask("", secDeps.length); //$NON-NLS-1$
		for (int i = 0; i < secDeps.length; i++) {
			IPluginModelBase base = manager.findModel(secDeps[i]);
			if (base != null) {
				ExportPackageDescription pkgs[] = findExportedPackages(base.getBundleDescription());
				for (int j = 0; j < pkgs.length; j++) {
					String pkgName = pkgs[j].getName();
					if (!ignorePkgs.contains(pkgName) && projectPkgs.contains(pkgName)) {
						additionalDeps.put(pkgs[j], secDeps[i]);
						ignorePkgs.add(pkgName);
						if (useRequireBundle) {
							while (j < pkgs.length) {
								ignorePkgs.add(pkgs[j].getName());
								j++;
							}
						}
					}
				}
			}
			searchMonitor.worked(1);
		}
	}

}
