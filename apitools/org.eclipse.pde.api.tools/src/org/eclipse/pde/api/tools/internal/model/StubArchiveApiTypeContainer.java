/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
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
	static class ArchiveApiTypeRoot extends AbstractApiTypeRoot implements Comparable {
		
		private String fTypeName;

		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.internal.provisional.IApiTypeRoot#getStructure()
		 */
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

		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.manifest.IClassFile#getTypeName()
		 */
		public String getTypeName() {
			if (fTypeName == null) {
				fTypeName = getName().replace('/', '.').substring(0, getName().length() - Util.DOT_CLASS_SUFFIX.length()); 
			}
			return fTypeName; 
		}

		/* (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		public int compareTo(Object o) {
			return getTypeName().compareTo(((ArchiveApiTypeRoot)o).getTypeName());
		}
		
		/**
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object obj) {
			if (obj instanceof ArchiveApiTypeRoot) {
				ArchiveApiTypeRoot classFile = (ArchiveApiTypeRoot) obj;
				return this.getName().equals(classFile.getName());
			}
			return false;
		}
		
		/**
		 * @see java.lang.Object#hashCode()
		 */
		public int hashCode() {
			return getName().hashCode();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.internal.model.AbstractApiTypeRoot#getContents()
		 */
		public byte[] getContents() throws CoreException {
			StubArchiveApiTypeContainer archive = (StubArchiveApiTypeContainer) getParent();
			ZipFile zipFile = archive.open();
			ZipEntry entry = zipFile.getEntry(getName());
			InputStream stream = null;
			if (entry != null) {
				try {
					stream = zipFile.getInputStream(entry);
				} catch (IOException e) {
					abort("Failed to open class file: " + getTypeName() + " in archive: " + archive.fLocation, e); //$NON-NLS-1$ //$NON-NLS-2$
					return null;
				}
				try {
					return Util.getInputStreamAsByteArray(stream, -1);
				}
				catch(IOException ioe) {
					abort("Unable to read class file: " + getTypeName(), ioe); //$NON-NLS-1$
					return null; // never gets here
				}
				finally {
					try {
						stream.close();
					} catch (IOException e) {
						ApiPlugin.log(e);
					}
				}
			}
			abort("Class file not found: " + getTypeName() + " in archive: " + archive.fLocation, null); //$NON-NLS-1$ //$NON-NLS-2$
			return null;
		}
	}
	
	/**
	 * Location of the archive in the local file system.
	 */
	String fLocation;
	
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

	/**
	 * Constructs an {@link IApiTypeContainer} container for the given jar or zip file
	 * at the specified location.
	 * 
	 * @param parent the parent {@link IApiElement} or <code>null</code> if none
	 * @param path location of the file in the local file system
	 */
	public StubArchiveApiTypeContainer(IApiElement parent, String path) {
		super(parent, IApiElement.API_TYPE_CONTAINER, path);
		this.fLocation = path;
	}

	/**
	 * @see org.eclipse.pde.api.tools.internal.AbstractApiTypeContainer#accept(org.eclipse.pde.api.tools.internal.provisional.ApiTypeContainerVisitor)
	 */
	public void accept(ApiTypeContainerVisitor visitor) throws CoreException {
		if(visitor.visit(this)) {
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
						classFiles.add(new ArchiveApiTypeRoot(this, entryName));
					}
					Collections.sort(classFiles);
					cfIterator = classFiles.iterator();
					while (cfIterator.hasNext()) {
						ArchiveApiTypeRoot classFile = (ArchiveApiTypeRoot) cfIterator.next();
						visitor.visit(pkg, classFile);
						visitor.end(pkg, classFile);
					}
				}
				visitor.endVisitPackage(pkg);
			}
		}
		visitor.end(this);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buff = new StringBuffer();
		buff.append("Archive Class File Container: "+getName()); //$NON-NLS-1$
		return buff.toString();
	}
	
	/**
	 * @see org.eclipse.pde.api.tools.internal.AbstractApiTypeContainer#close()
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

	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiTypeContainer#findTypeRoot(java.lang.String)
	 */
	public IApiTypeRoot findTypeRoot(String qualifiedName) throws CoreException {
		init();
		int index = qualifiedName.lastIndexOf('.');
		String packageName = Util.DEFAULT_PACKAGE_NAME;
		if (index >= 0) {
			packageName = qualifiedName.substring(0, index);
		}
		Set classFileNames = (Set) fPackages.get(packageName);
		if (classFileNames != null) {
			String fileName = qualifiedName.replace('.', '/');
			if (classFileNames.contains(fileName)) {
				return new ArchiveApiTypeRoot(this, fileName);
			}
		}
		return null;
	}

	/**
	 * @see org.eclipse.pde.api.tools.internal.AbstractApiTypeContainer#getPackageNames()
	 */
	public String[] getPackageNames() throws CoreException {
		init();
		synchronized (this) {
			if (fPackageNames == null) {
				Set names = fPackages.keySet();
				String[] result = new String[names.size()];
				names.toArray(result);
				Arrays.sort(result);
				fPackageNames = result;
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
		ZipFile zipFile = open();
		if (fPackages == null) {
			fPackages = new HashMap();
			Enumeration entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = (ZipEntry) entries.nextElement();
				String name = entry.getName();
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

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof StubArchiveApiTypeContainer) {
			return this.fLocation.equals(((StubArchiveApiTypeContainer) obj).fLocation);
		}
		return false;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return this.fLocation.hashCode();
	}

	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiTypeContainer#findTypeRoot(java.lang.String, java.lang.String)
	 */
	public IApiTypeRoot findTypeRoot(String qualifiedName, String id) throws CoreException {
		return findTypeRoot(qualifiedName);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer#getContainerType()
	 */
	public int getContainerType() {
		return ARCHIVE;
	}
}
