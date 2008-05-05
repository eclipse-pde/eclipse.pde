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

public interface IDSReference extends IDSObject {

	public void setReferenceName(String name);

	public String getReferenceName();

	public void setReferenceInterface(String interfaceName);

	public String getReferenceInterface();

	public void setReferenceCardinality(String cardinality);

	public String getReferenceCardinality();

	public void setReferencePolicy(String policy);

	public String getReferencePolicy();

	public void setReferenceTarget(String target);

	public String getReferenceTarget();

	public void setReferenceBind(String bind);

	public String getReferenceBind();

	public void setReferenceUnbind(String unbind);

	public String getReferenceUnbind();

}
