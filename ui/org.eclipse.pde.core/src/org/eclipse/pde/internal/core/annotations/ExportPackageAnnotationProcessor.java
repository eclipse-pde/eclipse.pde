/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.annotations;

import java.util.Optional;

import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.ExportPackageHeader;
import org.eclipse.pde.internal.core.text.bundle.ExportPackageObject;
import org.osgi.framework.Constants;

/**
 * Processes {@value OSGiAnnotations#ANNOTATION_BUNDLE_EXPORT} and
 * {@value OSGiAnnotations#ANNOTATION_VERSIONING_VERSION} annotations.
 */
public class ExportPackageAnnotationProcessor implements OSGiAnnotationProcessor {

	private boolean exportPackage = false;
	private Optional<String> version = Optional.empty();
	private final String packageName;

	public ExportPackageAnnotationProcessor(String packageName) {
		this.packageName = packageName;
	}

	@Override
	public void processAnnotation(Annotation annotation, String type) {
		exportPackage |= OSGiAnnotations.ANNOTATION_BUNDLE_EXPORT.equals(type);
		if (OSGiAnnotations.ANNOTATION_VERSIONING_VERSION.equals(type)) {
			version = OSGiAnnotationProcessor.value(annotation).flatMap(OSGiAnnotationProcessor::stringValue);
		}
	}

	@Override
	public void apply(IBaseModel model) {
		if (exportPackage) {
			ExportPackageObject packageObject = getExportPackage(model, packageName);
			if (packageObject != null) {
				packageObject.setVersion(version.orElse(null));
			}
		}
	}

	private static ExportPackageObject getExportPackage(IBaseModel model, String packageName) {
		if (model instanceof IBundlePluginModelBase) {
			IBundlePluginModelBase pluginModel = (IBundlePluginModelBase) model;
			IBundleModel bundleModel = pluginModel.getBundleModel();
			IBundle bundle = bundleModel.getBundle();
			IManifestHeader header = bundle.getManifestHeader(Constants.EXPORT_PACKAGE);
			if (header == null) {
				bundle.setHeader(Constants.EXPORT_PACKAGE, packageName);
				header = bundle.getManifestHeader(Constants.EXPORT_PACKAGE);
			}
			if (header instanceof ExportPackageHeader) {
				ExportPackageHeader exportPackageHeader = (ExportPackageHeader) header;
				ExportPackageObject packageObject = exportPackageHeader.getPackage(packageName);
				if (packageObject == null) {
					return exportPackageHeader.addPackage(packageName);
				}
				return packageObject;
			}
		}
		return null;
	}

}
