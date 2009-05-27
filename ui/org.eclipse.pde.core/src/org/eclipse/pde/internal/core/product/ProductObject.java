/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.product;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.pde.core.IModelChangeProvider;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.core.iproduct.IProductObject;
import org.eclipse.pde.internal.core.util.PDEXMLHelper;

public abstract class ProductObject extends PlatformObject implements IProductObject {

	private static final long serialVersionUID = 1L;
	private transient IProductModel fModel;

	public ProductObject(IProductModel model) {
		fModel = model;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductObject#getModel()
	 */
	public IProductModel getModel() {
		return fModel;
	}

	public void setModel(IProductModel model) {
		fModel = model;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductObject#getProduct()
	 */
	public IProduct getProduct() {
		return getModel().getProduct();
	}

	protected void firePropertyChanged(String property, Object oldValue, Object newValue) {
		firePropertyChanged(this, property, oldValue, newValue);
	}

	protected void firePropertyChanged(IProductObject object, String property, Object oldValue, Object newValue) {
		if (fModel.isEditable()) {
			IModelChangeProvider provider = fModel;
			provider.fireModelObjectChanged(object, property, oldValue, newValue);
		}
	}

	protected void fireStructureChanged(IProductObject child, int changeType) {
		fireStructureChanged(new IProductObject[] {child}, changeType);
	}

	protected void fireStructureChanged(IProductObject[] children, int changeType) {
		if (fModel.isEditable()) {
			IModelChangeProvider provider = fModel;
			provider.fireModelChanged(new ModelChangedEvent(provider, changeType, children, null));
		}
	}

	protected boolean isEditable() {
		return getModel().isEditable();
	}

	public String getWritableString(String source) {
		return PDEXMLHelper.getWritableString(source);
	}

}
