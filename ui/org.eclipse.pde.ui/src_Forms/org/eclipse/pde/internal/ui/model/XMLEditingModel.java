package org.eclipse.pde.internal.ui.model;

import java.io.*;

import javax.xml.parsers.*;

import org.eclipse.jface.text.*;
import org.eclipse.pde.core.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

/**
 * @author melhem
 *
 */
public abstract class XMLEditingModel extends AbstractEditingModel {
	
	private SAXParser fParser;

	public XMLEditingModel(IDocument document, boolean isReconciling) {
		super(document, isReconciling);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.AbstractEditingModel#getCharSetName()
	 */
	protected String getCharSetName() {
		return "UTF8";
	}
	
	public void load() {
		try {
			load(getInputStream(getDocument()), false);
		} catch (UnsupportedEncodingException e) {
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#load(java.io.InputStream, boolean)
	 */
	public void load(InputStream source, boolean outOfSync) {
		try {
			fIsLoaded = true;
			getParser().parse(source, createDocumentHandler(this));
		} catch (SAXException e) {
			fIsLoaded = false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
		
	protected abstract DefaultHandler createDocumentHandler(IModel model);
	
	private SAXParser getParser() {
		try {
			if (fParser == null) {
				fParser = SAXParserFactory.newInstance().newSAXParser();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return fParser;
	}
	
}
