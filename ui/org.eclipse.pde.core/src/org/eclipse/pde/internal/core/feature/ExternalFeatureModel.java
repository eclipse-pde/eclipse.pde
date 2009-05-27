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
package org.eclipse.pde.internal.core.feature;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.NLResourceHelper;

public class ExternalFeatureModel extends AbstractFeatureModel {
	private static final long serialVersionUID = 1L;
	private String location;

	/**
	 * @see AbstractModel#updateTimeStamp()
	 */
	protected void updateTimeStamp() {
	}

	/**
	 * @see IModel#isInSync()
	 */
	public boolean isInSync() {
		return true;
	}

	public boolean isEditable() {
		return false;
	}

	/**
	 * @see IModel#load()
	 */
	public void load() throws CoreException {
	}

	public void setInstallLocation(String location) {
		this.location = location;
	}

	public String getInstallLocation() {
		return location;
	}

	protected NLResourceHelper createNLResourceHelper() {
		if (location != null) {
			URL url = getNLLookupLocation();
			return new NLResourceHelper("feature", new URL[] {url}); //$NON-NLS-1$
		}
		return null;
	}

	public URL getNLLookupLocation() {
		try {
			if (location != null && new File(location).isDirectory() && !location.endsWith("/")) //$NON-NLS-1$
				return new URL("file:" + location + "/"); //$NON-NLS-1$ //$NON-NLS-2$
			return new URL("file:" + location); //$NON-NLS-1$
		} catch (MalformedURLException e) {
			return null;
		}
	}

}
