/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.IFeatureObject;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.ui.editor.feature.FeatureAdapterFactory;
import org.eclipse.pde.internal.ui.editor.manifest.PluginAdapterFactory;
import org.eclipse.pde.internal.ui.editor.schema.SchemaAdapterFactory;
import org.eclipse.pde.internal.ui.editor.text.ColorManager;
import org.eclipse.pde.internal.ui.launcher.LaunchListener;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.view.PluginsViewAdapterFactory;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.texteditor.ExtendedTextEditorPreferenceConstants;
import org.osgi.framework.*;

public class PDEPlugin extends AbstractUIPlugin implements IPDEUIConstants, IPreferenceConstants {

	// Shared instance
	private static PDEPlugin inst;
	// Resource bundle
	private ResourceBundle resourceBundle;
	// Launches listener
	private LaunchListener launchListener;
	
	private BundleContext context;

	private java.util.Hashtable counters;
	
	// Shared colors for all forms
	private FormColors formColors;
	private PDELabelProvider labelProvider;

	public PDEPlugin(IPluginDescriptor descriptor) {
		super(descriptor);
		inst = this;
		try {
			resourceBundle =
				ResourceBundle.getBundle("org.eclipse.pde.internal.ui.pderesources");
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
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
		return inst;
	}
	public Hashtable getDefaultNameCounters() {
		if (counters == null)
			counters = new Hashtable();
		return counters;
	}
	public static String getFormattedMessage(String key, String[] args) {
		String text = getResourceString(key);
		return java.text.MessageFormat.format(text, args);
	}
	public static String getFormattedMessage(String key, String arg) {
		String text = getResourceString(key);
		return java.text.MessageFormat.format(text, new Object[] { arg });
	}
	static IPath getInstallLocation() {
		return new Path(getDefault().getDescriptor().getInstallURL().getFile());
	}
	public static String getPluginId() {
		return getDefault().getDescriptor().getUniqueIdentifier();
	}
	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}
	public static String getResourceString(String key) {
		ResourceBundle bundle = PDEPlugin.getDefault().getResourceBundle();
		if (bundle != null) {
			try {
				String bundleString = bundle.getString(key);
				//return "$"+bundleString;
				return bundleString;
			} catch (MissingResourceException e) {
				// default actions is to return key, which is OK
			}
		}
		return key;
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
		if (formColors == null) {
			formColors = new FormColors(display);
			formColors.markShared();
		}
		return formColors;
	}

	public void startup() throws CoreException {
		super.startup();

		IAdapterManager manager = Platform.getAdapterManager();
		SchemaAdapterFactory schemaFactory = new SchemaAdapterFactory();
		manager.registerAdapters(schemaFactory, ISchemaObject.class);
		manager.registerAdapters(schemaFactory, ISchemaObjectReference.class);
		manager.registerAdapters(new PluginAdapterFactory(), IPluginObject.class);
		manager.registerAdapters(new FeatureAdapterFactory(), IFeatureObject.class);
		PluginsViewAdapterFactory factory = new PluginsViewAdapterFactory();
		manager.registerAdapters(factory, ModelEntry.class);
		manager.registerAdapters(factory, FileAdapter.class);
		// set eclipse home variable if not sets
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.Plugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		this.context = context;
	}
	
	public BundleContext getBundleContext() {
		return this.context;
	}
	
	public void shutdown() throws CoreException {
		if (launchListener!=null)
			launchListener.shutdown();
		if (formColors!=null) {
			formColors.dispose();
			formColors=null;
		}
		if (labelProvider != null) {
			labelProvider.dispose();
			labelProvider = null;
		}
		super.shutdown();
	}

	public static File getFileInPlugin(IPath path) {
		try {
			URL installURL =
				new URL(getDefault().getDescriptor().getInstallURL(), path.toString());
			URL localURL = Platform.asLocalURL(installURL);
			return new File(localURL.getFile());
		} catch (IOException e) {
			return null;
		}
	}

	public PDELabelProvider getLabelProvider() {
		if (labelProvider==null)
			labelProvider = new PDELabelProvider();
		return labelProvider;
	}
	
	public LaunchListener getLaunchesListener() {
		if (launchListener == null)
			launchListener = new LaunchListener();
		return launchListener;
	}
	
	protected void initializeDefaultPreferences(IPreferenceStore store) {
		ColorManager.initializeDefaults(store);
		store.setDefault(P_USE_SOURCE_PAGE, false);
		store.setDefault(PROP_SHOW_OBJECTS, VALUE_USE_IDS);
		store.setDefault(PROP_JAVAC_DEBUG_INFO, true);
		store.setDefault(PROP_JAVAC_FAIL_ON_ERROR, false);
		store.setDefault(PROP_JAVAC_VERBOSE, true);
		store.setDefault(PROP_JAVAC_SOURCE, "1.3");
		store.setDefault(PROP_JAVAC_TARGET, "1.2");
		ExtendedTextEditorPreferenceConstants.initializeDefaultValues(store);
	}
	
	public static boolean isFullNameModeEnabled() {
		IPreferenceStore store = getDefault().getPreferenceStore();
		return store.getString(PROP_SHOW_OBJECTS).equals(VALUE_USE_NAMES);
	}
	
}
