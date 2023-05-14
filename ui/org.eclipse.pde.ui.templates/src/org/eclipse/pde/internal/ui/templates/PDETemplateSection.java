/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 461877, 473694
 *******************************************************************************/
package org.eclipse.pde.internal.ui.templates;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.ResourceBundle;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.pde.core.plugin.IPluginReference;
import org.eclipse.pde.ui.templates.OptionTemplateSection;
import org.eclipse.pde.ui.templates.PluginReference;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public abstract class PDETemplateSection extends OptionTemplateSection {

	public static final String KEY_PRODUCT_BRANDING = "productBranding"; //$NON-NLS-1$
	public static final String KEY_PRODUCT_NAME = "productName"; //$NON-NLS-1$

	public static final String VALUE_PRODUCT_ID = "product"; //$NON-NLS-1$
	public static final String VALUE_PRODUCT_NAME = "RCP Product"; //$NON-NLS-1$
	public static final String VALUE_PERSPECTIVE_NAME = "RCP Perspective"; //$NON-NLS-1$
	public static final String VALUE_APPLICATION_ID = "application"; //$NON-NLS-1$

	private Bundle bundle = FrameworkUtil.getBundle(PDETemplateSection.class);

	@Override
	protected ResourceBundle getPluginResourceBundle() {
		return Platform.getResourceBundle(bundle);
	}

	@Override
	protected URL getInstallURL() {
		return bundle.getEntry("/"); //$NON-NLS-1$
	}

	@Override
	public URL getTemplateLocation() {
		try {
			String[] candidates = getDirectoryCandidates();
			for (String candidate : candidates) {
				if (bundle.getEntry(candidate) != null) {
					return new URL(getInstallURL(), candidate);
				}
			}
		} catch (MalformedURLException e) { // do nothing
		}
		return null;
	}

	private String[] getDirectoryCandidates() {
		double version = getTargetVersion();
		ArrayList<String> result = new ArrayList<>();
		if (version >= 3.5)
			result.add("templates_3.5" + "/" + getSectionId() + "/"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (version >= 3.4)
			result.add("templates_3.4" + "/" + getSectionId() + "/"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (version >= 3.3)
			result.add("templates_3.3" + "/" + getSectionId() + "/"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (version >= 3.2)
			result.add("templates_3.2" + "/" + getSectionId() + "/"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (version >= 3.1)
			result.add("templates_3.1" + "/" + getSectionId() + "/"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return result.toArray(new String[result.size()]);
	}

	@Override
	public String[] getNewFiles() {
		return new String[0];
	}

	protected String getFormattedPackageName(String id) {
		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < id.length(); i++) {
			char ch = id.charAt(i);
			if (buffer.length() == 0) {
				if (Character.isJavaIdentifierStart(ch))
					buffer.append(Character.toLowerCase(ch));
			} else {
				if (Character.isJavaIdentifierPart(ch) || ch == '.')
					buffer.append(ch);
			}
		}
		return buffer.toString().toLowerCase(Locale.ENGLISH);
	}

	@Override
	protected void generateFiles(IProgressMonitor monitor) throws CoreException {
		super.generateFiles(monitor);
		// Copy the default splash screen if the branding option is selected
		if (copyBrandingDirectory()) {
			super.generateFiles(monitor, bundle.getEntry("branding/")); //$NON-NLS-1$
		}
	}

	protected boolean copyBrandingDirectory() {
		return getBooleanOption(KEY_PRODUCT_BRANDING);
	}

	protected void createBrandingOptions() {
		addOption(KEY_PRODUCT_BRANDING, PDETemplateMessages.HelloRCPTemplate_productBranding, false, 0);
	}

	protected IPluginReference[] getRCP3xDependencies() {
		IPluginReference[] dep = new IPluginReference[4];
		dep[0] = new PluginReference("org.eclipse.core.runtime"); //$NON-NLS-1$
		dep[1] = new PluginReference("org.eclipse.ui"); //$NON-NLS-1$
		dep[2] = new PluginReference("org.apache.felix.scr"); //$NON-NLS-1$
		dep[3] = new PluginReference("org.eclipse.equinox.event"); //$NON-NLS-1$
		return dep;
	}

}
