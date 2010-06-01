/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rafael Oliveira Nóbrega <rafael.oliveira@gmail.com> - bug 223738
 *******************************************************************************/
package org.eclipse.pde.internal.ds.core.text;

import java.io.Serializable;

import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.core.text.DocumentObject;
import org.eclipse.pde.internal.ds.core.IDSComponent;
import org.eclipse.pde.internal.ds.core.IDSConstants;
import org.eclipse.pde.internal.ds.core.IDSModel;
import org.eclipse.pde.internal.ds.core.IDSObject;

/**
 * All modeled objects of a declarative services xml file must extend from this
 * abstract class.
 * 
 * @since 3.4
 * @see DSModel
 * @see DSDocumentFactory
 */
public abstract class DSObject extends DocumentObject implements IDSConstants,
		Serializable, IDSObject {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructs the DSObject and initializes its attributes.
	 * 
	 * @param model
	 *            The model to associate with this DSObject
	 * @param tagName
	 *            The xml tag name for this object
	 */
	public DSObject(DSModel model, String tagName) {
		super(model, tagName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.core.text.DocumentElementNode#getAttributeIndent()
	 */
	protected String getAttributeIndent() {
		return " "; //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.core.text.DocumentElementNode#getContentIndent()
	 */
	protected String getContentIndent() {
		return ""; //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSObject#canBeParent()
	 */
	public abstract boolean canBeParent();

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.DSObject#canAddChild(int)
	 */
	public abstract boolean canAddChild(int objectType);

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSObject#getModel()
	 */
	public IDSModel getModel() {
		final IModel sharedModel = getSharedModel();
		if (sharedModel instanceof DSModel) {
			return (DSModel) sharedModel;
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSObject#getRoot()
	 */
	public IDSComponent getComponent() {
		final IDSModel model = getModel();
		if (model != null) {
			return model.getDSComponent();
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSObject#getName()
	 */
	public abstract String getName();

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSObject#getType()
	 */
	public abstract int getType();
	
}
