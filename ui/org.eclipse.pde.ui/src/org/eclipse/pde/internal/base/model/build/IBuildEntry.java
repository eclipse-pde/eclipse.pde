package org.eclipse.pde.internal.base.model.build;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.base.model.IWritable;
/**
 * Jar entry represents one 'library=folder list' entry
 * in plugin.jars file.
 */
public interface IBuildEntry extends IWritable {

	/**
	 * A property name for changes to the 'name' field.
	 */
	public static final String P_NAME = "name";

	public static final String JAR_PREFIX = "source.";
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
 */
String getName();
/**
 * Returns an array of tokens for this entry
 * @return array of tokens
 */
String[] getTokens();

/**
 * Returns true if the provided token exists in this entry.
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
void renameToken(String oldName, String newName) throws CoreException;
/**
 * Sets the name of this build entry. This
 * method will throw a CoreException if
 * model is not editable.
 *
 * @param name the new name for the entry
 */
void setName(String name) throws CoreException;
}
