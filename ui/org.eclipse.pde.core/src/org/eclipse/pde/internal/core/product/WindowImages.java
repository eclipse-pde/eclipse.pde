package org.eclipse.pde.internal.core.product;

import java.io.*;

import org.eclipse.pde.internal.core.iproduct.*;
import org.w3c.dom.*;

public class WindowImages extends ProductObject implements IWindowImages {

	private static final long serialVersionUID = 1L;
	private String fLargeImagePath;
	private String fSmallImagePath;

	public WindowImages(IProductModel model) {
		super(model);
	}

	public String getLargeImagePath() {
		return fLargeImagePath;
	}

	public String getSmallImagePath() {
		return fSmallImagePath;
	}

	public void setLargeImagePath(String path) {
		String old = fLargeImagePath;
		fLargeImagePath = path;
		if (isEditable())
			firePropertyChanged(P_LARGE, old, fLargeImagePath);
	}

	public void setSmallImagePath(String path) {
		String old = fSmallImagePath;
		fSmallImagePath = path;
		if (isEditable())
			firePropertyChanged(P_SMALL, old, fSmallImagePath);
	}

	public void parse(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element element = (Element)node;
			fSmallImagePath = element.getAttribute("small"); //$NON-NLS-1$
			fLargeImagePath = element.getAttribute("large"); //$NON-NLS-1$
		}
	}

	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<windowImages"); //$NON-NLS-1$
		if (fSmallImagePath != null && fSmallImagePath.length() > 0) {
			writer.print(" small=\"" + getWritableString(fSmallImagePath) + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (fLargeImagePath != null && fLargeImagePath.length() > 0) {
			writer.print(" large=\"" + getWritableString(fLargeImagePath) + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		writer.println("/>"); //$NON-NLS-1$
	}

}
