package org.eclipse.pde.internal.ui.search.dependencies;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ClasspathUtilCore;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.search.PluginJavaSearchUtil;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageHeader;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageObject;
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
	private boolean fTopLevelOperation;
	private ArrayList fList;
	
	public GatherUnusedDependenciesOperation(IPluginModelBase model, boolean topLevel) {
		fTopLevelOperation = topLevel;
		fModel = model;
	}
	
	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		
		ImportPackageObject[] packages = null;
		if (ClasspathUtilCore.hasBundleStructure(fModel)) {
			IBundle bundle = ((IBundlePluginModelBase)fModel).getBundleModel().getBundle();
			IManifestHeader header = bundle.getManifestHeader(Constants.IMPORT_PACKAGE);
			if (header instanceof ImportPackageHeader) {
				packages = ((ImportPackageHeader)header).getPackages();
			} else if (header != null && header.getValue() != null) {
				header = new ImportPackageHeader(Constants.IMPORT_PACKAGE, header.getValue(), bundle, System.getProperty("line.separator")); //$NON-NLS-1$
				packages = ((ImportPackageHeader)header).getPackages();
			}
		}
		IPluginImport[] imports = fModel.getPluginBase().getImports();
	
		int totalWork = (packages != null) ? (packages.length + imports.length*3) : imports.length*3;
		if (fTopLevelOperation)
			monitor.beginTask("", totalWork); //$NON-NLS-1$
		fList = new ArrayList();
		for (int i = 0; i < imports.length; i++) {
			if (monitor.isCanceled())
				break;
			if (isUnused(imports[i], new SubProgressMonitor(monitor, 3))) {
				fList.add(imports[i]);
			}
			updateMonitor(monitor, fList.size());
		}
		
		if (packages != null && !monitor.isCanceled()) {
			for (int i = 0; i < packages.length; i++) {
				if (isUnused(packages[i], new SubProgressMonitor(monitor, 1))) {
					fList.add(packages[i]);
					updateMonitor(monitor, fList.size());
				}
			}
		}
	}

	private void updateMonitor(IProgressMonitor monitor, int size) {
		String message = 
			PDEUIMessages.UnusedDependencies_analyze
			+ size
			+ " " //$NON-NLS-1$
			+ PDEUIMessages.UnusedDependencies_unused
			+ " " //$NON-NLS-1$
			+ (size == 1
				? PDEUIMessages.DependencyExtent_singular
				: PDEUIMessages.DependencyExtent_plural) 
			+ " " //$NON-NLS-1$
			+ PDEUIMessages.DependencyExtent_found;
		if (fTopLevelOperation)
			monitor.setTaskName(message);
		else
			monitor.subTask(message);
	}
	
	private boolean isUnused(IPluginImport plugin, SubProgressMonitor monitor) {
		IPlugin[] plugins = PluginJavaSearchUtil.getPluginImports(plugin);
		return !provideJavaClasses(plugins, monitor);
	}
	
	private boolean isUnused(ImportPackageObject pkg, SubProgressMonitor monitor) {
		return !provideJavaClasses(pkg, monitor);
	}
	
	private boolean provideJavaClasses(IPlugin[] plugins, IProgressMonitor monitor) {
		try {
			IProject project = fModel.getUnderlyingResource().getProject();
			if (!project.hasNature(JavaCore.NATURE_ID))
				return false;
			
			IJavaProject jProject = JavaCore.create(project);
			IPackageFragment[] packageFragments = PluginJavaSearchUtil.collectPackageFragments(plugins, jProject, true);
			SearchEngine engine = new SearchEngine();
			IJavaSearchScope searchScope = PluginJavaSearchUtil.createSeachScope(jProject);
			if (fTopLevelOperation)
				monitor.beginTask("", packageFragments.length*2); //$NON-NLS-1$
			for (int i = 0; i < packageFragments.length; i++) {
				IPackageFragment pkgFragment = packageFragments[i];
				if (pkgFragment.hasChildren()) {
					Requestor requestor = new Requestor();
					engine.search(
							SearchPattern.createPattern(pkgFragment, IJavaSearchConstants.REFERENCES),
							new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()},
							searchScope, requestor, 
							fTopLevelOperation ? new SubProgressMonitor(monitor, 1) : null);
					if (requestor.foundMatches()) {
						if (provideJavaClasses(packageFragments[i], engine,
								searchScope, new SubProgressMonitor(monitor, 1))) {
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

	private boolean provideJavaClasses(IPackageFragment packageFragment,
			SearchEngine engine, IJavaSearchScope searchScope,
			IProgressMonitor monitor) throws JavaModelException, CoreException {
		Requestor requestor;
		IJavaElement[] children = packageFragment.getChildren();
		if (fTopLevelOperation)
			monitor.beginTask("", children.length); //$NON-NLS-1$

		try {
			for (int j = 0; j < children.length; j++) {
				IType[] types = null;
				if (children[j] instanceof ICompilationUnit) {
					types = ((ICompilationUnit) children[j]).getAllTypes();
				} else if (children[j] instanceof IClassFile) {
					types = new IType[] { ((IClassFile) children[j]).getType() };
				}
				if (types != null) {
					for (int t = 0; t < types.length; t++) {
						requestor = new Requestor();
						engine.search(SearchPattern.createPattern(types[t],
								IJavaSearchConstants.REFERENCES),
								new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() },
								searchScope, requestor, 
								fTopLevelOperation ? new SubProgressMonitor(monitor, 1) : null);
						if (requestor.foundMatches()) {
							return true;
						}
					}
				}
			}
		} finally {
			if (fTopLevelOperation)
				monitor.done();
		}
		return false;
	}
	
	private boolean provideJavaClasses(ImportPackageObject pkg, IProgressMonitor monitor) {
		try {
			IProject project = fModel.getUnderlyingResource().getProject();
			
			if (!project.hasNature(JavaCore.NATURE_ID))
				return false;
			
			if (fTopLevelOperation)
				monitor.beginTask("", 1); //$NON-NLS-1$
			IJavaProject jProject = JavaCore.create(project);
			SearchEngine engine = new SearchEngine();
			IJavaSearchScope searchScope = PluginJavaSearchUtil.createSeachScope(jProject);
			Requestor requestor = new Requestor();
			String packageName = pkg.getName();
			
			engine.search(
					SearchPattern.createPattern(packageName, IJavaSearchConstants.PACKAGE, 
							IJavaSearchConstants.REFERENCES, SearchPattern.R_EXACT_MATCH),
							new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()},
							searchScope, 
							requestor, fTopLevelOperation ? new SubProgressMonitor(monitor, 1) : null);
			
			if (requestor.foundMatches()) 
				return true;
		} catch (CoreException e) {
		} finally {
			if (fTopLevelOperation)
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
				} catch (CoreException e) {}
			else if (elements[i] instanceof ImportPackageObject) {
				if (pkgHeader == null) 
					pkgHeader = (ImportPackageHeader)((ImportPackageObject)elements[i]).getHeader();
				pkgHeader.removePackage((ImportPackageObject)elements[i]);			
			}
		}
	}
}
