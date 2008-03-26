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

public interface IDSRoot {

	public void setAttributeName(String name);

	public String getAttributeName();

	public void setEnabled(boolean bool);

	public boolean getEnabled();

	public void setFactory(String factory);

	public String getFactory();

	public void setImmediate(boolean bool);

	public boolean getImmediate();

}
