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
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginParent;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.IMetaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.plugin.ExternalFragmentModel;
import org.eclipse.pde.internal.core.plugin.ExternalPluginModel;
import org.eclipse.pde.internal.core.plugin.ExternalPluginModelBase;
import org.eclipse.pde.internal.core.project.PDEProject;
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

	private static final String CLASS_ATTRIBUTE = "class"; //$NON-NLS-1$

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
		ExternalPluginModelBase extensionsModel = isFragment ? new ExternalFragmentModel() : new ExternalPluginModel();
		IPath location = project.getLocation();
		if (location != null) {
			extensionsModel.setInstallLocation(location.toOSString());
		}
		try (InputStream stream = new BufferedInputStream(file.getContents(true))) {
			extensionsModel.load(stream, false);
		} catch (CoreException | IOException e) {
			// otherwise dependencies needed by extensions are silently reported as unused
			PDEPlugin.logException(e);
			return null;
		}
		return extensionsModel.isLoaded() ? extensionsModel : null;
	}

	private static Stream<String> findPackages(IPluginExtension extension) {
		ISchema schema = PDECore.getDefault().getSchemaRegistry().getSchema(extension.getPoint());
		return schema != null ? findPackages(schema, extension) : Stream.empty();
	}

	private static Stream<String> findPackages(ISchema schema, IPluginParent parent) {
		return Arrays.stream(parent.getChildren()).filter(IPluginElement.class::isInstance)
				.map(IPluginElement.class::cast).flatMap(element -> Stream
						.concat(findPackagesOfTypes(schema, element, parent), findPackages(schema, element)));
	}

	private static Stream<String> findPackagesOfTypes(ISchema schema, IPluginElement element, IPluginParent parent) {
		ISchemaElement schemaElement = schema.findElement(element.getName());
		if (schemaElement != null) {
			return Arrays.stream(element.getAttributes())
					.filter(attribute -> isTypeAttribute(schemaElement, attribute.getName()))
					.flatMap(attribute -> findPackageOfType(attribute.getValue()).stream());
		}
		// a type attribute given as nested element, e.g. to pass parameters
		boolean isTypeElement = parent instanceof IPluginElement parentElement
				&& isTypeAttribute(schema.findElement(parentElement.getName()), element.getName());
		IPluginAttribute attribute = isTypeElement ? element.getAttribute(CLASS_ATTRIBUTE) : null;
		return attribute == null ? Stream.empty() : findPackageOfType(attribute.getValue()).stream();
	}

	private static boolean isTypeAttribute(ISchemaElement schemaElement, String attributeName) {
		if (schemaElement == null) {
			return false;
		}
		ISchemaAttribute attribute = schemaElement.getAttribute(attributeName);
		return attribute != null && attribute.getKind() == IMetaAttribute.JAVA;
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
