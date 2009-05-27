/*******************************************************************************
 *  Copyright (c) 2006, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     David Carver - STAR - bug 212355
 *******************************************************************************/

package org.eclipse.pde.internal.core.schema;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * SchemaAnnotationHandler
 *
 */
public class SchemaAnnotationHandler extends BaseSchemaHandler {

	private final static String[] DESC_NESTED_ELEM = {"documentation", //$NON-NLS-1$
			"annotation", "schema"}; //$NON-NLS-1$ //$NON-NLS-2$

	private final static String META_SCHEMA_ELEM = "meta.schema"; //$NON-NLS-1$

	private final static String APP_INFO_ELEM = "appinfo"; //$NON-NLS-1$

	private final static String APP_INFO_ELEM_OLD = "appInfo"; //$NON-NLS-1$

	private final static String NAME_ATTR = "name"; //$NON-NLS-1$	

	private StringBuffer fDescription;

	private String fName;

	private boolean fMetaSchemaElemFlag;

	private boolean fAppInfoElemFlag;

	/**
	 * 
	 */
	public SchemaAnnotationHandler() {
		super();
	}

	protected void reset() {
		super.reset();
		fName = null;
		fDescription = new StringBuffer();
		fMetaSchemaElemFlag = false;
		fAppInfoElemFlag = false;
	}

	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		super.startElement(uri, localName, qName, attributes);

		if ((fElementList.size() >= 2) && ((((String) fElementList.get(1)).compareTo(APP_INFO_ELEM) == 0) || (((String) fElementList.get(1)).compareTo(APP_INFO_ELEM_OLD) == 0))) {
			fAppInfoElemFlag = true;
			if (qName.compareTo(META_SCHEMA_ELEM) == 0) {
				// Case:  <appInfo><meta.schema>
				fMetaSchemaElemFlag = true;
				if (attributes != null) {
					fName = attributes.getValue(NAME_ATTR);
				}
			} else {
				// Case:  <appInfo><xxxxx>
				fMetaSchemaElemFlag = false;
			}
		}
	}

	public void characters(char[] ch, int start, int length) {
		if (onTarget()) {
			for (int i = 0; i < length; i++) {
				fDescription.append(ch[start + i]);
			}
		}
	}

	protected boolean onTarget() {
		if (fElementList.size() >= DESC_NESTED_ELEM.length) {
			for (int i = 0; i < DESC_NESTED_ELEM.length; i++) {
				String currentElement = (String) fElementList.get(i);
				if (currentElement.compareTo(DESC_NESTED_ELEM[i]) != 0) {
					return false;
				}
			}
			if (fMetaSchemaElemFlag || !fAppInfoElemFlag) {
				return true;
			}
		}
		return false;
	}

	public String getDescription() {
		return fDescription.toString();
	}

	public String getName() {
		return fName;
	}
}
