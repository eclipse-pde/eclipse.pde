/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.cheatsheet.comp;

import java.io.BufferedInputStream;
import java.io.IOException;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSConstants;
import org.eclipse.pde.internal.core.util.SAXParserWrapper;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * CompCSFileValidator
 *
 */
public class CompCSFileValidator implements ISelectionStatusValidator {

	/**
	 * 
	 */
	public CompCSFileValidator() {
		// NO-OP
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.ISelectionStatusValidator#validate(java.lang.Object[])
	 */
	public IStatus validate(Object[] selection) {
		
		// Ensure something was selected
		if (selection.length == 0) {
			return errorStatus(""); //$NON-NLS-1$
		}
		// Ensure we have a file
		if ((selection[0] instanceof IFile) == false) {
			return errorStatus(""); //$NON-NLS-1$
		}
		IFile file = (IFile)selection[0];
		// Ensure we have a simple cheat sheet file
		if (isSimpleCSFile(file) == false) {
			return errorStatus(PDEUIMessages.CompCSFileValidator_errorInvalidSimpleCS);
		}
		// If we got this far, we have a valid file
		return okStatus(""); //$NON-NLS-1$
		
	}

	/**
	 * @param file
	 */
	private boolean isSimpleCSFile(IFile file) {

		SimpleCSContentTypeHandler handler = new SimpleCSContentTypeHandler();
		try {
			SAXParserWrapper parser = new SAXParserWrapper();
			parser.parse(new BufferedInputStream(file.getContents()), handler);
		} catch (ParserConfigurationException e) {
			return false;
		} catch (AbortParseException e) {
			return handler.isSimpleCS();
		} catch (SAXException e) {
			return false;
		} catch (FactoryConfigurationError e) {
			return false;
		} catch (IOException e) {
			return false;
		} catch (CoreException e) {
			return false;
		}
		return handler.isSimpleCS();
	}
	
	/**
	 * AbortParseException
	 *
	 */
	private static class AbortParseException extends SAXException {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		/**
		 * 
		 */
		public AbortParseException() {
			super("Parsing operation forcibly aborted to save on performance time."); //$NON-NLS-1$
		}
	}
	
	/**
	 * SimpleCSContentTypeHandler
	 *
	 */
	private static class SimpleCSContentTypeHandler extends DefaultHandler {
		
		private boolean fIsSimpleCS;
		
		/**
		 * 
		 */
		public SimpleCSContentTypeHandler() {
			fIsSimpleCS = false;
		}
		
		/* (non-Javadoc)
		 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
		 */
		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {
			if (qName.equals(ISimpleCSConstants.ELEMENT_CHEATSHEET)) {
				fIsSimpleCS = true;
			}
			// Only care about the root node
			// Abort parsing to save on performance
			throw new AbortParseException();
		}
		
		/**
		 * @return
		 */
		public boolean isSimpleCS() {
			return fIsSimpleCS;
		}
	}
	
	/**
	 * @param message
	 * @return
	 */
	private IStatus errorStatus(String message) {
		return new Status(
				IStatus.ERROR,
				PDEPlugin.getPluginId(),
				IStatus.ERROR,
				message,
				null);
	}
	
	/**
	 * @param message
	 * @return
	 */
	private IStatus okStatus(String message) {
		return new Status(
				IStatus.OK,
				PDEPlugin.getPluginId(),
				IStatus.OK,
				message, 
				null);		
	}

}
