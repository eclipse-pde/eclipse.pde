package org.eclipse.pde.internal.core.product;

import java.io.*;

import javax.xml.parsers.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.iproduct.*;

import org.w3c.dom.*;


public class ProductModel extends AbstractModel implements IProductModel {

	private static final long serialVersionUID = 1L;
	
	private IProductModelFactory fFactory;
	private IProduct fProduct;
	
	public ProductModel() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.AbstractModel#updateTimeStamp()
	 */
	protected void updateTimeStamp() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductModel#getProduct()
	 */
	public IProduct getProduct() {
		if (fProduct == null)
			fProduct = getFactory().createProduct();
		return fProduct;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductModel#getFactory()
	 */
	public IProductModelFactory getFactory() {
		if (fFactory == null)
			fFactory = new ProductModelFactory(this);
		return fFactory;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductModel#getInstallLocation()
	 */
	public String getInstallLocation() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductModel#isEnabled()
	 */
	public boolean isEnabled() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductModel#setEnabled(boolean)
	 */
	public void setEnabled(boolean enabled) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#isInSync()
	 */
	public boolean isInSync() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#load()
	 */
	public void load() throws CoreException {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#load(java.io.InputStream, boolean)
	 */
	public void load(InputStream stream, boolean outOfSync)
			throws CoreException {
		try {
			SAXParser parser = getSaxParser();
			XMLDefaultHandler handler = new XMLDefaultHandler();
			parser.parse(stream, handler);
			processDocument(handler.getDocument());
			setLoaded(true);
		} catch (Exception e) {
			PDECore.logException(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#reload(java.io.InputStream, boolean)
	 */
	public void reload(InputStream source, boolean outOfSync)
			throws CoreException {
		load(source, outOfSync);
		fireModelChanged(
				new ModelChangedEvent(this,
					IModelChangedEvent.WORLD_CHANGED,
					new Object[] { fProduct },
					null));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IBaseModel#isEditable()
	 */
	public boolean isEditable() {
		return false;
	}
	
	private void processDocument(Document doc) {
		Node rootNode = doc.getDocumentElement();
		if (fProduct == null) {
			fProduct = getFactory().createProduct();
		} else {
			fProduct.reset();
		}
		fProduct.parse(rootNode);
	}
}
