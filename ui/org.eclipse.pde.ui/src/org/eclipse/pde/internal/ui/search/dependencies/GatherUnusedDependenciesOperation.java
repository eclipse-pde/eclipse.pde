/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search.dependencies;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.search.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.ClasspathUtilCore;
import org.eclipse.pde.internal.core.ibundle.*;
import org.eclipse.pde.internal.core.search.PluginJavaSearchUtil;
import org.eclipse.pde.internal.core.text.bundle.*;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;

public class GatherUnusedDependenciesOperation implements IRunnableWithProgress {

	class Requestor extends SearchRequestor {
		boolean fFound = false;

		public void acceptSearchMatch(SearchMatch match) throws CoreException {
			fFound = true;
		}

		public boolean foundMatches() {
			return fFound;
		}
	}

	private IPluginModelBase fModel;
	private ArrayList fList;

	public GatherUnusedDependenciesOperation(IPluginModelBase model) {
		fModel = model;
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

		ImportPackageObject[] packages = null;
		Collection exportedPackages = null;
		if (ClasspathUtilCore.hasBundleStructure(fModel)) {
			IBundle bundle = ((IBundlePluginModelBase) fModel).getBundleModel().getBundle();
			IManifestHeader header = bundle.getManifestHeader(Constants.IMPORT_PACKAGE);
			if (header instanceof ImportPackageHeader) {
				packages = ((ImportPackageHeader) header).getPackages();
			} else if (header != null && header.getValue() != null) {
				header = new ImportPackageHeader(Constants.IMPORT_PACKAGE, header.getValue(), bundle, System.getProperty("line.separator")); //$NON-NLS-1$
				packages = ((ImportPackageHeader) header).getPackages();
			}

			header = bundle.getManifestHeader(Constants.EXPORT_PACKAGE);
			if (header instanceof ExportPackageHeader) {
				exportedPackages = ((ExportPackageHeader) header).getPackageNames();
			} else if (header != null && header.getValue() != null) {
				header = new ExportPackageHeader(Constants.EXPORT_PACKAGE, header.getValue(), bundle, System.getProperty("line.seperator")); //$NON-NLS-1$
				exportedPackages = ((ExportPackageHeader) header).getPackageNames();
			}
		}
		IPluginImport[] imports = fModel.getPluginBase().getImports();

		int totalWork = imports.length * 3 + (packages != null ? packages.length : 0) + 1;
		monitor.beginTask("", totalWork); //$NON-NLS-1$

		HashMap usedPlugins = new HashMap();
		fList = new ArrayList();
		for (int i = 0; i < imports.length; i++) {
			if (monitor.isCanceled())
				break;
			if (isUnused(imports[i], new SubProgressMonitor(monitor, 3))) {
				fList.add(imports[i]);
			} else
				usedPlugins.put(imports[i].getId(), imports[i]);
			updateMonitor(monitor, fList.size());
		}

		ArrayList usedPackages = new ArrayList();
		if (packages != null && !monitor.isCanceled()) {
			for (int i = 0; i < packages.length; i++) {
				if (monitor.isCanceled())
					break;
				if (isUnused(packages[i], exportedPackages, new SubProgressMonitor(monitor, 1))) {
					fList.add(packages[i]);
					updateMonitor(monitor, fList.size());
				} else
					usedPackages.add(packages[i]);
			}
		}
		if (!monitor.isCanceled())
			minimizeDependencies(usedPlugins, usedPackages, monitor);
		monitor.done();
	}

	private void updateMonitor(IProgressMonitor monitor, int size) {
		monitor.setTaskName(PDEUIMessages.UnusedDependencies_analyze + size + " " //$NON-NLS-1$
				+ PDEUIMessages.UnusedDependencies_unused + " " //$NON-NLS-1$
				+ (size == 1 ? PDEUIMessages.DependencyExtent_singular : PDEUIMessages.DependencyExtent_plural) + " " //$NON-NLS-1$
				+ PDEUIMessages.DependencyExtent_found);
	}

	private boolean isUnused(IPluginImport plugin, SubProgressMonitor monitor) {
		IPluginModelBase[] models = PluginJavaSearchUtil.getPluginImports(plugin);
		return !provideJavaClasses(models, monitor);
	}

	private boolean isUnused(ImportPackageObject pkg, Collection exportedPackages, SubProgressMonitor monitor) {
		if (exportedPackages != null && exportedPackages.contains(pkg.getValue())) {
			monitor.done();
			return false;
		}
		return !provideJavaClasses(pkg, monitor);
	}

