package org.eclipse.pde.internal.base.model.plugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.base.model.IWritable;
/**
 * Jar entry represents one 'library=folder list' entry
 * in plugin.jars file.
 */
public interface IJarEntry extends IWritable {
/**
 * A property name for changes to the 'name' field.
 */
	public static final String P_NAME = "name";
/**
 * Adds the folder to the list of folders for this entry.
 * This method will throw a CoreException if
 * the model is not editable.
 *
 * @param folderName a name to be added to the list of folders
 */
void addFolderName(String folderName) throws CoreException;
/**
 * Returns an array of folder names for this entry
 * @return array of folder names
 */
String[] getFolderNames();
/**
 * Returns a model that owns this entry
 * @return plugin.jars model
 */
IJarsModel getModel();
/**
 * Returns the name of this entry.
 */
String getName();
/**
 * Removes the folder from the list of folders for this entry.
 * This method will throw a CoreException if
 * the model is not editable.
 *
 * @param folderName a name to be removed from the list of folders
 */
void removeFolderName(String root) throws CoreException;
/**
 * Changes the name of the folder without changing its
 * position in the list. This method will throw
 * a CoreException if the model is not editable.
 *
 * @param oldName the old folder name
 * @param newName the new folder name
 */
void renameFolder(String oldName, String newName) throws CoreException;
/**
 * Sets the name of this Jar entry. This
 * method will throw a CoreException if
 * model is not editable.
 *
 * @param name the new name for the entry
 */
void setName(String name) throws CoreException;
}
