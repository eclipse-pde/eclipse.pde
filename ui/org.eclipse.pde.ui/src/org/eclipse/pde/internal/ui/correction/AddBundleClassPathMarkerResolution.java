/*******************************************************************************
 *  Copyright (c) 2007, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.text.bundle.*;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;

public class AddBundleClassPathMarkerResolution extends AbstractManifestMarkerResolution {

	private String fValue;

	public AddBundleClassPathMarkerResolution(int type, String value) {
		super(type);
		this.fValue = value;
	}

	public String getLabel() {
		return NLS.bind(PDEUIMessages.AddBundleClassPathResolution_add, fValue);
	}

	protected void createChange(BundleModel model) {
		IBundle bundle = model.getBundle();
		if (bundle instanceof Bundle) {
			BundleClasspathHeader header = (BundleClasspathHeader) bundle.getManifestHeader(Constants.BUNDLE_CLASSPATH);
			if (header != null)
				header.addLibrary(fValue);
			else
				model.getBundle().setHeader(Constants.BUNDLE_CLASSPATH, fValue);
		}
	}

}
