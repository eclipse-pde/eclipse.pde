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
 * Provides the values of the fields in the mandatory first
 * page of the template wizard. This interface allows 
 * templates to initialize options that are dependent on 
 * these values and cannot be initialized without context.
 * <p>
 * <b>Note:</b> This interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 */
public interface IFieldData {
	/**
	 * Returns the chosen name of the plug-in.
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	String getName();
	/**
	 * Returns the chosen version of the plug-in.
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	String getVersion();
	/**
	 * Returns the provider of the plug-in.
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	String getProvider();
	/**
	 * Returns the name of the top-level plug-in class (if this
	 * class is specified) or <samp>null</samp> if not applicable.
	 * <p> 
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public String getClassName();
	/**
	 * Returns true if the project that is about to be
	 * created is for a fragment, or false it is to be a plug-in.
	 *
	 * @return true if a fragment, false for a plug-in
	 */
	public boolean isFragment();
	
	public String getPluginId();
	public String getPluginVersion();
	public int getMatch();
	public boolean isDoMain();
	public boolean isThisCheck();
	public boolean isBundleCheck();
	public boolean isWorkspaceCheck();
	public boolean hasPreference();
	public boolean isUIPlugin();
	
}