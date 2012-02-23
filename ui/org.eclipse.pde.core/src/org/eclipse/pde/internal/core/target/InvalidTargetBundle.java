/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.pde.core.target.TargetBundle;

/**
 * Target bundle representing a problem with content in a target. Uses
 * the status codes found on {@link TargetBundle}.
 */
public class InvalidTargetBundle extends TargetBundle {

	private IStatus fStatus;

	/**
	 * Creates a new target bundle with the given status and additional bundle information
	 *  
	 * @param bundleInfo bundle info object containing information about the target content if available (symbolic name, version, location)
	 * @param status status describing the problem with this content
	 */
	public InvalidTargetBundle(BundleInfo bundleInfo, IStatus status) {
		fInfo = bundleInfo;
		fStatus = status;
	}

	public IStatus getStatus() {
		return fStatus;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof InvalidTargetBundle) {
			if (fInfo != null && fInfo.equals(((InvalidTargetBundle) obj).fInfo)) {
				return true;
			}
		}
		return super.equals(obj);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		if (fInfo != null) {
			return fInfo.hashCode();
		}
		return super.hashCode();
	}

}
