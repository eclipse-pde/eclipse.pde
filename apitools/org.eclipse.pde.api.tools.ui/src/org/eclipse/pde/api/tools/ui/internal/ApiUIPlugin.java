/*******************************************************************************
 * Copyright (c) 2007, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Alexander Fedorov <alexander.fedorov@arsysop.ru> - Bug 489181
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.ISession;
import org.eclipse.pde.api.tools.internal.provisional.ISessionListener;
import org.eclipse.pde.api.tools.ui.internal.views.APIToolingView;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * API Tools UI plug-in class.
 *
 * @since 1.0.0
 */
public class ApiUIPlugin extends AbstractUIPlugin {
	/**
	 * Maps Image descriptors to images for composite images
	 */
	private static Map<ImageDescriptor, Image> fCompositeImages = new HashMap<>();

	/**
	 * Singleton plug-in
	 */
	private static ApiUIPlugin fgDefault = null;

	/**
	 * Root path to icon directories.
	 */
	private static final String ICONS_PATH = "$nl$/icons/full/"; //$NON-NLS-1$

	private static final String ELCL = ICONS_PATH + "elcl16/"; //basic colors - size 16x16 //$NON-NLS-1$

	/**
	 * Relative path to object model icons.
	 */
	private final static String OBJECT = ICONS_PATH + "obj16/"; //basic colors - size 16x16 //$NON-NLS-1$
	private final static String OVR = ICONS_PATH + "ovr16/"; //basic colors - size 7x8 //$NON-NLS-1$
	/**
	 * The id of the plugin Value is "org.eclipse.pde.api.tools.ui"
	 */
	public static final String PLUGIN_ID = "org.eclipse.pde.api.tools.ui"; //$NON-NLS-1$
	private final static String WIZBAN = ICONS_PATH + "wizban/"; //basic colors - size 16x16 //$NON-NLS-1$

	private ActionFilterAdapterFactory fActionFilterAdapterFactory;

	/**
	 * Declare an Image in the registry table.
	 *
	 * @param reg image registry
	 * @param key The key to use when registering the image
	 * @param path The path where the image can be found. This path is relative
	 *            to where this plug-in class is found (i.e. typically the
	 *            packages directory)
	 */
	private final static void declareRegistryImage(ImageRegistry reg, String key, String path) {
		ImageDescriptor desc = ImageDescriptor.getMissingImageDescriptor();
		Bundle bundle = Platform.getBundle(IApiToolsConstants.ID_API_TOOLS_UI_PLUGIN);
		URL url = null;
		if (bundle != null) {
			url = FileLocator.find(bundle, IPath.fromOSString(path), null);
			desc = ImageDescriptor.createFromURL(url);
		}
		reg.put(key, desc);
	}

	/**
	 * Returns the singleton API Tools UI plug-in.
	 *
	 * @return plug-in
	 */
	public static ApiUIPlugin getDefault() {
		if (fgDefault == null) {
			fgDefault = new ApiUIPlugin();
		}
		return fgDefault;
	}

	/**
	 * Returns the image associated with the given image descriptor.
	 *
	 * @param descriptor the image descriptor for which there is a managed image
	 * @return the image associated with the image descriptor or
	 *         <code>null</code> if the image descriptor can't create the
	 *         requested image.
	 */
	public static Image getImage(ImageDescriptor descriptor) {
		ImageDescriptor ldesc = descriptor;
		if (ldesc == null) {
			ldesc = ImageDescriptor.getMissingImageDescriptor();
		}
		Image result = fCompositeImages.get(ldesc);
		if (result != null) {
			return result;
		}
		result = ldesc.createImage();
		if (result != null) {
			fCompositeImages.put(ldesc, result);
		}
		return result;
	}

	/**
	 * Returns an image descriptor from the registry with the given key or
	 * <code>null</code> if none.
	 *
	 * @param key image key
	 * @return image descriptor or <code>null</code>
	 */
	public static ImageDescriptor getImageDescriptor(String key) {
		return getDefault().getImageRegistry().getDescriptor(key);
	}

