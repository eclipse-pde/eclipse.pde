/*******************************************************************************
 *  Copyright (c) 2005, 2012 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.iproduct;

public interface ILauncherInfo extends IProductObject {

	public static final String LINUX_ICON = "linuxIcon"; //$NON-NLS-1$

	public static final String MACOSX_ICON = "macosxIcon"; //$NON-NLS-1$

	public static final String SOLARIS_LARGE = "solarisLarge"; //$NON-NLS-1$
	public static final String SOLARIS_MEDIUM = "solarisMedium"; //$NON-NLS-1$
	public static final String SOLARIS_SMALL = "solarisSmall"; //$NON-NLS-1$
	public static final String SOLARIS_TINY = "solarisTiny"; //$NON-NLS-1$

	public static final String WIN32_16_LOW = "winSmallLow"; //$NON-NLS-1$
	public static final String WIN32_16_HIGH = "winSmallHigh"; //$NON-NLS-1$
	public static final String WIN32_32_LOW = "winMediumLow"; //$NON-NLS-1$
	public static final String WIN32_32_HIGH = "winMediumHigh"; //$NON-NLS-1$
	public static final String WIN32_48_LOW = "winLargeLow"; //$NON-NLS-1$
	public static final String WIN32_48_HIGH = "winLargeHigh"; //$NON-NLS-1$
	public static final String WIN32_256_HIGH = "winExtraLargeHigh"; //$NON-NLS-1$

	public static final String P_USE_ICO = "useIco"; //$NON-NLS-1$
	public static final String P_ICO_PATH = "icoFile"; //$NON-NLS-1$
	public static final String P_LAUNCHER = "launcher"; //$NON-NLS-1$

	String getLauncherName();

	void setLauncherName(String name);

	void setIconPath(String iconId, String path);

	String getIconPath(String iconId);

	boolean usesWinIcoFile();

	void setUseWinIcoFile(boolean use);
}
