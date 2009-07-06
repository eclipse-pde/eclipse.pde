/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal;

import org.eclipse.pde.api.tools.internal.provisional.IApiAccess;

/**
 * Default implementation of {@link IApiAccess}
 * 
 * @since 1.0.1
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class ApiAccess implements IApiAccess {

	public static final IApiAccess NORMAL_ACCESS = new NormalAccess();
	
	static class NormalAccess implements IApiAccess {
		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.internal.provisional.IApiAccess#getAccessLevel()
		 */
		public int getAccessLevel() {
			return IApiAccess.NORMAL;
		}
	}
	
	private int access = IApiAccess.NORMAL;
	
	/**
	 * Constructor
	 * @param access
	 */
	public ApiAccess(int access) {
		this.access = access;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiAccess#getAccessLevel()
	 */
	public int getAccessLevel() {
		return this.access;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return this.access;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if(obj instanceof IApiAccess) {
			return this.access == ((IApiAccess)obj).getAccessLevel();
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Access Level: "); //$NON-NLS-1$
		buffer.append(getAccessText(getAccessLevel()));
		return buffer.toString();
	}
	
	/**
	 * Returns a textual representation of an {@link IApiAccess}
	 * 
	 * @param access
	 * @return the textual representation of an {@link IApiAccess}
	 */
	public static String getAccessText(int access) {
		switch(access) {
			case IApiAccess.NORMAL: return "NORMAL"; //$NON-NLS-1$
			case IApiAccess.FRIEND: return "FRIEND"; //$NON-NLS-1$
		}
		return "<UNKNOWN ACCESS LEVEL>"; //$NON-NLS-1$
	}
}
