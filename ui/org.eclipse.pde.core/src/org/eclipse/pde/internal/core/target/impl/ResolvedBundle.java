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
package org.eclipse.pde.internal.core.target.impl;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.pde.internal.core.target.provisional.IBundleContainer;
import org.eclipse.pde.internal.core.target.provisional.IResolvedBundle;

/**
 * Resolved bundle implementation.
 * 
 * @since 3.5
 */
public class ResolvedBundle implements IResolvedBundle {

	private BundleInfo fInfo;
	private IBundleContainer fContainer;
	private boolean fIsSource = false;
	private IStatus fStatus;
	private boolean fIsOptional = false;
	private boolean fIsFragment = false;
	// when this bundle is an old-style source bundle, the "path" attribute of its extension is known 
	private String fSourcePath = null;

	/**
	 * Constructs a resolved bundle 
	 * 
	 * @param info underlying bundle
	 * @param status any status regarding the bundle or <code>null</code> if OK
	 * @param source whether the bundle is a source bundle
	 * @param optional whether the bundle is optional
	 * @param whether the bundle is a fragment
	 */
	ResolvedBundle(BundleInfo info, IBundleContainer parentContainer, IStatus status, boolean source, boolean optional, boolean fragment) {
		fInfo = info;
		fContainer = parentContainer;
		if (status == null) {
			fStatus = Status.OK_STATUS;
		} else {
			fStatus = status;
		}
		fIsSource = source;
		fIsOptional = optional;
		fIsFragment = fragment;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.IResolvedBundle#getBundleInfo()
	 */
	public BundleInfo getBundleInfo() {
		return fInfo;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.IResolvedBundle#getParentContainer()
	 */
	public IBundleContainer getParentContainer() {
		return fContainer;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.IResolvedBundle#getStatus()
	 */
	public IStatus getStatus() {
		return fStatus;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.IResolvedBundle#isOptional()
	 */
	public boolean isOptional() {
		return fIsOptional;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.IResolvedBundle#isSourceBundle()
	 */
	public boolean isSourceBundle() {
		return fIsSource;
	}

	/**
	 * Sets this bundle to be optional.
	 * 
	 * @param optional whether optional
	 */
	void setOptional(boolean optional) {
		fIsOptional = optional;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.IResolvedBundle#isFragment()
	 */
	public boolean isFragment() {
		return fIsFragment;
	}

	/**
	 * Used to set the path attribute of an old-style source bundle.
	 * 
	 * @param path bundle relative path to source folders
	 */
	void setSourcePath(String path) {
		fSourcePath = path;
	}

	/**
	 * Returns bundle relative path to old-style source folders, or <code>null</code>
	 * if not applicable.
	 * 
	 * @return bundle relative path to old-style source folders, or <code>null</code>
	 */
	public String getSourcePath() {
		return fSourcePath;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer result = new StringBuffer().append(fInfo.toString());
		if (fStatus != null && !fStatus.isOK()) {
			result = result.append(' ').append(fStatus.toString());
		}
		return result.toString();
	}
}
