package org.eclipse.pde.internal;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

public class PDE extends Plugin {
	public static final String PLUGIN_ID = "org.eclipse.pde";

	public static final String MANIFEST_BUILDER_ID =
		PLUGIN_ID + "." + "ManifestBuilder";
	public static final String SCHEMA_BUILDER_ID =
		PLUGIN_ID + "." + "SchemaBuilder";
	public static final String PLUGIN_NATURE = PLUGIN_ID + "." + "PluginNature";
	public static final String FEATURE_NATURE = PLUGIN_ID + "." + "FeatureNature";
	public static final String FEATURE_BUILDER_ID =
		PLUGIN_ID + "." + "FeatureBuilder";

	private static boolean inVAJ;
	static {
		try {
			Class.forName("com.ibm.uvm.lang.ProjectClassLoader");
			inVAJ = true;
		} catch (Exception e) {
			inVAJ = false;
		}
	}

	// Shared instance
	private static PDE inst;
	// Resource bundle
	private ResourceBundle resourceBundle;

	public PDE(IPluginDescriptor descriptor) {
		super(descriptor);
		inst = this;
		try {
			resourceBundle =
				ResourceBundle.getBundle("org.eclipse.pde.internal.pderesources");
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
	}

	public static boolean hasPluginNature(IProject project) {
		// Be flexible with 1.0 IDs - check all combinations
		try {
			return project.hasNature(PLUGIN_NATURE);
		} catch (CoreException e) {
			log(e);
			return false;
		}
	}

	public static PDE getDefault() {
		return inst;
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
		return new Path(inst.getDescriptor().getInstallURL().getFile());
	}
	public static String getPluginId() {
		return inst.getDescriptor().getUniqueIdentifier();
	}
	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}
	public static String getResourceString(String key) {
		ResourceBundle bundle = inst.getResourceBundle();
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

}