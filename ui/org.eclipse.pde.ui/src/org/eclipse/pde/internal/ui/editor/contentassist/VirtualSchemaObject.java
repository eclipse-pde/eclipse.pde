/*******************************************************************************
 *  Copyright (c) 2006, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
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

	@Override
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

	@Override
	public String getName() {
		return fName;
	}

	@Override
	public ISchemaObject getParent() {
		return null;
	}

	@Override
	public ISchema getSchema() {
		return null;
	}

	@Override
	public void setParent(ISchemaObject parent) {
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}

	@Override
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
