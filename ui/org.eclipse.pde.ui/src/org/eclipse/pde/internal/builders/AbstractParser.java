/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.builders;

import org.apache.xerces.parsers.SAXParser;
import java.util.Stack;
import java.util.Vector;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.internal.PDEPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public abstract class AbstractParser extends DefaultHandler {
	public static final int PARSE_PROBLEM = 99;
	
	private static final String KEY_INTERNAL_STACK = "parse.internalStack";
	private static final String KEY_UNKNOWN_TOP_ELEMENT = "parse.unknownTopElement";
	private static final String KEY_ERROR = "parse.error";
	private static final String KEY_ERROR_NAME_LINE_COLUMN = "parse.errorNameLineColumn";

	// Current State Information
	Stack stateStack = new Stack();

	// Current object stack (used to hold the current object we are
	// populating
	Stack objectStack = new Stack();

	// model parser
	private static SAXParser parser;

	static {
		initializeParser();
	}

	// Valid States
	private final int IGNORED_ELEMENT_STATE = 0;
	private final int INITIAL_STATE = 1;

	private IModel model;

	public AbstractParser(IModel model) {
		super();
		this.model = model;
		parser.setContentHandler(this);
		parser.setDTDHandler(this);
		parser.setEntityResolver(this);
		parser.setErrorHandler(this);
	}
	
	protected IModel getModel() {
		return model;
	}

	private static void initializeParser() {
		parser = new SAXParser();
		try {
			((SAXParser) parser).setFeature(
				"http://xml.org/sax/features/string-interning",
				true);
		} catch (SAXException e) {
		}
	}

	protected abstract int getDocumentStateIndex();
	protected abstract String getDocumentElementName();
	protected abstract boolean canAcceptText(int state);
	protected abstract void acceptText(String text);
	protected abstract void handleErrorStatus(IStatus status);

	public void characters(char[] ch, int start, int length) {
		int state = ((Integer) stateStack.peek()).intValue();
		if (canAcceptText(state)) {
			String value = new String(ch, start, length);
			String newValue = value.trim();
			if (!newValue.equals("") || newValue.length() != 0)
				acceptText(newValue);
		}
	}

	public void endDocument() {
	}

	protected abstract void handleEndState(int state, String elementName);

	public void endElement(String uri, String elementName, String qName) {
		int state = ((Integer) stateStack.peek()).intValue();

		if (state == IGNORED_ELEMENT_STATE) {
			stateStack.pop();
		} else if (state == INITIAL_STATE) {
			// shouldn't get here
			internalError(PDEPlugin.getFormattedMessage(KEY_INTERNAL_STACK, elementName));
		} else
			handleEndState(state, elementName);
	}

	public void error(SAXParseException ex) {
		logStatus(ex);
	}

	public void fatalError(SAXParseException ex) throws SAXException {
		logStatus(ex);
		throw ex;
	}

	public void handleInitialState(String elementName, Attributes attributes) {
		if (elementName.equals(getDocumentElementName())) {
			stateStack.push(new Integer(getDocumentStateIndex()));
		} else {
			stateStack.push(new Integer(IGNORED_ELEMENT_STATE));
			internalError(PDEPlugin.getFormattedMessage(KEY_UNKNOWN_TOP_ELEMENT, elementName));
		}
	}

	public void ignoreableWhitespace(char[] ch, int start, int length) {
	}

	private void logStatus(SAXParseException ex) {
		String name = ex.getSystemId();
		if (name == null)
			name = "";
		else
			name = name.substring(1 + name.lastIndexOf("/"));

		String msg;
		if (name.equals(""))
			msg = PDEPlugin.getFormattedMessage(KEY_ERROR, ex.getMessage());
		else
			msg =
				PDEPlugin.getFormattedMessage(
					KEY_ERROR_NAME_LINE_COLUMN,
					new String[] {
						name,
						Integer.toString(ex.getLineNumber()),
						Integer.toString(ex.getColumnNumber()),
						ex.getMessage()});
		handleErrorStatus(
			new Status(
				IStatus.WARNING,
				PDEPlugin.getPluginId(),
				PARSE_PROBLEM,
				msg,
				ex));
	}

	static String replace(String s, String from, String to) {
		String str = s;
		int fromLen = from.length();
		int toLen = to.length();
		int ix = str.indexOf(from);
		while (ix != -1) {
			str = str.substring(0, ix) + to + str.substring(ix + fromLen);
			ix = str.indexOf(from, ix + toLen);
		}
		return str;
	}
	public void startDocument() {
		stateStack.push(new Integer(INITIAL_STATE));
	}

	protected void handleState(
		int state,
		String elementName,
		Attributes attributes) {
		if (state == INITIAL_STATE) {
			handleInitialState(elementName, attributes);
		} else {
			stateStack.push(new Integer(IGNORED_ELEMENT_STATE));
			internalError(PDEPlugin.getFormattedMessage(KEY_UNKNOWN_TOP_ELEMENT, elementName));
		}
	}

	public void startElement(
		String uri,
		String elementName,
		String qName,
		Attributes attributes) {
		int state = ((Integer) stateStack.peek()).intValue();
		handleState(state, elementName, attributes);
	}

	public void warning(SAXParseException ex) {	
		logStatus(ex);
	}
	
	private void internalError(String message) {
		handleErrorStatus(new Status(IStatus.WARNING, PDEPlugin.getPluginId(), PARSE_PROBLEM, message, null));
	}
}