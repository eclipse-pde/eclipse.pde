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
package org.eclipse.pde.internal.core;

import org.eclipse.core.runtime.*;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 */
public class SourceLocation {
	private IPath path;
	private boolean userDefined = true;
	private boolean enabled;

	public SourceLocation(IPath path, boolean enabled) {
		this.path = path;
		this.enabled = enabled;
	}

	public IPath getPath() {
		return path;
	}
	
	public void setPath(IPath path) {
		this.path = path;
	}

	public boolean isUserDefined() {
		return userDefined;
	}

	public void setUserDefined(boolean userDefined) {
		this.userDefined = userDefined;
	}
	
	public String toString() {
		return path.toOSString();
	}
	
	/**
	 * Gets the enabled.
	 * @return Returns a boolean
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Sets the enabled.
	 * @param enabled The enabled to set
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof SourceLocation) {
			SourceLocation object = (SourceLocation)obj;
			return object.getPath().equals(path);
		}
		return false;
	}

}