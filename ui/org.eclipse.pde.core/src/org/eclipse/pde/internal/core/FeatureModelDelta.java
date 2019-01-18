/*******************************************************************************
 *  Copyright (c) 2000, 2012 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.util.ArrayList;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;

public class FeatureModelDelta implements IFeatureModelDelta {
	private ArrayList<IFeatureModel> fAdded;

	private ArrayList<IFeatureModel> fRemoved;

	private ArrayList<IFeatureModel> fChanged;

	private int kind = 0;

	public FeatureModelDelta() {
	}

	@Override
	public int getKind() {
		return kind;
	}

	@Override
	public IFeatureModel[] getAdded() {
		return get(fAdded);
	}

	@Override
	public IFeatureModel[] getRemoved() {
		return get(fRemoved);
	}

	@Override
	public IFeatureModel[] getChanged() {
		return get(fChanged);
	}

	private IFeatureModel[] get(ArrayList<IFeatureModel> list) {
		if (list == null) {
			return new IFeatureModel[0];
		}
		return list.toArray(new IFeatureModel[list.size()]);
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

	private ArrayList<IFeatureModel> add(ArrayList<IFeatureModel> list, IFeatureModel model) {
		if (list == null) {
			list = new ArrayList<>();
		}
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
