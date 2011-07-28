/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.provisional;

/**
 * Class containing constants and utility methods for @since values.
 * 
 * This covers the corresponding profiles:
 * <ul>
 * <li>CDC-1.0_Foundation-1.0</li>
 * <li>CDC-1.1_Foundation-1.1</li>
 * <li>J2SE-1.2</li>
 * <li>J2SE-1.3</li>
 * <li>J2SE-1.4</li>
 * <li>J2SE-1.5</li>
 * <li>JavaSE-1.6</li>
 * <li>JRE-1.1</li>
 * <li>OSGi_Minimum-1.0</li>
 * <li>OSGi_Minimum-1.1</li>
 * <li>OSGi_Minimum-1.2</li>
 * </ul>
 *
 * @since 1.0.0
 */
public final class ProfileModifiers {

	public static final int NO_PROFILE_VALUE = 0;
	
	public static final String CDC_1_0_FOUNDATION_1_0_NAME = "CDC-1.0/Foundation-1.0"; //$NON-NLS-1$
	public static final String CDC_1_1_FOUNDATION_1_1_NAME = "CDC-1.1/Foundation-1.1"; //$NON-NLS-1$
	public static final String J2SE_1_2_NAME = "J2SE-1.2"; //$NON-NLS-1$
	public static final String J2SE_1_3_NAME = "J2SE-1.3"; //$NON-NLS-1$
	public static final String J2SE_1_4_NAME = "J2SE-1.4"; //$NON-NLS-1$
	public static final String J2SE_1_5_NAME = "J2SE-1.5"; //$NON-NLS-1$
	public static final String JAVASE_1_6_NAME = "JavaSE-1.6"; //$NON-NLS-1$
	public static final String JAVASE_1_7_NAME = "JavaSE-1.7"; //$NON-NLS-1$
	public static final String JRE_1_1_NAME = "JRE-1.1"; //$NON-NLS-1$
	public static final String OSGI_MINIMUM_1_0_NAME = "OSGi/Minimum-1.0"; //$NON-NLS-1$
	public static final String OSGI_MINIMUM_1_1_NAME = "OSGi/Minimum-1.1"; //$NON-NLS-1$
	public static final String OSGI_MINIMUM_1_2_NAME = "OSGi/Minimum-1.2"; //$NON-NLS-1$
	
	/**
	 * Constant indicating the corresponding element has been defined in the CDC-1.0_Foundation-1.0 profile
	 */
	public static final int CDC_1_0_FOUNDATION_1_0 = 0x0001;

	/**
	 * Constant indicating the corresponding element has been defined in the CDC-1.1_Foundation-1.1 profile
	 */
	public static final int CDC_1_1_FOUNDATION_1_1 = 0x0002;

	/**
	 * Constant indicating the corresponding element has been defined in the JRE-1.1 profile
	 */
	public static final int JRE_1_1 = 0x0004;

	/**
	 * Constant indicating the corresponding element has been defined in the J2SE-1.2 profile
	 */
	public static final int J2SE_1_2 = 0x0008;

	/**
	 * Constant indicating the corresponding element has been defined in the J2SE-1.3 profile
	 */
	public static final int J2SE_1_3 = 0x0010;

	/**
	 * Constant indicating the corresponding element has been defined in the J2SE-1.4 profile
	 */
	public static final int J2SE_1_4 = 0x0020;

	/**
	 * Constant indicating the corresponding element has been defined in the J2SE-1.5 profile
	 */
	public static final int J2SE_1_5 = 0x0040;

	/**
	 * Constant indicating the corresponding element has been defined in the JavaSE-1.6 profile
	 */
	public static final int JAVASE_1_6 = 0x0080;

	/**
	 * Constant indicating the corresponding element has been defined in the JavaSE-1.6 profile
	 */
	public static final int JAVASE_1_7 = 0x0100;

	/**
	 * Constant indicating the corresponding element has been defined in the OSGi_Minimum-1.0 profile
	 */
	public static final int OSGI_MINIMUM_1_0 = 0x0200;

	/**
	 * Constant indicating the corresponding element has been defined in the OSGi_Minimum-1.1 profile
	 */
	public static final int OSGI_MINIMUM_1_1 = 0x0400;

	/**
	 * Constant indicating the corresponding element has been defined in the OSGi_Minimum-1.2 profile
	 */
	public static final int OSGI_MINIMUM_1_2 = 0x0800;
	
	/**
	 * Constant indicating all the OSGi profiles are defined
	 */
	public static final int OSGI_MINIMUM_MASK = OSGI_MINIMUM_1_0 | OSGI_MINIMUM_1_1 | OSGI_MINIMUM_1_2;

