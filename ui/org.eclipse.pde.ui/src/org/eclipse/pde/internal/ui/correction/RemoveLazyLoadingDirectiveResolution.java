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

import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class RemoveLazyLoadingDirectiveResolution extends AbstractManifestMarkerResolution {

	private String fHeader = null;

	public RemoveLazyLoadingDirectiveResolution(int type, String currentHeader) {
		super(type);
		fHeader = currentHeader;
	}

	protected void createChange(BundleModel model) {
		model.getBundle().setHeader(fHeader, null);
	}

	public String getLabel() {
		return PDEUIMessages.RemoveLazyLoadingDirectiveResolution_remove;
	}

}
