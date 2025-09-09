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
import java.util.Optional;

import org.eclipse.core.resources.IMarker;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageHeader;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageObject;
import org.eclipse.pde.internal.core.util.VersionUtil;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

/**
 * Resolution to add available matching version for Imported package in MANIFEST
 */
public class VersionMatchImportPackageResolution extends AbstractManifestMarkerResolution {

	public VersionMatchImportPackageResolution(int type, IMarker marker) {
		super(type, marker);
	}

	public Version getVersion(Object inputElement) {
		IPluginModelBase[] models = PluginRegistry.getActiveModels();
		for (IPluginModelBase pluginModel : models) {
			BundleDescription desc = pluginModel.getBundleDescription();

			String id = desc == null ? null : desc.getSymbolicName();
			if (id == null) {
				continue;
			}
			ExportPackageDescription[] exported = desc.getExportPackages();
			for (ExportPackageDescription exportedPackage : exported) {
				String name = exportedPackage.getName();
				if (("java".equals(name) || name.startsWith("java."))) { //$NON-NLS-1$ //$NON-NLS-2$
					// $NON-NLS-2$
					continue;
				}
				if (name.equalsIgnoreCase(inputElement.toString())) {
					return exportedPackage.getVersion();
				}
			}
		}
		return null;
	}

	@Override
	protected void createChange(BundleModel model) {
		String bundleId = Objects.requireNonNull(marker.getAttribute("bundleId", (String) null)); //$NON-NLS-1$
		Bundle bundle = (Bundle) model.getBundle();
		ImportPackageHeader header = (ImportPackageHeader) bundle.getManifestHeader(Constants.IMPORT_PACKAGE);
		if (header != null) {
			for (ImportPackageObject importPackage : header.getPackages()) {
				if (bundleId.equals(importPackage.getName())) {
					Version version = getVersion(bundleId);
					if (version != null) {
						// Sanitize version: Remove a potential qualifier
						Optional<VersionRange> versionRange = VersionUtil.createConsumerRequirementRange(version);
						importPackage.setVersion(versionRange.map(VersionRange::toString).orElse(null));
					}
				}
			}
		}
	}

	@Override
	public String getLabel() {
		return PDEUIMessages.AddMatchingVersion;
	}


}
