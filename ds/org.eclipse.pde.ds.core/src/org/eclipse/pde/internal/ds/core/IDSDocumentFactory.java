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

import org.eclipse.pde.internal.ds.core.text.DSImplementation;
import org.eclipse.pde.internal.ds.core.text.DSProperties;
import org.eclipse.pde.internal.ds.core.text.DSProperty;
import org.eclipse.pde.internal.ds.core.text.DSProvide;
import org.eclipse.pde.internal.ds.core.text.DSReference;
import org.eclipse.pde.internal.ds.core.text.DSRoot;
import org.eclipse.pde.internal.ds.core.text.DSService;

public interface IDSDocumentFactory {

	public abstract DSProvide createProvide();

	public abstract DSProperty createProperty();

	public abstract DSReference createReference();

	public abstract DSService createService();

	public abstract DSProperties createProperties();

	public abstract DSImplementation createImplementation();

	public abstract DSRoot createRoot();

}
