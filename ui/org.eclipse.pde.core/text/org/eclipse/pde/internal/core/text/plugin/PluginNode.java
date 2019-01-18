/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text.plugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.IPlugin;

public class PluginNode extends PluginBaseNode implements IPlugin {

	private static final long serialVersionUID = 1L;

	@Override
	public String getClassName() {
		return getXMLAttributeValue(P_CLASS_NAME);
	}

	@Override
	public void setClassName(String className) throws CoreException {
		setXMLAttribute(P_CLASS_NAME, className);
	}

	@Override
	protected String[] getSpecificAttributes() {
		String classname = getClassName();
		if (classname != null && classname.trim().length() > 0) {
			return new String[] {"   " + P_CLASS_NAME + "=\"" + classname + "\""}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		return new String[0];
	}

	public boolean hasExtensibleAPI() {
		return false;
	}

}
