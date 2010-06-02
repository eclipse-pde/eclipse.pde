/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.site;

import com.ibm.icu.util.Calendar;
import java.util.Properties;
import org.eclipse.pde.internal.build.AbstractScriptGenerator;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.osgi.framework.Version;

public class QualifierReplacer implements IBuildPropertiesConstants {
	//	private static final String DOT_QUALIFIER = '.' + PROPERTY_QUALIFIER;
	private static String globalQualifier = null;

	/**
	 * Replaces the 'qualifier' in the given version with the qualifier replacement.
	 * 
	 * @param version version to replace qualifier in
	 * @param id id used when building a replacement using newVersions properties
	 * @param replaceTag replace qualifier with this tag instead of global qualifier, may be <code>null</code>
	 * @param newVersions properties that can be used to generate an alternative qualifier
	 * @return string version with qualifier replaced
	 */
	public static String replaceQualifierInVersion(String version, String id, String replaceTag, Properties newVersions) {
		if (!AbstractScriptGenerator.getPropertyAsBoolean(IBuildPropertiesConstants.PROPERTY_PACKAGER_AS_NORMALIZER))
			return version;
		if (!version.endsWith(PROPERTY_QUALIFIER))
			return version;

		String newQualifier = null;
		if (replaceTag == null || replaceTag.equalsIgnoreCase(PROPERTY_CONTEXT)) {
			if (globalQualifier != null)
				newQualifier = globalQualifier;

			if (newQualifier == null && newVersions != null && newVersions.size() != 0) { //Skip the lookup in the file if there is no entries
				newQualifier = (String) newVersions.get(getQualifierKey(id, version)); //First we check to see if there is a precise version
				if (newQualifier == null) //If not found, then lookup for the id,0.0.0
					newQualifier = (String) newVersions.get(id + ',' + Version.emptyVersion.toString());
				if (newQualifier == null)
					newQualifier = newVersions.getProperty(DEFAULT_MATCH_ALL);
			}
			if (newQualifier == null)
				newQualifier = getDateQualifier();
		} else if (replaceTag.equalsIgnoreCase(PROPERTY_NONE)) {
			newQualifier = ""; //$NON-NLS-1$
		} else {
			newQualifier = replaceTag;
		}

		version = version.replaceFirst(PROPERTY_QUALIFIER, newQualifier);
		if (version.endsWith(".")) //$NON-NLS-1$
			version = version.substring(0, version.length() - 1);
		return version;
	}

	//given a version ending in "qualifier" return the key to look up the replacement
	public static String getQualifierKey(String id, String version) {
		if (version == null || !version.endsWith(PROPERTY_QUALIFIER))
			return null;

		Version osgiVersion = new Version(version);
		String qualifier = osgiVersion.getQualifier();
		qualifier = qualifier.substring(0, qualifier.length() - PROPERTY_QUALIFIER.length());

		StringBuffer keyBuffer = new StringBuffer(id);
		keyBuffer.append(',');
		keyBuffer.append(osgiVersion.getMajor());
		keyBuffer.append('.');
		keyBuffer.append(osgiVersion.getMinor());
		keyBuffer.append('.');
		keyBuffer.append(osgiVersion.getMicro());

		if (qualifier.length() > 0) {
			keyBuffer.append('.');
			keyBuffer.append(qualifier);
		}
		return keyBuffer.toString();
	}

	/**
	 * Returns the current date/time as a string to be used as a qualifier
	 * replacement.  This is the default qualifier replacement.  Will
	 * be of the form YYYYMMDDHHMM.
	 * @return current date/time as a qualifier replacement 
	 */
	public static String getDateQualifier() {
		final String empty = ""; //$NON-NLS-1$
		int monthNbr = Calendar.getInstance().get(Calendar.MONTH) + 1;
		String month = (monthNbr < 10 ? "0" : empty) + monthNbr; //$NON-NLS-1$

		int dayNbr = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
		String day = (dayNbr < 10 ? "0" : empty) + dayNbr; //$NON-NLS-1$

		int hourNbr = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		String hour = (hourNbr < 10 ? "0" : empty) + hourNbr; //$NON-NLS-1$

		int minuteNbr = Calendar.getInstance().get(Calendar.MINUTE);
		String minute = (minuteNbr < 10 ? "0" : empty) + minuteNbr; //$NON-NLS-1$

		return empty + Calendar.getInstance().get(Calendar.YEAR) + month + day + hour + minute;
	}

	/**
	 * Sets the global variable used as the qualifier replacement during calls to
	 * {@link #replaceQualifierInVersion(String, String, String, Properties)}
	 * Setting the qualifier to <code>null</code> will result in the default
	 * date qualifier being used.
	 * @param globalQualifier string replacement or <code>null</code>
	 */
	public static void setGlobalQualifier(String globalQualifier) {
		if (globalQualifier == null || globalQualifier.length() == 0)
			QualifierReplacer.globalQualifier = null;
		else if (globalQualifier.length() > 0 && globalQualifier.charAt(0) != '$')
			QualifierReplacer.globalQualifier = globalQualifier;
	}
}
