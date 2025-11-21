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

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.target.NameVersionDescriptor;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageHeader;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageObject;
import org.eclipse.pde.internal.core.text.bundle.ManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.PackageObject;
import org.eclipse.pde.internal.core.util.VersionUtil;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;

/**
 * Resolution to add available matching version for Imported package in MANIFEST
 */
public class VersionMatchImportPackageResolution extends AbstractManifestMarkerResolution {

	public VersionMatchImportPackageResolution(int type, IMarker marker) {
		super(type, marker);
	}

	public String getVersion(Object inputElement) {
		IPluginModelBase[] models = PluginRegistry.getActiveModels();
		Set<NameVersionDescriptor> nameVersions = new HashSet<>();
		for (IPluginModelBase pluginModel : models) {
			BundleDescription desc = pluginModel.getBundleDescription();

			String id = desc == null ? null : desc.getSymbolicName();
			if (id == null) {
				continue;
			}
			ExportPackageDescription[] exported = desc.getExportPackages();
			for (ExportPackageDescription exportedPackage : exported) {
				String name = exportedPackage.getName();
				ManifestHeader mHeader = new ManifestHeader("Export-Package", "", //$NON-NLS-1$//$NON-NLS-2$
						new org.eclipse.pde.internal.core.bundle.Bundle(), "\n"); //$NON-NLS-1$
				PackageObject po = new PackageObject(mHeader, exportedPackage.getName(),
						exportedPackage.getVersion().toString(), "version"); //$NON-NLS-1$

				NameVersionDescriptor nameVersion = new NameVersionDescriptor(exportedPackage.getName(),
						exportedPackage.getVersion().toString(), NameVersionDescriptor.TYPE_PACKAGE);
				exportedPackage.getExporter().getBundle();

				if (("java".equals(name) || name.startsWith("java."))) { //$NON-NLS-1$ //$NON-NLS-2$
					// $NON-NLS-2$
					continue;
				}
				if (nameVersions.add(nameVersion)) {
					if (name.equalsIgnoreCase(inputElement.toString())) {
						return po.getVersion();
					}
				}
			}
		}
		return ""; //$NON-NLS-1$
	}

	@Override
	protected void createChange(BundleModel model) {
		String bundleId = Objects.requireNonNull(marker.getAttribute("bundleId", (String) null)); //$NON-NLS-1$
		Bundle bundle = (Bundle) model.getBundle();
		ImportPackageHeader header = (ImportPackageHeader) bundle.getManifestHeader(Constants.IMPORT_PACKAGE);
		if (header != null) {
			for (ImportPackageObject importPackage : header.getPackages()) {
				if (bundleId.equals(importPackage.getName())) {
					String version = getVersion(bundleId);
					if (!version.isEmpty()) {
						// Sanitize version: Remove a potential qualifier
						String versionRange = VersionUtil.computeInitialRequirementVersionRange(version);
						importPackage.setVersion(versionRange);
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