	private boolean provideJavaClasses(IPluginModelBase[] models, IProgressMonitor monitor) {
		try {
			IProject project = fModel.getUnderlyingResource().getProject();
			if (!project.hasNature(JavaCore.NATURE_ID))
				return false;

			IJavaProject jProject = JavaCore.create(project);
			IPackageFragment[] packageFragments = PluginJavaSearchUtil.collectPackageFragments(models, jProject, true);
			SearchEngine engine = new SearchEngine();
			IJavaSearchScope searchScope = PluginJavaSearchUtil.createSeachScope(jProject);

			monitor.beginTask("", packageFragments.length * 2); //$NON-NLS-1$
			for (int i = 0; i < packageFragments.length; i++) {
				IPackageFragment pkgFragment = packageFragments[i];
				if (pkgFragment.hasChildren()) {
					Requestor requestor = new Requestor();
					engine.search(SearchPattern.createPattern(pkgFragment, IJavaSearchConstants.REFERENCES), new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()}, searchScope, requestor, new SubProgressMonitor(monitor, 1));
					if (requestor.foundMatches()) {
						if (provideJavaClasses(packageFragments[i], engine, searchScope, new SubProgressMonitor(monitor, 1))) {
							return true;
						}
					} else
						monitor.worked(1);
				} else {
					monitor.worked(2);
				}
			}
		} catch (CoreException e) {
		} finally {
			monitor.done();
		}
		return false;
	}

	private boolean provideJavaClasses(IPackageFragment packageFragment, SearchEngine engine, IJavaSearchScope searchScope, IProgressMonitor monitor) throws JavaModelException, CoreException {
		Requestor requestor;
		IJavaElement[] children = packageFragment.getChildren();
		monitor.beginTask("", children.length); //$NON-NLS-1$

		try {
			for (int j = 0; j < children.length; j++) {
				IType[] types = null;
				if (children[j] instanceof ICompilationUnit) {
					types = ((ICompilationUnit) children[j]).getAllTypes();
				} else if (children[j] instanceof IClassFile) {
					types = new IType[] {((IClassFile) children[j]).getType()};
				}
				if (types != null) {
					for (int t = 0; t < types.length; t++) {
						requestor = new Requestor();
						engine.search(SearchPattern.createPattern(types[t], IJavaSearchConstants.REFERENCES), new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()}, searchScope, requestor, new SubProgressMonitor(monitor, 1));
						if (requestor.foundMatches()) {
							return true;
						}
					}
				}
			}
		} finally {
			monitor.done();
		}
		return false;
	}

	private boolean provideJavaClasses(ImportPackageObject pkg, IProgressMonitor monitor) {
		try {
			IProject project = fModel.getUnderlyingResource().getProject();

			if (!project.hasNature(JavaCore.NATURE_ID))
				return false;

			monitor.beginTask("", 1); //$NON-NLS-1$
			IJavaProject jProject = JavaCore.create(project);
			SearchEngine engine = new SearchEngine();
			IJavaSearchScope searchScope = PluginJavaSearchUtil.createSeachScope(jProject);
			Requestor requestor = new Requestor();
			String packageName = pkg.getName();

			engine.search(SearchPattern.createPattern(packageName, IJavaSearchConstants.PACKAGE, IJavaSearchConstants.REFERENCES, SearchPattern.R_EXACT_MATCH), new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()}, searchScope, requestor, new SubProgressMonitor(monitor, 1));

			if (requestor.foundMatches())
				return true;
		} catch (CoreException e) {
		} finally {
			monitor.done();
		}
		return false;
	}

	public ArrayList getList() {
		return fList;
	}

	public static void removeDependencies(IPluginModelBase model, Object[] elements) {
		ImportPackageHeader pkgHeader = null;
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] instanceof IPluginImport)
				try {
					model.getPluginBase().remove((IPluginImport) elements[i]);
				} catch (CoreException e) {
				}
			else if (elements[i] instanceof ImportPackageObject) {
				if (pkgHeader == null)
					pkgHeader = (ImportPackageHeader) ((ImportPackageObject) elements[i]).getHeader();
				pkgHeader.removePackage((ImportPackageObject) elements[i]);
			}
		}
	}

	private void minimizeDependencies(HashMap usedPlugins, ArrayList usedPackages, IProgressMonitor monitor) {
		ListIterator li = usedPackages.listIterator();
		while (li.hasNext()) {
			ImportPackageObject ipo = (ImportPackageObject) li.next();
			String bundle = ipo.getAttribute(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE);
			if (usedPlugins.containsKey(bundle))
				fList.add(ipo);
		}

		Iterator it = usedPlugins.keySet().iterator();
		Stack plugins = new Stack();
		while (it.hasNext())
			plugins.push(it.next().toString());

		while (!(plugins.isEmpty())) {
			String pluginId = (String) plugins.pop();
			IPluginModelBase base = PluginRegistry.findModel(pluginId);
			if (base == null)
				continue;
			IPluginImport[] imports = base.getPluginBase().getImports();

			for (int j = 0; j < imports.length; j++)
				if (imports[j].isReexported()) {
					String reExportedId = imports[j].getId();
					Object pluginImport = usedPlugins.remove(imports[j].getId());
					if (pluginImport != null) {
						fList.add(pluginImport);
						updateMonitor(monitor, fList.size());
					}
					plugins.push(reExportedId);
				}
		}
	}
}
