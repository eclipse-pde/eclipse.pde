/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core.osgi.bundle;

import org.eclipse.pde.core.plugin.*;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public interface IBundlePluginModelBase extends IPluginModelBase {
	IBundleModel getBundleModel();
	IExtensionsModel getExtensionsModel();
	void setBundleModel(IBundleModel bundleModel);
	void setExtensionsModel(IExtensionsModel extensionsModel);
	IPluginImport createImport();
	IPluginLibrary createLibrary();
	void save();
}