/*******************************************************************************
 * Copyright (c) 2004, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
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
	
	public static String replaceQualifierInVersion(String version, String id, String replaceTag, Properties newVersions) {
		if (! AbstractScriptGenerator.getPropertyAsBoolean(IBuildPropertiesConstants.PROPERTY_PACKAGER_AS_NORMALIZER))
			return version;
		if (!version.endsWith(PROPERTY_QUALIFIER))
			return version;

		String newQualifier = null;
		if (replaceTag == null || replaceTag.equalsIgnoreCase(PROPERTY_CONTEXT)) {
			if (globalQualifier != null)
				newQualifier = globalQualifier;

			if (newQualifier == null && newVersions != null && newVersions.size() != 0) { //Skip the lookup in the file if there is no entries
				newQualifier = (String) newVersions.get(id + ',' + version.substring(0, version.length() - PROPERTY_QUALIFIER.length() - 1)); //First we check to see if there is a precise version
				if (newQualifier == null) //If not found, then lookup for the id,0.0.0
					newQualifier = (String) newVersions.get(id + ',' + Version.emptyVersion.toString());
				if (newQualifier == null)
					newQualifier = newVersions.getProperty(DEFAULT_MATCH_ALL);
			}
			if (newQualifier == null)
				newQualifier = getDate();

			//			newQualifier = '.' + newQualifier;
		} else if (replaceTag.equalsIgnoreCase(PROPERTY_NONE)) {
			newQualifier = ""; //$NON-NLS-1$
		} else {
			newQualifier = replaceTag;
		}

		return version.replaceFirst(PROPERTY_QUALIFIER, newQualifier);
	}

	private static String getDate() {
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

	public static void setGlobalQualifier(String globalQualifier) {
		if (globalQualifier == null || globalQualifier.length() == 0)
			QualifierReplacer.globalQualifier = null;
		else if (globalQualifier.length() > 0 && globalQualifier.charAt(0) != '$')
			QualifierReplacer.globalQualifier = globalQualifier;
	}
}
