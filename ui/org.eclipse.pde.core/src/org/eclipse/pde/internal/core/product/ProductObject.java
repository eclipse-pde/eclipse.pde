package org.eclipse.pde.internal.core.product;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.pde.core.IModelChangeProvider;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.core.iproduct.IProductObject;


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
	
	protected void firePropertyChanged(
		String property,
		Object oldValue,
		Object newValue) {
		firePropertyChanged(this, property, oldValue, newValue);
	}
	
	protected void firePropertyChanged(
		IProductObject object,
		String property,
		Object oldValue,
		Object newValue) {
		if (fModel.isEditable() && fModel instanceof IModelChangeProvider) {
			IModelChangeProvider provider = (IModelChangeProvider) fModel;
			provider.fireModelObjectChanged(object, property, oldValue, newValue);
		}
	}
	
	protected void fireStructureChanged(IProductObject child, int changeType) {
		fireStructureChanged(new IProductObject[] { child }, changeType);
	}
	
	protected void fireStructureChanged(
		IProductObject[] children,
		int changeType) {
		if (fModel.isEditable() && fModel instanceof IModelChangeProvider) {
			IModelChangeProvider provider = (IModelChangeProvider) fModel;
			provider.fireModelChanged(new ModelChangedEvent(provider, changeType, children, null));
		}
	}
	
	protected boolean isEditable() {
		return getModel().isEditable();
	}
	
	public String getWritableString(String source) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < source.length(); i++) {
			char c = source.charAt(i);
			switch (c) {
				case '&' :
					buf.append("&amp;"); //$NON-NLS-1$
					break;
				case '<' :
					buf.append("&lt;"); //$NON-NLS-1$
					break;
				case '>' :
					buf.append("&gt;"); //$NON-NLS-1$
					break;
				case '\'' :
					buf.append("&apos;"); //$NON-NLS-1$
					break;
				case '\"' :
					buf.append("&quot;"); //$NON-NLS-1$
					break;
				default :
					buf.append(c);
					break;
			}
		}
		return buf.toString();
	}



}
