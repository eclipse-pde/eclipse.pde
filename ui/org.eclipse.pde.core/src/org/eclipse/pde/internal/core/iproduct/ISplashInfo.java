/*******************************************************************************
 *  Copyright (c) 2005, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.iproduct;

public interface ISplashInfo extends IProductObject {

	public static final String P_LOCATION = "location"; //$NON-NLS-1$
	public static final String P_PROGRESS_GEOMETRY = "startupProgressRect"; //$NON-NLS-1$
	public static final String P_MESSAGE_GEOMETRY = "startupMessageRect"; //$NON-NLS-1$
	public static final String P_FOREGROUND_COLOR = "startupForegroundColor"; //$NON-NLS-1$

	public static final String F_ATTRIBUTE_HANDLER_TYPE = "handlerType"; //$NON-NLS-1$

	void setLocation(String location, boolean blockNotification);

	String getLocation();

	void addProgressBar(boolean add, boolean blockNotification);

	/**
	 * 
	 * @param geo array of length 4 where geo[0] = x
	 * 									  geo[1] = y
	 * 									  geo[1] = width
	 * 									  geo[1] = height
	 * @param blockNotification
	 */
	void setProgressGeometry(int[] geo, boolean blockNotification);

	int[] getProgressGeometry();

	void addProgressMessage(boolean add, boolean blockNotification);

	/**
	 * 
	 * @param geo array of length 4 where geo[0] = x
	 * 									  geo[1] = y
	 * 									  geo[1] = width
	 * 									  geo[1] = height
	 * @param blockNotification
	 */
	void setMessageGeometry(int[] geo, boolean blockNotification);

	int[] getMessageGeometry();

	void setForegroundColor(String hexColor, boolean blockNotification) throws IllegalArgumentException;

	String getForegroundColor();

	public void setFieldSplashHandlerType(String type, boolean blockNotification);

	public String getFieldSplashHandlerType();

	public boolean isDefinedSplashHandlerType();

	public boolean isDefinedGeometry();

}
