package org.eclipse.pde.internal.core.product;

import java.io.*;

import org.eclipse.pde.internal.core.iproduct.*;
import org.w3c.dom.*;


public class ProductPlugin extends ProductObject implements IProductPlugin {

	private static final long serialVersionUID = 1L;
	private String fId;

	/**
	 * 
	 */
	public ProductPlugin(IProductModel model) {
		super(model);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductPlugin#getId()
	 */
	public String getId() {
		return fId.trim();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductPlugin#setId(java.lang.String)
	 */
	public void setId(String id) {
		fId = id;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductObject#parse(org.w3c.dom.Node)
	 */
	public void parse(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE)
			fId = ((Element)node).getAttribute("id");
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.product.ProductObject#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		String id = getId();
		if (id.length() > 0)
			writer.println(indent + "<plugin id=\"" + id + "\"/>");
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.product.ProductObject#isValid()
	 */
	public boolean isValid() {
		return getId().length() > 0;
	}

}
