/*******************************************************************************
 *  Copyright (c) 2006, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.contentassist;

import java.io.PrintWriter;
import java.net.URL;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.core.schema.SchemaAnnotationHandler;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;
import org.eclipse.pde.internal.core.util.SchemaUtil;
import org.eclipse.pde.internal.core.util.XMLComponentRegistry;

public class VirtualSchemaObject implements ISchemaObject {

	private String fName;

	private Object fDescription;

	private int fType;

	public VirtualSchemaObject(String name, Object description, int type) {
		fName = name;
		fDescription = description;
		fType = type;
	}

	public String getDescription() {
		if (fDescription instanceof String) {
			return (String) fDescription;
		} else if (fDescription instanceof IPluginExtensionPoint) {
			// Making the description an Object was necessary to defer
			// the retrieval of the schema description String to
			// only when it is need - instead of ahead of time.
			// Retrieval of the String involves reparsing the schema from
			// file which is has a huge performance cost during content
			// assist sessions.
			return getSchemaDescription((IPluginExtensionPoint) fDescription);
		}
		return null;
	}

	public String getName() {
		return fName;
	}

	public ISchemaObject getParent() {
		return null;
	}

	public ISchema getSchema() {
		return null;
	}

	public void setParent(ISchemaObject parent) {
	}

	public Object getAdapter(Class adapter) {
		return null;
	}

	public void write(String indent, PrintWriter writer) {
	}

	public int getVType() {
		return fType;
	}

	public void setVType(int type) {
		fType = type;
	}

	private String getSchemaDescription(IPluginExtensionPoint point) {
		String description = null;
		if (point != null) {
			description = XMLComponentRegistry.Instance().getDescription(point.getFullId(), XMLComponentRegistry.F_SCHEMA_COMPONENT);
			if (description == null) {
				URL url = SchemaRegistry.getSchemaURL(point);
				if (url != null) {
					SchemaAnnotationHandler handler = new SchemaAnnotationHandler();
					SchemaUtil.parseURL(url, handler);
					description = handler.getDescription();
					XMLComponentRegistry.Instance().putDescription(point.getFullId(), description, XMLComponentRegistry.F_SCHEMA_COMPONENT);
				}
			}
		}

		return description;
	}

}