	/**
	 * Constant indicating all the CDC/Foundation profiles are defined
	 */
	public static final int CDC_FOUNDATION_MAX = 0x0003;

	/**
	 * Constant indicating all the jres profiles are defined
	 */
	public static final int JRES_MAX = JRE_1_1 | J2SE_1_2 | J2SE_1_3 | J2SE_1_4 | J2SE_1_5 | JAVASE_1_6 | JAVASE_1_7;

	/**
	 * Constructor
	 * no instantiating
	 */
	private ProfileModifiers() {}
	
	/**
	 * Returns if the CDC_1_0_FOUNDATION_1_0 modifier has been set in the given modifiers.
	 *
	 * @param modifiers the given modifiers
	 * @return true if the CDC_1_0_FOUNDATION_1_0 modifier has been set in the specified modifiers, false otherwise
	 */
	public static boolean isCDC_1_0_FOUNDATION_1_0(int modifiers) {
		return (modifiers & CDC_1_0_FOUNDATION_1_0) > 0;
	}

	/**
	 * Returns if the CDC_1_1_FOUNDATION_1_1 modifier has been set in the given modifiers.
	 *
	 * @param modifiers the given modifiers
	 * @return true if the CDC_1_1_FOUNDATION_1_1 modifier has been set in the specified modifiers, false otherwise
	 */
	public static boolean isCDC_1_1_FOUNDATION_1_1(int modifiers) {
		return (modifiers & CDC_1_1_FOUNDATION_1_1) > 0;
	}

	/**
	 * Returns if the J2SE_1_2 modifier has been set in the given modifiers.
	 *
	 * @param modifiers the given modifiers
	 * @return true if the J2SE_1_2 modifier has been set in the specified modifiers, false otherwise
	 */
	public static boolean isJ2SE_1_2(int modifiers) {
		return (modifiers & J2SE_1_2) > 0;
	}

	/**
	 * Returns if the J2SE_1_3 modifier has been set in the given modifiers.
	 *
	 * @param modifiers the given modifiers
	 * @return true if the J2SE_1_3 modifier has been set in the specified modifiers, false otherwise
	 */
	public static boolean isJ2SE_1_3(int modifiers) {
		return (modifiers & J2SE_1_3) > 0;
	}

	/**
	 * Returns if the J2SE_1_4 modifier has been set in the given modifiers.
	 *
	 * @param modifiers the given modifiers
	 * @return true if the J2SE_1_4 modifier has been set in the specified modifiers, false otherwise
	 */
	public static boolean isJ2SE_1_4(int modifiers) {
		return (modifiers & J2SE_1_4) > 0;
	}

	/**
	 * Returns if the J2SE_1_5 modifier has been set in the given modifiers.
	 *
	 * @param modifiers the given modifiers
	 * @return true if the J2SE_1_5 modifier has been set in the specified modifiers, false otherwise
	 */
	public static boolean isJ2SE_1_5(int modifiers) {
		return (modifiers & J2SE_1_5) > 0;
	}

	/**
	 * Returns if the JAVASE_1_6 modifier has been set in the given modifiers.
	 *
	 * @param modifiers the given modifiers
	 * @return true if the JAVASE_1_6 modifier has been set in the specified modifiers, false otherwise
	 */
	public static boolean isJAVASE_1_6(int modifiers) {
		return (modifiers & JAVASE_1_6) > 0;
	}

	/**
	 * Returns if the JAVASE_1_7 modifier has been set in the given modifiers.
	 *
	 * @param modifiers the given modifiers
	 * @return true if the JAVASE_1_7 modifier has been set in the specified modifiers, false otherwise
	 */
	public static boolean isJAVASE_1_7(int modifiers) {
		return (modifiers & JAVASE_1_7) > 0;
	}

	/**
	 * Returns if the JRE_1_1 modifier has been set in the given modifiers.
	 *
	 * @param modifiers the given modifiers
	 * @return true if the JRE_1_1 modifier has been set in the specified modifiers, false otherwise
	 */
	public static boolean isJRE_1_1(int modifiers) {
		return (modifiers & JRE_1_1) > 0;
	}

	/**
	 * Returns if the OSGI_MINIMUM_1_0 modifier has been set in the given modifiers.
	 *
	 * @param modifiers the given modifiers
	 * @return true if the OSGI_MINIMUM_1_0 modifier has been set in the specified modifiers, false otherwise
	 */
	public static boolean isOSGI_MINIMUM_1_0(int modifiers) {
		return (modifiers & OSGI_MINIMUM_1_0) > 0;
	}	

