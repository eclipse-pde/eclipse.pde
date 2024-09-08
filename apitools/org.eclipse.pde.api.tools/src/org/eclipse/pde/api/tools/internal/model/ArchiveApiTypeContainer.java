/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.internal.model;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.model.ApiTypeContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;
import org.eclipse.pde.api.tools.internal.util.Signatures;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * {@link IApiTypeContainer} container for an archive (jar or zip) file.
 *
 * @since 1.0.0
 */
public class ArchiveApiTypeContainer extends ApiElement implements IApiTypeContainer {

	/**
	 * {@link IApiTypeRoot} implementation within an archive
	 */
	static class ArchiveApiTypeRoot extends AbstractApiTypeRoot implements Comparable<Object> {

		private final String fTypeName;
		private byte[] fContents = null;

		/**
		 * Constructs a new handle to an {@link IApiTypeRoot} in the archive.
		 *
		 * @param container archive
		 * @param entryName zip entry name
		 */
		public ArchiveApiTypeRoot(ArchiveApiTypeContainer container, String typeName, String entryName) {
			super(container, entryName);
			this.fTypeName = typeName;
		}

		@Override
		public String getTypeName() {
			return fTypeName;
		}

		@Override
		public int compareTo(Object o) {
			return getTypeName().compareTo(((ArchiveApiTypeRoot) o).getTypeName());
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ArchiveApiTypeRoot classFile) {
				return this.getName().equals(classFile.getName());
			}
			return false;
		}

		@Override
		public int hashCode() {
			return getName().hashCode();
		}

		@Override
		public byte[] getContents() throws CoreException {
			if (fContents != null) {
				return fContents;
			}
			ArchiveApiTypeContainer archive = (ArchiveApiTypeContainer) getParent();
			try {
				Path location = archive.getLocation();
				Path classLocation = location.resolve(getName());
				fContents = Files.readAllBytes(classLocation);
			} catch (IOException e) {
				abort("Failed to open class file: " + getTypeName() + " in archive: " + archive.fLocation, e); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return fContents;
		}

		@Override
		public String toString() {
			return getTypeName();
		}
	}

	/**
	 * Location of the archive in the local file system.
	 */
	String fLocation;

	/**
	 * Cache of package names to a map of class names to class files paths in that
	 * package, or <code>null</code> if not yet initialized.
	 */
	private Map<String, Map<String, String>> fPackages;

	/**
	 * Cache of package names in this archive.
	 */
	private String[] fPackageNames;

	/**
	 * Constructs an {@link IApiTypeContainer} container for the given jar or zip
	 * file at the specified location.
	 *
	 * @param parent the parent {@link IApiElement} or <code>null</code> if none
	 * @param path   location of the file in the local file system
	 */
	public ArchiveApiTypeContainer(IApiElement parent, String path) {
		super(parent, IApiElement.API_TYPE_CONTAINER, path);
		this.fLocation = path;
	}

	/**
	 * Converts the location to a path in the applicable file system.
	 *
	 * @return the path corresponding to the location.
	 */
	@SuppressWarnings("restriction")
	private Path getLocation() throws IOException {
		Path path = Path.of(fLocation);
		if (fLocation.endsWith(org.eclipse.jdt.internal.compiler.util.JRTUtil.JRT_FS_JAR)) {
			Path jreRoot = path.getParent().getParent();
			FileSystem jrtFileSystem = org.eclipse.jdt.internal.compiler.util.JRTUtil.getJrtFileSystem(jreRoot);
			return jrtFileSystem.getPath("modules"); //$NON-NLS-1$
		} else {
			return org.eclipse.jdt.internal.compiler.util.JRTUtil.getJarFileSystem(path).getPath("/"); //$NON-NLS-1$
		}
	}

	/**
	 * @see AbstractApiTypeContainer#accept(ApiTypeContainerVisitor)
	 */
	@Override
	public void accept(ApiTypeContainerVisitor visitor) throws CoreException {
		if (visitor.visit(this)) {
			init();
			for (Map.Entry<String, Map<String, String>> entry : fPackages.entrySet()) {
				String pkg = entry.getKey();
				if (visitor.visitPackage(pkg)) {
					Map<String, String> classes = entry.getValue();
					List<ArchiveApiTypeRoot> classFiles = new ArrayList<>(classes.size());
					for (Map.Entry<String, String> classEntry : classes.entrySet()) {
						classFiles.add(new ArchiveApiTypeRoot(this, classEntry.getKey(), classEntry.getValue()));
					}
					for (ArchiveApiTypeRoot classfile : classFiles) {
						visitor.visit(pkg, classfile);
						visitor.end(pkg, classfile);
					}
					visitor.endVisitPackage(pkg);
				}
			}
		}
		visitor.end(this);
	}

	@Override
	public String toString() {
		StringBuilder buff = new StringBuilder();
		buff.append("Archive Class File Container: " + getName()); //$NON-NLS-1$
		return buff.toString();
	}

	/**
	 * @see AbstractApiTypeContainer#close()
	 */
	@Override
	public synchronized void close() throws CoreException {
		//
	}

	/**
	 * @see IApiTypeContainer#findTypeRoot(java.lang.String)
	 */
	@Override
	public IApiTypeRoot findTypeRoot(String qualifiedName) throws CoreException {
		init();
		String packageName = Signatures.getPackageName(qualifiedName);
		Map<String, String> classFileNames = fPackages.get(packageName);
		if (classFileNames != null) {
			String fileName = classFileNames.get(qualifiedName);
			if (fileName != null) {
				return new ArchiveApiTypeRoot(this, qualifiedName, fileName);
			}
		}
		return null;
	}

	/**
	 * @see AbstractApiTypeContainer#getPackageNames()
	 */
	@Override
	public String[] getPackageNames() throws CoreException {
		init();
		synchronized (this) {
			if (fPackageNames == null) {
				fPackageNames = fPackages.keySet().toArray(String[]::new);
			}
			return fPackageNames;
		}
	}

	/**
	 * Initializes cache of packages and types.
	 */
	private synchronized void init() throws CoreException {
		if (fPackages == null) {
			fPackages = new TreeMap<>();
			try {
				Path location = getLocation();
				boolean isJrt = "jrt".equals(location.toUri().getScheme()); //$NON-NLS-1$
				try (Stream<Path> walk = Files.walk(location)) {
					walk.forEach(it -> {
						String name = location.relativize(it).toString();
						if (name.endsWith(Util.DOT_CLASS_SUFFIX)) {
							// In the JRT file system, the first segment will be the module name,
							// which we must strip.
							String className = name.substring(isJrt ? name.indexOf('/') + 1 : 0,
									name.length() - Util.DOT_CLASS_SUFFIX.length()).replace('/', '.');
							String pkg = Signatures.getPackageName(className);
							Map<String, String> fileNames = fPackages.computeIfAbsent(pkg, p -> new TreeMap<>());
							fileNames.put(className, name);
						}
					});
				}
			} catch (IOException e) {
				abort("Failed to process archive: " + fLocation, e); //$NON-NLS-1$
			}
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ArchiveApiTypeContainer) {
			return this.fLocation.equals(((ArchiveApiTypeContainer) obj).fLocation);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.fLocation.hashCode();
	}

	/**
	 * @see IApiTypeContainer#findTypeRoot(java.lang.String, java.lang.String)
	 */
	@Override
	public IApiTypeRoot findTypeRoot(String qualifiedName, String id) throws CoreException {
		return findTypeRoot(qualifiedName);
	}

	@Override
	public int getContainerType() {
		return ARCHIVE;
	}
}
