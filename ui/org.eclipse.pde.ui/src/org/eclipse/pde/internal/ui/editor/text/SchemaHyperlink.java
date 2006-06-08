/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

	public void open() {
		if (fResource != null)
			SchemaEditor.openSchema(fResource.getProject().getFile(fElement));
	}

}
