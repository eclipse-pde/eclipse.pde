package org.eclipse.pde.internal.ui.search.dependencies;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.search.*;
import org.eclipse.jface.action.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.progress.*;


public class UnusedDependenciesJob extends Job {
	
	class Requestor extends SearchRequestor {
		boolean found = false;
		public void acceptSearchMatch(SearchMatch match) throws CoreException {
			found = true;
		}
		public boolean foundMatches() {
			return found;
		}
	}

	private IPluginModelBase fModel;

	public UnusedDependenciesJob(String name, IPluginModelBase model) {
		super(name);
		fModel = model;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.internal.jobs.InternalJob#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IStatus run(IProgressMonitor monitor) {
		IPluginImport[] imports = fModel.getPluginBase().getImports();
		try {
			monitor.beginTask("", imports.length);
			ArrayList list = new ArrayList();
			for (int i = 0; i < imports.length; i++) {
				if (monitor.isCanceled())
					break;
				if (isUnused(imports[i], new SubProgressMonitor(monitor, 1))) {
					list.add(imports[i]);
				}
				monitor.setTaskName(
						PDEPlugin.getResourceString("UnusedDependencies.analyze") //$NON-NLS-1$
							+ list.size()
							+ " " //$NON-NLS-1$
							+ PDEPlugin.getResourceString("UnusedDependencies.unused") //$NON-NLS-1$
							+ " " //$NON-NLS-1$
							+ (list.size() == 1
								? PDEPlugin.getResourceString("DependencyExtent.singular") //$NON-NLS-1$
								: PDEPlugin.getResourceString("DependencyExtent.plural")) //$NON-NLS-1$
							+ " " //$NON-NLS-1$
							+ PDEPlugin.getResourceString("DependencyExtent.found")); //$NON-NLS-1$
			}
			IPluginImport[] unused = (IPluginImport[])list.toArray(new IPluginImport[list.size()]);
			if (isModal()) {
				showResults(unused);
			} else {
		        setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);
		        setProperty(IProgressConstants.ACTION_PROPERTY, getShowResultsAction(unused));
		    }
		} finally {
			monitor.done();
		}
		return new Status(IStatus.OK, PDEPlugin.getPluginId(), IStatus.OK, "View Results", null);
	}
	
	private boolean isModal() {
		Boolean isModal = (Boolean)getProperty(IProgressConstants.PROPERTY_IN_DIALOG);
		return (isModal == null) ? false : isModal.booleanValue();
	}

	private boolean isUnused(IPluginImport plugin, SubProgressMonitor monitor) {
		IPlugin[] plugins = PluginJavaSearchUtil.getPluginImports(plugin);
		if (PluginJavaSearchUtil.provideExtensionPoint(fModel, plugins))
			return false;
		return !provideJavaClasses(plugins, monitor);
	}
	
	private boolean provideJavaClasses(IPlugin[] plugins, IProgressMonitor monitor) {
		try {
			IProject project = fModel.getUnderlyingResource().getProject();
			if (!project.hasNature(JavaCore.NATURE_ID))
				return false;
			
			IJavaProject jProject = JavaCore.create(project);
			IPackageFragment[] packageFragments = PluginJavaSearchUtil.collectPackageFragments(plugins, jProject);
			SearchEngine engine = new SearchEngine();
			monitor.beginTask("", packageFragments.length);
			for (int i = 0; i < packageFragments.length; i++) {
				IPackageFragment pkgFragment = packageFragments[i];
				if (pkgFragment.hasChildren()) {
					Requestor requestor = new Requestor();
					engine.search(
							SearchPattern.createPattern(pkgFragment, IJavaSearchConstants.REFERENCES),
							new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()},
							PluginJavaSearchUtil.createSeachScope(jProject), 
							requestor, 
							new SubProgressMonitor(monitor, 1));
					if (requestor.foundMatches()) 
						return true;
				} else {
					monitor.worked(1);
				}
			}	
		} catch (CoreException e) {
		} finally {
			monitor.done();
		}
		return false;
	}
	
	
	private Action getShowResultsAction(IPluginImport[] unused) {
		return new ShowResultsAction(unused);
	}
	
    protected void showResults(final IPluginImport[] unused) {
        Display.getDefault().asyncExec(new Runnable() {
           public void run() {
              getShowResultsAction(unused).run();
           }
        });
     }
}
