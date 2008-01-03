/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Bartosz Michalik <bartosz.michalik@gmail.com> - bug 214156
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class RemoveLazyLoadingDirectiveResolution extends AbstractManifestMarkerResolution {

	public RemoveLazyLoadingDirectiveResolution(int type) {
		super(type);
	}

	protected void createChange(BundleModel model) {
		Bundle bundle = (Bundle) model.getBundle();
		IManifestHeader header = bundle.getManifestHeader(ICoreConstants.ECLIPSE_LAZYSTART);
		if (header != null)
			bundle.setHeader(ICoreConstants.ECLIPSE_LAZYSTART, null);

	}

	public String getLabel() {
		return PDEUIMessages.RemoveLazyLoadingDirectiveResolution_remove;
	}

}
