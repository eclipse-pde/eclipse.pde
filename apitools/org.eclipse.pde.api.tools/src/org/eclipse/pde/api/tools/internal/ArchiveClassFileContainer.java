/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.ClassFileContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.IClassFile;
import org.eclipse.pde.api.tools.internal.provisional.IClassFileContainer;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Class file container for an archive (jar or zip) file.
 * 
 * @since 1.0.0
 */
public class ArchiveClassFileContainer implements IClassFileContainer {
		
	/**
	 * Location of the archive in the local file system.
	 */
	private String fLocation;
	
	/**
	 * Origin of this class file container
	 */
	private String fOrigin;
	
	/**
	 * Cache of package names to class file paths in that package,
	 * or <code>null</code> if not yet initialized.
	 */
	private Map fPackages;
	
	/**
	 * Cache of package names in this archive.
	 */
	private String[] fPackageNames;
	
	/**
	 * Open zip file, or <code>null</code> if file is currently closed.
	 */
	private ZipFile fZipFile = null;
	
	class ArchiveClassFile extends AbstractClassFile implements Comparable {
		
		private ArchiveClassFileContainer fArchive;
		private String fEntryName;
		private String fTypeName;
		
		/**
		 * Constructs a new handle to a class file in the archive.
		 * 
		 * @param container archive
		 * @param entryName zip entry name
		 */
		public ArchiveClassFile(ArchiveClassFileContainer container, String entryName) {
			fArchive = container;
			fEntryName = entryName;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.manifest.IClassFile#getTypeName()
		 */
		public String getTypeName() {
			if (fTypeName == null) {
				fTypeName = fEntryName.replace('/', '.').substring(0, fEntryName.length() - Util.DOT_CLASS_SUFFIX.length()); 
			}
			return fTypeName; 
		}

		/* (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		public int compareTo(Object o) {
			return getTypeName().compareTo(((ArchiveClassFile)o).getTypeName());
		}
		

		public boolean equals(Object obj) {
			if (obj instanceof ArchiveClassFile) {
				ArchiveClassFile classFile = (ArchiveClassFile) obj;
				return this.fEntryName.equals(classFile.fEntryName);
			}
			return false;
		}
		
		public int hashCode() {
			return fEntryName.hashCode();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.model.component.IClassFile#getInputStream()
		 */
		public InputStream getInputStream() throws CoreException {
			ZipFile zipFile = fArchive.open();
			ZipEntry entry = zipFile.getEntry(fEntryName);
			if (entry != null) {
				try {
					return zipFile.getInputStream(entry);
				} catch (IOException e) {
					abort("Failed to open class file: " + getTypeName() + " in archive: " + fArchive.fLocation, e); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			abort("Class file not found: " + getTypeName() + " in archive: " + fArchive.fLocation, null); //$NON-NLS-1$ //$NON-NLS-2$
			return null;
		}
	}

	/**
	 * Constructs a class file container for the given jar or zip file
	 * at the specified location.
	 * 
	 * @param path location of the file in the local file system
	 */
	public ArchiveClassFileContainer(String path, String origin) {
		this.fLocation = path;
		this.fOrigin = origin;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.manifest.IClassFileContainer#accept(org.eclipse.pde.api.tools.manifest.ClassFileContainerVisitor)
	 */
	public void accept(ClassFileContainerVisitor visitor) throws CoreException {
		init();
		List packages = new ArrayList(fPackages.keySet());
		Collections.sort(packages);
		Iterator iterator = packages.iterator();
		while (iterator.hasNext()) {
			String pkg = (String) iterator.next();
			if (visitor.visitPackage(pkg)) {
				List types = new ArrayList((Set) fPackages.get(pkg));
				Iterator cfIterator = types.iterator();
				List classFiles = new ArrayList(types.size());
				while (cfIterator.hasNext()) {
					String entryName = (String) cfIterator.next();
					classFiles.add(new ArchiveClassFile(this, entryName));
				}
				Collections.sort(classFiles);
				cfIterator = classFiles.iterator();
				while (cfIterator.hasNext()) {
					ArchiveClassFile classFile = (ArchiveClassFile) cfIterator.next();
					visitor.visit(pkg, classFile);
					visitor.end(pkg, classFile);
				}
			}
			visitor.endVisitPackage(pkg);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.manifest.IClassFileContainer#close()
	 */
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

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.manifest.IClassFileContainer#findClassFile(java.lang.String)
	 */
	public IClassFile findClassFile(String qualifiedName) throws CoreException {
		init();
		int index = qualifiedName.lastIndexOf('.');
		String packageName = Util.DEFAULT_PACKAGE_NAME;
		if (index >= 0) {
			packageName = qualifiedName.substring(0, index);
		}
		Set classFileNames = (Set) fPackages.get(packageName);
		if (classFileNames != null) {
			String fileName = qualifiedName.replace('.', '/') + Util.DOT_CLASS_SUFFIX;
			if (classFileNames.contains(fileName)) {
				return new ArchiveClassFile(this, fileName);
			}
		}
		return null;
	}

	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.manifest.IClassFileContainer#getPackageNames()
	 */
	public String[] getPackageNames() throws CoreException {
		init();
		synchronized (this) {
			if (fPackageNames == null) {
				Set names = fPackages.keySet();
				fPackageNames = (String[])names.toArray(new String[names.size()]);
			}
			return fPackageNames;
		}
	}
	
	/**
	 * Initializes cache of packages and types.
	 * 
	 * @throws CoreException
	 */
	private synchronized void init() throws CoreException {
		if (fPackages == null) {
			fPackages = new HashMap();
			ZipFile zipFile = open();
			Enumeration entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = (ZipEntry) entries.nextElement();
				String name = entry.getName();
				if (name.endsWith(Util.DOT_CLASS_SUFFIX)) {
					String pkg = Util.DEFAULT_PACKAGE_NAME;
					int index = name.lastIndexOf('/');
					if (index >= 0) {
						pkg = name.substring(0, index).replace('/', '.');
					}
					Set fileNames = (Set) fPackages.get(pkg);
					if (fileNames == null) {
						fileNames = new HashSet();
						fPackages.put(pkg, fileNames);
					}
					fileNames.add(name);
				}
			}
		}
	}
	
	/**
	 * Returns an open zip file for this archive.
	 * 
	 * @return zip file
	 * @throws IOException if unable to open the archive
	 */
	private synchronized ZipFile open() throws CoreException {
		if (fZipFile == null) {
			try {
				fZipFile = new ZipFile(fLocation);
			} catch (IOException e) {
				abort("Failed to open archive: " + fLocation, e); //$NON-NLS-1$
			}
		}
		return fZipFile;
	}

	/**
	 * Throws a core exception.
	 * 
	 * @param message message
	 * @param e underlying exception or <code>null</code>
	 * @throws CoreException
	 */
	private void abort(String message, Throwable e) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR,
				ApiPlugin.PLUGIN_ID, message, e));
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof ArchiveClassFileContainer) {
			return this.fLocation.equals(((ArchiveClassFileContainer) obj).fLocation);
		}
		return false;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return this.fLocation.hashCode();
	}
	
	public IClassFile findClassFile(String qualifiedName, String id) throws CoreException {
		return findClassFile(qualifiedName);
	}
	
	public String getOrigin() {
		return this.fOrigin;
	}
}
