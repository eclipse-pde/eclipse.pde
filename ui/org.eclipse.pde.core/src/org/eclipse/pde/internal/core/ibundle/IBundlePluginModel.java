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

import org.eclipse.pde.core.plugin.IPluginModel;

/**
 * An extension of the bundle plug-in model base that
 * is used specifically for plug-in models with OSGi manifests.
 * 
 * @since 3.0
 */
public interface IBundlePluginModel extends IBundlePluginModelBase, IPluginModel {

}
