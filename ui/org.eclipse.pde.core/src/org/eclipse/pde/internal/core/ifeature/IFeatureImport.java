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
public interface IFeatureImport extends IFeatureObject, IPluginReference {
	String P_PATCH = "patch";
	String P_KIND = "kind";

	int KIND_PLUGIN = 0;
	int KIND_FEATURE = 1;	
/**
 * Returns true if this reference entry is used to
 * declare the feature as a patch of the referenced feature.
 */
	boolean isPatch();
	
	void setPatch(boolean value) throws CoreException;
/**
 * Returns kind of the import (KIND_PLUGIN or KIND_FEATURE).
 */
	int getKind();
	
	void setKind(int kind) throws CoreException;
}
