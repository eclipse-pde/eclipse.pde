/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.ischema;

import java.net.URL;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IModelChangeProvider;

/**
 * Objects of this class encapsulate data loaded from the XML Schema file that
 * defines an Eclipse extension point. These files are used for three reasons:
 * <ul>
 * <li>To provide grammar that can be used by validation parsers to validate
 * extensions that plug into this extension point
 * <li>To provide additional metadata about this extension point that can be
 * used by PDE to provide user assistence in plug-in development
 * <li>To provide enough material that can be used by tools to compose a
 * reference HTML documentation about this extension point.
 * </ul>
 * Objects of this class can be changed if editable. Other classes can register
 * as change listener in order to receive notification about these changes.
 */
public interface ISchema extends ISchemaObject, IBaseModel, IModelChangeProvider {
	String P_POINT = "pointId"; //$NON-NLS-1$

	String P_PLUGIN = "pluginId"; //$NON-NLS-1$

	int REFRESH_ADD = 1;

	int REFRESH_DELETE = 2;

	int REFRESH_RENAME = 3;

	/**
	 * Returns a reference to a schema element defined in this schema or
	 * <samp>null</samp> if not found.
	 * 
	 * @param name
	 *            name of the element to find
	 */
	ISchemaElement findElement(String name);

	/**
	 * Returns an array of schema elements that can be children of the provided
	 * schema element. The information is computed based on the grammar rules in
	 * this schema. The computation ignores number of occurances of each
	 * element. Therefore, it will always return the same result even if there
	 * could be only one element of a certain type in the document and it
	 * already exists.
	 * 
	 * @param element
	 *            the parent element that is used for the calculation
	 * @return an array of elements that can appear as children of the provided
	 *         element, according to the grammar of this schema
	 */
	ISchemaElement[] getCandidateChildren(ISchemaElement element);

	/**
	 * Returns an array of document sections that are defined in this schema.
	 * 
	 * @return an array of sections in this schema
	 */
	IDocumentSection[] getDocumentSections();

	/**
	 * Returns a number of elements with global scope defined in this schema.
	 * 
	 * @return number of global elements
	 */
	public int getElementCount();

	/**
	 * Returns a total number of elements after the included schemas have been
	 * resolved and their elements added to the list.
	 * 
	 * @return the total number of elements including external schemas
	 */
	public int getResolvedElementCount();

	/**
	 * Returns an array of elements with the global scope defined in this
	 * schema.
	 * 
	 * @return an array of global elements
	 */
	public ISchemaElement[] getElements();

	/**
	 * Returns an array of element names with the global scope defined in this
	 * schema.
	 * 
	 * @return an array of global elements
	 */
	public String[] getElementNames();

	/**
	 * Returns an array of elements with the global scope defined in this schema
	 * and all the included schemas.
	 * 
	 * @return an expanded array of global elements
	 */
	public ISchemaElement[] getResolvedElements();

	/**
	 * Returns an Id of the extension point that is defined in this schema.
	 * 
	 * @return extension point Id of this schema
	 */
	public String getQualifiedPointId();

	public String getPointId();

	public void setPointId(String pointId) throws CoreException;

	public String getPluginId();

	public void setPluginId(String pluginId) throws CoreException;

	/**
	 * Returns an object that holds a reference to this schema. Descriptors are
	 * responsible for loading and disposing schema objects and could be
	 * implemented in various ways, depending on whether the schema is defined
	 * inside the workspace or is referenced externally.
	 * 
	 * @return schema descriptor that holds this schema
	 */
	public ISchemaDescriptor getSchemaDescriptor();

	/**
	 * Returns a URL that defines this schema's location.
	 * 
	 * @return a URL that points to this schema's location.
	 */
	public URL getURL();

	/**
	 * Returns a list of elements that correspond to the <samp>include</samp>
	 * statements in the schema file. Included schemas are incorporated into the
	 * model and references can be made to elements in the included files.
	 * 
	 * @return an array of included schema elements or a zero-size array if
	 *         none.
	 */
	ISchemaInclude[] getIncludes();

	/**
	 * Returns whether the root schema element (the <extension> element)
	 * has been marked deprecated, making this schema deprecated.
	 * @return true if this schema is deprecated
	 */
	public boolean isDeperecated();

	/**
	 * Returns whether the root schema element (the <extension> element)
	 * has been marked internal, making this schema internal.
	 * @return true if this schema is internal
	 * 
	 * @since 3.4
	 */
	public boolean isInternal();

	/**
	 * Returns replacement schema in case this one is deprecated.
	 * @return the replacement schema 
	 */
	public String getDeprecatedSuggestion();

	/**
	 * Returns the schema version to use when writing xml
	 * @return schema version
	 */
	public double getSchemaVersion();
}
