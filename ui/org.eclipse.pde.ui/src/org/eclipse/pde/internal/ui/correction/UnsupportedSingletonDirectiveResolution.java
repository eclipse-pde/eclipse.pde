/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.*;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;

public class UnsupportedSingletonDirectiveResolution extends AbstractManifestMarkerResolution {

	public UnsupportedSingletonDirectiveResolution(int type) {
		super(type);
	}

	public String getDescription() {
		return PDEUIMessages.RevertUnsupportSingletonResolution_desc;
	}

	public String getLabel() {
		return PDEUIMessages.RevertUnsupportSingletonResolution_desc;
	}

	protected void createChange(BundleModel model) {
		IBundle bundle = model.getBundle();
		if (bundle instanceof Bundle) {
			Bundle bun = (Bundle) bundle;
			IManifestHeader header = bun.getManifestHeader(Constants.BUNDLE_SYMBOLICNAME);
			if (header instanceof BundleSymbolicNameHeader) {
				((BundleSymbolicNameHeader) header).fixUnsupportedDirective();
			}
		}
	}
}
