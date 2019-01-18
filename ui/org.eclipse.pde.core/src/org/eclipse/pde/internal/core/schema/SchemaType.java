/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.pde.internal.core.schema;

import java.io.Serializable;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaType;

public abstract class SchemaType implements ISchemaType, Serializable {
	private static final long serialVersionUID = 1L;
	private final String name;
	transient private ISchema schema;

	public SchemaType(ISchema schema, String typeName) {
		this.schema = schema;
		name = typeName;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public ISchema getSchema() {
		return schema;
	}

	@Override
	public void setSchema(ISchema schema) {
		this.schema = schema;
	}

	@Override
	public String toString() {
		return name;
	}
}
