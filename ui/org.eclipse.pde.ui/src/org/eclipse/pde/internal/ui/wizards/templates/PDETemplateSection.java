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

package org.eclipse.pde.internal.ui.wizards.templates;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.ui.templates.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.osgi.framework.*;

import java.net.*;
import java.util.*;

public abstract class PDETemplateSection extends OptionTemplateSection {

	protected ResourceBundle getPluginResourceBundle() {
		Bundle bundle = Platform.getBundle(PDEPlugin.getPluginId());
		return Platform.getResourceBundle(bundle);
	}
	
	protected URL getInstallURL() {
		return PDEPlugin.getDefault().getInstallURL();
	}
	
	public URL getTemplateLocation() {
		try {
			String[] candidates = getDirectoryCandidates();
			for (int i = 0; i < candidates.length; i++) {
				if (PDEPlugin.getDefault().getBundle().getEntry(candidates[i]) != null) {
					URL candidate = new URL(getInstallURL(), candidates[i]);
					return candidate;
				}
			}
		} catch (MalformedURLException e) {
		}
		return null;
	}

	private String[] getDirectoryCandidates() {
		String version = model.getPluginBase().getTargetVersion();
		if ("3.0".equals(version)) //$NON-NLS-1$
			return new String[] { "templates_3.0" + "/" + getSectionId() + "/" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if ("3.1".equals(version)) //$NON-NLS-1$
			return new String[] { "templates_3.1" + "/" + getSectionId() + "/", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					"templates_3.0" + "/" + getSectionId() + "/" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return new String[] { "templates" + "/" + getSectionId() + "/" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.ITemplateSection#getFoldersToInclude()
	 */
	public String[] getNewFiles() {
		return new String[0];
	}
	
	protected String getFormattedPackageName(String id){
		StringBuffer buffer = new StringBuffer();
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
		return buffer.toString();
	}
}
