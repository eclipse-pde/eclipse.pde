/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.wizards.templates;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.ui.templates.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.osgi.framework.*;

import java.io.*;
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
			URL url = getInstallURL();
			url = Platform.asLocalURL(url);
			File dir = new File(url.getFile());
			String[] candidates = getDirectoryCandidates();
			for (int i = 0; i < candidates.length; i++) {
				if (new File(dir, candidates[i]).exists())
					return new URL(url, candidates[i]);
			}
		} catch (MalformedURLException e) {
		} catch (IOException e) {
		}
		return null;
	}
	
	private String[] getDirectoryCandidates() {
		String version = model.getPluginBase().getTargetVersion();
		if ("3.0".equals(version)) //$NON-NLS-1$
			return new String[] {"templates_3.0" + File.separator + getSectionId()}; //$NON-NLS-1$
		if ("3.1".equals(version)) //$NON-NLS-1$
			return new String[] {"templates_3.1" + File.separator + getSectionId(), //$NON-NLS-1$
								 "templates_3.0" + File.separator + getSectionId()};	 //$NON-NLS-1$
		return new String[] {"templates" + File.separator + getSectionId()};  //$NON-NLS-1$
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
