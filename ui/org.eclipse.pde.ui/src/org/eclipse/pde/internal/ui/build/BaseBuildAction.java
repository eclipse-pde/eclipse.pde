package org.eclipse.pde.internal.ui.build;

import java.io.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;

import org.eclipse.ant.internal.ui.launchConfigurations.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.*;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.*;

public abstract class BaseBuildAction implements IObjectActionDelegate, IPreferenceConstants {
	
	protected IFile file;

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	public void run(IAction action) {
		if (!file.exists())
			return;

		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				IWorkspaceRunnable wop = new IWorkspaceRunnable() {
					public void run(IProgressMonitor monitor) throws CoreException {
						try {
							doBuild(monitor);
						} catch (InvocationTargetException e) {
							PDEPlugin.logException(e);
						}
					}
				};
				try {
					PDEPlugin.getWorkspace().run(wop, monitor);
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		};
		try {
			PlatformUI.getWorkbench().getProgressService().run(false, false, op);			
		} catch (InterruptedException e) {
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
		}

	}

	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			Object obj = ((IStructuredSelection) selection).getFirstElement();
			if (obj != null && obj instanceof IFile) {
				this.file = (IFile) obj;
			}
		}

	}
	
	private void doBuild(IProgressMonitor monitor)
		throws CoreException, InvocationTargetException {
			monitor.beginTask(PDEPlugin.getResourceString("BuildAction.Validate"), 4); //$NON-NLS-1$
			if (!ensureValid(file, monitor)) {
				monitor.done();
				return;
			}
			monitor.worked(1);
			monitor.setTaskName(PDEPlugin.getResourceString("BuildAction.Generate")); //$NON-NLS-1$
			makeScripts(monitor);
			monitor.worked(1);
			monitor.setTaskName(PDEPlugin.getResourceString("BuildAction.Update")); //$NON-NLS-1$
			refreshLocal(monitor);
			monitor.worked(1);
			setDefaultValues();
			monitor.worked(1);
			
		}
		
	protected abstract void makeScripts(IProgressMonitor monitor)
		throws InvocationTargetException, CoreException;

	public static boolean ensureValid(IFile file, IProgressMonitor monitor) throws CoreException {
		// Force the build if autobuild is off
		IProject project = file.getProject();
		if (!project.getWorkspace().isAutoBuilding()) {
			project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
		}

		if (hasErrors(file)) {
			// There are errors against this file - abort
			MessageDialog.openError(
				null,
				PDEPlugin.getResourceString("BuildAction.ErrorDialog.Title"), //$NON-NLS-1$
				PDEPlugin.getResourceString("BuildAction.ErrorDialog.Message")); //$NON-NLS-1$
			return false;
		}
		return true;
	}
	
	public static  boolean hasErrors(IFile file) throws CoreException {
		IMarker[] markers = file.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
		for (int i = 0; i < markers.length; i++) {
			Object att = markers[i].getAttribute(IMarker.SEVERITY);
			if (att != null && att instanceof Integer) {
				if (((Integer) att).intValue() == IMarker.SEVERITY_ERROR)
					return true;
			}
		}
		return false;
	}
	
	protected void refreshLocal(IProgressMonitor monitor) throws CoreException {
		file.getProject().refreshLocal(IResource.DEPTH_ONE, monitor);
	}
	
	private void setDefaultValues() {
		IProject project = file.getProject();
		IFile generatedFile = (IFile) project.findMember("build.xml"); //$NON-NLS-1$
		if (generatedFile == null)
			return;

		try {
			List configs =
				AntLaunchShortcut.findExistingLaunchConfigurations(generatedFile);
			ILaunchConfigurationWorkingCopy launchCopy;
			if (configs.size() == 0) {
				ILaunchConfiguration config =
					AntLaunchShortcut.createDefaultLaunchConfiguration(generatedFile);
				launchCopy = config.getWorkingCopy();
			} else {
				launchCopy = ((ILaunchConfiguration) configs.get(0)).getWorkingCopy();
			}
			if (launchCopy == null)
				return;

			Map properties = new HashMap();
			properties =
				launchCopy.getAttribute(IAntLaunchConfigurationConstants.ATTR_ANT_PROPERTIES, properties);
			properties.put("basews", TargetPlatform.getWS()); //$NON-NLS-1$
			properties.put("baseos", TargetPlatform.getOS()); //$NON-NLS-1$
			properties.put("basearch", TargetPlatform.getOSArch()); //$NON-NLS-1$
			properties.put("basenl", TargetPlatform.getNL()); //$NON-NLS-1$
			properties.put("eclipse.running", "true"); //$NON-NLS-1$ //$NON-NLS-2$
			
			IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
			properties.put("javacFailOnError", store.getString(PROP_JAVAC_FAIL_ON_ERROR)); //$NON-NLS-1$
			properties.put("javacDebugInfo", store.getBoolean(PROP_JAVAC_DEBUG_INFO) ? "on" : "off"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			properties.put("javacVerbose", store.getString(PROP_JAVAC_VERBOSE)); //$NON-NLS-1$
			properties.put("javacSource", store.getString(PROP_JAVAC_SOURCE)); //$NON-NLS-1$
			properties.put("javacTarget", store.getString(PROP_JAVAC_TARGET)); //$NON-NLS-1$
			launchCopy.setAttribute(IAntLaunchConfigurationConstants.ATTR_ANT_PROPERTIES, properties);
			launchCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_NAME, 
					(String)null);
			launchCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_TYPE, 
					(String)null);
			launchCopy.doSave();
		} catch (CoreException e) {
		}
	}
	
	public static URL getDevEntriesProperties(String fileName) {
		File file = new File(fileName);
		if (!file.exists()) {
			File directory = file.getParentFile();
			if (directory != null && (!directory.exists() || directory.isFile())) {
				directory.mkdirs();
			}
		}
		Properties properties = new Properties();
		properties.put("*", "bin"); //$NON-NLS-1$ //$NON-NLS-2$
		WorkspaceModelManager manager = PDECore.getDefault().getWorkspaceModelManager();
		IPluginModelBase[] models = manager.getAllModels();
		for (int i = 0; i < models.length; i++) {
			String id = models[i].getPluginBase().getId();
			if (id == null)
				continue;
			String entry = getDevEntry(models[i]);
			if (entry != null)
				properties.put(id, entry);
		}
		
		try {
			FileOutputStream stream = new FileOutputStream(fileName);
			properties.store(stream, ""); //$NON-NLS-1$
			stream.flush();
			stream.close();
			return new URL("file:" + fileName); //$NON-NLS-1$
		} catch (IOException e) {
			PDECore.logException(e);
		}
		return null;
	}
	
	
	private static String getDevEntry(IPluginModelBase model) {
		ArrayList result = new ArrayList();
		IProject project = model.getUnderlyingResource().getProject();
		try {
			if (project.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject jProject = JavaCore.create(project);
				addPath(result, jProject.getOutputLocation());
				IPackageFragmentRoot[] roots = jProject.getPackageFragmentRoots();
				for (int i = 0; i < roots.length; i++) {
					if (roots[i].getKind() == IPackageFragmentRoot.K_SOURCE)
						addPath(result, roots[i].getRawClasspathEntry().getOutputLocation());
				}
			}
		} catch (JavaModelException e) {
		} catch (CoreException e) {
		}
		if (result.size() == 0)
			return null;
		
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < result.size(); i++) {
			buffer.append(result.get(i).toString());
			if (i < result.size() - 1)
				buffer.append(","); //$NON-NLS-1$
		}
		return buffer.toString();
	}
	
	private static void addPath(ArrayList result, IPath path) {
		if (path != null) {
			if (path.getDevice() != null) {
				result.add(path);
			} else if (path.segmentCount() > 1) {
				result.add(path.removeFirstSegments(1));
			}
		}
	}		
}
