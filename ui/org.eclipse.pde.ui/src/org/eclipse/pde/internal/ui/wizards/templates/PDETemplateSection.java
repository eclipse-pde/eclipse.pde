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

import org.eclipse.pde.ui.templates.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import java.net.*;
import java.util.*;

public abstract class PDETemplateSection extends OptionTemplateSection {

	protected ResourceBundle getPluginResourceBundle() {
		return PDEPlugin.getDefault().getDescriptor().getResourceBundle();
	}
	
	protected URL getInstallURL() {
		return PDEPlugin.getDefault().getDescriptor().getInstallURL();
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
