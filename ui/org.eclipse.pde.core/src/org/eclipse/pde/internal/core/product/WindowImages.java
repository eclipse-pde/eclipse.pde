/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.product;

import java.io.PrintWriter;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.core.iproduct.IWindowImages;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class WindowImages extends ProductObject implements IWindowImages {

	private static final long serialVersionUID = 1L;
	private String f16ImagePath;
	private String f32ImagePath;
	private String f48ImagePath;
	private String f64ImagePath;
	private String f128ImagePath;

	public WindowImages(IProductModel model) {
		super(model);
	}

	public String getImagePath(int size) {
		switch (size) {
			case 0 :
				return f16ImagePath;
			case 1 :
				return f32ImagePath;
			case 2 :
				return f48ImagePath;
			case 3 :
				return f64ImagePath;
			case 4 :
				return f128ImagePath;
		}
		return null;
	}

	public void setImagePath(String path, int size) {
		String old;
		switch (size) {
			case 0 :
				old = f16ImagePath;
				f16ImagePath = path;
				if (isEditable())
					firePropertyChanged(P_16, old, f16ImagePath);
				break;
			case 1 :
				old = f32ImagePath;
				f32ImagePath = path;
				if (isEditable())
					firePropertyChanged(P_32, old, f32ImagePath);
				break;
			case 2 :
				old = f48ImagePath;
				f48ImagePath = path;
				if (isEditable())
					firePropertyChanged(P_48, old, f48ImagePath);
				break;
			case 3 :
				old = f64ImagePath;
				f64ImagePath = path;
				if (isEditable())
					firePropertyChanged(P_64, old, f64ImagePath);
				break;
			case 4 :
				old = f128ImagePath;
				f128ImagePath = path;
				if (isEditable())
					firePropertyChanged(P_128, old, f128ImagePath);
				break;
		}

	}

	public void parse(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element element = (Element) node;
			f16ImagePath = element.getAttribute(P_16);
			// try the old 3.1 attribute name
			if (f16ImagePath == null || f16ImagePath.length() == 0)
				f16ImagePath = element.getAttribute("small"); //$NON-NLS-1$

			f32ImagePath = element.getAttribute(P_32);
			// try the old 3.1 attribute name
			if (f32ImagePath == null || f32ImagePath.length() == 0)
				f32ImagePath = element.getAttribute("large"); //$NON-NLS-1$

			f48ImagePath = element.getAttribute(P_48);
			f64ImagePath = element.getAttribute(P_64);
			f128ImagePath = element.getAttribute(P_128);
		}
	}

	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<windowImages"); //$NON-NLS-1$
		if (f16ImagePath != null && f16ImagePath.length() > 0) {
			writer.print(" " + P_16 + "=\"" + getWritableString(f16ImagePath) + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		if (f32ImagePath != null && f32ImagePath.length() > 0) {
			writer.print(" " + P_32 + "=\"" + getWritableString(f32ImagePath) + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		if (f48ImagePath != null && f48ImagePath.length() > 0) {
			writer.print(" " + P_48 + "=\"" + getWritableString(f48ImagePath) + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		if (f64ImagePath != null && f64ImagePath.length() > 0) {
			writer.print(" " + P_64 + "=\"" + getWritableString(f64ImagePath) + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		if (f128ImagePath != null && f128ImagePath.length() > 0) {
			writer.print(" " + P_128 + "=\"" + getWritableString(f128ImagePath) + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		writer.println("/>"); //$NON-NLS-1$
	}

}
