package org.eclipse.pde.internal.ui.ischema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModelChangeProvider;
/**
 * Objects of this class encapsulate data loaded from
 * the XML Schema file that defines an Eclipse extension point.
 * These files are used for three reasons:
 * <ul>
 * <li>To provide grammar that can be used by validation parsers
 * to validate extensions that plug into this extension point
 * <li>To provide additional metadata about this extension point
 * that can be used by PDE to provide user assistence in plug-in
 * development
 * <li>To provide enough material that can be used by
 * tools to compose a reference HTML documentation about
 * this extension point.
 * </ul>
 * Objects of this class can be changed if editable.
 * Other classes can register as change listener in order
 * to receive notification about these changes.
 */
public interface ISchema extends ISchemaObject, IModelChangeProvider {
	String P_POINT = "pointId";
	String P_PLUGIN = "pluginId";
/**
 * Releases all data in this schema. From this point on,
 * the first subsequent reference of this schema will
 * trigger a reload.
 */
void dispose();
/**
 * Returns a reference to a schema element defined in
 * this schema or <samp>null</samp> if not found.
 * @param name name of the element to find
 */
ISchemaElement findElement(String name);
/**
 * Returns an array of schema elements that can be
 * children of the provided schema element. The information
 * is computed based on the grammar rules in this schema.
 * The computation ignores number of occurances of each
 * element. Therefore, it will always return the same
 * result even if there could be only one element of
 * a certain type in the document and it already exists.
 *
 * @param element the parent element that is used for the calculation
 * @return an array of elements that can appear as children of the
 * provided element, according to the grammar of this schema
 */
ISchemaElement [] getCandidateChildren(ISchemaElement element);
/**
 * Returns an array of document sections that are
 * defined in this schema.
 * @return an array of sections in this schema
 */
IDocumentSection[] getDocumentSections();
/**
 * Returns a number of elements with global scope defined in this schema.
 * @return number of global elements
 */
public int getElementCount();
/**
 * Returns an array of elements with the global scope defined in this schema.
 * @return an array of global elements
 */
public ISchemaElement[] getElements();
/**
 * Returns an Id of the extension point that is defined in this schema.
 * @return extension point Id of this schema
 */
public String getQualifiedPointId();

public String getPointId();
public void setPointId(String pointId) throws CoreException;
public String getPluginId();
public void setPluginId(String pluginId) throws CoreException;
/**
 * Returns an object that holds a reference to this schema.
 * Descriptors are responsible for loading and disposing
 * schema objects and could be implemented in various ways,
 * depending on whether the schema is defined inside the workspace
 * or is referenced externally.
 * @return schema descriptor that holds this schema
 */
public ISchemaDescriptor getSchemaDescriptor();
/**
 * Returns a URL that defines this schema's location.
 * @return a URL that points to this schema's location.
 */
public URL getURL();
/**
 * Returns whether this schema object has been exposed and can be reloaded.
 * @return true if this schema has been disposed
 */
boolean isDisposed();
/**
 * Returns whether this schema can be modified.
 * @return true if this schema can be modified 
 */
boolean isEditable();
}
