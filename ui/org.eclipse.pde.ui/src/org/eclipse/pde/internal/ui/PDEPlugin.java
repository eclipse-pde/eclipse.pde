/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Hashtable;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationListener;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.pde.internal.core.FileAdapter;
import org.eclipse.pde.internal.core.ModelEntry;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.launcher.LaunchConfigurationListener;
import org.eclipse.pde.internal.ui.launcher.LaunchListener;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.view.PluginsViewAdapterFactory;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.osgi.framework.BundleContext;

public class PDEPlugin extends AbstractUIPlugin implements IPDEUIConstants {

	// Shared instance
	private static PDEPlugin fInstance;
	
	// Launches listener
	private LaunchListener fLaunchListener;
	
	private BundleContext fBundleContext;

	private java.util.Hashtable fCounters;
	
	// Shared colors for all forms
	private FormColors fFormColors;
	private PDELabelProvider fLabelProvider;
	private ILaunchConfigurationListener fLaunchConfigurationListener;

	/**
	 * The shared text file document provider.
	 * @since 3.2
	 */
	private IDocumentProvider fTextFileDocumentProvider;

	public PDEPlugin() {
		fInstance = this;
	}
	
	public URL getInstallURL() {
		return getDefault().getBundle().getEntry("/"); //$NON-NLS-1$
	}
	
	public static IWorkbenchPage getActivePage() {
		return getDefault().internalGetActivePage();
	}
	public static Shell getActiveWorkbenchShell() {
		IWorkbenchWindow window = getActiveWorkbenchWindow();
		if (window != null) {
			return window.getShell();
		}
		return null;
	}
	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		return getDefault().getWorkbench().getActiveWorkbenchWindow();
	}
	public static PDEPlugin getDefault() {
		return fInstance;
	}
	public Hashtable getDefaultNameCounters() {
		if (fCounters == null)
			fCounters = new Hashtable();
		return fCounters;
	}
	public static String getPluginId() {
		return getDefault().getBundle().getSymbolicName();
	}
	
	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}
	
	private IWorkbenchPage internalGetActivePage() {
		return getWorkbench().getActiveWorkbenchWindow().getActivePage();
	}

	public static void log(IStatus status) {
		ResourcesPlugin.getPlugin().getLog().log(status);
	}

	public static void logErrorMessage(String message) {
		log(new Status(IStatus.ERROR, getPluginId(), IStatus.ERROR, message, null));
	}

	public static void logException(
		Throwable e,
		final String title,
		String message) {
		if (e instanceof InvocationTargetException) {
			e = ((InvocationTargetException) e).getTargetException();
		}
		IStatus status = null;
		if (e instanceof CoreException)
			status = ((CoreException) e).getStatus();
		else {
			if (message == null)
				message = e.getMessage();
			if (message == null)
				message = e.toString();
			status = new Status(IStatus.ERROR, getPluginId(), IStatus.OK, message, e);
		}
		ResourcesPlugin.getPlugin().getLog().log(status);
		Display display = SWTUtil.getStandardDisplay();
		final IStatus fstatus = status;
		display.asyncExec(new Runnable() {
			public void run() {
				ErrorDialog.openError(null, title, null, fstatus);
			}
		});
	}

	public static void logException(Throwable e) {
		logException(e, null, null);
	}

	public static void log(Throwable e) {
		if (e instanceof InvocationTargetException)
			e = ((InvocationTargetException) e).getTargetException();
		IStatus status = null;
		if (e instanceof CoreException)
			status = ((CoreException) e).getStatus();
		else
			status =
				new Status(IStatus.ERROR, getPluginId(), IStatus.OK, e.getMessage(), e);
		log(status);
	}
	
	public FormColors getFormColors(Display display) {
		if (fFormColors == null) {
			fFormColors = new FormColors(display);
			fFormColors.markShared();
		}
		return fFormColors;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.Plugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		this.fBundleContext = context;
		IAdapterManager manager = Platform.getAdapterManager();
		PluginsViewAdapterFactory factory = new PluginsViewAdapterFactory();
		manager.registerAdapters(factory, ModelEntry.class);
		manager.registerAdapters(factory, FileAdapter.class);

		fLaunchConfigurationListener = new LaunchConfigurationListener();
		DebugPlugin.getDefault().getLaunchManager().addLaunchConfigurationListener(fLaunchConfigurationListener);
	}
	
	public BundleContext getBundleContext() {
		return fBundleContext;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		if (fLaunchListener!=null)
			fLaunchListener.shutdown();
		if (fFormColors!=null) {
			fFormColors.dispose();
			fFormColors=null;
		}
		if (fLabelProvider != null) {
			fLabelProvider.dispose();
			fLabelProvider = null;
		}
		if (fLaunchConfigurationListener != null) {
			DebugPlugin.getDefault().getLaunchManager().removeLaunchConfigurationListener(fLaunchConfigurationListener);
			fLaunchConfigurationListener = null;
		}
		super.stop(context);
	}

	public PDELabelProvider getLabelProvider() {
		if (fLabelProvider==null)
			fLabelProvider = new PDELabelProvider();
		return fLabelProvider;
	}
	
	public LaunchListener getLaunchListener() {
		if (fLaunchListener == null)
			fLaunchListener = new LaunchListener();
		return fLaunchListener;
	}
	
	public static boolean isFullNameModeEnabled() {
		IPreferenceStore store = getDefault().getPreferenceStore();
		return store.getString(IPreferenceConstants.PROP_SHOW_OBJECTS).equals(IPreferenceConstants.VALUE_USE_NAMES);
	}
	
	/**
	 * Returns the shared text file document provider for this plug-in.
	 * 
	 * @return the shared text file document provider
	 * @since 3.2
	 */
	public synchronized IDocumentProvider getTextFileDocumentProvider() {
		if (fTextFileDocumentProvider == null) {
			fTextFileDocumentProvider = new TextFileDocumentProvider() {
				protected FileInfo createFileInfo(Object element) throws CoreException {
					FileInfo info = super.createFileInfo(element);
					if (info != null && info.fTextFileBuffer != null)
						info.fModel = info.fTextFileBuffer.getAnnotationModel();
					setUpSynchronization(info);
					return info;
				}
			};
		}
		return fTextFileDocumentProvider;
	}
	
	public static PDEFormEditor getOpenManifestEditor(IProject project) {
		return getOpenEditor(project, IPDEUIConstants.MANIFEST_EDITOR_ID);
	}
	
	public static PDEFormEditor getOpenBuildPropertiesEditor(IProject project) {
		return getOpenEditor(project, IPDEUIConstants.BUILD_EDITOR_ID);
	}
	
	private static PDEFormEditor getOpenEditor(IProject project, String editorId) {
		IWorkbenchPage page = getActivePage();
		IEditorReference[] editors = page.getEditorReferences();
		for (int i = 0; i < editors.length; i++) {
			if (editors[i].getId().equals(editorId)) {
				IPersistableElement persistable;
				try {
					persistable = editors[i].getEditorInput().getPersistable();
					if (!(persistable instanceof IFileEditorInput))
						continue;
					if (!((IFileEditorInput)persistable).getFile().getProject().equals(project))
						continue;
					return (PDEFormEditor)page.findEditor(editors[i].getEditorInput());
				} catch (PartInitException e) {
				}
			}
		}
		return null;
	}
}
