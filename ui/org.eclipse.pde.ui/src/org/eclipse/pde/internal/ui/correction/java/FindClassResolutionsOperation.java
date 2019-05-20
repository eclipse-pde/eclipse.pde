/*******************************************************************************
 * Copyright (c) 2008, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction.java;

import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.search.*;
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
 */
public class FindClassResolutionsOperation implements IRunnableWithProgress {

	String fClassName = null;
	IProject fProject = null;
	AbstractClassResolutionCollector fCollector = null;
	private CompilationUnit fCompilationUnit;

	/**
	 * This class is meant to be sub-classed for use with FindClassResolutionsOperation.  The subclass is responsible for creating
	 * corresponding proposals with the help of JavaResolutionFactory.
	 *
	 * @see JavaResolutionFactory
	 */
	public static abstract class AbstractClassResolutionCollector {

		/**
		 * This method is meant to be sub-classed. The subclass should decide if
		 * it wishes to create a proposals for either Require-Bundle and/or
		 * Import-Package. The proposals can be created with the help of the
		 * JavaResolutionFactory
		 */
		abstract public void addResolutionModification(IProject project, ExportPackageDescription desc);

		/**
		 * This method is meant to be sub-classed. The subclass should decide if
		 * it wishes to create a proposals for either Require-Bundle and/or
		 * Import-Package. The proposals can be created with the help of the
		 * JavaResolutionFactory
		 */
		abstract public void addResolutionModification(IProject project, ExportPackageDescription desc,
				CompilationUnit cu, String qualifiedTypeToImport);

		/**
		 * Adds an export package proposal. Subclasses should implement the
		 * actual adding to the collection.
		 */
		public Object addExportPackageResolutionModification(IPackageFragment aPackage) {
			if (aPackage.exists()) {
				IResource packageResource = aPackage.getResource();
				if (packageResource != null) {
					return JavaResolutionFactory.createExportPackageProposal(packageResource.getProject(), aPackage, JavaResolutionFactory.TYPE_JAVA_COMPLETION, 100);
				}
			}
			return null;
		}

		/**
		 * Adds a require bundle proposal. Subclasses should implement the actual adding to the collection.
		 */
		public Object addRequireBundleModification(IProject project, ExportPackageDescription desc, int relevance) {
			return addRequireBundleModification(project, desc, relevance, null, ""); //$NON-NLS-1$
		}

		/**
		 * Adds a require bundle proposal. Subclasses should implement the
		 * actual adding to the collection.
		 */
		public Object addRequireBundleModification(IProject project, ExportPackageDescription desc, int relevance,
				CompilationUnit cu, String qualifiedTypeToImport) {
			return JavaResolutionFactory.createRequireBundleProposal(project, desc,
					JavaResolutionFactory.TYPE_JAVA_COMPLETION, relevance, cu, qualifiedTypeToImport);
		}


		/**
		 * Adds a search repositories proposal. Subclasses should implement the actual adding to the collection.
		 */
		public Object addSearchRepositoriesModification(String packageName) {
			return JavaResolutionFactory.createSearchRepositoriesProposal(packageName);
		}

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
	public FindClassResolutionsOperation(IProject project, String className,
			AbstractClassResolutionCollector collector) {
		fProject = project;
		fClassName = className;
		fCollector = collector;
	}

	/**
	 * This class is used to try to find resolutions to unresolved java classes.
	 * When either an Import-Package or Require-Bundle might resolve a class,
	 * the ExportPackageDescription which contains the package/bundle will be
	 * passed to the AbstractClassResoltuionCollector. The collector is then
	 * responsible for creating an corresponding resolutions with the help of
	 * JavaResolutionFactory.
	 *
	 * @param project
	 *            the project which contains the unresolved class
	 * @param cu
	 *            the AST root of the java source file in which the fix was
	 *            invoked
	 * @param className
	 *            the name of the class which is unresolved
	 * @param collector
	 *            a subclass of AbstractClassResolutionCollector to
	 *            collect/handle possible resolutions
	 */
	public FindClassResolutionsOperation(IProject project, CompilationUnit cu, String className,
			AbstractClassResolutionCollector collector) {
		fProject = project;
		fCompilationUnit = cu;
		fClassName = className;
		fCollector = collector;
	}

