/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.ibundle;

import org.eclipse.pde.core.plugin.IFragment;

/**
 * An extension of bundle plug-in base that is used 
 * specifically for root model objects of fragments with
 * OSGi manifest. The goal is to continue to preserve
 * pre-3.0 compatibility for all the clients that
 * depend on IFragment interface.
 * 
 * @since 3.0
 */
public interface IBundleFragment extends IBundlePluginBase, IFragment {

}
