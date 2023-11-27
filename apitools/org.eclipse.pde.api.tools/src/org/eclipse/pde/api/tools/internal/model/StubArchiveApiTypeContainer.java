/*******************************************************************************
 * Copyright (c) 2009, 2023 IBM Corporation and others.
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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.ApiTypeContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * {@link IApiTypeContainer} container for an archive (jar or zip) file.
 *
 * @since 1.0.0
 */
public class StubArchiveApiTypeContainer extends ApiElement implements IApiTypeContainer {

	/**
	 * {@link IApiTypeRoot} implementation within an archive
	 */
	static class ArchiveApiTypeRoot extends AbstractApiTypeRoot implements Comparable<Object> {

		private String fTypeName;

		@Override
		public IApiType getStructure() throws CoreException {
			return TypeStructureBuilder.buildStubTypeStructure(getContents(), getApiComponent(), this);
		}

		/**
		 * Constructs a new handle to an {@link IApiTypeRoot} in the archive.
		 *
		 * @param container archive
		 * @param entryName zip entry name
		 */
		public ArchiveApiTypeRoot(StubArchiveApiTypeContainer container, String entryName) {
			super(container, entryName);
		}

		@Override
		public String getTypeName() {
			if (fTypeName == null) {
				fTypeName = getName().replace('/', '.').substring(0, getName().length() - Util.DOT_CLASS_SUFFIX.length());
			}
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
			StubArchiveApiTypeContainer archive = (StubArchiveApiTypeContainer) getParent();
			try (ZipFile zipFile = archive.open()) {
				ZipEntry entry = zipFile.getEntry(getName());
				if (entry != null) {
					try (InputStream stream = zipFile.getInputStream(entry)) {
						return stream.readAllBytes();
					}
				}
				throw abortException("Class file not found: " + getTypeName() + " in archive: " + archive.fLocation, //$NON-NLS-1$ //$NON-NLS-2$
						null);
			} catch (IOException e) {
				ApiPlugin.log(e);
				throw abortException(
						"Failed to read class file: " + getTypeName() + " in archive: " + archive.fLocation, e); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	/**
	 * Location of the archive in the local file system.
	 */
	String fLocation;

	/**
	 * Cache of package names to class file paths in that package, or
	 * <code>null</code> if not yet initialized.
	 */
	private Map<String, Set<String>> fPackages;

	/**
	 * Cache of package names in this archive.
	 */
	private String[] fPackageNames;

	/**
	 * Open zip file, or <code>null</code> if file is currently closed.
	 */
	private ZipFile fZipFile = null;

	/**
	 * Constructs an {@link IApiTypeContainer} container for the given jar or
	 * zip file at the specified location.
	 *
	 * @param parent the parent {@link IApiElement} or <code>null</code> if none
	 * @param path location of the file in the local file system
	 */
	public StubArchiveApiTypeContainer(IApiElement parent, String path) {
		super(parent, IApiElement.API_TYPE_CONTAINER, path);
		this.fLocation = path;
	}

	@Override
	public void accept(ApiTypeContainerVisitor visitor) throws CoreException {
		if (visitor.visit(this)) {
			init();
			List<String> packages = new ArrayList<>(fPackages.keySet());
			Collections.sort(packages);
			for (String pkg : packages) {
				if (visitor.visitPackage(pkg)) {
					List<String> types = new ArrayList<>(fPackages.get(pkg));
					List<ArchiveApiTypeRoot> classFiles = new ArrayList<>(types.size());
					for (String entryName : types) {
						classFiles.add(new ArchiveApiTypeRoot(this, entryName));
					}
					Collections.sort(classFiles);
					for (ArchiveApiTypeRoot classFile : classFiles) {
						visitor.visit(pkg, classFile);
						visitor.end(pkg, classFile);
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
		buff.append("Archive Class File Container: " + getName()); //$NON-NLS-1$
		return buff.toString();
	}

	@Override
	public synchronized void close() throws CoreException {
		if (fZipFile != null) {
			try {
				fZipFile.close();
				fZipFile = null;
			} catch (IOException e) {
				abort("Failed to close class file archive", e); //$NON-NLS-1$
			}
		}
	}

	@Override
	public IApiTypeRoot findTypeRoot(String qualifiedName) throws CoreException {
		init();
		int index = qualifiedName.lastIndexOf('.');
		String packageName = Util.DEFAULT_PACKAGE_NAME;
		if (index >= 0) {
			packageName = qualifiedName.substring(0, index);
		}
		Set<String> classFileNames = fPackages.get(packageName);
		if (classFileNames != null) {
			String fileName = qualifiedName.replace('.', '/');
			if (classFileNames.contains(fileName)) {
				return new ArchiveApiTypeRoot(this, fileName);
			}
		}
		return null;
	}

	@Override
	public String[] getPackageNames() throws CoreException {
		init();
		synchronized (this) {
			if (fPackageNames == null) {
				fPackageNames = fPackages.keySet().stream().sorted().toArray(String[]::new);
			}
			return fPackageNames;
		}
	}

	/**
	 * Initializes cache of packages and types.
	 */
	private synchronized void init() throws CoreException {
		ZipFile zipFile = open();
		if (fPackages == null) {
			fPackages = new HashMap<>();
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				String name = entry.getName();
				String pkg = Util.DEFAULT_PACKAGE_NAME;
				int index = name.lastIndexOf('/');
				if (index >= 0) {
					pkg = name.substring(0, index).replace('/', '.');
				}
				Set<String> fileNames = fPackages.get(pkg);
				if (fileNames == null) {
					fileNames = new HashSet<>();
					fPackages.put(pkg, fileNames);
				}
				fileNames.add(name);
			}
		}
	}

	/**
	 * Returns an open zip file for this archive.
	 *
	 * @return zip file
	 * @throws IOException if unable to open the archive
	 */
	synchronized ZipFile open() throws CoreException {
		if (fZipFile == null) {
			try {
				fZipFile = new ZipFile(fLocation);
			} catch (IOException e) {
				abort("Failed to open archive: " + fLocation, e); //$NON-NLS-1$
			}
		}
		return fZipFile;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof StubArchiveApiTypeContainer) {
			return this.fLocation.equals(((StubArchiveApiTypeContainer) obj).fLocation);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.fLocation.hashCode();
	}

	@Override
	public IApiTypeRoot findTypeRoot(String qualifiedName, String id) throws CoreException {
		return findTypeRoot(qualifiedName);
	}

	@Override
	public int getContainerType() {
		return ARCHIVE;
	}
}
