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
package org.eclipse.pde.ui.templates;

/**
 * TODO decide where this interface should go. Ad javadoc.
 *
 */
public interface IFieldData {
	
	String getId();
	
	String getVersion();
	
	String getName();
	
	String getProvider();
	
	String getLibraryName();
	
	String getSourceFolderName();
	
	String getOutputFolderName();
	
	boolean isLegacy();
	
	boolean hasBundleStructure();
	
	boolean isSimple();
}
