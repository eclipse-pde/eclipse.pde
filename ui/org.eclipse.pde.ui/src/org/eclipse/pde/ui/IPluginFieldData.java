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
package org.eclipse.pde.ui;

import org.eclipse.pde.ui.templates.*;

/**
 * TODO add javadoc
 *
 */
public interface IPluginFieldData extends IFieldData {
/**
 * TODO add javadoc
 * @return
 */	
	String getClassname();
/**
 * TODO add javadoc
 * @return
 */	
	boolean isUIPlugin();
/**
 * TODO add javadoc
 * @return
 */	
	ITemplateSection[] getTemplateSections();
/**
 * TODO add javadoc and change the signature
 * @return
 */	
	boolean doGenerateClass();
}