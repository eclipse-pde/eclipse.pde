/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.util.ArrayList;

import org.eclipse.pde.internal.core.ifeature.IFeatureModel;

public class FeatureModelDelta implements IFeatureModelDelta {
	private ArrayList fAdded;

	private ArrayList fRemoved;

	private ArrayList fChanged;

	private int kind = 0;

	public FeatureModelDelta() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.core.IFeatureModelDelta#getKind()
	 */
	public int getKind() {
		return kind;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.core.IFeatureModelDelta#getAdded()
	 */
	public IFeatureModel[] getAdded() {
		return get(fAdded);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.core.IFeatureModelDelta#getRemoved()
	 */
	public IFeatureModel[] getRemoved() {
		return get(fRemoved);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.core.IFeatureModelDelta#getChanged()
	 */
	public IFeatureModel[] getChanged() {
		return get(fChanged);
	}

	private IFeatureModel[] get(ArrayList list) {
		if (list == null)
			return new IFeatureModel[0];
		return (IFeatureModel[]) list.toArray(new IFeatureModel[list.size()]);
	}

	void add(IFeatureModel model, int type) {
		switch (type) {
			case ADDED :
				fAdded = add(fAdded, model);
				break;
			case REMOVED :
				fRemoved = add(fRemoved, model);
				break;
			case CHANGED :
				fChanged = add(fChanged, model);
				break;
		}
		kind |= type;
	}

	private ArrayList add(ArrayList list, IFeatureModel model) {
		if (list == null)
			list = new ArrayList();
		list.add(model);
		return list;
	}

	boolean contains(IFeatureModel model, int type) {
		if ((type & ADDED) != 0 && fAdded != null && fAdded.contains(model)) {
			return true;
		}
		if ((type & REMOVED) != 0 && fRemoved != null && fRemoved.contains(model)) {
			return true;
		}
		if ((type & CHANGED) != 0 && fChanged != null && fChanged.contains(model)) {
			return true;
		}
		return false;
	}
}
