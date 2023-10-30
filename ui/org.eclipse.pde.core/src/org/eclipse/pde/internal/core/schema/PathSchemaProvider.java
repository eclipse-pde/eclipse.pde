/*******************************************************************************
 *  Copyright (c) 2000, 2023 IBM Corporation and others.
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
 *     Christoph Läubrich - extract to schema provider
 *******************************************************************************/
package org.eclipse.pde.internal.core.schema;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaDescriptor;

public class PathSchemaProvider implements SchemaProvider {

	private List<IPath> searchPath;

	public PathSchemaProvider(List<IPath> searchPath) {
		this.searchPath = searchPath;
	}

	@Override
	public ISchema createSchema(ISchemaDescriptor descriptor, String location) {

		try {
			URL schemaURL = IncludedSchemaDescriptor.computeURL(descriptor, location, searchPath);
			if (schemaURL == null) {
				return null;
			}
			Schema ischema = new Schema(null, schemaURL, false);
			ischema.load();
			return ischema;
		} catch (MalformedURLException e) {
			return null;
		}
	}

}
