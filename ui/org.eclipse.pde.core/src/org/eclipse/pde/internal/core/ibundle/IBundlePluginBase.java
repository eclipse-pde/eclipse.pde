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

import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginLibrary;

/**
 * This interface represents an adapter that merges the notion of
 * a plug-in base (either plug-in or fragment) and OSGi bundle.
 * The goal is to maintain the expected plug-in interface
 * to the rest of the framework for both classic plug-ins
 * (with plugin.xml/fragment.xml) and new style (OSGi)
 * plug-ins with META-INF/MANIFEST.MF file.
 */
public interface IBundlePluginBase extends IPluginBase, IModelChangedListener {
	
	String getTargetVersion();
	
	void setTargetVersion(String target);
	
	public int getIndexOf(IPluginImport targetImport);
	
	public IPluginImport getPreviousImport(IPluginImport targetImport);
	
	public IPluginImport getNextImport(IPluginImport targetImport);
	
	public void add(IPluginImport iimport, int index);
	
	public int getIndexOf(IPluginLibrary targetLibrary);
	
	public IPluginLibrary getPreviousLibrary(IPluginLibrary targetLibrary);
	
	public IPluginLibrary getNextLibrary(IPluginLibrary targetLibrary);
	
	public void add(IPluginLibrary library, int index);	
	
}
