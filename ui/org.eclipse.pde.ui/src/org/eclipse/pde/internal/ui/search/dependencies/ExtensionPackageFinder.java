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
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginParent;
import org.eclipse.pde.internal.core.natures.PluginProject;
import org.eclipse.pde.internal.core.plugin.ExternalFragmentModel;
import org.eclipse.pde.internal.core.plugin.ExternalPluginModel;
import org.eclipse.pde.internal.core.plugin.ExternalPluginModelBase;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.ui.PDEPlugin;

/**
 * Finds the packages of Java types that a bundle references in the extensions
 * of its plugin.xml/fragment.xml. Such types are instantiated reflectively by
 * the extension registry at runtime, so the dependency providing them is
 * required even though no compiled class of the bundle refers to it.
 * <p>
 * Every attribute value and element text is considered a potential type
 * reference, and it is one if it resolves to a type on the project's class
 * path, which is how the builder validates the type attributes of extensions as
 * well. The resolved type also yields the package to retain, so that neither
 * the notation of the type name nor any naming convention has to be
 * interpreted. A value that is not meant to be a type but happens to name one,
 * for example an identifier that repeats a class name, thus retains a
 * dependency as well. Projects without Java nature have no class path to
 * resolve against and are not analyzed.
 * <p>
 * The extensions are read from the file, so references that are only present in
 * an unsaved editor are not seen.
 */
public final class ExtensionPackageFinder {

	private final IJavaProject fJavaProject;

	private ExtensionPackageFinder(IJavaProject javaProject) {
		fJavaProject = javaProject;
	}

	/**
	 * Returns the packages of all types that the extensions of the given model
	 * reference in its plugin.xml/fragment.xml.
	 */
	public static Set<String> findPackagesInExtensions(IPluginModelBase model) {
		IResource resource = model.getUnderlyingResource();
		if (resource == null || !PluginProject.isJavaProject(resource.getProject())) {
			return Set.of();
		}
		IProject project = resource.getProject();
		IPluginModelBase extensionsModel = loadExtensionsModel(model, project);
		if (extensionsModel == null) {
			return Set.of();
		}
		ExtensionPackageFinder finder = new ExtensionPackageFinder(JavaCore.create(project));
		return Arrays.stream(extensionsModel.getPluginBase().getExtensions()).flatMap(finder::findPackages)
				.collect(Collectors.toSet());
	}

	/**
	 * Loads the extensions that the given model declares in its
	 * plugin.xml/fragment.xml into a model of its own.
	 * <p>
	 * The given model cannot be asked for them: a model that belongs to a
	 * project of the workspace deliberately ignores the extensions while
	 * reading that file and obtains them from the extension registry instead,
	 * which only knows the extension points that can be resolved. A model that
	 * belongs to no project, the kind used for the bundles of the target
	 * platform, does read them from the file, which is why such a model is
	 * created for the file of the project here. The distinction is made in
	 * PluginBase#processChild.
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
			// without the extensions, dependencies that are only referenced
			// from them are reported as unused, so make the cause visible
			PDEPlugin.log(e);
			return null;
		}
		return extensionsModel.isLoaded() ? extensionsModel : null;
	}

	private Stream<String> findPackages(IPluginParent parent) {
		return Arrays.stream(parent.getChildren()).filter(IPluginElement.class::isInstance)
				.map(IPluginElement.class::cast)
				.flatMap(element -> Stream.concat(findPackagesOfValues(element), findPackages(element)));
	}

	private Stream<String> findPackagesOfValues(IPluginElement element) {
		return Stream.concat(Arrays.stream(element.getAttributes()).map(IPluginAttribute::getValue),
				Stream.of(element.getText())).flatMap(value -> findPackageOfType(value).stream());
	}

	/**
	 * Returns the package of the type that the given value references, which is
	 * the package of the type it resolves to, or no package if it resolves to
	 * none.
	 */
	private Optional<String> findPackageOfType(String value) {
		if (value == null) {
			return Optional.empty();
		}
		// be careful: the value may have the form typeName:initializationData
		String typeName = value.trim();
		int initializationDataIndex = typeName.indexOf(':');
		if (initializationDataIndex != -1) {
			typeName = typeName.substring(0, initializationDataIndex).trim();
		}
		// a type of the default package has no package to retain, and it could
		// not be provided by a dependency anyway, as that package is not
		// exportable
		return findType(typeName).map(type -> type.getPackageFragment().getElementName())
				.filter(packageName -> !packageName.isEmpty());
	}

	private Optional<IType> findType(String typeName) {
		try {
			// member types are separated by a dot rather than by a dollar here
			return Optional.ofNullable(fJavaProject.findType(typeName.replace('$', '.'))).filter(IType::exists);
		} catch (JavaModelException e) {
			PDEPlugin.log(e);
			return Optional.empty();
		}
	}

}
