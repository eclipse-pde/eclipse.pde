/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction.java;

import java.util.*;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.PDECore;

/**
 * This Operation is used to find possible resolutions to an unresolved class reference in a plug-in project.
 * When it is run, it will pass any ExportPackageDescriptions which provide the package to the AbstractClassResolutionCollector.
 * The AbstractClassResolutionCollector is responsible for creating the appropriate resolutions.
 *
 * @since 3.4
 */
public class FindClassResolutionsOperation implements IRunnableWithProgress {

	String fClassName = null;
	IProject fProject = null;
	AbstractClassResolutionCollector fCollector = null;

	/**
	 * This class is meant to be sub-classed for use with FindClassResolutionsOperation.  The subclass is responsible for creating 
	 * corresponding proposals with the help of JavaResolutionFactory.
	 * 
	 * @since 3.4
	 * @see JavaResolutionFactory
	 */
	public static abstract class AbstractClassResolutionCollector {

		/**
		 * This method is meant to be sub-classed.  The subclass should decide if it wishes to create a proposals for either
		 * Require-Bundle and/or Import-Package.  The proposals can be created with the help of the JavaResolutionFactory
		 */
		abstract public void addResolutionModification(IProject project, ExportPackageDescription desc);

		/*
		 * Optimization for case where users is only interested in Import-Package and therefore can quit after first dependency is found
		 */
		public boolean isDone() {
			return false;
		}

	}

	/**
	 * This class is used to try to find resolutions to unresolved java classes.  When either an Import-Package or Require-Bundle might 
	 * resolve a class, the ExportPackageDescription which contains the package/bundle will be passed to the AbstractClassResoltuionCollector.
	 * The collector is then responsible for creating an corresponding resolutions with the help of JavaResolutionFactory.
	 * @param project the project which contains the unresolved class
	 * @param className	the name of the class which is unresolved
	 * @param collector a subclass of AbstractClassResolutionCollector to collect/handle possible resolutions 
	 */
	public FindClassResolutionsOperation(IProject project, String className, AbstractClassResolutionCollector collector) {
		fProject = project;
		fClassName = className;
		fCollector = collector;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) {
		int idx = fClassName.lastIndexOf('.');
		String packageName = idx != -1 ? fClassName.substring(0, idx) : null;
		String typeName = fClassName.substring(idx + 1);
		if (typeName.length() == 1 && typeName.charAt(0) == '*') {
			typeName = null;
		}

		if (packageName != null && !isImportedPackage(packageName)) {
			Set validPackages = getValidPackages(packageName);
			Iterator validPackagesIter = validPackages.iterator();
			Set visiblePkgs = null;

			while (validPackagesIter.hasNext() && !fCollector.isDone()) {
				// since getting visible packages is not very efficient, only do it once and cache result
				if (visiblePkgs == null) {
					visiblePkgs = getVisiblePackages();
				}
				ExportPackageDescription currentPackage = (ExportPackageDescription) validPackagesIter.next();
				// if package is already visible, skip over
				if (visiblePkgs.contains(currentPackage)) {
					continue;
				}
				// if currentPackage will resolve class and is valid, pass it to collector
				fCollector.addResolutionModification(fProject, currentPackage);
			}
		}
	}

	private boolean isImportedPackage(String packageName) {
		IPluginModelBase model = PluginRegistry.findModel(fProject.getProject());
		if (model != null && model.getBundleDescription() != null) {
			ImportPackageSpecification[] importPkgs = model.getBundleDescription().getImportPackages();
			for (int i = 0; i < importPkgs.length; i++) {
				if (importPkgs[i].getName().equals(packageName)) {
					return true;
				}
			}
			return false;
		}
		// if no BundleDescription, we return true so we don't create any proposals.  This is the safe way out if no BundleDescription is available.
		return true;
	}

	private static Set getValidPackages(String pkgName) {
		ExportPackageDescription[] knownPackages = PDECore.getDefault().getModelManager().getState().getState().getExportedPackages();
		Set validPackages = new HashSet();
		for (int i = 0; i < knownPackages.length; i++) {
			if (knownPackages[i].getName().equals(pkgName)) {
				validPackages.add(knownPackages[i]);
			}
		}
		// remove system packages if they happen to be included. Adding a system package won't resolve anything, since package package already comes from JRE
		if (!validPackages.isEmpty()) {
			knownPackages = PDECore.getDefault().getModelManager().getState().getState().getSystemPackages();
			for (int i = 0; i < knownPackages.length; i++) {
				validPackages.remove(knownPackages[i]);
			}
		}
		return validPackages;
	}

	private Set getVisiblePackages() {
		IPluginModelBase base = PluginRegistry.findModel(fProject);
		BundleDescription desc = base.getBundleDescription();

		StateHelper helper = Platform.getPlatformAdmin().getStateHelper();
		ExportPackageDescription[] visiblePkgs = helper.getVisiblePackages(desc);

		HashSet set = new HashSet();
		for (int i = 0; i < visiblePkgs.length; i++) {
			set.add(visiblePkgs[i]);
		}
		return set;
	}

}