	/**
	 * @return the id of this plugin. Value is
	 *         {@code org.eclipse.pde.api.tools.ui}
	 */
	public static String getPluginIdentifier() {
		return PLUGIN_ID;
	}

	/**
	 * Returns an image from the registry with the given key or
	 * <code>null</code> if none.
	 *
	 * @param key image key
	 * @return image or <code>null</code>
	 */
	public static Image getSharedImage(String key) {
		return getDefault().getImageRegistry().get(key);
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
		} else {
			return window.getShell();
		}
		return null;
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
		// this message is intentionally not internationalized, as an exception
		// may
		// be due to the resource bundle itself
		log(newErrorStatus("Internal message logged from API Tools UI: " + message, null)); //$NON-NLS-1$
	}

	/**
	 * Returns a new error status for this plug-in with the given message
	 *
	 * @param message the message to be included in the status
	 * @param exception the exception to be included in the status or
	 *            <code>null</code> if none
	 * @return a new error status
	 */
	public static IStatus newErrorStatus(String message, Throwable exception) {
		return Status.error(message, exception);
	}

	private final ISessionListener sessionListener = new ISessionListener() {
		@Override
		public void sessionAdded(ISession addedSession) {
			Display.getDefault().asyncExec(() -> showAPIToolingView());
		}

		@Override
		public void sessionRemoved(ISession removedSession) {
		}

		@Override
		public void sessionActivated(ISession session) {
		}
	};

	/**
	 * Constructor
	 */
	public ApiUIPlugin() {
		fgDefault = this;
	}

	/**
	 * Returns dialog settings with the given name, creating a new section if
	 * one does not exist.
	 *
	 * @param name section name
	 * @return dialog settings
	 */
	public IDialogSettings getDialogSettingsSection(String name) {
		IDialogSettings dialogSettings = getDialogSettings();
		IDialogSettings section = dialogSettings.getSection(name);
		if (section == null) {
			section = dialogSettings.addNewSection(name);
		}
		return section;
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
		// model objects
		declareRegistryImage(reg, IApiToolsConstants.IMG_OBJ_API_COMPONENT, OBJECT + "api_tools.svg"); //$NON-NLS-1$
		declareRegistryImage(reg, IApiToolsConstants.IMG_OBJ_API_SYSTEM_LIBRARY, OBJECT + "library_obj.svg"); //$NON-NLS-1$
		declareRegistryImage(reg, IApiToolsConstants.IMG_OBJ_API_SEARCH, OBJECT + "extract_references.svg"); //$NON-NLS-1$
		declareRegistryImage(reg, IApiToolsConstants.IMG_OBJ_BUNDLE, OBJECT + "plugin_obj.svg"); //$NON-NLS-1$
		declareRegistryImage(reg, IApiToolsConstants.IMG_OBJ_FRAGMENT, OBJECT + "frgmt_obj.svg"); //$NON-NLS-1$
		declareRegistryImage(reg, IApiToolsConstants.IMG_OBJ_ECLIPSE_PROFILE, OBJECT + "eclipse16.svg"); //$NON-NLS-1$
		declareRegistryImage(reg, IApiToolsConstants.IMG_OBJ_BUNDLE_VERSION, OBJECT + "bundleversion.svg"); //$NON-NLS-1$
		declareRegistryImage(reg, IApiToolsConstants.IMG_OBJ_CHANGE_CORRECTION, OBJECT + "correction_change.svg"); //$NON-NLS-1$
		declareRegistryImage(reg, IApiToolsConstants.IMG_ELCL_SETUP_APITOOLS, OBJECT + "category_menu.svg"); //$NON-NLS-1$

		// overlays
		declareRegistryImage(reg, IApiToolsConstants.IMG_OVR_ERROR, OVR + "error_ovr.svg"); //$NON-NLS-1$
		declareRegistryImage(reg, IApiToolsConstants.IMG_OVR_WARNING, OVR + "warning_ovr.svg"); //$NON-NLS-1$
		declareRegistryImage(reg, IApiToolsConstants.IMG_OVR_SUCCESS, OVR + "success_ovr.svg"); //$NON-NLS-1$
		// wizards
		declareRegistryImage(reg, IApiToolsConstants.IMG_WIZBAN_PROFILE, WIZBAN + "profile_wiz.svg"); //$NON-NLS-1$
		declareRegistryImage(reg, IApiToolsConstants.IMG_WIZBAN_COMPARE_TO_BASELINE, WIZBAN + "compare_wiz.svg"); //$NON-NLS-1$
		// enabled images
		declareRegistryImage(reg, IApiToolsConstants.IMG_ELCL_FILTER, ELCL + "filter_ps.svg"); //$NON-NLS-1$
		declareRegistryImage(reg, IApiToolsConstants.IMG_ELCL_REMOVE, ELCL + "remove_exc.svg"); //$NON-NLS-1$
		declareRegistryImage(reg, IApiToolsConstants.IMG_ELCL_OPEN_PAGE, ELCL + "open_page.svg"); //$NON-NLS-1$
		declareRegistryImage(reg, IApiToolsConstants.IMG_ELCL_HELP_PAGE, ELCL + "help.svg"); //$NON-NLS-1$
		declareRegistryImage(reg, IApiToolsConstants.IMG_ELCL_COMPARE_APIS, ELCL + "compare_apis.svg"); //$NON-NLS-1$
		declareRegistryImage(reg, IApiToolsConstants.IMG_ELCL_CONFIG_SEV, ELCL + "configure_problem_severity.svg"); //$NON-NLS-1$
		declareRegistryImage(reg, IApiToolsConstants.IMG_ELCL_EXPORT, ELCL + "export.svg"); //$NON-NLS-1$
		declareRegistryImage(reg, IApiToolsConstants.IMG_ELCL_NEXT_NAV, ELCL + "next_nav.svg"); //$NON-NLS-1$
		declareRegistryImage(reg, IApiToolsConstants.IMG_ELCL_PREV_NAV, ELCL + "prev_nav.svg"); //$NON-NLS-1$
		declareRegistryImage(reg, IApiToolsConstants.IMG_ELCL_EXPANDALL, ELCL + "expandall.svg"); //$NON-NLS-1$
		declareRegistryImage(reg, IApiToolsConstants.IMG_ELCL_TEXT_EDIT, ELCL + "text_edit.svg"); //$NON-NLS-1$
	}

	void showAPIToolingView() {
		showView(APIToolingView.ID);
	}

	public void showPropertiesView() {
		showView("org.eclipse.ui.views.PropertySheet"); //$NON-NLS-1$
	}

	private void showView(String id) {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window == null) {
			return;
		}
		IWorkbenchPage page = window.getActivePage();
		if (page != null) {
			try {
				IViewPart view = page.showView(id);
				page.bringToTop(view);
			} catch (PartInitException e) {
				log(e);
			}
		}
	}

	@Override
	public void start(BundleContext context) throws Exception {
		ApiPlugin.getDefault().getSessionManager().addSessionListener(this.sessionListener);
		fActionFilterAdapterFactory = new ActionFilterAdapterFactory();
		Platform.getAdapterManager().registerAdapters(fActionFilterAdapterFactory, IJavaElement.class);
		super.start(context);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		// dispose composite images
		for (Image image : fCompositeImages.values()) {
			image.dispose();
		}
		fCompositeImages.clear();
		ApiPlugin.getDefault().getSessionManager().removeSessionListener(this.sessionListener);
		Platform.getAdapterManager().unregisterAdapters(fActionFilterAdapterFactory, IJavaElement.class);
		super.stop(context);
	}
}
