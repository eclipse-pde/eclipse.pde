/*******************************************************************************
 *  Copyright (c) 2005, 2017 IBM Corporation and others.
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
 *     Johannes Ahlers <Johannes.Ahlers@gmx.de> - bug 477677
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
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.TextUtil;
import org.osgi.framework.Constants;

public class GatherUnusedDependenciesOperation implements IRunnableWithProgress {

	class Requestor extends SearchRequestor {
		boolean fFound = false;

		@Override
		public void acceptSearchMatch(SearchMatch match) throws CoreException {
			fFound = true;
		}

		public boolean foundMatches() {
			return fFound;
		}
	}

	private IPluginModelBase fModel;
	private ArrayList<Object> fList;

	public GatherUnusedDependenciesOperation(IPluginModelBase model) {
		fModel = model;
	}

	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

		ImportPackageObject[] packages = null;
		Collection<?> exportedPackages = null;
		if (ClasspathUtilCore.hasBundleStructure(fModel)) {
			IBundle bundle = ((IBundlePluginModelBase) fModel).getBundleModel().getBundle();
			IManifestHeader header = bundle.getManifestHeader(Constants.IMPORT_PACKAGE);
			if (header instanceof ImportPackageHeader) {
				packages = ((ImportPackageHeader) header).getPackages();
			} else if (header != null && header.getValue() != null) {
				header = new ImportPackageHeader(Constants.IMPORT_PACKAGE, header.getValue(), bundle,
						TextUtil.getDefaultLineDelimiter());
				packages = ((ImportPackageHeader) header).getPackages();
			}

			header = bundle.getManifestHeader(Constants.EXPORT_PACKAGE);
			if (header instanceof ExportPackageHeader) {
				exportedPackages = ((ExportPackageHeader) header).getPackageNames();
			} else if (header != null && header.getValue() != null) {
				header = new ExportPackageHeader(Constants.EXPORT_PACKAGE, header.getValue(), bundle,
						TextUtil.getDefaultLineDelimiter());
				exportedPackages = ((ExportPackageHeader) header).getPackageNames();
			}
		}
		IPluginImport[] imports = fModel.getPluginBase().getImports();

		int totalWork = imports.length * 3 + (packages != null ? packages.length : 0) + 1;
		SubMonitor subMonitor = SubMonitor.convert(monitor, totalWork);

		HashMap<String, IPluginImport> usedPlugins = new HashMap<>();
		fList = new ArrayList<>();
		for (IPluginImport pluginImport : imports) {
			if (subMonitor.isCanceled())
				break;
			if (isUnused(pluginImport, subMonitor.split(3))) {
				fList.add(pluginImport);
			} else
				usedPlugins.put(pluginImport.getId(), pluginImport);
			updateMonitor(subMonitor, fList.size());
		}

		ArrayList<ImportPackageObject> usedPackages = new ArrayList<>();
		if (packages != null && !subMonitor.isCanceled()) {
			for (ImportPackageObject importPackage : packages) {
				if (subMonitor.isCanceled())
					break;
				if (isUnused(importPackage, exportedPackages, subMonitor.split(1))) {
					fList.add(importPackage);
					updateMonitor(subMonitor, fList.size());
				} else
					usedPackages.add(importPackage);
			}
		}
		if (!subMonitor.isCanceled()) {
			minimizeDependencies(usedPlugins, usedPackages, subMonitor);
		}
	}

	private void updateMonitor(IProgressMonitor monitor, int size) {
		monitor.setTaskName(PDEUIMessages.UnusedDependencies_analyze + size + " " //$NON-NLS-1$
				+ PDEUIMessages.UnusedDependencies_unused + " " //$NON-NLS-1$
				+ (size == 1 ? PDEUIMessages.DependencyExtent_singular : PDEUIMessages.DependencyExtent_plural) + " " //$NON-NLS-1$
				+ PDEUIMessages.DependencyExtent_found);
	}

	private boolean isUnused(IPluginImport plugin, IProgressMonitor monitor) {
		IPluginModelBase[] models = PluginJavaSearchUtil.getPluginImports(plugin);
		return !provideJavaClasses(models, monitor);
	}

	private boolean isUnused(ImportPackageObject pkg, Collection<?> exportedPackages, IProgressMonitor monitor) {
		if (exportedPackages != null && exportedPackages.contains(pkg.getValue())) {
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

			SubMonitor subMonitor = SubMonitor.convert(monitor, packageFragments.length * 2);
			for (IPackageFragment pkgFragment : packageFragments) {
				SubMonitor iterationMonitor = subMonitor.split(2);
				if (pkgFragment.hasChildren()) {
					Requestor requestor = new Requestor();
					engine.search(SearchPattern.createPattern(pkgFragment, IJavaSearchConstants.REFERENCES),
							new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() }, searchScope,
							requestor, iterationMonitor.split(1));
					if (requestor.foundMatches()) {
						if (provideJavaClasses(pkgFragment, engine, searchScope,
								iterationMonitor.split(1))) {
							return true;
						}
					}
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		return false;
	}

	private boolean provideJavaClasses(IPackageFragment packageFragment, SearchEngine engine, IJavaSearchScope searchScope, IProgressMonitor monitor) throws JavaModelException, CoreException {
		Requestor requestor;
		IJavaElement[] children = packageFragment.getChildren();
		SubMonitor subMonitor = SubMonitor.convert(monitor, children.length);

		for (IJavaElement child : children) {
			IType[] types = null;
			if (child instanceof ICompilationUnit) {
				types = ((ICompilationUnit) child).getAllTypes();
			} else if (child instanceof IOrdinaryClassFile) {
				types = new IType[] { ((IOrdinaryClassFile) child).getType() };
			}
			if (types != null) {
				SubMonitor iterationMonitor = subMonitor.split(1).setWorkRemaining(types.length);
				for (IType type : types) {
					requestor = new Requestor();
					engine.search(SearchPattern.createPattern(type, IJavaSearchConstants.REFERENCES),
							new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() }, searchScope,
							requestor, iterationMonitor.split(1));
					if (requestor.foundMatches()) {
						return true;
					}
				}
			} else {
				subMonitor.worked(1);
			}
		}
		return false;
	}

	private boolean provideJavaClasses(ImportPackageObject pkg, IProgressMonitor monitor) {
		try {
			IProject project = fModel.getUnderlyingResource().getProject();

			if (!project.hasNature(JavaCore.NATURE_ID))
				return false;

			SubMonitor subMonitor = SubMonitor.convert(monitor, 1);
			IJavaProject jProject = JavaCore.create(project);
			SearchEngine engine = new SearchEngine();
			IJavaSearchScope searchScope = PluginJavaSearchUtil.createSeachScope(jProject);
			Requestor requestor = new Requestor();
			String packageName = pkg.getName();

			engine.search(
					SearchPattern.createPattern(packageName, IJavaSearchConstants.PACKAGE,
							IJavaSearchConstants.REFERENCES, SearchPattern.R_EXACT_MATCH),
					new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() }, searchScope, requestor,
					subMonitor.split(1));

			if (requestor.foundMatches())
				return true;
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		return false;
	}

	public ArrayList<Object> getList() {
		return fList;
	}

	public static void removeDependencies(IPluginModelBase model, Object[] elements) {
		ImportPackageHeader pkgHeader = null;
		for (Object element : elements) {
			if (element instanceof IPluginImport)
				try {
					model.getPluginBase().remove((IPluginImport) element);
				} catch (CoreException e) {
				}
			else if (element instanceof ImportPackageObject) {
				if (pkgHeader == null)
					pkgHeader = (ImportPackageHeader) ((ImportPackageObject) element).getHeader();
				pkgHeader.removePackage((ImportPackageObject) element);
			}
		}
	}

	private void minimizeDependencies(HashMap<String, IPluginImport> usedPlugins, ArrayList<ImportPackageObject> usedPackages, IProgressMonitor monitor) {
		ListIterator<ImportPackageObject> li = usedPackages.listIterator();
		while (li.hasNext()) {
			ImportPackageObject ipo = li.next();
			String bundle = ipo.getAttribute(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE);
			if (usedPlugins.containsKey(bundle))
				fList.add(ipo);
		}

		Iterator<String> it = usedPlugins.keySet().iterator();
		Stack<String> plugins = new Stack<>();
		while (it.hasNext())
			plugins.push(it.next().toString());
		SubMonitor subMonitor = SubMonitor.convert(monitor);
		while (!(plugins.isEmpty())) {
			String pluginId = plugins.pop();
			IPluginModelBase base = PluginRegistry.findModel(pluginId);
			if (base == null)
				continue;
			IPluginImport[] imports = base.getPluginBase().getImports();
			SubMonitor iterationMonitor = subMonitor.setWorkRemaining(Math.max(plugins.size() + 1, 5)).split(1)
					.setWorkRemaining(imports.length);
			for (IPluginImport imp : imports) {
				if (imp.isReexported()) {
					String reExportedId = imp.getId();
					if (reExportedId != null && reExportedId.equals(pluginId)) {
						continue;
					}
					Object pluginImport = usedPlugins.remove(imp.getId());
					if (pluginImport != null) {
						fList.add(pluginImport);
						updateMonitor(iterationMonitor, fList.size());
					}
					plugins.push(reExportedId);
				}
				iterationMonitor.worked(1);
			}
		}
	}
}
