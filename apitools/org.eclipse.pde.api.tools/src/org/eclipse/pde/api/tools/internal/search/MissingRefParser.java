/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.search;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.SAXParser;

import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.IApiXmlConstants;
import org.eclipse.pde.api.tools.internal.problems.ApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IComponentDescriptor;
import org.eclipse.pde.api.tools.internal.search.MissingRefReportConverter.MissingRefVisitor;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

class MissingRefParser extends UseScanParser {

	class MissingRefProblemHandler extends DefaultHandler {

		List problems = new ArrayList();
		private String typename;
		private int linenumber;
		private int charstart;
		private int charend;
		private int id;
		private List messageargs = new ArrayList();
		private Map extraargs = new HashMap();

		public void startDocument() throws SAXException {

		}

		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if (IApiXmlConstants.ELEMENT_API_PROBLEM.equalsIgnoreCase(qName)) {
				typename = attributes.getValue(IApiXmlConstants.ATTR_TYPE_NAME);
				linenumber = Integer.parseInt(attributes.getValue(IApiXmlConstants.ATTR_LINE_NUMBER));
				charstart = Integer.parseInt(attributes.getValue(IApiXmlConstants.ATTR_CHAR_START));
				charend = Integer.parseInt(attributes.getValue(IApiXmlConstants.ATTR_CHAR_END));
				id = Integer.parseInt(attributes.getValue(IApiXmlConstants.ATTR_ID));
			} else if (IApiXmlConstants.ELEMENT_PROBLEM_MESSAGE_ARGUMENTS.equalsIgnoreCase(qName)) {
				messageargs.clear();
			} else if (IApiXmlConstants.ELEMENT_PROBLEM_MESSAGE_ARGUMENT.equalsIgnoreCase(qName)) {
				messageargs.add(attributes.getValue(IApiXmlConstants.ATTR_VALUE));
			} else if (IApiXmlConstants.ELEMENT_PROBLEM_EXTRA_ARGUMENTS.equalsIgnoreCase(qName)) {
				extraargs.clear();
			} else if (IApiXmlConstants.ELEMENT_PROBLEM_EXTRA_ARGUMENT.equalsIgnoreCase(qName)) {
				extraargs.put(attributes.getValue(IApiXmlConstants.ATTR_ID), attributes.getValue(IApiXmlConstants.ATTR_VALUE));
			}
		}

		public void endElement(String uri, String localName, String qName) throws SAXException {
			if (IApiXmlConstants.ELEMENT_API_PROBLEM.equalsIgnoreCase(qName)) {
				String[] argumentids = new String[extraargs.size()];
				Object[] arguments = new Object[extraargs.size()];
				int i = 0;
				for (Iterator iterator = extraargs.keySet().iterator(); iterator.hasNext(); i++) {
					argumentids[i] = (String) iterator.next();
					arguments[i] = extraargs.get(argumentids[i]);
				}
				ApiProblem problem = new ApiProblem(null, typename, (String[]) messageargs.toArray(new String[messageargs.size()]), argumentids, arguments, linenumber, charstart, charend, id);
				problems.add(problem);
			}
		}

		public void endDocument() throws SAXException {
		}

		public List getProblems() {
			return problems;
		}
	}

	static final FileFilter filter = new FileFilter() {
		public boolean accept(File pathname) {
			return pathname.isFile() && pathname.getName().endsWith(".xml");  //$NON-NLS-1$
		}
	};
	
	/**
	 * @param xmlLocation
	 * @param visitor
	 * @throws Exception
	 */
	public void parse(String xmlLocation, MissingRefVisitor visitor) throws Exception {
		if (xmlLocation == null) {
			throw new Exception(SearchMessages.missing_xml_files_location);
		}
		File reportsRoot = new File(xmlLocation);
		if (!reportsRoot.exists() || !reportsRoot.isDirectory()) {
			throw new Exception(NLS.bind(SearchMessages.invalid_directory_name, xmlLocation));
		}
		File[] components = getDirectories(reportsRoot);
		components = sort(components);

		visitor.visitScan();
		SAXParser parser = getParser();
		// Treat each top level directory as a producer component
		for (int i = 0; i < components.length; i++) {
			if (components[i].isDirectory()) {
				String[] idv = getIdVersion(components[i].getName());
				IComponentDescriptor targetComponent = Factory.componentDescriptor(idv[0], idv[1]);
				if (visitor.visitComponent(targetComponent)) {
					File[] xmlfiles = Util.getAllFiles(components[i], filter);
					if (xmlfiles != null && xmlfiles.length > 0) {
						xmlfiles = sort(xmlfiles); // sort to visit in determined order
						for (int k = 0; k < xmlfiles.length; k++) {
							try {
								MissingRefProblemHandler handler = new MissingRefProblemHandler();
								parser.parse(xmlfiles[k], handler);
								List apiProblems = handler.getProblems();
								visitor.addToCurrentReport(apiProblems);
							} catch (SAXException e) {
							} catch (IOException e) {
							}
						}
					}
					visitor.endVisitComponent();
				}
			}
		}
	}
}