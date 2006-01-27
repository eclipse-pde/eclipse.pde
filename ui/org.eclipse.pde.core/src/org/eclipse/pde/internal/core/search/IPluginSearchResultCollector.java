/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.search;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.core.plugin.IPluginObject;

public interface IPluginSearchResultCollector {
	
	void accept(IPluginObject match);
	
	void done();

	PluginSearchOperation getOperation();
	
	void searchStarted();
	
	void setOperation(PluginSearchOperation operation);
	
	void setProgressMonitor(IProgressMonitor monitor);
	
}
