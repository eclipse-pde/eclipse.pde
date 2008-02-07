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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.ClassFileContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.IClassFile;
import org.eclipse.pde.api.tools.internal.provisional.IClassFileContainer;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Common implementation of a composite class file container.
 * 
 * @since 1.0.0
 */
public abstract class AbstractClassFileContainer implements IClassFileContainer {
	
	/**
	 * Collection of class file containers
	 */
	private List fClassFileContainers = null;	
		

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IClassFileContainer#accept(org.eclipse.pde.api.tools.model.component.ClassFileContainerVisitor)
	 */
	public void accept(ClassFileContainerVisitor visitor) throws CoreException {
		IClassFileContainer[] containers = getClassFileContainers();
		for (int i = 0; i < containers.length; i++) {
			containers[i].accept(visitor);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IClassFileContainer#close()
	 */
	public synchronized void close() throws CoreException {
		MultiStatus multi = null;
		IStatus single = null;
		IClassFileContainer[] containers = getClassFileContainers();
		for (int i = 0; i < containers.length; i++) {
			try {
				containers[i].close();
			} catch (CoreException e) {
				if (single == null) {
					single = e.getStatus();
				} else {
					if (multi == null) {
						multi = new MultiStatus(ApiPlugin.getPluginIdentifier(), single.getCode(), single.getMessage(), single.getException());
					}
					multi.add(e.getStatus());
				}
			}
		}
		if (multi != null) {
			throw new CoreException(multi);
		}
		if (single != null) {
			throw new CoreException(single);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IClassFileContainer#findClassFile(java.lang.String)
	 */
	public IClassFile findClassFile(String qualifiedName) throws CoreException {
		IClassFileContainer[] containers = getClassFileContainers();
		for (int i = 0; i < containers.length; i++) {
			IClassFile file = containers[i].findClassFile(qualifiedName);
			if (file != null) {
				return file;
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IClassFileContainer#findClassFile(java.lang.String)
	 */
	public IClassFile[] findClassFiles(String qualifiedName) throws CoreException {
		IClassFile[] allClassFiles = null;
		int counter = 0;
		IClassFileContainer[] containers = getClassFileContainers();
		for (int i = 0; i < containers.length; i++) {
			IClassFile file = containers[i].findClassFile(qualifiedName);
			if (file != null) {
				if (allClassFiles == null) {
					allClassFiles = new IClassFile[2];
					allClassFiles[counter++] = file;
				} else {
					if (counter == allClassFiles.length) {
						System.arraycopy(
								allClassFiles,
								0,
								(allClassFiles = new IClassFile[counter + 1]),
								0,
								counter);
					}
					allClassFiles[counter++] = file;
				}
			}
		}
		if (counter == 0) {
			return Util.NO_CLASS_FILES;
		}
		if (counter != allClassFiles.length) {
			System.arraycopy(
					allClassFiles,
					0,
					(allClassFiles = new IClassFile[counter]),
					0,
					counter);
		}
		return allClassFiles;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IClassFileContainer#getPackageNames()
	 */
	public String[] getPackageNames() throws CoreException {
		List names = new ArrayList();
		IClassFileContainer[] containers = getClassFileContainers();
		for (int i = 0; i < containers.length; i++) {
			String[] packageNames = containers[i].getPackageNames();
			for (int j = 0; j < packageNames.length; j++) {
				names.add(packageNames[j]);
			}
		}
		return (String[]) names.toArray(new String[names.size()]);
	}
	
	/**
	 * Throws a core exception with the given message and underlying exception,
	 * if any.
	 * 
	 * @param message error message
	 * @param e underlying exception or <code>null</code>
	 * @throws CoreException
	 */
	protected void abort(String message, Throwable e) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, ApiPlugin.getPluginIdentifier(), message, e));
	}


	/**
	 * Returns the class file containers in this container. Creates the containers if
	 * they are not yet created.
	 * 
	 * @return class file containers
	 */
	protected synchronized IClassFileContainer[] getClassFileContainers() {
		if (fClassFileContainers == null) {
			try {
				fClassFileContainers = createClassFileContainers();
			} catch (CoreException e) {
				ApiPlugin.log(e);
			}
		}
		return (IClassFileContainer[]) fClassFileContainers.toArray(new IClassFileContainer[fClassFileContainers.size()]);
	}	
	
	/**
	 * Creates and returns the class file containers for this component.
	 * Subclasses must override.
	 * 
	 * @return list of class file containers for this component
	 */
	protected abstract List createClassFileContainers() throws CoreException;	

	/**
	 * Sets the class file containers in this container.
	 * 
	 * @param containers class file containers
	 */
	protected synchronized void setClassFileContainers(IClassFileContainer[] containers) {
		if (fClassFileContainers != null) { 
			try {
				close();
			} catch (CoreException e) {
				// TODO log error
			}
			fClassFileContainers.clear();
		} else {
			fClassFileContainers = new ArrayList(containers.length);
		}
		for (int i = 0; i < containers.length; i++) {
			fClassFileContainers.add(containers[i]);
		}
	}

	/**
	 * Resets the class file containers for this container. 
	 * A reset constitutes setting the cached containers to <code>null</code>
	 */
	protected synchronized void resetClassFileContainers() {
		if(fClassFileContainers != null) {
			try {
				close();
			} catch (CoreException e) {
				ApiPlugin.log(e.getStatus());
			}
			fClassFileContainers.clear();
		}
		fClassFileContainers = null;
	}
}
