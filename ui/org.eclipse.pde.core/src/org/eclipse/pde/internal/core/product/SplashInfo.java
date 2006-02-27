/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
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
import java.util.StringTokenizer;

import org.apache.crimson.tree.ElementNode;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.core.iproduct.ISplashInfo;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SplashInfo extends ProductObject implements ISplashInfo {

	private static final char[] VALID_HEX_CHARS = new char[] {
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
		'a', 'b', 'c', 'd', 'e', 'f',
		'A', 'B', 'C', 'D', 'E', 'F'
	};
	private static final long serialVersionUID = 1L;
	private String fLocation;
	private boolean fCustomizeProgressBar;
	private int[] fProgressGeometry;
	private boolean fCustomizeProgressMessage;
	private int[] fMessageGeometry;
	private boolean fCustomizeForegroundColor;
	private String fForegroundColor;

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
			setLocation(element.getAttribute(P_LOCATION));
			NodeList children = element.getElementsByTagName(P_PROPERTY);
			for (int i = 0; i < children.getLength(); i++) {
				ElementNode child = (ElementNode)children.item(i);
				String name = child.getAttribute(P_PROPERTY_NAME);
				String value = child.getAttribute(P_PROPERTY_VALUE);
				if (P_PROGRESS_GEOMETRY.equals(name))
					setProgressGeometry(getGeometryArray(value));
				else if (P_MESSAGE_GEOMETRY.equals(name))
					setMessageGeometry(getGeometryArray(value));
				else if (P_FOREGROUND_COLOR.equals(name))
					setForegroundColor(value);
			}
			if (!isValidHexValue(fForegroundColor))
				fForegroundColor = null;
		}
	}

	public void write(String indent, PrintWriter writer) {
		if (!hasData())
			return;
		
		if (fLocation != null && fLocation.length() > 0)
			writer.println(indent + "<splash " + P_LOCATION + "=\"" + getWritableString(fLocation) + "\">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		else
			writer.println(indent + "<splash>"); //$NON-NLS-1$
		
		String progres = getGeometryString(fProgressGeometry);
		if (fCustomizeProgressBar && progres != null) 
			writeProperty(indent + indent, writer, P_PROGRESS_GEOMETRY, getWritableString(progres));
		
		String message = getGeometryString(fMessageGeometry);
		if (fCustomizeProgressMessage && message != null)
			writeProperty(indent + indent, writer, P_MESSAGE_GEOMETRY, getWritableString(message));
		
		if (fCustomizeForegroundColor && isValidHexValue(fForegroundColor))
			writeProperty(indent + indent, writer, P_FOREGROUND_COLOR, getWritableString(fForegroundColor));
		
		writer.println(indent + "</splash>"); //$NON-NLS-1$
	}

	private void writeProperty(String indent, PrintWriter writer, String name, String value) {
		writer.println(indent + "<property"); //$NON-NLS-1$
		writer.println(indent + indent + P_PROPERTY_NAME + "=\"" + name + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println(indent + indent + P_PROPERTY_VALUE + "=\"" + value + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public void setProgressGeometry(int[] geo) {
		fCustomizeProgressBar = geo != null;
		int[] old = fProgressGeometry;
		fProgressGeometry = geo;
		if (isEditable())
			firePropertyChanged(P_PROGRESS_GEOMETRY, old, fProgressGeometry);
	}

	public int[] getProgressGeometry() {
		return fCustomizeProgressBar ? fProgressGeometry : null;
	}
	
	public void setMessageGeometry(int[] geo) {
		fCustomizeProgressMessage = geo != null;
		int[] old = fMessageGeometry;
		fMessageGeometry = geo;
		if (isEditable())
			firePropertyChanged(P_MESSAGE_GEOMETRY, old, fMessageGeometry);
	}

	public int[] getMessageGeometry() {
		return fCustomizeProgressMessage ? fMessageGeometry : null;
	}

	public void setForegroundColor(String hexColor) throws IllegalArgumentException {
		if (hexColor != null && !isValidHexValue(hexColor))
			throw new IllegalArgumentException();
		fCustomizeForegroundColor = hexColor != null;
		String old = fForegroundColor;
		fForegroundColor = hexColor;
		if (isEditable())
			firePropertyChanged(P_FOREGROUND_COLOR, old, fForegroundColor);
	}

	public String getForegroundColor() {
		return fCustomizeForegroundColor ? fForegroundColor : null;
	}
	
	public static String getGeometryString(int[] geometry) {
		if (geometry == null || geometry.length < 4)
			return null;
		return Integer.toString(geometry[0]) + "," + //$NON-NLS-1$
			   Integer.toString(geometry[1]) + "," +  //$NON-NLS-1$
			   Integer.toString(geometry[2]) + "," +  //$NON-NLS-1$
			   Integer.toString(geometry[3]);
	}
	
	public static int[] getGeometryArray(String tokenizedValue) {
		if (tokenizedValue == null || tokenizedValue.length() == 0)
			return null;
		
		StringTokenizer tokenizer = new StringTokenizer(tokenizedValue, ","); //$NON-NLS-1$
		int position = 0;
		int[] geo = new int[4];
		while (tokenizer.hasMoreTokens()) 
			geo[position++] = Integer.parseInt(tokenizer.nextToken());
		return geo;
	}
	
	private boolean isValidHexValue(String value) {
		if (value == null || value.length() != 6)
			return false;
		for (int i = 0; i < value.length(); i++) {
			boolean found = false;
			for (int j = 0; j < VALID_HEX_CHARS.length; j++) {
				if (value.charAt(i) == VALID_HEX_CHARS[j]) {
					found = true;
					break;
				}
			}
			if (!found)
				return false;
		}
		return true;
	}
	
	private boolean hasData() {
		return	(fLocation != null && fLocation.length() > 0) || 
				(fCustomizeForegroundColor && fForegroundColor != null && isValidHexValue(fForegroundColor)) ||
				(fCustomizeProgressBar && fProgressGeometry != null) ||
				(fCustomizeProgressMessage && fMessageGeometry != null);
	}

	public void addProgressBar(boolean add, boolean blockNotification) {
		boolean old = fCustomizeProgressBar;
		fCustomizeProgressBar = add;
		if (!blockNotification && isEditable())
			firePropertyChanged("", Boolean.toString(old), Boolean.toString(fCustomizeProgressBar)); //$NON-NLS-1$
	}

	public void addProgressMessage(boolean add, boolean blockNotification) {
		boolean oldM = fCustomizeProgressMessage;
		boolean oldC = fCustomizeForegroundColor;
		fCustomizeProgressMessage = add;
		fCustomizeForegroundColor = add;
		if (!blockNotification && isEditable())
			firePropertyChanged("", Boolean.toString(oldM || oldC), Boolean.toString(add)); //$NON-NLS-1$
	}
}
