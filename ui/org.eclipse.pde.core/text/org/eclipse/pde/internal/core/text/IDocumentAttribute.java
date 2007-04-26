/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.ISharedPluginModel;
import org.eclipse.pde.internal.core.ischema.ISchema;

public interface IDocumentAttribute extends Serializable {
	
	void setEnclosingElement(IDocumentNode node);	
	IDocumentNode getEnclosingElement();
	
	void setNameOffset(int offset);
	int getNameOffset();
	
	void setNameLength(int length);
	int getNameLength();
	
	void setValueOffset(int offset);
	int getValueOffset();
	
	void setValueLength(int length);
	int getValueLength();
	
	String getAttributeName();
	String getAttributeValue();
	
	void setAttributeName(String name) throws CoreException;
	void setAttributeValue(String value) throws CoreException;
	
	String write();

	public void reconnect(ISharedPluginModel model, ISchema schema, IDocumentNode parent);
	
}
