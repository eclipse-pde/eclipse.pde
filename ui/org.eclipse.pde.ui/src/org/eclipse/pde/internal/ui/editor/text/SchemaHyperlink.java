/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IRegion;
import org.eclipse.pde.internal.ui.editor.schema.SchemaEditor;

public class SchemaHyperlink extends AbstractHyperlink {

	private IResource fResource;

	public SchemaHyperlink(IRegion region, String schema, IResource res) {
		super(region, schema);
		fResource = res;
	}

	@Override
	public void open() {
		if (fResource != null)
			SchemaEditor.openSchema(fResource.getProject().getFile(fElement));
	}

}
