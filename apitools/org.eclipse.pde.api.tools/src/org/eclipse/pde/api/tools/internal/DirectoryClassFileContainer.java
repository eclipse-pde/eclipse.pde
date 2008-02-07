/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.ClassFileContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.IClassFile;
import org.eclipse.pde.api.tools.internal.provisional.IClassFileContainer;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * A class file container rooted at a directory in the file system.
 * 
 * @since 1.0.0
 */
public class DirectoryClassFileContainer implements IClassFileContainer {
	
	/**
	 * Root directory of the class file container
	 */
	private File fRoot;
	
	/**
	 * Map of package names to associated directory (file)
	 */
	private Map fPackages;
	
	/**
	 * Cache of package names
	 */
	private String[] fPackageNames;
	
	/**
	 * Implementation of a class file in the local file system.
	 * 
	 * @since 1.0.0
	 */
	class ClassFile extends AbstractClassFile implements Comparable {
		
		/**
		 * Associated file
		 */
		private File fFile;
		
		/**
		 * Qualified type name. Package is dot separated and types are $-separated.
		 */
		private String fTypeName;
		
		/**
		 * Constructs a class file on the given file
		 * @param file file
		 * @param qualified type name
		 */
		public ClassFile(File file, String typeName) {
			fFile = file;
			fTypeName = typeName;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.model.component.IClassFile#getTypeName()
		 */
		public String getTypeName() {
			return fTypeName;
		}

		/* (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		public int compareTo(Object o) {
			return fTypeName.compareTo(((ClassFile)o).fTypeName);
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object obj) {
			if (obj instanceof ClassFile) {
				return ((ClassFile) obj).fTypeName.equals(this.fTypeName);
			}
			return false;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		public int hashCode() {
			return this.fTypeName.hashCode();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.model.component.IClassFile#getInputStream()
		 */
		public InputStream getInputStream() throws CoreException {
			try {
				return new FileInputStream(fFile);
			} catch (FileNotFoundException e) {
				abort("File not found", e);
			}
			return null; // never reaches here
		}
	}	
	
	/**
	 * Constructs a class file container rooted at the specified path.
	 * 
	 * @param location absolute path in the local file system
	 */
	public DirectoryClassFileContainer(String location) {
		fRoot = new File(location);
	}

	/**
	 * Throws a core exception with the given message and underlying exception,
	 * if any.
	 * 
	 * @param message error message
	 * @param e underlying exception or <code>null</code>
	 * @throws CoreException
	 */
	private void abort(String message, Throwable e) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID, message, e));
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IClassFileContainer#accept(org.eclipse.pde.api.tools.model.component.ClassFileContainerVisitor)
	 */
	public void accept(ClassFileContainerVisitor visitor) throws CoreException {
		init();
		String[] packageNames = getPackageNames();
		for (int i = 0; i < packageNames.length; i++) {
			String pkg = packageNames[i];
			if (visitor.visitPackage(pkg)) {
				File dir = (File) fPackages.get(pkg);
				File[] files = dir.listFiles(new FileFilter() {
					public boolean accept(File file) {
						return file.isFile() && file.getName().endsWith(Util.DOT_CLASS_SUFFIX);
					}
				});
				List classFiles = new ArrayList();
				for (int j = 0; j < files.length; j++) {
					File file = files[j];
					String name = file.getName();
					String typeName = name.substring(0, name.length() - 6);
					if (pkg.length() > 0) {
						typeName = pkg + "." + typeName; //$NON-NLS-1$
					}
					classFiles.add(new ClassFile(file, typeName));
				}
				Collections.sort(classFiles);
				Iterator cfIterator = classFiles.iterator();
				while (cfIterator.hasNext()) {
					IClassFile classFile = (IClassFile) cfIterator.next();
					visitor.visit(pkg, classFile);
					visitor.end(pkg, classFile);
				}
			}
			visitor.endVisitPackage(pkg);
		}
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
	public IClassFile findClassFile(String qualifiedName) throws CoreException {
		init();
		int index = qualifiedName.lastIndexOf('.');
		String cfName = qualifiedName;
		String pkg = Util.DEFAULT_PACKAGE_NAME;
		if (index > 0) {
			pkg = qualifiedName.substring(0, index);
			cfName = qualifiedName.substring(index + 1);
		}
		File dir = (File) fPackages.get(pkg);
		if (dir != null) {
			File file = new File(dir, cfName + Util.DOT_CLASS_SUFFIX);
			if (file.exists()) {
				return new ClassFile(file, qualifiedName);
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.manifest.IClassFileContainer#findClassFiles(java.lang.String)
	 */
	public IClassFile[] findClassFiles(String qualifiedName) throws CoreException {
		IClassFile classFile = findClassFile(qualifiedName);
		if (classFile == null) {
			return Util.NO_CLASS_FILES;
		}
		return new IClassFile[] { classFile };
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IClassFileContainer#getPackageNames()
	 */
	public String[] getPackageNames() throws CoreException {
		init();
		if (fPackageNames == null) {
			List names = new ArrayList(fPackages.keySet());
			Collections.sort(names);
			fPackageNames = (String[]) names.toArray(new String[names.size()]);
		}
		return fPackageNames;
	}
	
	/**
	 * Builds cache of package names to directories
	 */
	private synchronized void init() {
		if (fPackages == null) {
			fPackages = new HashMap();
			processDirectory(Util.DEFAULT_PACKAGE_NAME, fRoot);
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
					dirs.add(file);
				} else if (!hasClassFiles) {
					if (file.getName().endsWith(Util.DOT_CLASS_SUFFIX)) {
						fPackages.put(packageName, dir);
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
}
