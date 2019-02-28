/*******************************************************************************
 *  Copyright (c) 2005, 2019 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;

public abstract class AbstractManifestMarkerResolution extends AbstractPDEMarkerResolution {

	public AbstractManifestMarkerResolution(int type) {
		super(type);
	}

	public AbstractManifestMarkerResolution(int type, IMarker marker) {
		super(type, marker);
	}

	protected abstract void createChange(BundleModel model);

	@Override
	protected void createChange(IBaseModel model) {
		if (model instanceof IBundlePluginModelBase)
			model = ((IBundlePluginModelBase) model).getBundleModel();
		if (model instanceof BundleModel)
			createChange((BundleModel) model);
	}

}
