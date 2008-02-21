/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal;

import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;

/**
 * API tooling UI plug-in class.
 * 
 * @since 1.0.0
 */
public class ApiUIPlugin extends AbstractUIPlugin {

	/**
	 * Singleton plug-in
	 */
	private static ApiUIPlugin fgDefault = null;
	
	/**
	 * The id of the plugin
	 */
	public static final String PLUGIN_ID = "org.eclipse.pde.api.tools.ui"; //$NON-NLS-1$
	
	/**
	 * Status code indicating an unexpected internal error.
	 */
	public static final int INTERNAL_ERROR = 120;
	
	/**
	 * Root path to icon directories.
	 */
	private static final String ICONS_PATH = "$nl$/icons/full/"; //$NON-NLS-1$
	
	/**
	 * Relative path to object model icons.
	 */
	private final static String OBJECT= ICONS_PATH + "obj16/"; //basic colors - size 16x16 //$NON-NLS-1$
	
	/**
	 * Constructor
	 */
	public ApiUIPlugin() {
		fgDefault = this;
	}
	
	/**
	 * Returns the singleton API UI Tooling plug-in.
	 * 
	 * @return plug-in
	 */
	public static ApiUIPlugin getDefault() {
		if(fgDefault == null) {
			fgDefault = new ApiUIPlugin();
		}
		return fgDefault;
	}
	
	/**
	 * Returns dialog settings with the given name, creating a new section
	 * if one does not exist.
	 * 
	 * @param name section name
	 * @return dialog settings
	 */
	public IDialogSettings getDialogSettingsSection(String name) {
		IDialogSettings dialogSettings= getDialogSettings();
		IDialogSettings section= dialogSettings.getSection(name);
		if (section == null) {
			section= dialogSettings.addNewSection(name);
		}
		return section;
	}	

	/* (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#initializeImageRegistry(org.eclipse.jface.resource.ImageRegistry)
	 */
	protected void initializeImageRegistry(ImageRegistry reg) {
		declareRegistryImage(reg, IApiToolsConstants.IMG_OBJ_API_COMPONENT, OBJECT + "api_tools.gif"); //$NON-NLS-1$
		declareRegistryImage(reg, IApiToolsConstants.IMG_OBJ_API_SYSTEM_LIBRARY, OBJECT + "library_obj.gif"); //$NON-NLS-1$
		declareRegistryImage(reg, IApiToolsConstants.IMG_OBJ_API_SEARCH, OBJECT + "extract_references.gif"); //$NON-NLS-1$
		declareRegistryImage(reg, IApiToolsConstants.IMG_OBJ_BUNDLE, OBJECT + "plugin_obj.gif"); //$NON-NLS-1$
		declareRegistryImage(reg, IApiToolsConstants.IMG_OBJ_FRAGMENT, OBJECT + "frgmt_obj.gif"); //$NON-NLS-1$
	}

	/**
	 * Logs the specified status with this plug-in's log.
	 * 
	 * @param status status to log
	 */
	public static void log(IStatus status) {
		if (getDefault() == null) {
			Throwable exception = status.getException();
			if (exception != null) {
				exception.printStackTrace();
			}
		} else {
			getDefault().getLog().log(status);
		}
	}
	
	/**
	 * Logs the specified throwable with this plug-in's log.
	 * 
	 * @param t throwable to log 
	 */
	public static void log(Throwable t) {
		log(newErrorStatus("Error logged from API Tools UI: ", t)); //$NON-NLS-1$
	}
	
	/**
	 * Logs an internal error with the specified message.
	 * 
	 * @param message the error message to log
	 */
	public static void logErrorMessage(String message) {
		// this message is intentionally not internationalized, as an exception may
		// be due to the resource bundle itself
		log(newErrorStatus("Internal message logged from API Tools UI: " + message, null)); //$NON-NLS-1$	
	}
	
	/**
	 * Returns a new error status for this plug-in with the given message
	 * @param message the message to be included in the status
	 * @param exception the exception to be included in the status or <code>null</code> if none
	 * @return a new error status
	 */
	public static IStatus newErrorStatus(String message, Throwable exception) {
		return new Status(IStatus.ERROR, getPluginIdentifier(), INTERNAL_ERROR, message, exception);
	}
	
	/**
	 * Returns the currently active workbench window shell or <code>null</code>
	 * if none.
	 * 
	 * @return the currently active workbench window shell or <code>null</code>
	 */
	public static Shell getShell() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window == null) {
			IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
			if (windows.length > 0) {
				return windows[0].getShell();
			}
		} 
		else {
			return window.getShell();
		}
		return null;
	}
	
	/**
	 * @return the id of this plugin.
	 * Value is <code><org.eclipse.pde.api.tools.ui></code>
	 */
	public static String getPluginIdentifier() {
		return PLUGIN_ID;
	}
	
	/**
	 * Declare an Image in the registry table.
	 * @param reg	image registry
	 * @param key 	The key to use when registering the image
	 * @param path	The path where the image can be found. This path is relative to where
	 *				this plug-in class is found (i.e. typically the packages directory)
	 */
	private final static void declareRegistryImage(ImageRegistry reg, String key, String path) {
		ImageDescriptor desc = ImageDescriptor.getMissingImageDescriptor();
		Bundle bundle = Platform.getBundle(IApiToolsConstants.ID_API_TOOLS_UI_PLUGIN);
		URL url = null;
		if (bundle != null){
			url = FileLocator.find(bundle, new Path(path), null);
			desc = ImageDescriptor.createFromURL(url);
		}
		reg.put(key, desc);
	}	
	
	/**
	 * Returns an image from the registry with the given key or <code>null</code> if none.
	 * 
	 * @param key image key
	 * @return image or <code>null</code>
	 */
	public static Image getSharedImage(String key) {
		return getDefault().getImageRegistry().get(key);
	}
	
	/**
	 * Returns an image descriptor from the registry with the given key or <code>null</code> if none.
	 * 
	 * @param key image key
	 * @return image descriptor or <code>null</code>
	 */
	public static ImageDescriptor getImageDescriptor(String key) {
		return getDefault().getImageRegistry().getDescriptor(key);
	}
}
