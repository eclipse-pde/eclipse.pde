/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core.plugin;

/**
 * This interface contains constants used throughout the plug-in
 * for plug-in reference matching. These rules are used to
 * control when determining if two compared versions are
 * equivalent.
 */
public interface IMatchRules {
	/**
	 * No rule.
	 */
	int NONE = 0;

	/**
	 * A match that is equivalent to the required version.
	 */
	int EQUIVALENT = 1;

	/**
	 * Attribute value for the 'equivalent' rule.
	 */
	String RULE_EQUIVALENT = "equivalent";
	/**
	 * A match that is compatible with the required version.
	 */
	int COMPATIBLE = 2;
	/**
	 * Attribute value for the 'compatible' rule.
	 */
	String RULE_COMPATIBLE = "compatible";
	/**
	 * An perfect match.
	 */
	int PERFECT = 3;

	/**
	 *  Attribute value for the 'perfect' rule.
	 */
	String RULE_PERFECT = "perfect";
	/**
	 * A match requires that a version is greater or equal to the
	 * specified version.
	 */
	int GREATER_OR_EQUAL = 4;
	/**
	 * Attribute value for the 'greater or equal' rule
	 */
	String RULE_GREATER_OR_EQUAL = "greaterOrEqual";
	/**
	 * An id match requires that the specified id is a prefix of
	 * a candidate id.
	 */
	int PREFIX = 5;
	/**
	 * Attribute value for the 'prefix' id rule
	 */
	String RULE_PREFIX = "prefix";
	/**
	 * Table of rule names that match rule values defined in this 
	 * interface. It can be used directly against the rule values
	 * used in plug-in models.
	 */
	String[] RULE_NAME_TABLE =
		{ "", RULE_EQUIVALENT, RULE_COMPATIBLE, RULE_PERFECT, RULE_GREATER_OR_EQUAL };
}
