package org.eclipse.pde.internal.ui.search;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchResultCollector;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.PDEPlugin;

public class UnusedDependenciesOperation implements IRunnableWithProgress {
	private IPluginModelBase model;
	private IProject parentProject;
	HashSet unused = new HashSet();

	class SearchResultCollector implements IJavaSearchResultCollector {
		int count = 0;

		public void accept(
			IResource resource,
			int start,
			int end,
			IJavaElement enclosingElement,
			int accuracy)
			throws CoreException {
			if (accuracy == IJavaSearchConstants.EXACT_MATCH) {
				count += 1;
			}
		}

		public void aboutToStart() {}

		public void done() {}

		public IProgressMonitor getProgressMonitor() {
			return null;
		}

		public boolean isEmpty() {
			return count == 0;
		}
	}

	public UnusedDependenciesOperation(IPluginModelBase model) {
		this.model = model;
		this.parentProject = model.getUnderlyingResource().getProject();
	}

	public void run(IProgressMonitor monitor)
		throws InvocationTargetException, InterruptedException {
		try {
			IPluginImport[] imports = model.getPluginBase().getImports();
			if (imports.length == 0)
				return;

			monitor.setTaskName(
				PDEPlugin.getResourceString("UnusedDependencies.analyze"));
			monitor.beginTask("", imports.length);
			for (int i = 0; i < imports.length; i++) {
				if (!isUsed(imports[i], new SubProgressMonitor(monitor, 1)))
					unused.add(imports[i]);
				monitor.setTaskName(
					PDEPlugin.getResourceString("UnusedDependencies.analyze")
						+ unused.size()
						+ " "
						+ PDEPlugin.getResourceString("UnusedDependencies.unused")
						+ " "
						+ (unused.size() == 1
							? PDEPlugin.getResourceString("DependencyExtent.singular")
							: PDEPlugin.getResourceString("DependencyExtent.plural"))
						+ " "
						+ PDEPlugin.getResourceString("DependencyExtent.found"));
			}
		} finally {
			monitor.done();
		}

	}

	private boolean isUsed(IPluginImport dependency, IProgressMonitor monitor) {
		try {
			HashSet set = new HashSet();
			PluginJavaSearchUtil.collectAllPrerequisites(
				PDECore.getDefault().findPlugin(dependency.getId()),
				set);
			IPluginBase[] models =
				(IPluginBase[]) set.toArray(new IPluginBase[set.size()]);

			IPackageFragment[] packageFragments = new IPackageFragment[0];
			if (parentProject.hasNature(JavaCore.NATURE_ID))
				packageFragments =
					PluginJavaSearchUtil.collectPackageFragments(models, parentProject);

			monitor.beginTask("", packageFragments.length + 1);

			HashSet ids = new HashSet();
			for (int i = 0; i < models.length; i++)
				ids.add(models[i].getId());
			if (providesExtensionPoint(ids))
				return true;
			monitor.worked(1);
			
			if (packageFragments.length > 0)
				return doJavaSearch(packageFragments, new SubProgressMonitor(monitor, packageFragments.length));

		} catch (JavaModelException e) {
		} catch (CoreException e) {
		} finally {
			monitor.done();
		}
		return true;
	}

	public IPluginImport[] getUnusedDependencies() {
		return (IPluginImport[]) unused.toArray(new IPluginImport[unused.size()]);
	}

	private boolean providesExtensionPoint(HashSet ids) {
		IPluginExtension[] extensions = model.getPluginBase().getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			if (ids.contains(extensions[i].getPoint()))
				return true;
		}
		return false;
	}

	private boolean doJavaSearch(
		IPackageFragment[] packageFragments,
		IProgressMonitor monitor)
		throws JavaModelException {
		SearchEngine searchEngine = new SearchEngine();
		IJavaSearchScope scope = getSearchScope();

		for (int i = 0; i < packageFragments.length; i++) {
			IPackageFragment packageFragment = packageFragments[i];
			boolean used = false;
			if (!packageFragment.hasSubpackages()) {
				SearchResultCollector collector = new SearchResultCollector();
				searchEngine.search(
					PDEPlugin.getWorkspace(),
					SearchEngine.createSearchPattern(
						packageFragment.getElementName() + ".*",
						IJavaSearchConstants.TYPE,
						IJavaSearchConstants.REFERENCES,
						true),
					scope,
					collector);
				used = !collector.isEmpty();
			} else {
				used = searchForTypes(packageFragment, searchEngine, scope, monitor);
			}
			monitor.worked(1);
			if (used)
				return true;
		}
		return false;
	}

	private boolean searchForTypes(
		IPackageFragment fragment,
		SearchEngine searchEngine,
		IJavaSearchScope scope,
		IProgressMonitor monitor)
		throws JavaModelException {
		IJavaElement[] children = fragment.getChildren();
		for (int i = 0; i < children.length; i++) {
			IJavaElement child = children[i];
			IType[] types = new IType[0];
			if (child instanceof IClassFile)
				types = new IType[] {((IClassFile) child).getType()};
			else if (child instanceof ICompilationUnit)
				types = ((ICompilationUnit) child).getAllTypes();

			for (int j = 0; j < types.length; j++) {
				SearchResultCollector collector = new SearchResultCollector();
				searchEngine.search(
					PDEPlugin.getWorkspace(),
					SearchEngine.createSearchPattern(
						types[j],
						IJavaSearchConstants.REFERENCES),
					scope,
					collector);
				if (!collector.isEmpty()) {
					return true;
				}
			}
		}
		return false;
	}

	private IJavaSearchScope getSearchScope() throws JavaModelException {
		IPackageFragmentRoot[] roots =
			JavaCore.create(parentProject).getPackageFragmentRoots();
		ArrayList filteredRoots = new ArrayList();
		for (int i = 0; i < roots.length; i++) {
			if (roots[i].getResource() != null
				&& roots[i].getResource().getProject().equals(parentProject)) {
				filteredRoots.add(roots[i]);
			}
		}
		return SearchEngine.createJavaSearchScope(
			(IJavaElement[]) filteredRoots.toArray(
				new IJavaElement[filteredRoots.size()]));
	}

}
