/*
 * Copyright (c) 2002 IBM Corp.  All rights reserved.
 * This file is made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 */
package org.eclipse.pde.internal.core.ifeature;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.IPluginReference;
/**
 * @version 	1.0
 * @author
 */
public interface IFeatureImport extends IFeatureObject, IPluginReference, IEnvironment {
	String P_TYPE = "type";
	String P_PATCH = "patch";
	
	int PLUGIN = 0;
	int FEATURE = 1;
	
	int getType();
	
	void setType(int type) throws CoreException;
	
	boolean isPatch();
	void setPatch(boolean patch) throws CoreException;
}
