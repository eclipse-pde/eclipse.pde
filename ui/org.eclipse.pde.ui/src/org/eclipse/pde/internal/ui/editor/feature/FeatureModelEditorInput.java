/*******************************************************************************
 *  Copyright (c) 2021 Christoph Läubrich and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

final class FeatureModelEditorInput implements IEditorInput {

	private final IFeatureModel model;

	public FeatureModelEditorInput(IFeatureModel model) {
		this.model = model;
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IFeatureModel.class) {
			return adapter.cast(model);
		}
		return null;
	}

	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	@Override
	public String getName() {
		return model.getFeature().getId();
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return null;
	}

}
