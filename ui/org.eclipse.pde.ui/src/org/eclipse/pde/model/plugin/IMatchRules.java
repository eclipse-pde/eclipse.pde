/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.pde.model.plugin;

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
	 * Table of rule names that match rule values defined in this 
	 * interface. It can be used directly against the rule values
	 * used in plug-in models.
	 */
	String[] RULE_NAME_TABLE =
		{ "", RULE_EQUIVALENT, RULE_COMPATIBLE, RULE_PERFECT, RULE_GREATER_OR_EQUAL };
}