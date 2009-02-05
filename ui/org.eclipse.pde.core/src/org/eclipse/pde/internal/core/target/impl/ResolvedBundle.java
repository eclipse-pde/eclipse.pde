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
import org.eclipse.pde.internal.core.target.provisional.IResolvedBundle;

/**
 * Resolved bundle implementation.
 * 
 * @since 3.5
 */
class ResolvedBundle implements IResolvedBundle {

	private BundleInfo fInfo;
	private boolean fIsSource = false;
	private IStatus fStatus;
	private boolean fIsOptional = false;
	private boolean fIsFragment = false;

	/**
	 * Constructs a resolved bundle 
	 * 
	 * @param info underlying bundle
	 * @param status any status regarding the bundle or <code>null</code> if OK
	 * @param source whether the bundle is a source bundle
	 * @param optional whether the bundle is optional
	 * @param whether the bundle is a fragment
	 */
	ResolvedBundle(BundleInfo info, IStatus status, boolean source, boolean optional, boolean fragment) {
		fInfo = info;
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

}