	/**
	 * Returns if the OSGI_MINIMUM_1_1 modifier has been set in the given modifiers.
	 *
	 * @param modifiers the given modifiers
	 * @return true if the OSGI_MINIMUM_1_1 modifier has been set in the specified modifiers, false otherwise
	 */
	public static boolean isOSGI_MINIMUM_1_1(int modifiers) {
		return (modifiers & OSGI_MINIMUM_1_1) > 0;
	}	

	/**
	 * Returns if the OSGI_MINIMUM_1_2 modifier has been set in the given modifiers.
	 *
	 * @param modifiers the given modifiers
	 * @return true if the OSGI_MINIMUM_1_2 modifier has been set in the specified modifiers, false otherwise
	 */
	public static boolean isOSGI_MINIMUM_1_2(int modifiers) {
		return (modifiers & OSGI_MINIMUM_1_2) > 0;
	}
	
	public static int getValue(String profileName) {
		if (profileName == null) {
			return NO_PROFILE_VALUE;
		}
		if (CDC_1_0_FOUNDATION_1_0_NAME.equals(profileName)) {
			return CDC_1_0_FOUNDATION_1_0;
		}
		if (CDC_1_1_FOUNDATION_1_1_NAME.equals(profileName)) {
			return CDC_1_1_FOUNDATION_1_1;
		}
		if (J2SE_1_2_NAME.equals(profileName)) {
			return J2SE_1_2;
		}
		if (J2SE_1_3_NAME.equals(profileName)) {
			return J2SE_1_3;
		}
		if (J2SE_1_4_NAME.equals(profileName)) {
			return J2SE_1_4;
		}
		if (J2SE_1_5_NAME.equals(profileName)) {
			return J2SE_1_5;
		}
		if (JAVASE_1_6_NAME.equals(profileName)) {
			return JAVASE_1_6;
		}
		if (JAVASE_1_7_NAME.equals(profileName)) {
			return JAVASE_1_7;
		}
		if (JRE_1_1_NAME.equals(profileName)) {
			return JRE_1_1;
		}
		if (OSGI_MINIMUM_1_0_NAME.equals(profileName)) {
			return OSGI_MINIMUM_1_0;
		}
		if (OSGI_MINIMUM_1_1_NAME.equals(profileName)) {
			return OSGI_MINIMUM_1_1;
		}
		if (OSGI_MINIMUM_1_2_NAME.equals(profileName)) {
			return OSGI_MINIMUM_1_2;
		}
		return NO_PROFILE_VALUE;
	}

	public static String getName(int profile) {
		switch(profile) {
			case CDC_1_0_FOUNDATION_1_0 :
				return CDC_1_0_FOUNDATION_1_0_NAME;
			case CDC_1_1_FOUNDATION_1_1 :
				return CDC_1_1_FOUNDATION_1_1_NAME;
			case J2SE_1_2 :
				return J2SE_1_2_NAME;
			case J2SE_1_3 :
				return J2SE_1_3_NAME;
			case J2SE_1_4 :
				return J2SE_1_4_NAME;
			case J2SE_1_5 :
				return J2SE_1_5_NAME;
			case JAVASE_1_6 :
				return JAVASE_1_6_NAME;
			case JAVASE_1_7 :
				return JAVASE_1_7_NAME;
			case JRE_1_1 :
				return JRE_1_1_NAME;
			case OSGI_MINIMUM_1_0 :
				return OSGI_MINIMUM_1_0_NAME;
			case OSGI_MINIMUM_1_1 :
				return OSGI_MINIMUM_1_1_NAME;
			case OSGI_MINIMUM_1_2 :
				return OSGI_MINIMUM_1_2_NAME;
			default:
				return null;
		}
	}
	public static boolean isJRE(int value) {
		return (value & JRES_MAX) != 0;
	}	
	public static boolean isJRE(String name) {
		int value = getValue(name);
		return isJRE(value);
	}
	public static boolean isOSGi(String name) {
		int value = getValue(name);
		return isOSGi(value);
	}
	public static boolean isOSGi(int value) {
		return (value & OSGI_MINIMUM_MASK) != 0;
	}
	public static boolean isCDC_Foundation(String name) {
		int value = getValue(name);
		return isCDC_Foundation(value);
	}
	public static boolean isCDC_Foundation(int value) {
		return (value & CDC_FOUNDATION_MAX) != 0;
	}
	
	public static int[] getAllIds() {
		return new int[] {
			CDC_1_0_FOUNDATION_1_0,
			CDC_1_1_FOUNDATION_1_1,
			JRE_1_1,
			J2SE_1_2,
			J2SE_1_3,
			J2SE_1_4,
			J2SE_1_5,
			JAVASE_1_6,
			JAVASE_1_7,
			OSGI_MINIMUM_1_0,
			OSGI_MINIMUM_1_1,
			OSGI_MINIMUM_1_2,
		};
	}
}

