/*******************************************************************************
 * Copyright (c) 2026 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;

/**
 * Resolution to add (or fix) the <code>Multi-Release: true</code> header in
 * the manifest, required when the project has release-specific source
 * folders (see {@link org.eclipse.jdt.core.IClasspathAttribute#RELEASE}) on
 * its classpath.
 */
public class AddMultiReleaseHeaderResolution extends AbstractManifestMarkerResolution {

	public AddMultiReleaseHeaderResolution(int type, IMarker marker) {
		super(type, marker);
	}

	@Override
	public String getLabel() {
		return PDEUIMessages.AddMultiReleaseHeaderResolution_label;
	}

	@Override
	public String getDescription() {
		return PDEUIMessages.AddMultiReleaseHeaderResolution_description;
	}

	@Override
	protected void createChange(BundleModel model) {
		model.getBundle().setHeader(ICoreConstants.MULTI_RELEASE, String.valueOf(true));
	}

}
