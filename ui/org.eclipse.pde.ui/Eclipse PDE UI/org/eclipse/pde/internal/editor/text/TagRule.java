package org.eclipse.pde.internal.editor.text;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.text.rules.*;

public class TagRule extends MultiLineRule {

public TagRule(IToken token) {
	super("<", ">", token);
}
protected boolean sequenceDetected(
	ICharacterScanner scanner,
	char[] sequence,
	boolean eofAllowed) {
	int c = scanner.read();
	if (sequence[0] == '<') {
		if (c == '?') {
			// processing instruction - abort
			scanner.unread();
			return false;
		}
		if (c == '!') {
			scanner.unread();
			// comment - abort
			return false;
		}
	}
	return super.sequenceDetected(scanner, sequence, eofAllowed);
}
}
