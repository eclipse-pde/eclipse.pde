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

package org.eclipse.pde.internal.core.cheatsheet.simple;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.pde.core.IModelChangeProvider;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject;

/**
 * SimpleCSObject
 *
 */
public abstract class SimpleCSObject extends PlatformObject implements ISimpleCSObject {

	private transient ISimpleCSModel fModel;
	
	private transient ISimpleCSObject fParent;

	protected static final HashSet TAG_EXCEPTIONS = new HashSet(3);
	
	protected static final HashMap SUBSTITUTE_CHARS = new HashMap(5);

	
	static {
		TAG_EXCEPTIONS.add("b"); //$NON-NLS-1$
		TAG_EXCEPTIONS.add("/b"); //$NON-NLS-1$
		TAG_EXCEPTIONS.add("br/"); //$NON-NLS-1$
		
		SUBSTITUTE_CHARS.put(new Character('&'), "&amp;"); //$NON-NLS-1$
		SUBSTITUTE_CHARS.put(new Character('<'), "&lt;"); //$NON-NLS-1$
		SUBSTITUTE_CHARS.put(new Character('>'), "&gt;"); //$NON-NLS-1$
		SUBSTITUTE_CHARS.put(new Character('\''), "&apos;"); //$NON-NLS-1$
		SUBSTITUTE_CHARS.put(new Character('\"'), "&quot;"); //$NON-NLS-1$
	}
	
	/**
	 * 
	 */
	public SimpleCSObject(ISimpleCSModel model, ISimpleCSObject parent) {
		fModel = model;
		fParent = parent;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#getModel()
	 */
	public ISimpleCSModel getModel() {
		return fModel;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#getSimpleCS()
	 */
	public ISimpleCS getSimpleCS() {
		return fModel.getSimpleCS();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#setModel(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel)
	 */
	public void setModel(ISimpleCSModel model) {
		fModel = model;
	}

	/**
	 * @param property
	 * @param oldValue
	 * @param newValue
	 */
	protected void firePropertyChanged(String property, Object oldValue,
			Object newValue) {
		firePropertyChanged(this, property, oldValue, newValue);
	}
		
	/**
	 * @param object
	 * @param property
	 * @param oldValue
	 * @param newValue
	 */
	private void firePropertyChanged(ISimpleCSObject object, String property,
		Object oldValue, Object newValue) {
		if (fModel.isEditable()) {
			IModelChangeProvider provider = fModel;
			provider.fireModelObjectChanged(object, property, oldValue, newValue);
		}
	}
		
	/**
	 * @param child
	 * @param changeType
	 */
	protected void fireStructureChanged(ISimpleCSObject child, int changeType) {
		fireStructureChanged(new ISimpleCSObject[] { child }, changeType);
	}
	
	/**
	 * @param newValue
	 * @param oldValue
	 * @param changeType
	 */
	protected void fireStructureChanged(ISimpleCSObject newValue,
			ISimpleCSObject oldValue) {

		int changeType = -1;
		ISimpleCSObject object = null;
		if (newValue == null) {
			changeType = IModelChangedEvent.REMOVE;
			object = oldValue;
		} else {
			changeType = IModelChangedEvent.INSERT;
			object = newValue;
		}
		fireStructureChanged(object, changeType);
	}
	
	/**
	 * @param children
	 * @param changeType
	 */
	private void fireStructureChanged(ISimpleCSObject[] children,
			int changeType) {
		if (fModel.isEditable()) {
			IModelChangeProvider provider = fModel;
			provider.fireModelChanged(new ModelChangedEvent(provider,
					changeType, children, null));
		}
	}
		
	/**
	 * @return
	 */
	protected boolean isEditable() {
		return fModel.isEditable();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#getParent()
	 */
	public ISimpleCSObject getParent() {
		return fParent;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#getType()
	 */
	public abstract int getType();
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#getName()
	 */
	public abstract String getName();
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#getChildren()
	 */
	public abstract List getChildren();

}
