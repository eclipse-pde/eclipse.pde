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
package org.eclipse.pde.core.build;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IWritable;
/**
 * Jar entry represents one 'library=folder list' entry
 * in plugin.jars file.
 */
public interface IBuildEntry extends IWritable {
	/**
	 * A property name for changes to the 'name' field.
	 */
	public static final String P_NAME = "name"; //$NON-NLS-1$
	/**
	 * The prefix for any key denoting the source folders that
	 * should be compiled into a JAR.  The suffix will
	 * be the name of the JAR.
	 */
	public static final String JAR_PREFIX = "source."; //$NON-NLS-1$
	/**
	 * The prefix for any key denoting output folders for a particular 
	 * JAR.  The suffix will be the name of the JAR.
	 */	
	public static final String OUTPUT_PREFIX = "output."; //$NON-NLS-1$
	/**
	 * The name of the key that lists all the folders and files
	 * to be included in the binary build.
	 */	
	public static final String BIN_INCLUDES = "bin.includes"; //$NON-NLS-1$
	/**
	 * The name of the key that lists all the folders and files
	 * to be included in the source build.
	 */	
	public static final String SRC_INCLUDES = "src.includes"; //$NON-NLS-1$
	/**
	 * The name of the key that declares extra library entries to be added
	 * to the class path at build time only..
	 */	
	public static final String JARS_EXTRA_CLASSPATH = "jars.extra.classpath"; //$NON-NLS-1$
	/**
	 * The name of the key that declares additiona plug-in dependencies to augment development classpath
	 */	
	public static final String ADDITIONAL_BUNDLES = "additional.bundles"; //$NON-NLS-1$
	/**
	 * Adds the token to the list of token for this entry.
	 * This method will throw a CoreException if
	 * the model is not editable.
	 *
	 * @param token a name to be added to the list of tokens
	 */
	void addToken(String token) throws CoreException;
	/**
	 * Returns a model that owns this entry
	 * @return build.properties model
	 */
	IBuildModel getModel();
	/**
	 * Returns the name of this entry.
	 * @return the entry name
	 */
	String getName();
	/**
	 * Returns an array of tokens for this entry
	 * @return array of tokens
	 */
	String[] getTokens();

	/**
	 * Returns true if the provided token exists in this entry.
	 * @return true if the token exists in the entry
	 */
	boolean contains(String token);
	/**
	 * Removes the token from the list of tokens for this entry.
	 * This method will throw a CoreException if
	 * the model is not editable.
	 *
	 * @param token a name to be removed from the list of tokens
	 */
	void removeToken(String token) throws CoreException;
	/**
	 * Changes the name of the token without changing its
	 * position in the list. This method will throw
	 * a CoreException if the model is not editable.
	 *
	 * @param oldToken the old token name
	 * @param newToken the new token name
	 */
	void renameToken(String oldToken, String newToken) throws CoreException;
	/**
	 * Sets the name of this build entry. This
	 * method will throw a CoreException if
	 * model is not editable.
	 *
	 * @param name the new name for the entry
	 */
	void setName(String name) throws CoreException;
}
