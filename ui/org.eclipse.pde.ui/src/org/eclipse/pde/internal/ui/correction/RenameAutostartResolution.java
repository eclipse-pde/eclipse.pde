/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class RenameAutostartResolution extends ManifestHeaderErrorResolution {

	public RenameAutostartResolution(int type) {
		super(type);
	}

	public String getDescription() {
		return PDEUIMessages.RenameAutostartResolution_desc;
	}

	public String getLabel() {
		return PDEUIMessages.RenameAutostartResolution_label;
	}

	protected void createChange(BundleModel model) {
		model.getBundle().renameHeader(ICoreConstants.ECLIPSE_AUTOSTART, ICoreConstants.ECLIPSE_LAZYSTART);
	}
	
}
