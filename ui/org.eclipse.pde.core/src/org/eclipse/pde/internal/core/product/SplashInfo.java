package org.eclipse.pde.internal.core.product;

import java.io.*;

import org.eclipse.pde.internal.core.iproduct.*;
import org.w3c.dom.*;

public class SplashInfo extends ProductObject implements ISplashInfo {

	private static final long serialVersionUID = 1L;
	private String fLocation;

	public SplashInfo(IProductModel model) {
		super(model);
	}

	public void setLocation(String location) {
		String old = fLocation;
		fLocation = location;
		if (isEditable())
			firePropertyChanged(P_LOCATION, old, fLocation);
	}

	public String getLocation() {
		return fLocation;
	}

	public void parse(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element element = (Element)node;
			fLocation = element.getAttribute(P_LOCATION);
		}
	}

	public void write(String indent, PrintWriter writer) {
		if (fLocation != null && fLocation.length() > 0)
			writer.println(indent + "<splash " + P_LOCATION + "=\"" + fLocation + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

}
