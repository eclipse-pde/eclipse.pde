package org.eclipse.pde.internal.core.product;

import java.io.*;

import org.eclipse.pde.internal.core.iproduct.*;
import org.w3c.dom.*;


public class Product extends ProductObject implements IProduct {

	private static final long serialVersionUID = 1L;
	private String fId;
	private String fName;
	private String fApplication;
	private IAboutInfo fAboutInfo;
	private boolean fUseProduct;

	/**
	 * 
	 */
	public Product(IProductModel model) {
		super(model);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#getId()
	 */
	public String getId() {
		return fId;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#getName()
	 */
	public String getName() {
		return fName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#getApplication()
	 */
	public String getApplication() {
		return fApplication;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#setId(java.lang.String)
	 */
	public void setId(String id) {
		fId = id;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#setName(java.lang.String)
	 */
	public void setName(String name) {
		fName = name;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#setApplication(java.lang.String)
	 */
	public void setApplication(String application) {
		fApplication = application;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.product.ProductObject#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		writer.println(indent + "<product");
		if (fName != null && fName.length() > 0)
			writer.println("   name=\"" + fName + "\"");
		if (fId != null && fId.length() > 0)
			writer.println("   id=\"" + fId + "\"");
		if (fApplication != null && fApplication.length() > 0)
			writer.println("   application=\"" + fApplication + "\"");
		writer.println("   useProduct=\"" + Boolean.toString(fUseProduct) + "\">");
		
		if (fAboutInfo != null)
			fAboutInfo.write(indent + "   ", writer);
		
		writer.println("</product>");
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#getAboutInfo()
	 */
	public IAboutInfo getAboutInfo() {
		return fAboutInfo;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#reset()
	 */
	public void reset() {
		fAboutInfo = null;
		fApplication = null;
		fId = null;
		fName = null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductObject#parse(org.w3c.dom.Node)
	 */
	public void parse(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE 
				&& node.getNodeName().equals("product")) {
			Element element = (Element)node;
			fApplication = element.getAttribute("application");
			fId = element.getAttribute("id");
			fName = element.getAttribute("name");
			NodeList children = node.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child.getNodeType() == Node.ELEMENT_NODE) {
					if (child.getNodeName().equals("aboutInfo")) {
						fAboutInfo = getModel().getFactory().createAboutInfo();
						fAboutInfo.setInTheModel(true);
						fAboutInfo.parse(child);
					}
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#usesProduct()
	 */
	public boolean usesProduct() {
		return fUseProduct;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#setUseProduct(boolean)
	 */
	public void setUseProduct(boolean use) {
		fUseProduct = use;
	}

}
