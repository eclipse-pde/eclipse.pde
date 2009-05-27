/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.ibundle;

import org.eclipse.pde.core.plugin.IFragmentModel;

/**
 * An extension of the bundle plug-in model base that
 * is used specifically for fragment models with OSGi manifests.
 * 
 * @since 3.0
 */
public interface IBundleFragmentModel extends IBundlePluginModelBase, IFragmentModel {

}