	@Override
	public void run(IProgressMonitor monitor) {
		int idx = fClassName.lastIndexOf('.');
		String packageName = idx != -1 ? fClassName.substring(0, idx) : null;
		String typeName = fClassName.substring(idx + 1);
		if (typeName.length() == 1 && typeName.charAt(0) == '*') {
			typeName = null;
		}

		Set<IPackageFragment> packagesToExport = new HashSet<>();
		Map<String, ExportPackageDescription> validPackages = getValidPackages(typeName, fClassName, packageName,
				packagesToExport, monitor);
		if (validPackages != null) {

			if (validPackages.isEmpty()) {
				for (IPackageFragment fragment : packagesToExport) {
					fCollector.addExportPackageResolutionModification(fragment);
				}
				return;
			}

			Iterator<Entry<String, ExportPackageDescription>> validPackagesIter = validPackages.entrySet().iterator();
			Set<ExportPackageDescription> visiblePkgs = null;
			boolean allowMultipleFixes = packageName == null;
			while (validPackagesIter.hasNext() && (allowMultipleFixes || !fCollector.isDone())) {
				// since getting visible packages is not very efficient, only do it once and cache result
				if (visiblePkgs == null) {
					visiblePkgs = getVisiblePackages();
				}
				Entry<String, ExportPackageDescription> currentEntry = validPackagesIter.next();
				ExportPackageDescription currentPackage = currentEntry.getValue();
				// if package is already visible, skip over
				if (visiblePkgs.contains(currentPackage)) {
					continue;
				}
				// if currentPackage will resolve class and is valid, pass it to collector
				fCollector.addResolutionModification(fProject, currentPackage, fCompilationUnit, currentEntry.getKey());
			}

			// additionally add require bundle proposals
			Set<String> bundleNames = getCurrentBundleNames();
			for (validPackagesIter = validPackages.entrySet().iterator(); validPackagesIter.hasNext();) {
				Entry<String, ExportPackageDescription> currentEntry = validPackagesIter.next();
				ExportPackageDescription currentPackage = currentEntry.getValue();
				BundleDescription desc = currentPackage.getExporter();
				// Ignore already required bundles and duplicate proposals (currently we do not consider version constraints)
				if (desc != null && !bundleNames.contains(desc.getName())) {
					fCollector.addRequireBundleModification(fProject, currentPackage, 3, fCompilationUnit,
							currentEntry.getKey());
					bundleNames.add(desc.getName());
				}
			}
		}
	}

	private Map<String, ExportPackageDescription> getValidPackages(String typeName, String qualifiedTypeToImport,
			String packageName, Set<IPackageFragment> packagesToExport, IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 3);

		Map<String, ExportPackageDescription> validPackages = null;
		ImportPackageSpecification[] importPkgs = null;
		IPluginModelBase model = PluginRegistry.findModel(fProject);
		if (model != null && model.getBundleDescription() != null) {
			importPkgs = model.getBundleDescription().getImportPackages();
		}
		subMonitor.split(1);

		if (importPkgs != null) {
			if (packageName != null) {
				if (!isImportedPackage(packageName, importPkgs)) {
					validPackages = getValidPackages(packageName, qualifiedTypeToImport);
				}
				subMonitor.split(1);
			} else {
				// find possible types in the global packages
				validPackages = findValidPackagesContainingSimpleType(typeName, importPkgs, packagesToExport, subMonitor.split(1));
			}
		}
		return validPackages;
	}

	/**
	 * Finds all exported packages containing the simple type aTypeName. The packages
	 * will be filtered from the given packages which are already imported, and all
	 * system packages.
	 *
	 * If no exported package is left, packagesToExport will be filled with those
	 * packages that would have been returned, if they were exported.
	 * @param aTypeName the simple type to search for
	 * @param importPkgs the packages which are already imported
	 * @param packagesToExport return parameter that will be filled with packages to export
	 * 		 if no valid package to import was found
	 * @param monitor
	 * @return the set of packages to import
	 */
	private Map<String, ExportPackageDescription> findValidPackagesContainingSimpleType(String aTypeName,
			ImportPackageSpecification[] importPkgs, Set<IPackageFragment> packagesToExport, IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor);

		IPluginModelBase[] activeModels = PluginRegistry.getActiveModels();
		Set<IJavaProject> javaProjects = new LinkedHashSet<>(activeModels.length * 2);

		for (IPluginModelBase model : activeModels) {
			IResource resource = model.getUnderlyingResource();
			if (resource != null && resource.isAccessible()) {
				IJavaProject javaProject = JavaCore.create(resource.getProject());
				if (javaProject.exists()) {
					javaProjects.add(javaProject);
				}
			}
		}
		final IJavaProject currentJavaProject = JavaCore.create(fProject);
		javaProjects.remove(currentJavaProject); // no need to search in current project itself

