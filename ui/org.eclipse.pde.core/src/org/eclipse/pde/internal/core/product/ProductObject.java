package org.eclipse.pde.internal.core.product;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.pde.internal.core.iproduct.*;


public abstract class ProductObject extends PlatformObject implements IProductObject {

	private static final long serialVersionUID = 1L;
	private boolean fInTheModel;
	private IProductModel fModel;

	public ProductObject(IProductModel model) {
		fModel = model;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductObject#getModel()
	 */
	public IProductModel getModel() {
		return fModel;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductObject#getProduct()
	 */
	public IProduct getProduct() {
		return getModel().getProduct();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductObject#isInTheModel()
	 */
	public boolean isInTheModel() {
		return fInTheModel;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductObject#isValid()
	 */
	public boolean isValid() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductObject#setInTheModel(boolean)
	 */
	public void setInTheModel(boolean inTheModel) {
		fInTheModel = inTheModel;
	}

}
