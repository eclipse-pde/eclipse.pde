/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.wizards.imports;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.model.LibraryModel;
import org.eclipse.core.runtime.model.PluginModel;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.pde.selfhosting.internal.PluginUtil;
import org.eclipse.pde.selfhosting.internal.SelfHostingPlugin;

public class UpdateClasspathAction implements IWorkbenchWindowActionDelegate {

	private ISelection fSelection;

	/*
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		if (fSelection instanceof IStructuredSelection) {
			Object[] elems= ((IStructuredSelection)fSelection).toArray();
			final ArrayList plugins= new ArrayList(elems.length);
			final ArrayList projects= new ArrayList(elems.length);
		
			try {
				for (int i= 0; i < elems.length; i++) {
					if (elems[i] instanceof IFile) {
						IFile file= (IFile) elems[i];
						IProject project= file.getProject();
						if (project.hasNature(JavaCore.NATURE_ID)) {
							PluginModel plugin= PluginUtil.getPluginModel(file.getLocation());
							if (plugin != null) {
								plugins.add(plugin);
								projects.add(JavaCore.create(project));
							}
						}
					}
				}
			
				ProgressMonitorDialog dialog= new ProgressMonitorDialog(SelfHostingPlugin.getActiveWorkbenchShell());
				dialog.run(true, false, new IRunnableWithProgress() {
				 	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				 		try {
				 			IWorkspaceRunnable runnable= new IWorkspaceRunnable() {
				 				public void run(IProgressMonitor monitor) throws CoreException {
				 					doUpdateClasspath(monitor, plugins, projects);
				 				}
				 			};
				 			SelfHostingPlugin.getWorkspace().run(runnable, monitor);
				 		} catch (CoreException e) {
				 			throw new InvocationTargetException(e);
				 		} catch (OperationCanceledException e) {
				 			throw new InterruptedException(e.getMessage());
				 		}
				 	}
				});
			} catch (InterruptedException e) {
				return;
			} catch (InvocationTargetException e) {
				String title= "Update Classpaths";
				String message= "Updating failed. See log for details.";
				SelfHostingPlugin.logAndDisplay(e, SelfHostingPlugin.getActiveWorkbenchShell(), title, message);
			} catch (CoreException e) {
				String title= "Update Classpaths";
				String message= "Updating failed. See log for details.";
				SelfHostingPlugin.logAndDisplay(e, SelfHostingPlugin.getActiveWorkbenchShell(), title, message);
			}
		}		
	}
	
	private void doUpdateClasspath(IProgressMonitor monitor, ArrayList plugins, ArrayList projects) throws CoreException {
		monitor.beginTask("Update classpaths...", plugins.size());
		try {
			for (int i= 0; i < plugins.size(); i++) {
				PluginModel plugin= (PluginModel) plugins.get(i);
				IJavaProject jproject= (IJavaProject) projects.get(i);
				
				ArrayList entries= new ArrayList();
				IClasspathEntry[] oldClasspath= jproject.getRawClasspath();
				for (int k= 0; k < oldClasspath.length; k++) {
					IClasspathEntry curr= oldClasspath[k];
					if (curr.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
						entries.add(curr);
					}
				}
				LibraryModel[] libraries= plugin.getRuntime();
				IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
				for (int k= 0; k < libraries.length; k++) {
					IClasspathEntry libEntry= UpdateClasspathOperation.getLibraryEntry(jproject.getProject(), libraries[k], true);
					if (root.findMember(libEntry.getPath()) != null) {
						entries.add(libEntry);
					}
				}
				
				IClasspathEntry[] classpathEntries= (IClasspathEntry[]) entries.toArray(new IClasspathEntry[entries.size()]);
				
				UpdateClasspathOperation op= new UpdateClasspathOperation(jproject, plugin, classpathEntries, jproject.getOutputLocation());
				op.run(new SubProgressMonitor(monitor, 1));
			}
		} finally {
			monitor.done();
		}
	}
	
	/*
	 * @see IWorkbenchWindowActionDelegate#dispose()
	 */
	public void dispose() {
	}

	/*
	 * @see IWorkbenchWindowActionDelegate#init(IWorkbenchWindow)
	 */
	public void init(IWorkbenchWindow window) {
	}

	/*
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		fSelection= selection;
	}

}