		try {
			IJavaSearchScope searchScope = SearchEngine.createJavaSearchScope(javaProjects.toArray(new IJavaElement[javaProjects.size()]));

			final Map<String, IPackageFragment> packages = new HashMap<>();
			final Map<String, String> qualifiedTypeNames = new HashMap<>();
			SearchRequestor requestor = new SearchRequestor() {

				@Override
				public void acceptSearchMatch(SearchMatch aMatch) throws CoreException {
					Object element = aMatch.getElement();
					if (element instanceof IType) {
						IType type = (IType) element;
						// Only try to import types we can access (Bug 406232)
						if (Flags.isPublic(type.getFlags())) {
							if (!currentJavaProject.equals(type.getJavaProject())) {
								IPackageFragment packageFragment = type.getPackageFragment();
								if (packageFragment.exists()) {
									packages.put(packageFragment.getElementName(), packageFragment);
									qualifiedTypeNames.put(packageFragment.getElementName(),
											type.getFullyQualifiedName());
								}
							}
						}
					}
				}
			};

			SearchPattern typePattern = SearchPattern.createPattern(aTypeName, IJavaSearchConstants.TYPE, IJavaSearchConstants.DECLARATIONS, SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE);
			new SearchEngine().search(typePattern, new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()}, searchScope, requestor, subMonitor.split(1));

			if (!packages.isEmpty()) {
				// transform to ExportPackageDescriptions
				Map<String, ExportPackageDescription> exportDescriptions = new HashMap<>(packages.size());

				// remove system packages if they happen to be included. Adding a system package won't resolve anything, since package package already comes from JRE
				ExportPackageDescription[] systemPackages = PDECore.getDefault().getModelManager().getState().getState().getSystemPackages();
				for (ExportPackageDescription systemPackage : systemPackages) {
					packages.remove(systemPackage.getName());
				}
				// also remove packages that are already imported
				for (ImportPackageSpecification importPackage : importPkgs) {
					packages.remove(importPackage.getName());
				}

				// finally create the list of ExportPackageDescriptions
				ExportPackageDescription[] knownPackages = PDECore.getDefault().getModelManager().getState().getState().getExportedPackages();
				for (ExportPackageDescription knownPackage : knownPackages) {
					if (packages.containsKey(knownPackage.getName())) {
						exportDescriptions.put(qualifiedTypeNames.get(knownPackage.getName()), knownPackage);
					}
				}
				if (exportDescriptions.isEmpty()) {
					// no packages to import found, maybe there are packages to export
					packagesToExport.addAll(packages.values());
				}

				return exportDescriptions;
			}

			return Collections.emptyMap();
		} catch (CoreException ex) {
			// ignore, return an empty set
			return Collections.emptyMap();
		}
	}

	private boolean isImportedPackage(String packageName, ImportPackageSpecification[] importPkgs) {
		for (ImportPackageSpecification importPackage : importPkgs) {
			if (importPackage.getName().equals(packageName)) {
				return true;
			}
		}
		return false;
	}

	private static Map<String, ExportPackageDescription> getValidPackages(String pkgName, String qualifiedTypeToImport) {
		ExportPackageDescription[] knownPackages = PDECore.getDefault().getModelManager().getState().getState().getExportedPackages();
		Map<String, ExportPackageDescription> validPackages = new HashMap<>();
		for (ExportPackageDescription knownPackage : knownPackages) {
			if (knownPackage.getName().equals(pkgName)) {
				validPackages.put(knownPackage.getName(), knownPackage);
			}
		}
		// remove system packages if they happen to be included. Adding a system package won't resolve anything, since package package already comes from JRE
		if (!validPackages.isEmpty()) {
			knownPackages = PDECore.getDefault().getModelManager().getState().getState().getSystemPackages();
			for (ExportPackageDescription knownPackage : knownPackages) {
				validPackages.remove(knownPackage.getName());
			}
		}
		Map<String, ExportPackageDescription> packages = new HashMap<>();
		for (ExportPackageDescription exportPackageDescription : validPackages.values()) {
			packages.put(qualifiedTypeToImport, exportPackageDescription);
		}
		return packages;
	}

	private Set<ExportPackageDescription> getVisiblePackages() {
		IPluginModelBase base = PluginRegistry.findModel(fProject);
		if (base != null) {
			BundleDescription desc = base.getBundleDescription();

			StateHelper helper = Platform.getPlatformAdmin().getStateHelper();
			ExportPackageDescription[] visiblePkgs = helper.getVisiblePackages(desc);

			HashSet<ExportPackageDescription> set = new HashSet<>();
			for (ExportPackageDescription visiblePackage : visiblePkgs) {
				set.add(visiblePackage);
			}
			return set;
		}
		return Collections.emptySet();
	}

	/**
	 * Returns the set of String bundle names that are in the project's list of required
	 * bundles.
	 *
	 * @return set of required bundle names, possibly empty
	 */
	private Set<String> getCurrentBundleNames() {
		IPluginModelBase base = PluginRegistry.findModel(fProject);
		if (base != null) {
			Set<String> bundleNames = new HashSet<>();
			BundleSpecification[] reqBundles = base.getBundleDescription().getRequiredBundles();
			for (BundleSpecification reqBundle : reqBundles) {
				bundleNames.add(reqBundle.getName());
			}
			return bundleNames;
		}
		return Collections.emptySet();

	}

}
