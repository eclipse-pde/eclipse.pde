package org.eclipse.pde.internal.core.product;

import java.io.*;

import org.eclipse.pde.internal.core.iproduct.*;
import org.w3c.dom.*;


public class AboutInfo extends ProductObject implements IAboutInfo {

	private static final long serialVersionUID = 1L;
	private String fImagePath;
	private String fAboutText;

	public AboutInfo(IProductModel model) {
		super(model);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IAboutInfo#setText(java.lang.String)
	 */
	public void setText(String text) {
		fAboutText = text;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IAboutInfo#getText()
	 */
	public String getText() {
		return fAboutText;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IAboutInfo#setImagePath(java.lang.String)
	 */
	public void setImagePath(String path) {
		fImagePath = path;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IAboutInfo#getImagePath()
	 */
	public String getImagePath() {
		return fImagePath;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.product.ProductObject#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		if (isAboutImageDefined() || isAboutTextDefined()) {
			writer.println(indent + "<aboutInfo>");
			if (isAboutImageDefined())
				writer.println(indent + "   <image path=\"" + fImagePath.trim() + "\"/>");
			if (isAboutTextDefined()) {
				writer.println(indent + "   <text>");
				writer.println(indent + "      " + fAboutText.trim());
				writer.println(indent + "   </text>");
			}
			writer.println(indent + "</aboutInfo>");
		}
	}
	
	private boolean isAboutTextDefined() {
		return fAboutText != null && fAboutText.length() > 0;
	}
	
	private boolean isAboutImageDefined() {
		return fImagePath != null && fImagePath.length() > 0;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductObject#parse(org.w3c.dom.Node)
	 */
	public void parse(Node node) {
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if (child.getNodeName().equals("image")) {
					fImagePath = ((Element)child).getAttribute("path");
				} else if (child.getNodeName().equals("text")) {
					child.normalize();
					if (child.getChildNodes().getLength() > 0) {
						Node text = child.getFirstChild();
						if (text.getNodeType() == Node.TEXT_NODE)
							fAboutText = ((Text)text).getData().trim();
					}
				}
			}
		}
	}
	
}
