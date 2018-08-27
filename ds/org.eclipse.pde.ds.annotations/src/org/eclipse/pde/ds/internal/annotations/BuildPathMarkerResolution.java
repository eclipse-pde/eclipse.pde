/*******************************************************************************
 * Copyright (c) 2015 Ecliptical Software Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Ecliptical Software Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ds.internal.annotations;

import org.eclipse.core.resources.IMarker;
import org.eclipse.pde.internal.ui.util.ModelModification;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution2;

@SuppressWarnings("restriction")
public abstract class BuildPathMarkerResolution implements IMarkerResolution2 {

	private final String label;

	private final String description;

	private final Image image;

	public BuildPathMarkerResolution(String label, String description, Image image) {
		this.label = label;
		this.description = description;
		this.image = image;
	}

	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public Image getImage() {
		return image;
	}

	@Override
	public void run(IMarker marker) {
		PDEModelUtility.modifyModel(createModification(marker), null);
	}

	protected abstract ModelModification createModification(IMarker marker);
}
