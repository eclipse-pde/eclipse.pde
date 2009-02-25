/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui;

/**
 * The class that implements this interface is used to provide information
 * captured in the 'New Plug-in Project' wizard pages as entered by the user.
 * The information is the provided to other consumers when generating content so
 * that the content can be configured/customized according to the data.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 2.0
 */
public interface IFieldData {
	/**
	 * Plug-in identifier field.
	 * 
	 * @return plug-in identifier as entered in the wizard
	 */
	String getId();

	/**
	 * Plug-in version field.
	 * 
	 * @return plug-in version as entered in the wizard
	 */
	String getVersion();

	/**
	 * Plug-in name field
	 * 
	 * @return plug-in name as entered in the wizard
	 */
	String getName();

	/**
	 * Plug-in provider field
	 * 
	 * @return plug-in provider as entered in the wizard
	 */
	String getProvider();

	/**
	 * Plug-in library field
	 * 
	 * @return the name of the initial Java library
	 */
	String getLibraryName();

	/**
	 * Source folder field
	 * 
	 * @return the name of the Java source folder
	 */
	String getSourceFolderName();

	/**
	 * Output folder field
	 * 
	 * @return the name of the Java output folder
	 */
	String getOutputFolderName();

	/**
	 * Legacy selection
	 * 
	 * @return <code>true</code> if the plug-in is created for use with
	 *         products based on Eclipse before release 3.0, <code>false</code>
	 *         if the plug-in is compatible with Eclipse 3.0.
	 */
	boolean isLegacy();

	/**
	 * OSGi bundle selection
	 * 
	 * @return <code>true</code> if the plug-in has structure as expected by
	 *         OSGi framework in Eclipse 3.0 runtime, <code>false</code> if
	 *         the plug-in has standard pre-3.0 layout.
	 */
	boolean hasBundleStructure();

	/**
	 * Simple project selection
	 * 
	 * @return <code>true</code> if the plug-in should have no Java code and
	 *         nature, <code>false</code> otherwise.
	 */
	boolean isSimple();
}
