/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
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

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.model.ApiTypeContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * An {@link IApiTypeContainer} rooted at a directory in the file system.
 *
 * @since 1.0.0
 */
public class DirectoryApiTypeContainer extends ApiElement implements IApiTypeContainer {

	/**
	 * Implementation of an {@link IApiTypeRoot} in the local file system.
	 */
	static class LocalApiTypeRoot extends AbstractApiTypeRoot implements Comparable<Object> {

		String fLocation = null;

		/**
		 * Constructs a class file on the given file
		 *
		 * @param directory the parent {@link IApiElement} directory
		 * @param location
		 * @param qualified type name
		 * @param component owning API component
		 */
		public LocalApiTypeRoot(DirectoryApiTypeContainer directory, String location, String typeName) {
			super(directory, typeName);
			fLocation = location;
		}

		@Override
		public String getTypeName() {
			return getName();
		}

		@Override
		public int compareTo(Object o) {
			return getName().compareTo(((LocalApiTypeRoot) o).getName());
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof LocalApiTypeRoot) {
				return ((LocalApiTypeRoot) obj).getName().equals(this.getName());
			}
			return false;
		}

		@Override
		public int hashCode() {
			return this.getName().hashCode();
		}

		@Override
		public byte[] getContents() throws CoreException {
			try {
				return Files.readAllBytes(new File(fLocation).toPath());
			} catch (FileNotFoundException e) {
				abort("File not found", e); //$NON-NLS-1$
				return null;
			} catch (IOException ioe) {
				abort("Unable to read class file: " + getTypeName(), ioe); //$NON-NLS-1$
				return null;
			}
		}
	}

	/**
	 * Map of package names to associated directory (file)
	 */
	private Map<String, String> fPackages;

	/**
	 * Cache of package names
	 */
	private String[] fPackageNames;

	/**
	 * Constructs an {@link IApiTypeContainer} rooted at the specified path.
	 *
	 * @param parent the parent {@link IApiElement} or <code>null</code> if none
	 * @param location absolute path in the local file system
	 */
	public DirectoryApiTypeContainer(IApiElement parent, String location) {
		super(parent, IApiElement.API_TYPE_CONTAINER, location);
	}

	@Override
	public void accept(ApiTypeContainerVisitor visitor) throws CoreException {
		if (visitor.visit(this)) {
			init();
			String[] packageNames = getPackageNames();
			for (String pkg : packageNames) {
				if (visitor.visitPackage(pkg)) {
					String location = fPackages.get(pkg);
					if (location == null) {
						continue;
					}
					File dir = new File(location);
					if (!dir.exists()) {
						continue;
					}
					File[] files = dir.listFiles((FileFilter) file -> file.isFile() && file.getName().endsWith(Util.DOT_CLASS_SUFFIX));
					if (files != null) {
						List<LocalApiTypeRoot> classFiles = new ArrayList<>();
						for (File file : files) {
							String name = file.getName();
							String typeName = name.substring(0, name.length() - 6);
							if (pkg.length() > 0) {
								typeName = pkg + "." + typeName; //$NON-NLS-1$
							}
							classFiles.add(new LocalApiTypeRoot(this, file.getAbsolutePath(), typeName));
						}
						Collections.sort(classFiles);
						for (IApiTypeRoot classFile : classFiles) {
							visitor.visit(pkg, classFile);
							visitor.end(pkg, classFile);
						}
					}
				}
				visitor.endVisitPackage(pkg);
			}
		}
		visitor.end(this);
	}

	@Override
	public String toString() {
		StringBuilder buff = new StringBuilder();
		buff.append("Directory Class File Container: " + getName()); //$NON-NLS-1$
		return buff.toString();
	}

	@Override
	public synchronized void close() throws CoreException {
		fPackages = null;
		fPackageNames = null;
	}

	@Override
	public IApiTypeRoot findTypeRoot(String qualifiedName) throws CoreException {
		init();
		int index = qualifiedName.lastIndexOf('.');
		String cfName = qualifiedName;
		String pkg = Util.DEFAULT_PACKAGE_NAME;
		if (index > 0) {
			pkg = qualifiedName.substring(0, index);
			cfName = qualifiedName.substring(index + 1);
		}
		String location = fPackages.get(pkg);
		if (location != null) {
			File file = new File(location, cfName + Util.DOT_CLASS_SUFFIX);
			if (file.exists()) {
				return new LocalApiTypeRoot(this, file.getAbsolutePath(), qualifiedName);
			}
		}
		return null;
	}

	@Override
	public String[] getPackageNames() throws CoreException {
		init();
		if (fPackageNames == null) {
			fPackageNames = fPackages.keySet().stream().sorted().toArray(String[]::new);
		}
		return fPackageNames;
	}

	/**
	 * Builds cache of package names to directories
	 */
	private synchronized void init() {
		if (fPackages == null) {
			fPackages = new HashMap<>();
			processDirectory(Util.DEFAULT_PACKAGE_NAME, new File(getName()));
		}
	}

	/**
	 * Traverses a directory to determine if it has class files and then visits
	 * sub-directories.
	 *
	 * @param packageName package name of directory being visited
	 * @param dir directory being visited
	 */
	private void processDirectory(String packageName, File dir) {
		File[] files = dir.listFiles();
		if (files != null) {
			boolean hasClassFiles = false;
			List<File> dirs = new ArrayList<>();
			for (File file : files) {
				if (file.isDirectory()) {
					dirs.add(file.getAbsoluteFile());
				} else if (!hasClassFiles) {
					if (file.getName().endsWith(Util.DOT_CLASS_SUFFIX)) {
						fPackages.put(packageName, dir.getAbsolutePath());
						hasClassFiles = true;
					}
				}
			}
			Iterator<File> iterator = dirs.iterator();
			while (iterator.hasNext()) {
				File child = iterator.next();
				String nextName = null;
				if (packageName.length() == 0) {
					nextName = child.getName();
				} else {
					nextName = packageName + "." + child.getName(); //$NON-NLS-1$
				}
				processDirectory(nextName, child);
			}
		}
	}

	@Override
	public IApiTypeRoot findTypeRoot(String qualifiedName, String id) throws CoreException {
		return findTypeRoot(qualifiedName);
	}

	@Override
	public int getContainerType() {
		return DIRECTORY;
	}
}
