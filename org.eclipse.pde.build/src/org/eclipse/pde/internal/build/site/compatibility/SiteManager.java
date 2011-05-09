/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.pde.internal.build.site.compatibility;

import java.util.StringTokenizer;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.p2.publisher.eclipse.IPlatformEntry;

public class SiteManager {
	private static String os;
	private static String ws;
	private static String arch;
	private static String nl;

	public static void setOS(String os) {
		SiteManager.os = os;
	}

	public static void setWS(String ws) {
		SiteManager.ws = ws;
	}

	public static void setArch(String arch) {
		SiteManager.arch = arch;
	}

	public static void setNL(String nl) {
		SiteManager.nl = nl;
	}

	public static String getOS() {
		if (os == null)
			os = Platform.getOS();
		return os;
	}

	public static String getWS() {
		if (ws == null)
			ws = Platform.getWS();
		return ws;
	}

	public static String getArch() {
		if (arch == null)
			arch = Platform.getOSArch();
		return arch;
	}

	public static String getNL() {
		if (nl == null)
			nl = Platform.getNL();
		return nl;
	}

	public static boolean isValidEnvironment(IPlatformEntry candidate) {
		if (candidate == null)
			return false;
		String candidateOS = candidate.getOS();
		String candidateWS = candidate.getWS();
		String candidateArch = candidate.getArch();
		String candiateNL = candidate.getNL();
		if (candidateOS != null && !isMatching(candidateOS, SiteManager.getOS()))
			return false;
		if (candidateWS != null && !isMatching(candidateWS, SiteManager.getWS()))
			return false;
		if (candidateArch != null && !isMatching(candidateArch, SiteManager.getArch()))
			return false;
		if (candiateNL != null && !isMatchingLocale(candiateNL, SiteManager.getNL()))
			return false;
		return true;
	}

	private static boolean isMatching(String candidateValues, String siteValues) {
		if (siteValues == null)
			return false;
		if ("*".equals(candidateValues))return true; //$NON-NLS-1$
		if ("".equals(candidateValues))return true; //$NON-NLS-1$
		StringTokenizer siteTokens = new StringTokenizer(siteValues, ","); //$NON-NLS-1$
		//$NON-NLS-1$	
		while (siteTokens.hasMoreTokens()) {
			StringTokenizer candidateTokens = new StringTokenizer(candidateValues, ","); //$NON-NLS-1$
			String siteValue = siteTokens.nextToken();
			while (candidateTokens.hasMoreTokens()) {
				if (siteValue.equalsIgnoreCase(candidateTokens.nextToken()))
					return true;
			}
		}
		return false;
	}

	private static boolean isMatchingLocale(String candidateValues, String locale) {
		if (locale == null)
			return false;
		if ("*".equals(candidateValues))return true; //$NON-NLS-1$
		if ("".equals(candidateValues))return true; //$NON-NLS-1$

		locale = locale.toUpperCase();
		candidateValues = candidateValues.toUpperCase();
		StringTokenizer stok = new StringTokenizer(candidateValues, ","); //$NON-NLS-1$
		while (stok.hasMoreTokens()) {
			String candidate = stok.nextToken();
			if (locale.indexOf(candidate) == 0)
				return true;
			if (candidate.indexOf(locale) == 0)
				return true;
		}
		return false;
	}
}
