/*******************************************************************************
 *  Copyright (c) 2005, 2011 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.product;

import java.io.PrintWriter;
import java.util.StringTokenizer;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.core.iproduct.ISplashInfo;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class SplashInfo extends ProductObject implements ISplashInfo {

	public static final int F_DEFAULT_BAR_X_OFFSET = 5;
	public static final int F_DEFAULT_BAR_Y_OFFSET = 275;
	public static final int F_DEFAULT_BAR_WIDTH = 445;
	public static final int F_DEFAULT_BAR_HEIGHT = 15;

	public static final int F_DEFAULT_MESSAGE_X_OFFSET = 7;
	public static final int F_DEFAULT_MESSAGE_Y_OFFSET = 252;
	public static final int F_DEFAULT_MESSAGE_WIDTH = 445;
	public static final int F_DEFAULT_MESSAGE_HEIGHT = 20;

	private static final char[] VALID_HEX_CHARS = new char[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'A', 'B', 'C', 'D', 'E', 'F'};
	private static final long serialVersionUID = 1L;
	private String fLocation;
	private boolean fCustomizeProgressBar;
	private int[] fProgressGeometry;
	private boolean fCustomizeProgressMessage;
	private int[] fMessageGeometry;
	private boolean fCustomizeForegroundColor;
	private String fForegroundColor;

	private String fFieldSplashHandlerType;

	public SplashInfo(IProductModel model) {
		super(model);
	}

	@Override
	public void setLocation(String location, boolean blockNotification) {
		String old = fLocation;
		fLocation = location;
		if (!blockNotification && isEditable()) {
			firePropertyChanged(P_LOCATION, old, fLocation);
		}
	}

	@Override
	public String getLocation() {
		return fLocation;
	}

	@Override
	public void parse(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element element = (Element) node;
			setLocation(element.getAttribute(P_LOCATION), true);
			setProgressGeometry(getGeometryArray(element.getAttribute(P_PROGRESS_GEOMETRY)), true);
			setMessageGeometry(getGeometryArray(element.getAttribute(P_MESSAGE_GEOMETRY)), true);
			setForegroundColor(element.getAttribute(P_FOREGROUND_COLOR), true);
			// Parse the splash handler type
			setFieldSplashHandlerType(element.getAttribute(F_ATTRIBUTE_HANDLER_TYPE), true);
		}
	}

	@Override
	public void write(String indent, PrintWriter writer) {
		if (!hasData()) {
			return;
		}

		writer.print(indent + "<splash"); //$NON-NLS-1$

		if (fLocation != null && fLocation.length() > 0) {
			writeProperty(indent, writer, P_LOCATION, getWritableString(fLocation));
		}

		String progres = getGeometryString(fProgressGeometry);
		if (fCustomizeProgressBar && progres != null) {
			writeProperty(indent, writer, P_PROGRESS_GEOMETRY, getWritableString(progres));
		}

		String message = getGeometryString(fMessageGeometry);
		if (fCustomizeProgressMessage && message != null) {
			writeProperty(indent, writer, P_MESSAGE_GEOMETRY, getWritableString(message));
		}

		if (fCustomizeForegroundColor && isValidHexValue(fForegroundColor)) {
			writeProperty(indent, writer, P_FOREGROUND_COLOR, getWritableString(fForegroundColor));
		}

		// Write the splash handler type if it is defined
		if (isDefinedSplashHandlerType()) {
			writeProperty(indent, writer, F_ATTRIBUTE_HANDLER_TYPE, fFieldSplashHandlerType);
		}

		writer.print(" />"); //$NON-NLS-1$
	}

	private void writeProperty(String indent, PrintWriter writer, String name, String value) {
		writer.println();
		writer.print(indent + indent + name + "=\"" + value + "\""); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public void setProgressGeometry(int[] geo, boolean blockNotification) {
		fCustomizeProgressBar = geo != null;
		int[] old = fProgressGeometry;
		fProgressGeometry = geo;
		if (!blockNotification && isEditable()) {
			firePropertyChanged(P_PROGRESS_GEOMETRY, old, fProgressGeometry);
		}
	}

	@Override
	public int[] getProgressGeometry() {
		return fCustomizeProgressBar ? fProgressGeometry : null;
	}

	@Override
	public void setMessageGeometry(int[] geo, boolean blockNotification) {
		fCustomizeProgressMessage = geo != null;
		int[] old = fMessageGeometry;
		fMessageGeometry = geo;
		if (!blockNotification && isEditable()) {
			firePropertyChanged(P_MESSAGE_GEOMETRY, old, fMessageGeometry);
		}
	}

	@Override
	public int[] getMessageGeometry() {
		return fCustomizeProgressMessage ? fMessageGeometry : null;
	}

	@Override
	public void setForegroundColor(String hexColor, boolean blockNotification) throws IllegalArgumentException {
		if (hexColor != null && hexColor.length() == 0) {
			hexColor = null;
		}
		if (hexColor != null && !isValidHexValue(hexColor)) {
			throw new IllegalArgumentException();
		}
		fCustomizeForegroundColor = hexColor != null;
		String old = fForegroundColor;
		fForegroundColor = hexColor;
		if (!blockNotification && isEditable()) {
			firePropertyChanged(P_FOREGROUND_COLOR, old, fForegroundColor);
		}
	}

	@Override
	public String getForegroundColor() {
		return fCustomizeForegroundColor ? fForegroundColor : null;
	}

	public static String getGeometryString(int[] geometry) {
		if (geometry == null || geometry.length < 4) {
			return null;
		}
		return Integer.toString(geometry[0]) + "," + //$NON-NLS-1$
				Integer.toString(geometry[1]) + "," + //$NON-NLS-1$
				Integer.toString(geometry[2]) + "," + //$NON-NLS-1$
				Integer.toString(geometry[3]);
	}

	public static int[] getGeometryArray(String tokenizedValue) {
		if (tokenizedValue == null || tokenizedValue.length() == 0) {
			return null;
		}

		StringTokenizer tokenizer = new StringTokenizer(tokenizedValue, ","); //$NON-NLS-1$
		int position = 0;
		int[] geo = new int[4];
		while (tokenizer.hasMoreTokens()) {
			geo[position++] = Integer.parseInt(tokenizer.nextToken());
		}
		return geo;
	}

	private boolean isValidHexValue(String value) {
		if (value == null || value.length() != 6) {
			return false;
		}
		for (int i = 0; i < value.length(); i++) {
			boolean found = false;
			for (char validChar : VALID_HEX_CHARS) {
				if (value.charAt(i) == validChar) {
					found = true;
					break;
				}
			}
			if (!found) {
				return false;
			}
		}
		return true;
	}

	private boolean hasData() {
		return (fLocation != null && fLocation.length() > 0) || (fCustomizeForegroundColor && fForegroundColor != null && isValidHexValue(fForegroundColor)) || isDefinedGeometry() || isDefinedSplashHandlerType();
	}

	@Override
	public boolean isDefinedSplashHandlerType() {
		if ((fFieldSplashHandlerType != null) && (fFieldSplashHandlerType.length() > 0)) {
			return true;
		}
		return false;
	}

	@Override
	public void addProgressBar(boolean add, boolean blockNotification) {
		boolean old = fCustomizeProgressBar;
		fCustomizeProgressBar = add;
		int[] geo = getProgressGeometry();
		if (add) {
			setProgressGeometry(geo != null ? geo : new int[] {F_DEFAULT_BAR_X_OFFSET, F_DEFAULT_BAR_Y_OFFSET, F_DEFAULT_BAR_WIDTH, F_DEFAULT_BAR_HEIGHT}, blockNotification);
		} else if (!blockNotification && isEditable()) {
			firePropertyChanged("", Boolean.toString(old), Boolean.toString(add)); //$NON-NLS-1$
		}
	}

	@Override
	public void addProgressMessage(boolean add, boolean blockNotification) {
		boolean mold = fCustomizeProgressMessage;
		boolean cold = fCustomizeForegroundColor;
		fCustomizeProgressMessage = add;
		fCustomizeForegroundColor = add;
		int[] geo = getMessageGeometry();
		String foreground = getForegroundColor();
		if (add) {
			setMessageGeometry(geo != null ? geo : new int[] {F_DEFAULT_MESSAGE_X_OFFSET, F_DEFAULT_MESSAGE_Y_OFFSET, F_DEFAULT_MESSAGE_WIDTH, F_DEFAULT_MESSAGE_HEIGHT}, blockNotification);
			setForegroundColor(foreground != null ? foreground : "000000", blockNotification); //$NON-NLS-1$
		} else if (!blockNotification && isEditable()) {
			firePropertyChanged("", Boolean.toString(mold || cold), Boolean.toString(add)); //$NON-NLS-1$
		}
	}

	@Override
	public String getFieldSplashHandlerType() {
		return fFieldSplashHandlerType;
	}

	@Override
	public void setFieldSplashHandlerType(String type, boolean blockNotification) {
		String old = fFieldSplashHandlerType;
		fFieldSplashHandlerType = type;
		if ((blockNotification == false) && isEditable()) {
			firePropertyChanged(F_ATTRIBUTE_HANDLER_TYPE, old, fFieldSplashHandlerType);
		}
	}

	@Override
	public boolean isDefinedGeometry() {
		if ((fCustomizeProgressBar && fProgressGeometry != null) || (fCustomizeProgressMessage && fMessageGeometry != null)) {
			return true;
		}
		return false;
	}
}
