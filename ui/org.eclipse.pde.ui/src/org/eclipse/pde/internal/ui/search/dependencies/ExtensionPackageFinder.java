/*******************************************************************************
 *  Copyright (c) 2026 Vector Informatik GmbH and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search.dependencies;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.IMetaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.plugin.ExternalPluginModelBase;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.schema.ExtensionInspector;
import org.eclipse.pde.internal.ui.PDEPlugin;

/**
 * Finds the packages of Java types that a bundle references in the extensions
 * of its plugin.xml/fragment.xml. The extension registry instantiates them
 * reflectively, so no compiled class refers to them.
 * <p>
 * Only attributes that the extension point's schema declares to be of the kind
 * java are considered; extensions of points without a schema are skipped. The
 * package is everything before the last dot of the binary type name.
 */
public final class ExtensionPackageFinder {

	private ExtensionPackageFinder() { // static utility
	}

	/**
	 * Returns the packages of all types that the extensions of the given model
	 * reference in its plugin.xml/fragment.xml.
	 */
	public static Set<String> findPackagesInExtensions(IPluginModelBase model) {
		IResource resource = model.getUnderlyingResource();
		if (resource == null) {
			return Set.of();
		}
		IPluginModelBase extensionsModel = loadExtensionsModel(model, resource.getProject());
		if (extensionsModel == null) {
			return Set.of();
		}
		return Arrays.stream(extensionsModel.getPluginBase().getExtensions())
				.flatMap(ExtensionPackageFinder::findPackages).collect(Collectors.toSet());
	}

	/**
	 * Loads the extensions from the file into an external model, because a
	 * workspace model takes them from the extension registry, which omits
	 * extensions of unresolvable points (see PluginBase#processChild).
	 */
	private static IPluginModelBase loadExtensionsModel(IPluginModelBase model, IProject project) {
		boolean isFragment = model instanceof IFragmentModel;
		IFile file = isFragment ? PDEProject.getFragmentXml(project) : PDEProject.getPluginXml(project);
		if (!file.exists()) {
			return null;
		}
		IPath location = project.getLocation();
		String installLocation = location != null ? location.toOSString() : null;
		ExternalPluginModelBase extensionsModel;
		try (InputStream stream = new BufferedInputStream(file.getContents(true))) {
			extensionsModel = ExternalPluginModelBase.load(stream, isFragment, installLocation);
		} catch (CoreException | IOException e) {
			// otherwise dependencies needed by extensions are silently reported as unused
			PDEPlugin.logException(e);
			return null;
		}
		return extensionsModel.isLoaded() ? extensionsModel : null;
	}

	private static Stream<String> findPackages(IPluginExtension extension) {
		ISchema schema = PDECore.getDefault().getSchemaRegistry().getSchema(extension.getPoint());
		return schema != null
				? new ExtensionInspector(schema).findAttributes(extension, IMetaAttribute.JAVA)
						.flatMap(attribute -> findPackageOfType(attribute.getValue()).stream())
				: Stream.empty();
	}

	/**
	 * Returns the package of the referenced type, or none for the default
	 * package, which no dependency can provide.
	 */
	private static Optional<String> findPackageOfType(String value) {
		if (value == null) {
			return Optional.empty();
		}
		// the value may have the form typeName:initializationData
		String typeName = value.trim();
		int initializationDataIndex = typeName.indexOf(':');
		if (initializationDataIndex != -1) {
			typeName = typeName.substring(0, initializationDataIndex).trim();
		}
		int packageSeparatorIndex = typeName.lastIndexOf('.');
		return packageSeparatorIndex > 0 ? Optional.of(typeName.substring(0, packageSeparatorIndex))
				: Optional.empty();
	}

}
