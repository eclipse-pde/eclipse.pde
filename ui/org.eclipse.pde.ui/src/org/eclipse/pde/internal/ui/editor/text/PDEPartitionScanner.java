package org.eclipse.pde.internal.ui.editor.text;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.text.rules.*;

public class PDEPartitionScanner extends RuleBasedScanner {
	public final static String XML_DEFAULT= "__xml_default";
	public final static String XML_COMMENT =    "__xml_comment";
	public final static String XML_TAG =    "__xml_tag";

public PDEPartitionScanner() {

	IRule[] rules = new IRule[2];

	IToken xmlComment = new Token(XML_COMMENT);
	IToken tag = new Token(XML_TAG);

	rules[0] = new MultiLineRule("<!--", "-->", xmlComment);
	rules[1] = new TagRule(tag);

	setRules(rules);
}
}
