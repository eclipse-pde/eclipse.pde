/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import org.eclipse.core.runtime.IPath;

public class SourceLocation {
	private IPath path;
	private boolean userDefined = true;

	public SourceLocation(IPath path) {
		this.path = path;
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

	public boolean equals(Object obj) {
		if (obj instanceof SourceLocation) {
			SourceLocation object = (SourceLocation) obj;
			return object.getPath().equals(path);
		}
		return false;
	}

	public int hashCode() {
		return path.hashCode();
	}

}
