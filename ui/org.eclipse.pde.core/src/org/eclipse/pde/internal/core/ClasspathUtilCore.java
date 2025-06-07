/*******************************************************************************
 * Copyright (c) 2000, 2024 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import static java.util.Collections.singleton;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.bundle.BundleFragment;
import org.eclipse.pde.internal.core.bundle.BundlePlugin;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.natures.PluginProject;
import org.eclipse.pde.internal.core.plugin.Fragment;
import org.eclipse.pde.internal.core.plugin.Plugin;
import org.eclipse.pde.internal.core.plugin.PluginBase;
import org.osgi.framework.Bundle;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Resource;

public class ClasspathUtilCore {

	public static void addLibraries(IPluginModelBase model, ArrayList<IClasspathEntry> result) {

		for (ClasspathLibrary library : collectLibraries(model)) {
			IClasspathEntry entry = library.createClasspathEntry();
			if (!result.contains(entry)) {
				result.add(entry);
			}
		}
	}

	public static Collection<ClasspathLibrary> collectLibraries(IPluginModelBase model) {
		if (new File(model.getInstallLocation()).isFile()) {
			return singleton(new ClasspathLibrary(IPath.fromOSString(model.getInstallLocation()), model, null));
		}

		return collectLibraryEntries(model);
	}

	private static Collection<ClasspathLibrary> collectLibraryEntries(IPluginModelBase model) {
		IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
		ArrayList<ClasspathLibrary> entries = new ArrayList<>();
		for (IPluginLibrary library : libraries) {
			if (IPluginLibrary.RESOURCE.equals(library.getType())) {
				continue;
			}

			addLibraryEntry(library, entries);
		}

		return entries;
	}

	public static Stream<IClasspathEntry> classpathEntriesForBundle(String id) {
		return classpathEntriesForBundle(id, false, new IClasspathAttribute[0]);
	}

	public static Stream<IClasspathEntry> classpathEntriesForBundle(String id, boolean includeRequired,
			IClasspathAttribute[] extra) {
		// first look if we have something in the workspace...
		IPluginModelBase model = PluginRegistry.findModel(id);
		if (model != null && model.isEnabled()) {
			Stream<IClasspathEntry> modelBundleClasspath = classpathEntriesForModelBundle(model, extra);
			if (includeRequired) {
				return Stream.concat(modelBundleClasspath,
						getRequiredByDescription(model.getBundleDescription(), extra));
			}
			return modelBundleClasspath;
		}
		// if not found in the models, try to use one from the running eclipse
		Bundle runtimeBundle = Platform.getBundle(id);
		if (runtimeBundle == null) {
			return Stream.empty();
		}
		Stream<IClasspathEntry> bundleClasspath = classpathEntriesForRuntimeBundle(runtimeBundle, extra).stream();
		if (includeRequired) {
			return Stream.concat(bundleClasspath, getRequiredByWire(runtimeBundle, extra));
		}
		return bundleClasspath;
	}

	private static Stream<IClasspathEntry> getRequiredByDescription(BundleDescription description,
			IClasspathAttribute[] extra) {
		BundleWiring wiring = description.getWiring();
		if (wiring == null) {
			return Stream.empty();
		}

		List<BundleWire> wires = wiring.getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE);
		return wires.stream().map(wire -> {
			return wire.getProvider();
		}).distinct().flatMap(provider -> {
			IPluginModelBase model = PluginRegistry.findModel(provider);
			if (model != null && model.isEnabled()) {
				return classpathEntriesForModelBundle(model, extra);
			}
			return Stream.empty();
		});
	}

	protected static Stream<IClasspathEntry> classpathEntriesForModelBundle(IPluginModelBase model,
			IClasspathAttribute[] extra) {
		IResource resource = model.getUnderlyingResource();
		if (resource != null && PluginProject.isJavaProject(resource.getProject())) {
			IJavaProject javaProject = JavaCore.create(resource.getProject());
			return Stream.of(JavaCore.newProjectEntry(javaProject.getPath()));
		}
		String location = model.getInstallLocation();
		if (location == null) {
			return Stream.empty();
		}
		boolean isJarShape = new File(location).isFile();
		IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
		if (isJarShape || libraries.length == 0) {
			return Stream.of(getEntryForPath(IPath.fromOSString(location), extra));
		}
		return Arrays.stream(libraries).filter(library -> !IPluginLibrary.RESOURCE.equals(library.getType()))
				.map(library -> {
					String name = library.getName();
					String expandedName = ClasspathUtilCore.expandLibraryName(name);
					return ClasspathUtilCore.getPath(model, expandedName, isJarShape);
				}).filter(Objects::nonNull).map(entry -> getEntryForPath(entry, extra));
	}

	public static Stream<IClasspathEntry> getRequiredByWire(Bundle bundle, IClasspathAttribute[] extra) {
		BundleWiring wiring = bundle.adapt(BundleWiring.class);
		if (wiring == null) {
			return Stream.empty();
		}
		List<BundleWire> wires = wiring.getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE);
		return wires.stream().map(wire -> wire.getProviderWiring().getBundle()).distinct()
				.filter(b -> b.getBundleId() != 0)
				.flatMap(b -> classpathEntriesForRuntimeBundle(b, extra).stream());
	}

	private static Optional<IClasspathEntry> classpathEntriesForRuntimeBundle(Bundle bundle,
			IClasspathAttribute[] extra) {
		return Optional.ofNullable(bundle.adapt(File.class)).filter(File::exists)
				.map(File::toPath).map(Path::normalize).map(path -> IPath.fromOSString(path.toString()))
				.map(entry -> getEntryForPath(entry, extra));
	}

	public static boolean isEntryForModel(IClasspathEntry entry, IPluginModelBase projectModel) {
		if (entry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
			IResource resource = projectModel.getUnderlyingResource();
			if (resource != null) {
				return resource.getProject().getFullPath().equals(entry.getPath());
			}
		}
		return false;
	}

	private static IClasspathEntry getEntryForPath(IPath path, IClasspathAttribute[] extra) {
		return JavaCore.newLibraryEntry(path, path, IPath.ROOT, new IAccessRule[0], extra, false);
	}

	private static void addLibraryEntry(IPluginLibrary library, Collection<ClasspathLibrary> entries) {

		String name = library.getName();
		String expandedName = expandLibraryName(name);

		IPluginModelBase model = library.getPluginModel();
		IPath path = getPath(model, expandedName);
		if (path == null) {
			if (model.isFragmentModel() || !containsVariables(name)) {
				return;
			}
			model = resolveLibraryInFragments(library, expandedName);
			if (model == null) {
				return;
			}
			path = getPath(model, expandedName);
		}

		entries.add(new ClasspathLibrary(path, model, expandedName));
	}

	public static boolean hasExtensibleAPI(IPluginModelBase model) {
		IPluginBase pluginBase = model.getPluginBase();
		if (pluginBase instanceof IPlugin) {
			return hasExtensibleAPI((IPlugin) pluginBase);
		}
		return false;
	}

	public static boolean isPatchFragment(Resource desc) {
		IPluginModelBase model = PluginRegistry.findModel(desc);
		return model instanceof IFragmentModel i ? isPatchFragment(i.getFragment()) : false;
	}

	public static boolean isPatchFragment(IPluginModelBase model) {
		IPluginBase pluginBase = model.getPluginBase();
		if (pluginBase instanceof IFragment) {
			return isPatchFragment((IFragment) pluginBase);
		}
		return false;
	}

	private static boolean hasExtensibleAPI(IPlugin plugin) {
		if (plugin instanceof Plugin) {
			return ((Plugin) plugin).hasExtensibleAPI();
		}
		if (plugin instanceof BundlePlugin) {
			return ((BundlePlugin) plugin).hasExtensibleAPI();
		}
		return false;
	}

	private static boolean isPatchFragment(IFragment fragment) {
		if (fragment instanceof Fragment) {
			return ((Fragment) fragment).isPatch();
		}
		if (fragment instanceof BundleFragment) {
			return ((BundleFragment) fragment).isPatch();
		}
		return false;
	}

	public static boolean hasBundleStructure(IPluginModelBase model) {
		if (model.getUnderlyingResource() != null) {
			return model instanceof IBundlePluginModelBase;
		}

		IPluginBase plugin = model.getPluginBase();
		if (plugin instanceof PluginBase) {
			return ((PluginBase) plugin).hasBundleStructure();
		}
		return false;
	}

	public static boolean containsVariables(String name) {
		return name.contains("$os$") //$NON-NLS-1$
				|| name.contains("$ws$") //$NON-NLS-1$
				|| name.contains("$nl$") //$NON-NLS-1$
				|| name.contains("$arch$"); //$NON-NLS-1$
	}

	public static String expandLibraryName(String source) {
		if (source == null || source.length() == 0) {
			return ""; //$NON-NLS-1$
		}
		if (source.contains("$ws$")) { //$NON-NLS-1$
			source = source.replaceAll("\\$ws\\$", //$NON-NLS-1$
					"ws" + IPath.SEPARATOR + TargetPlatform.getWS()); //$NON-NLS-1$
		}
		if (source.contains("$os$")) { //$NON-NLS-1$
			source = source.replaceAll("\\$os\\$", //$NON-NLS-1$
					"os" + IPath.SEPARATOR + TargetPlatform.getOS()); //$NON-NLS-1$
		}
		if (source.contains("$nl$")) { //$NON-NLS-1$
			source = source.replaceAll("\\$nl\\$", //$NON-NLS-1$
					"nl" + IPath.SEPARATOR + TargetPlatform.getNL()); //$NON-NLS-1$
		}
		if (source.contains("$arch$")) { //$NON-NLS-1$
			source = source.replaceAll("\\$arch\\$", //$NON-NLS-1$
					"arch" + IPath.SEPARATOR + TargetPlatform.getOSArch()); //$NON-NLS-1$
		}
		return source;
	}

	public static IPath getSourceAnnotation(IPluginModelBase model, String libraryName) {
		return getSourceAnnotation(model, libraryName, false);
	}

	public static IPath getSourceAnnotation(IPluginModelBase model, String libraryName, boolean isJarShape) {
		String newlibraryName = TargetWeaver.getWeavedSourceLibraryName(model, libraryName);
		String zipName = getSourceZipName(newlibraryName);
		IPath path = getPath(model, zipName, isJarShape);
		if (path == null) {
			SourceLocationManager manager = PDECore.getDefault().getSourceLocationManager();
			path = manager.findSourcePath(model.getPluginBase(), IPath.fromOSString(zipName));
		}
		return path;
	}

	public static String getSourceZipName(String libraryName) {
		int dot = libraryName.lastIndexOf('.');
		return (dot != -1) ? libraryName.substring(0, dot) + "src.zip" : libraryName; //$NON-NLS-1$
	}

	private static IPluginModelBase resolveLibraryInFragments(IPluginLibrary library, String libraryName) {
		IFragmentModel[] fragments = PDEManager.findFragmentsFor(library.getPluginModel());

		for (IFragmentModel fragment : fragments) {
			IPath path = getPath(fragment, libraryName);
			if (path != null) {
				return fragment;
			}
		}
		return null;
	}

	public static IPath getPath(IPluginModelBase model, String libraryName) {
		return getPath(model, libraryName, false);
	}

	public static IPath getPath(IPluginModelBase model, String libraryName, boolean installLocationIsFile) {
		IResource resource = model.getUnderlyingResource();
		if (resource != null) {
			IResource jarFile = resource.getProject().findMember(libraryName);
			if (jarFile != null) {
				return jarFile.getFullPath();
			}
		} else {
			File file = new File(libraryName);
			if (file.isAbsolute()) {
				if (file.exists()) {
					return IPath.fromOSString(libraryName);
				}
				// absolute file can not be relative
				return null;
			}
			if (!installLocationIsFile) {
				// model.getInstallLocation() is not a file so it may be a
				// directory containing a file
				file = new File(model.getInstallLocation(), libraryName);
				if (file.exists()) {
					return IPath.fromOSString(file.getAbsolutePath());
				}
			}
		}
		return null;
	}

	public static IBuild getBuild(IPluginModelBase model) throws CoreException {
		IBuildModel buildModel = PluginRegistry.createBuildModel(model);
		return (buildModel != null) ? buildModel.getBuild() : null;
	}

	public static String getFilename(IPluginModelBase model) {
		return IPath.fromOSString(model.getInstallLocation()).lastSegment();
	}

	public static class ClasspathLibrary {

		private final IPath fPath;
		private final IPluginModelBase fModel;
		private final String fLibraryName;

		public ClasspathLibrary(IPath path, IPluginModelBase model, String libraryName) {
			fPath = path;
			fModel = model;
			fLibraryName = libraryName;
		}

		public IPath getPath() {
			return fPath;
		}

		public IClasspathEntry createClasspathEntry() {
			IPath sourcePath = findSourcePath();
			return JavaCore.newLibraryEntry(fPath, sourcePath, null, false);
		}

		private IPath findSourcePath() {
			if (fLibraryName == null) {
				IPath sourcePath = getSourceAnnotation(fModel, "."); //$NON-NLS-1$
				if (sourcePath == null) {
					sourcePath = fPath;
				}
				return sourcePath;
			}

			return getSourceAnnotation(fModel, fLibraryName);
		}
	}

}
