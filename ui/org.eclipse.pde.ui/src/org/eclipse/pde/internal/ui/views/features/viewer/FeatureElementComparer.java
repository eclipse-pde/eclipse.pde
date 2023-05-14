/*******************************************************************************
 * Copyright (c) 2019 Ed Scadding.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Ed Scadding <edscadding@secondfiddle.org.uk> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.views.features.viewer;

import java.util.Objects;

import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureChild;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductFeature;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.core.iproduct.IProductPlugin;

public class FeatureElementComparer implements IElementComparer {

	@Override
	public boolean equals(Object aObj, Object bObj) {
		if (areInstances(aObj, bObj, IFeatureModel.class)) {
			IFeature a = ((IFeatureModel) aObj).getFeature();
			IFeature b = ((IFeatureModel) bObj).getFeature();
			return Objects.equals(a.getId(), b.getId()) && Objects.equals(a.getVersion(), b.getVersion());
		} else if (areInstances(aObj, bObj, IProductModel.class)) {
			IProduct a = ((IProductModel) aObj).getProduct();
			IProduct b = ((IProductModel) bObj).getProduct();
			return Objects.equals(a.getId(), b.getId()) && Objects.equals(a.getVersion(), b.getVersion());
		} else if (areInstances(aObj, bObj, IProductFeature.class)) {
			IProductFeature a = (IProductFeature) aObj;
			IProductFeature b = (IProductFeature) bObj;
			return Objects.equals(a.getId(), b.getId()) && Objects.equals(a.getVersion(), b.getVersion())
					&& equals(a.getModel(), b.getModel());
		} else if (areInstances(aObj, bObj, IProductPlugin.class)) {
			IProductPlugin a = (IProductPlugin) aObj;
			IProductPlugin b = (IProductPlugin) bObj;
			return Objects.equals(a.getId(), b.getId()) && Objects.equals(a.getVersion(), b.getVersion())
					&& equals(a.getModel(), b.getModel());
		} else if (areInstances(aObj, bObj, IFeatureChild.class)) {
			IFeatureChild a = (IFeatureChild) aObj;
			IFeatureChild b = (IFeatureChild) bObj;
			return Objects.equals(a.getId(), b.getId()) && Objects.equals(a.getVersion(), b.getVersion())
					&& equals(a.getModel(), b.getModel());
		} else if (areInstances(aObj, bObj, IFeaturePlugin.class)) {
			IFeaturePlugin a = (IFeaturePlugin) aObj;
			IFeaturePlugin b = (IFeaturePlugin) bObj;
			return Objects.equals(a.getId(), b.getId()) && Objects.equals(a.getVersion(), b.getVersion())
					&& equals(a.getModel(), b.getModel());
		} else {
			return aObj.equals(bObj);
		}
	}

	@Override
	public int hashCode(Object element) {
		if (element instanceof IFeatureModel) {
			return ((IFeatureModel) element).getFeature().getId().hashCode();
		} else if (element instanceof IProductModel) {
			return ((IProductModel) element).getProduct().getId().hashCode();
		} else if (element instanceof IProductFeature) {
			return ((IProductFeature) element).getId().hashCode();
		} else if (element instanceof IProductPlugin) {
			return ((IProductPlugin) element).getId().hashCode();
		} else if (element instanceof IFeatureChild) {
			return ((IFeatureChild) element).getId().hashCode();
		} else if (element instanceof IFeaturePlugin) {
			return ((IFeaturePlugin) element).getId().hashCode();
		} else {
			return element.hashCode();
		}
	}

	private boolean areInstances(Object aObj, Object bObj, Class<?> target) {
		return target.isInstance(aObj) && target.isInstance(bObj);
	}

}
