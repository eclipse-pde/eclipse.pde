package org.eclipse.pde.internal.base.model.plugin;

import org.eclipse.core.runtime.CoreException;
/**
 * The class that implements this interface represents a
 * reference to the library that is defined in the plug-in
 * manifest.
 */
public interface IPluginLibrary extends IPluginObject {
/**
 * A name of the property that will be used to
 * notify about changes of the "exported" field.
 */
	public static final String P_EXPORTED = "exported";
/**
 * A name of the property that will be used to
 * notify about changes in the content filters.
 */
	public static final String P_CONTENT_FILTERS = "contentFilters";
/**
 * Returns optional context filters that
 * should be applied to calculate what classes
 * to export from this library.
 *
 *@return an array of content filter strings
 */
String[] getContentFilters();
/**
 * Returns true if this library contains types
 * that will be visible to other plug-ins.
 *
 * @return true if there are exported types in the library
 */
boolean isExported();
/**
 * Returns true if all the types in this library
 * will be visible to other plug-ins.
 *
 * @return true if all the types are exported
 * in the library
 */
boolean isFullyExported();
/**
 * Sets the optional content filters for
 * this library. This method may throw
 * a CoreException if the model is not
 * editable.
 *
 * @param filters an array of filter strings
 */
void setContentFilters(String[] filters) throws CoreException;
/**
 * Sets whether types in this library will be
 * visible to other plug-ins. This method
 * may throw a CoreException if the model is
 * not editable.
 *
 *
 */
void setExported(boolean value) throws CoreException;
}
