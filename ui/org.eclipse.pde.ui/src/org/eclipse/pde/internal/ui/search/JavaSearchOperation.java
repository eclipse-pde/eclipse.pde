package org.eclipse.pde.internal.ui.search;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.actions.WorkspaceModifyOperation;


class JavaSearchOperation extends WorkspaceModifyOperation {
	IJavaElement element;
	IProject parentProject;
	private static final String KEY_MATCH = "Search.singleMatch";
	private static final String KEY_MATCHES = "Search.multipleMatches";
	
	public JavaSearchOperation(IJavaElement element, IProject parentProject) {
		this.element = element;
		this.parentProject = parentProject;
	}

	protected void execute(IProgressMonitor monitor)
		throws CoreException, InvocationTargetException, InterruptedException {
		doJavaSearch(monitor);

	}
	private void doJavaSearch(IProgressMonitor monitor) {
		try {
			SearchEngine searchEngine = new SearchEngine();
			searchEngine.search(
				PDEPlugin.getWorkspace(),
				element,
				IJavaSearchConstants.REFERENCES,
				getSearchScope(),
				new JavaSearchCollector(this, monitor));
		} catch (JavaModelException e) {
		}
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
	
	public String getPluralLabel() {
		return element.getElementName() + " - {0} " + PDEPlugin.getResourceString(KEY_MATCHES);
	}

	public String getSingularLabel() {
		return element.getElementName() + " - 1 " + PDEPlugin.getResourceString(KEY_MATCH);
	}

}