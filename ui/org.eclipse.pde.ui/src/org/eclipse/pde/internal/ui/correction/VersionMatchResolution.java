/*******************************************************************************
 *  Copyright (c) 2025, 2025 IBM Corporation and others.
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

import java.util.Objects;

import org.eclipse.core.resources.IMarker;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.bundle.RequireBundleHeader;
import org.eclipse.pde.internal.core.text.bundle.RequireBundleObject;
import org.eclipse.pde.internal.core.util.VersionUtil;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;

/**
 * Resolution to add available matching version for Required bundles in MANIFEST
 */
public class VersionMatchResolution extends AbstractManifestMarkerResolution {
	public VersionMatchResolution(int type, IMarker marker) {
		super(type, marker);
	}

	@Override
	protected void createChange(BundleModel model) {
		String bundleId = Objects.requireNonNull(marker.getAttribute("bundleId", (String) null)); //$NON-NLS-1$
		Bundle bundle = (Bundle) model.getBundle();
		RequireBundleHeader header = (RequireBundleHeader) bundle.getManifestHeader(Constants.REQUIRE_BUNDLE);
		if (header != null) {
			for (RequireBundleObject requiredBundle : header.getRequiredBundles()) {
				if (bundleId.equals(requiredBundle.getId())) {
					IPluginModelBase modelBase = PluginRegistry.findModel(bundleId);
					if (modelBase != null) {
						String version = modelBase.getPluginBase().getVersion();
						// Sanitize version: Remove a potential qualifier
						version = VersionUtil.computeInitialPluginVersion(version);
						requiredBundle.setVersion(version);
					}
				}
			}
		}
	}

	@Override
	public String getLabel() {
		return PDEUIMessages.AddMatchingVersion_RequireBundle;
	}

}
