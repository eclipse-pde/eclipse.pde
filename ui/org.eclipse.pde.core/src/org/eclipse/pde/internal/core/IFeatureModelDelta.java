package org.eclipse.pde.internal.core;

import org.eclipse.pde.internal.core.ifeature.IFeatureModel;

public interface IFeatureModelDelta {

	public static final int ADDED = 1;

	public static final int REMOVED = 2;

	public static final int CHANGED = 4;

	public abstract int getKind();

	public abstract IFeatureModel[] getAdded();

	public abstract IFeatureModel[] getRemoved();

	public abstract IFeatureModel[] getChanged();

}