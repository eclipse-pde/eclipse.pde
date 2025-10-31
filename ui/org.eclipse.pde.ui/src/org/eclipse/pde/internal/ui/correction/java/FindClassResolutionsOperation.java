/*******************************************************************************
 * Copyright (c) 2008, 2025 IBM Corporation and others.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.StateHelper;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.build.BundleHelper;
import org.eclipse.pde.internal.core.PDECore;

/**
 * This Operation is used to find possible resolutions to an unresolved class reference in a plug-in project.
 * When it is run, it will pass any ExportPackageDescriptions which provide the package to the AbstractClassResolutionCollector.
 * The AbstractClassResolutionCollector is responsible for creating the appropriate resolutions.
 */
public class FindClassResolutionsOperation implements IRunnableWithProgress {

	private String fClassName = null;
	private IProject fProject = null;
	private AbstractClassResolutionCollector fCollector = null;
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
		public void addResolutionModification(IProject project, ExportPackageDescription desc) {
			addResolutionModification(project, desc, null, ""); //$NON-NLS-1$
		}

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
		public IJavaCompletionProposal addExportPackageResolutionModification(IPackageFragment aPackage) {
			if (aPackage.exists()) {
				IResource packageResource = aPackage.getResource();
				if (packageResource != null) {
					IProject project = packageResource.getProject();
					var change = JavaResolutionFactory.createExportPackageChange(project, aPackage);
					return JavaResolutionFactory.createJavaCompletionProposal(change, 100);
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
		public IJavaCompletionProposal addRequireBundleModification(IProject project, ExportPackageDescription desc,
				int relevance, CompilationUnit cu, String qualifiedTypeToImport) {
			BundleDescription exporter = desc.getExporter();
			if (exporter == null) {
				return null;
			}
			var change = JavaResolutionFactory.createRequireBundleChange(project, exporter, cu, qualifiedTypeToImport);
			return JavaResolutionFactory.createJavaCompletionProposal(change, relevance);
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
			// getting visible packages is not very efficient -> do it only once
			Set<ExportPackageDescription> visiblePackages = !validPackages.isEmpty() ? getVisiblePackages() : null;
			boolean allowMultipleFixes = packageName == null;
			for (var validPackagesIter = validPackages.entrySet().iterator(); validPackagesIter.hasNext()
					&& (allowMultipleFixes || !fCollector.isDone());) {
				var entry = validPackagesIter.next();
				String qualifiedType = entry.getKey();
				ExportPackageDescription currentPackage = entry.getValue();
				// if package is already visible, skip over
				if (visiblePackages.contains(currentPackage)) {
					continue;
				}
				// if currentPackage will resolve class and is valid, pass it to collector
				fCollector.addResolutionModification(fProject, currentPackage, fCompilationUnit, qualifiedType);
			}
			// additionally add require bundle proposals
			Set<String> bundleNames = getCurrentBundleNames();
			validPackages.forEach((key, currentPackage) -> {
				BundleDescription desc = currentPackage.getExporter();
				// Ignore already required bundles and duplicate proposals (currently we do not consider version constraints)
				if (desc != null && !bundleNames.contains(desc.getName())) {
					fCollector.addRequireBundleModification(fProject, currentPackage, 3, fCompilationUnit, key);
					bundleNames.add(desc.getName());
				}
			});
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
					List<ExportPackageDescription> packages = getValidPackages(packageName);
					validPackages = !packages.isEmpty() ? Map.of(qualifiedTypeToImport, packages.getLast()) : Map.of();
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
	 * @return the set of packages to import
	 */
	private Map<String, ExportPackageDescription> findValidPackagesContainingSimpleType(String aTypeName,
			ImportPackageSpecification[] importPkgs, Set<IPackageFragment> packagesToExport, IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor);

		Set<IJavaProject> javaProjects = Arrays.stream(PluginRegistry.getWorkspaceModels())
				.map(IPluginModelBase::getUnderlyingResource).map(IResource::getProject).map(JavaCore::create)
				.filter(IJavaProject::exists).collect(Collectors.toCollection(LinkedHashSet::new));

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
					if (element instanceof IType type) {
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
			if (typePattern == null) {
				return Collections.emptyMap();
			}
			new SearchEngine().search(typePattern, new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()}, searchScope, requestor, subMonitor.split(1));

			if (!packages.isEmpty()) {
				// transform to ExportPackageDescriptions
				State state = PDECore.getDefault().getModelManager().getState().getState();

				// remove system packages if they happen to be included. Adding a system package won't resolve anything, since package package already comes from JRE
				ExportPackageDescription[] systemPackages = state.getSystemPackages();
				for (ExportPackageDescription systemPackage : systemPackages) {
					packages.remove(systemPackage.getName());
				}
				// also remove packages that are already imported
				for (ImportPackageSpecification importPackage : importPkgs) {
					packages.remove(importPackage.getName());
				}

				// finally create the list of ExportPackageDescriptions
				Map<String, ExportPackageDescription> exportDescriptions = new HashMap<>(packages.size());
				ExportPackageDescription[] knownPackages = state.getExportedPackages();
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

	private static List<ExportPackageDescription> getValidPackages(String packageName) {
		State state = PDECore.getDefault().getModelManager().getState().getState();
		List<ExportPackageDescription> validPackages = Arrays.stream(state.getExportedPackages())
				.filter(p -> packageName.equals(p.getName())).toList();
		// remove system packages if they happen to be included. Adding a system package won't resolve anything, since package package already comes from JRE
		if (!validPackages.isEmpty()) {
			ExportPackageDescription[] systemPackages = state.getSystemPackages();
			if (Arrays.stream(systemPackages).map(ExportPackageDescription::getName).anyMatch(packageName::equals)) {
				return List.of();
			}
		}
		return validPackages;
	}

	private Set<ExportPackageDescription> getVisiblePackages() {
		IPluginModelBase base = PluginRegistry.findModel(fProject);
		if (base != null) {
			BundleDescription desc = base.getBundleDescription();
			StateHelper helper = BundleHelper.getPlatformAdmin().getStateHelper();
			return Set.of(helper.getVisiblePackages(desc));
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
