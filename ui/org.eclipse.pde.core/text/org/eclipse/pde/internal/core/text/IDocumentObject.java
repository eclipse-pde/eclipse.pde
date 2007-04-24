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

package org.eclipse.pde.internal.core.text;

import java.io.Serializable;

import org.eclipse.pde.core.plugin.ISharedPluginModel;
import org.eclipse.pde.internal.core.ischema.ISchema;

/**
 * IDocumentObject
 *
 */
public interface IDocumentObject extends Serializable {
	// TODO: MP: CCP TOUCH

	public void reconnect(ISharedPluginModel model, ISchema schema, IDocumentNode parent);
	
}
