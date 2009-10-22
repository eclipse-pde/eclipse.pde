/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.model;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
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
	static class LocalApiTypeRoot extends AbstractApiTypeRoot implements Comparable {
		
		String fLocation = null;
		
		/**
		 * Constructs a class file on the given file
		 * @param directory the parent {@link IApiElement} directory
		 * @param location
		 * @param qualified type name
		 * @param component owning API component
		 */
		public LocalApiTypeRoot(DirectoryApiTypeContainer directory, String location, String typeName) {
			super(directory, typeName);
			fLocation = location;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.model.component.IClassFile#getTypeName()
		 */
		public String getTypeName() {
			return getName();
		}

		/* (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		public int compareTo(Object o) {
			return getName().compareTo(((LocalApiTypeRoot)o).getName());
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object obj) {
			if (obj instanceof LocalApiTypeRoot) {
				return ((LocalApiTypeRoot) obj).getName().equals(this.getName());
			}
			return false;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		public int hashCode() {
			return this.getName().hashCode();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.internal.model.AbstractApiTypeRoot#getContents()
		 */
		public byte[] getContents() throws CoreException {
			InputStream stream = null;
			try {
				stream = new FileInputStream(new File(fLocation));
			} catch (FileNotFoundException e) {
				abort("File not found", e); //$NON-NLS-1$
				return null;
			}
			try {
				return Util.getInputStreamAsByteArray(stream, -1);
			}
			catch(IOException ioe) {
				abort("Unable to read class file: " + getTypeName(), ioe); //$NON-NLS-1$
				return null;
			}
			finally {
				try {
					stream.close();
				} catch (IOException e) {
					ApiPlugin.log(e);
				}
			}
		}
	}	

	/**
	 * Map of package names to associated directory (file)
	 */
	private Map fPackages;
	
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
	
	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiTypeContainer#accept(org.eclipse.pde.api.tools.internal.provisional.ApiTypeContainerVisitor)
	 */
	public void accept(ApiTypeContainerVisitor visitor) throws CoreException {
		if(visitor.visit(this)) {
			init();
			String[] packageNames = getPackageNames();
			for (int i = 0; i < packageNames.length; i++) {
				String pkg = packageNames[i];
				if (visitor.visitPackage(pkg)) {
					String location = (String) fPackages.get(pkg);
					if(location == null) {
						continue;
					}
					File dir = new File(location);
					if(!dir.exists()) {
						continue;
					}
					File[] files = dir.listFiles(new FileFilter() {
						public boolean accept(File file) {
							return file.isFile() && file.getName().endsWith(Util.DOT_CLASS_SUFFIX);
						}
					});
					if (files != null) {
						List classFiles = new ArrayList();
						for (int j = 0; j < files.length; j++) {
							String name = files[j].getName();
							String typeName = name.substring(0, name.length() - 6);
							if (pkg.length() > 0) {
								typeName = pkg + "." + typeName; //$NON-NLS-1$
							}
							classFiles.add(new LocalApiTypeRoot(this, files[j].getAbsolutePath(), typeName));
						}
						Collections.sort(classFiles);
						Iterator cfIterator = classFiles.iterator();
						while (cfIterator.hasNext()) {
							IApiTypeRoot classFile = (IApiTypeRoot) cfIterator.next();
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

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buff = new StringBuffer();
		buff.append("Directory Class File Container: "+getName()); //$NON-NLS-1$
		return buff.toString();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IClassFileContainer#close()
	 */
	public synchronized void close() throws CoreException {
		fPackages = null;
		fPackageNames = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IClassFileContainer#findClassFile(java.lang.String)
	 */
	public IApiTypeRoot findTypeRoot(String qualifiedName) throws CoreException {
		init();
		int index = qualifiedName.lastIndexOf('.');
		String cfName = qualifiedName;
		String pkg = Util.DEFAULT_PACKAGE_NAME;
		if (index > 0) {
			pkg = qualifiedName.substring(0, index);
			cfName = qualifiedName.substring(index + 1);
		}
		String location = (String) fPackages.get(pkg);
		if(location != null) {
			File file = new File(location, cfName + Util.DOT_CLASS_SUFFIX);
			if (file.exists()) {
				return new LocalApiTypeRoot(this, file.getAbsolutePath(), qualifiedName);
			}
		}
		return null;
	}

	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IClassFileContainer#getPackageNames()
	 */
	public String[] getPackageNames() throws CoreException {
		init();
		if (fPackageNames == null) {
			List names = new ArrayList(fPackages.keySet());
			String[] result = new String[names.size()];
			names.toArray(result);
			Arrays.sort(result);
			fPackageNames = result;
		}
		return fPackageNames;
	}
	
	/**
	 * Builds cache of package names to directories
	 */
	private synchronized void init() {
		if (fPackages == null) {
			fPackages = new HashMap();
			processDirectory(Util.DEFAULT_PACKAGE_NAME, new File(getName()));
		}
	}
	
	/**
	 * Traverses a directory to determine if it has class files and
	 * then visits sub-directories.
	 * 
	 * @param packageName package name of directory being visited
	 * @param dir directory being visited
	 */
	private void processDirectory(String packageName, File dir) {
		File[] files = dir.listFiles();
		if (files != null) {
			boolean hasClassFiles = false;
			List dirs = new ArrayList();
			for (int i = 0; i < files.length; i++) {
				File file = files[i];
				if (file.isDirectory()) {
					dirs.add(file.getAbsoluteFile());
				} else if (!hasClassFiles) {
					if (file.getName().endsWith(Util.DOT_CLASS_SUFFIX)) {
						fPackages.put(packageName, dir.getAbsolutePath());
						hasClassFiles = true;
					}
				}
			}
			Iterator iterator = dirs.iterator();
			while (iterator.hasNext()) {
				File child = (File)iterator.next();
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

	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiTypeContainer#findClassFile(java.lang.String, java.lang.String)
	 */
	public IApiTypeRoot findTypeRoot(String qualifiedName, String id) throws CoreException {
		return findTypeRoot(qualifiedName);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer#getContainerType()
	 */
	public int getContainerType() {
		return DIRECTORY;
	}
}
