package org.eclipse.pde.internal.ui.search;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchResultCollector;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginPathUpdater;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModelBase;
import org.eclipse.pde.internal.ui.PDEPlugin;
/**
 * @author wassimm
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class DependencyExtentSearchOperation extends WorkspaceModifyOperation {
	
	private static final String KEY_DEPENDENCY = "DependencyExtent.singular";
	private static final String KEY_DEPENDENCIES = "DependencyExtent.plural";
	private static final String KEY_SEARCHING = "DependencyExtent.searching";

	IPluginImport object;
	IProject parentProject;
	IPluginBase[] models = new IPluginBase[0];
	IPackageFragment[] packageFragments = new IPackageFragment[0];
	DependencyExtentSearchResultCollector resultCollector;
	
	class SearchResultCollector implements IJavaSearchResultCollector {
	
		protected IProgressMonitor monitor;
		HashSet result = new HashSet();
		
		
		public SearchResultCollector(IProgressMonitor monitor) {
			this.monitor = monitor;
		}
		
		public void accept(
			IResource resource,
			int start,
			int end,
			IJavaElement enclosingElement,
			int accuracy)
			throws CoreException {
			if (accuracy == IJavaSearchConstants.EXACT_MATCH)	
				result.add(enclosingElement);
		}
	
		public void aboutToStart() {
		}
	
		public void done() {}
	
		public IProgressMonitor getProgressMonitor() {
			return monitor;
		}
		
		public IJavaElement[] getResult() {
			return (IJavaElement[])result.toArray(new IJavaElement[result.size()]);
		}
		
	}

	public DependencyExtentSearchOperation(IPluginImport object) {
		this.object = object;
		parentProject = object.getModel().getUnderlyingResource().getProject();
	}

	protected void execute(IProgressMonitor monitor)
		throws CoreException, InvocationTargetException, InterruptedException {
		resultCollector =
			new DependencyExtentSearchResultCollector(this, monitor);

		try {
			HashSet set = new HashSet();
			collectAllPrerequisites(
				PDECore.getDefault().findPlugin(object.getId()),
				set);
			models = (IPluginBase[]) set.toArray(new IPluginBase[set.size()]);

			if (parentProject.hasNature(JavaCore.NATURE_ID))
				collectPackageFragments();

			monitor.setTaskName(PDEPlugin.getResourceString(KEY_SEARCHING));
			monitor.beginTask("",packageFragments.length + 1);
			resultCollector.searchStarted();
			
			findExtensionPoints(monitor);

			if (packageFragments.length > 0)
				doJavaSearch(monitor);

		} catch (JavaModelException e) {
			PDEPlugin.log(e.getStatus());
		
		} finally {
			resultCollector.done();
		}
	}

	private void findExtensionPoints(IProgressMonitor monitor) {
		IPluginExtension[] extensions = object.getPluginBase().getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			IPluginExtensionPoint point = getExtensionPoint(extensions[i].getPoint());
			if (point != null) {
				resultCollector.accept(point);
			}
		}
		monitor.worked(1);
	}
	
	private IPluginExtensionPoint getExtensionPoint(String targetId) {				
		for (int i = 0; i < models.length; i++) {
			IPluginExtensionPoint[] extPoints = models[i].getExtensionPoints();
			for (int j = 0; j < extPoints.length; j++) {
				String id = extPoints[j].getPluginBase().getId() + "." + extPoints[j].getId();
				if (id.equals(targetId))
					return extPoints[j];
			}
		}
		return null;
	}
			
		
	private void doJavaSearch(IProgressMonitor monitor)
		throws JavaModelException {
		SearchEngine searchEngine = new SearchEngine();
		
		for (int i = 0; i < packageFragments.length; i++) {
			IPackageFragment packageFragment = packageFragments[i];
			SearchResultCollector collector = new SearchResultCollector(monitor);
			searchEngine.search(
				PDEPlugin.getWorkspace(),
				SearchEngine.createSearchPattern(
					packageFragment.getElementName() + ".*",
					IJavaSearchConstants.TYPE,
					IJavaSearchConstants.REFERENCES,
					true),
				getSearchScope(),
				collector);
			IJavaElement[] enclosingElements = collector.getResult();
			if (enclosingElements.length > 0) {
				IJavaElement[] children = packageFragment.getChildren();
				for (int j = 0; j < children.length; j++) {
					IType type = null;
					if (children[j] instanceof IClassFile) 
						type = ((IClassFile)children[j]).getType();
					else if (children[j] instanceof ICompilationUnit) 
						type = ((ICompilationUnit)children[j]).getTypes()[0];
					
					if (type == null)
						continue;
					SearchResultCollector collector2 =
						new SearchResultCollector(monitor);
					searchEngine.search(
						PDEPlugin.getWorkspace(),
						SearchEngine.createSearchPattern(
							type.getElementName(),
							IJavaSearchConstants.TYPE,
							IJavaSearchConstants.REFERENCES,
							true),
						SearchEngine.createJavaSearchScope(enclosingElements),
						collector2);
					if (collector2.getResult().length > 0) {
						resultCollector.accept(type);
					}
				}
			}
			monitor.worked(1);
		}
	}
	

	private void collectPackageFragments() throws JavaModelException {
		ArrayList result = new ArrayList();
		IPackageFragmentRoot[] roots =
			JavaCore.create(parentProject).getAllPackageFragmentRoots();

		for (int i = 0; i < models.length; i++) {
			IPluginBase preReq = models[i];
			IResource resource = preReq.getModel().getUnderlyingResource();
			if (resource == null) {
				ArrayList libraryPaths = getLibraryPaths(preReq);
				for (int j = 0; j < roots.length; j++) {
					if (libraryPaths
						.contains(new Path(roots[j].getElementName()))) {
						extractFragments(roots[j], result);
					}
				}
			} else {
				IProject project = resource.getProject();
				for (int j = 0; j < roots.length; j++) {
					IJavaProject jProject = (IJavaProject) roots[j].getParent();
					if (jProject.getProject().equals(project)) {
						extractFragments(roots[j], result);
					}
				}
			}
		}
		packageFragments = (IPackageFragment[])result.toArray(new IPackageFragment[result.size()]);
		
	}
			
	private void extractFragments(
		IPackageFragmentRoot root,
		ArrayList result) {
		try {
			IJavaElement[] children = root.getChildren();
			for (int i = 0; i < children.length; i++) {
				IPackageFragment fragment = (IPackageFragment) children[i];
				if (fragment.getChildren().length > 0)
					result.add(fragment);
			}
		} catch (JavaModelException e) {
		}
	}


	private IJavaSearchScope getSearchScope() throws JavaModelException {
		IPackageFragmentRoot[] roots = JavaCore.create(parentProject).getPackageFragmentRoots();
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
	
	public String getPluralLabel() {
		return object.getId() + " - {0} " + PDEPlugin.getResourceString(KEY_DEPENDENCIES);
	}
	
	public String getSingularLabel() {
		return object.getId() + " 1 " + PDEPlugin.getResourceString(KEY_DEPENDENCIES);
	}
	
	public IProject getProject() {
		return parentProject;
	}
	
	private void collectAllPrerequisites(IPlugin plugin, HashSet set) {		
		if (!set.add(plugin))
			return;
		
		if (plugin.getModel() instanceof WorkspacePluginModelBase) {
			IFragment[] fragments = PDECore.getDefault().getWorkspaceModelManager().getFragmentsFor(plugin.getId(),plugin.getVersion());
			for (int i = 0; i < fragments.length; i++) {
				set.add(fragments[i]);
			}
		}
			
		IPluginImport[] imports = plugin.getImports();
		for (int i = 0; i < imports.length; i++) {
			if (imports[i].isReexported()) {
				IPlugin child = PDECore.getDefault().findPlugin(imports[i].getId());
				if (child != null)
					collectAllPrerequisites(child, set);
			}
		}
	}
	
	private ArrayList getLibraryPaths(IPluginBase plugin) {
		ArrayList libraryPaths = new ArrayList();
		IFragmentModel[] fragments =
			PDECore.getDefault().getExternalModelManager().getFragmentsFor(
				(IPluginModel) plugin.getModel());

		IPluginLibrary[] libraries = plugin.getLibraries();
		for (int i = 0; i < libraries.length; i++) {
			String libraryName = PluginPathUpdater.expandLibraryName(libraries[i].getName());
			String path =
				plugin.getModel().getInstallLocation()
					+ Path.SEPARATOR
					+ libraryName;
			if (new File(path).exists()) {
				libraryPaths.add(new Path(path));
			} else {
				findLibraryInFragments(fragments, libraryName, libraryPaths);
			}
		}
		return libraryPaths;
	}
	
	private void findLibraryInFragments(IFragmentModel[] fragments, String libraryName, ArrayList libraryPaths) {
		for (int i = 0; i < fragments.length; i++) {
			String path = fragments[i].getInstallLocation() + Path.SEPARATOR + libraryName;
			if (new File(path).exists()) {
				libraryPaths.add(new Path(path));
				break;
			} 
		}
	}
}
