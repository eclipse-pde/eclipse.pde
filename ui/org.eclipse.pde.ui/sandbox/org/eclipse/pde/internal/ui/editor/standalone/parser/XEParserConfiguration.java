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

package org.eclipse.pde.internal.ui.editor.standalone.parser;

import org.apache.xerces.impl.*;
import org.apache.xerces.parsers.*;
import org.apache.xerces.util.*;
import org.apache.xerces.xni.grammars.*;
import org.apache.xerces.xni.parser.*;

/**
 * 
 */
public class XEParserConfiguration extends DTDXSParserConfiguration {
	
	public static final String DOCUMENT_SCANNER= StandardParserConfiguration.DOCUMENT_SCANNER;
	public static final String ENTITY_MANAGER= StandardParserConfiguration.ENTITY_MANAGER;
	public static final String DTD_VALIDATOR= StandardParserConfiguration.DTD_VALIDATOR;

	/**
	 * Constructor for XEParserConfiguration.
	 */
	public XEParserConfiguration() {
		super();
	}

	/**
	 * Constructor for XEParserConfiguration.
	 * @param symbolTable
	 */
	public XEParserConfiguration(SymbolTable symbolTable) {
		super(symbolTable);
	}

	/**
	 * Constructor for XEParserConfiguration.
	 * @param symbolTable
	 * @param grammarPool
	 */
	public XEParserConfiguration(SymbolTable symbolTable, XMLGrammarPool grammarPool) {
		super(symbolTable, grammarPool);
	}

	/**
	 * Constructor for XEParserConfiguration.
	 * @param symbolTable
	 * @param grammarPool
	 * @param parentSettings
	 */
	public XEParserConfiguration(SymbolTable symbolTable, XMLGrammarPool grammarPool, XMLComponentManager parentSettings) {
		super(symbolTable, grammarPool, parentSettings);
	}

	/*
	 * @see org.apache.xerces.parsers.StandardParserConfiguration#createDocumentScanner()
	 */
	protected XMLDocumentScanner createDocumentScanner() {
		return new XEDocumentScanner();
	}

	/*
	 * @see org.apache.xerces.parsers.StandardParserConfiguration#createEntityManager()
	 */
	protected XMLEntityManager createEntityManager() {
		return new XEEntityManager();
	}

	/*
	 * @see org.apache.xerces.parsers.StandardParserConfiguration#createErrorReporter()
	 */
	protected XMLErrorReporter createErrorReporter() {
		return new XEErrorReporter();
	}
}
