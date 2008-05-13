/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rafael Oliveira Nóbrega <rafael.oliveira@gmail.com> - bug 223738 
 *******************************************************************************/
package org.eclipse.pde.internal.ds.core;

import org.eclipse.core.resources.IResource;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelChangeProvider;

public interface IDSModel extends IModelChangeProvider, IModel {

	public abstract IDSDocumentFactory getFactory();

	public abstract IDSComponent getDSComponent();

	public abstract void setUnderlyingResource(IResource resource);

	public abstract void save();

}
