/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.IApiXmlConstants;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IComponentDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parses a use scan (XML) to visit a {@link UseScanVisitor}
 */
public class UseScanParser {
	
	private UseScanVisitor visitor;
	
	private IComponentDescriptor targetComponent;
	private IComponentDescriptor referencingComponent;
	private IMemberDescriptor targetMember;
	private int referenceKind;
	private int visibility;
	
	private boolean visitReferencingComponent = true;
	private boolean visitMembers = true;
	private boolean visitReferences = true;

	/**
	 * Handler to resolve a reference
	 */
	class ReferenceHandler extends DefaultHandler {

		// type of file being analyzed - type reference, method reference, field reference
		private int type = 0;

		/**
		 * Constructor
		 * 
		 * @param type one of IReference.T_TYPE_REFERENCE, IReference.T_METHOD_REFERENCE,
		 * 			IReference.T_FIELD_REFERENCE
		 */
		public ReferenceHandler(int type) {
			this.type = type;
		}
		
		private String[] getIdVersion(String value) {
			int index = value.indexOf(' ');
			if (index > 0) {
				String id = value.substring(0, index);
				String version = value.substring(index + 1);
				if (version.startsWith("(")) { //$NON-NLS-1$
					version = version.substring(1);
					if (version.endsWith(")")) { //$NON-NLS-1$
						version = version.substring(0,version.length() - 2);
					}
				}
				return new String[]{id, version};
			}
			return new String[]{value, null};
		}
		
		
		/* (non-Javadoc)
		 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
		 */
		public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
			if (IApiXmlConstants.REFERENCES.equals(name)) {
				String target = attributes.getValue(IApiXmlConstants.ATTR_REFEREE);
				String source = attributes.getValue(IApiXmlConstants.ATTR_ORIGIN);
				String[] idv = getIdVersion(target);
				enterTargetComponent(Factory.componentDescriptor(idv[0], idv[1]));
				idv = getIdVersion(source);
				enterReferencingComponent(Factory.componentDescriptor(idv[0], idv[1]));
				String visString = attributes.getValue(IApiXmlConstants.ATTR_REFERENCE_VISIBILITY);
				try {
					int vis = Integer.parseInt(visString);
					enterVisibility(vis);
				} catch (NumberFormatException e) {
					// TODO:
					enterVisibility(-1);
					System.out.println("Internal error: invalid visibility: " + visString); //$NON-NLS-1$
				}
			} else if(IApiXmlConstants.ELEMENT_TARGET.equals(name)) {
				String qName = attributes.getValue(IApiXmlConstants.ATTR_TYPE);
				String memberName = attributes.getValue(IApiXmlConstants.ATTR_MEMBER_NAME);
				String signature = attributes.getValue(IApiXmlConstants.ATTR_SIGNATURE);
				IMemberDescriptor member = null;
				switch (type) {
					case IReference.T_TYPE_REFERENCE:
						member = Factory.typeDescriptor(qName);
						break;
					case IReference.T_METHOD_REFERENCE:
						member = Factory.methodDescriptor(qName, memberName, signature);
						break;
					case IReference.T_FIELD_REFERENCE:
						member = Factory.fieldDescriptor(qName, memberName);
						break;
				}
				enterTargetMember(member);
			} else if (IApiXmlConstants.REFERENCE_KIND.equals(name)) {
				String value = attributes.getValue(IApiXmlConstants.ATTR_KIND);
				if (value != null) {
					try {
						enterReferenceKind(Integer.parseInt(value));
					} catch (NumberFormatException e) {
						// ERROR
						System.out.println(NLS.bind("Internal error: invalid reference kind: {0}", value)); //$NON-NLS-1$
					}
				}
			} else if (IApiXmlConstants.ATTR_REFERENCE.equals(name)) {
				String qName = attributes.getValue(IApiXmlConstants.ATTR_TYPE);
				String memberName = attributes.getValue(IApiXmlConstants.ATTR_MEMBER_NAME);
				String signature = attributes.getValue(IApiXmlConstants.ATTR_SIGNATURE);
				IMemberDescriptor origin = null;
				if (signature != null) {
					origin = Factory.methodDescriptor(qName, memberName, signature);
				} else if (memberName != null) {
					origin = Factory.fieldDescriptor(qName, memberName);
				} else {
					origin = Factory.typeDescriptor(qName);
				}
				String line = attributes.getValue(IApiXmlConstants.ATTR_LINE_NUMBER);
				try {
					int num = Integer.parseInt(line);
					setReference(origin, num);
				} catch (NumberFormatException e) {
					// TODO:
					System.out.println("Internal error: invalid line number: " + line); //$NON-NLS-1$
				}
			}
		}
	}
	
	/**
	 * Resolves references from an API use scan rooted at the specified location in the file
	 * system in the given baseline.
	 * 
	 * @param xmlLocation root of API use scan (XML directory).
	 * @param monitor progress monitor
	 * @param baseline API baseline to resolve references in
	 */
	public void parse(String xmlLocation, IProgressMonitor monitor, UseScanVisitor usv) throws Exception {
		if (xmlLocation == null) {
			throw new Exception(SearchMessages.missing_xml_files_location);
		}
		visitor = usv;
		File reportsRoot = new File(xmlLocation);
		if (!reportsRoot.exists() || !reportsRoot.isDirectory()) {
			throw new Exception(NLS.bind(SearchMessages.invalid_directory_name, xmlLocation));
		}
		SubMonitor localmonitor = SubMonitor.convert(monitor, SearchMessages.UseScanParser_parsing, 8);
		localmonitor.setTaskName(SearchMessages.ApiUseReportConverter_collecting_dir_info);
		File[] referees = getDirectories(reportsRoot);
		Util.updateMonitor(localmonitor, 1);
		File[] origins = null;
		File[] xmlfiles = null;
		SubMonitor smonitor = localmonitor.newChild(7);
		smonitor.setWorkRemaining(referees.length);
		visitor.visitScan();
		try {
			SAXParser parser = getParser();
			for (int i = 0; i < referees.length; i++) {
				smonitor.setTaskName(NLS.bind(SearchMessages.UseScanParser_analyzing_references, new String[] {referees[i].getName()}));
				origins = getDirectories(referees[i]);
				origins = sort(origins); // sort to visit in determined order
				for (int j = 0; j < origins.length; j++) {
					xmlfiles = Util.getAllFiles(origins[j], new FileFilter() {
						public boolean accept(File pathname) {
							return pathname.isDirectory() || pathname.getName().endsWith(".xml"); //$NON-NLS-1$
						}
					});
					if (xmlfiles != null) {
						xmlfiles = sort(xmlfiles); // sort to visit in determined order
						for (int k = 0; k < xmlfiles.length; k++) {
							try {
								ReferenceHandler handler = new ReferenceHandler(getTypeFromFileName(xmlfiles[k]));
								parser.parse(xmlfiles[k], handler);
							} 
							catch (SAXException e) {}
							catch (IOException e) {}
						}
					}
				}
				Util.updateMonitor(smonitor, 1);
			}
			endMember();
			endReferencingComponent();
			endTargetComponent();
		}
		finally {
			visitor.endVisitScan();
			if(!smonitor.isCanceled()) {
				smonitor.done();
			}
		}		
	}
	
	/**
	 * Returns a parser
	 * @return default parser
	 * @throws Exception forwarded general exception that can be trapped in Ant builds
	 */
	SAXParser getParser() throws Exception {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		try {
			return factory.newSAXParser();
		} catch (ParserConfigurationException pce) {
			throw new Exception(SearchMessages.ApiUseReportConverter_pce_error_getting_parser, pce);
		} catch (SAXException se) {
			throw new Exception(SearchMessages.ApiUseReportConverter_se_error_parser_handle, se);
		}
	}	
	
	/**
	 * Returns all the child directories form the given directory
	 * @param file
	 * @return
	 */
	private File[] getDirectories(File file) {
		File[] directories = file.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.isDirectory() && !pathname.isHidden();
			}
		});
		return directories;
	}	
	
	/**
	 * Returns the {@link IReference} type from the file name
	 * @param xmlfile
	 * @return the type from the file name
	 */
	private int getTypeFromFileName(File xmlfile) {
		if(xmlfile.getName().indexOf(XmlReferenceDescriptorWriter.TYPE_REFERENCES) > -1) {
			return IReference.T_TYPE_REFERENCE;
		}
		if(xmlfile.getName().indexOf(XmlReferenceDescriptorWriter.METHOD_REFERENCES) > -1) {
			return IReference.T_METHOD_REFERENCE;
		}
		return IReference.T_FIELD_REFERENCE;
	}
	
	public void enterTargetComponent(IComponentDescriptor component) {
		boolean different = false;
		if (targetComponent == null) {
			different = true;
		} else {
			if (!targetComponent.equals(component)) {
				different = true;
			}
		}
		if (different) {
			// end visit
			endMember();
			endReferencingComponent();
			endTargetComponent();
			
			// start next
			targetComponent = component;
			visitReferencingComponent = visitor.visitComponent(targetComponent);
		}
	}
	
	public void enterReferencingComponent(IComponentDescriptor component) {
		boolean different = false;
		if (referencingComponent == null) {
			different = true;
		} else {
			if (!referencingComponent.equals(component)) {
				different = true;
			}
		}
		if (different) {
			// end visit
			endMember();
			endReferencingComponent();
			
			// start next
			referencingComponent = component;
			if (visitReferencingComponent) {
				visitMembers = visitor.visitReferencingComponent(referencingComponent);
			}
		}		
	}
	
	public void enterVisibility(int vis) {
		visibility = vis;
	}
	
	public void enterTargetMember(IMemberDescriptor member) {
		if (targetMember == null || !targetMember.equals(member)) {
			endMember();
			targetMember = member;
			if (visitReferencingComponent && visitMembers) {
				visitReferences  =visitor.visitMember(targetMember);
			}
		}
	}
	
	public void enterReferenceKind(int refKind) {
		referenceKind = refKind;
	}
	
	public void setReference(IMemberDescriptor from, int lineNumber) {
		if (visitReferencingComponent&& visitMembers && visitReferences) {
			visitor.visitReference(referenceKind, from, lineNumber, visibility);
		}
	}
	
	private void endMember() {
		if (targetMember != null) {
			visitor.endVisitMember(targetMember);
			targetMember = null;
		}
	}
	
	private void endReferencingComponent() {
		if (referencingComponent != null) {
			visitor.endVisitReferencingComponent(referencingComponent);
			referencingComponent = null;
		}
	}
	
	private void endTargetComponent() {
		if (targetComponent != null) {
			visitor.endVisit(targetComponent);
			targetComponent = null;
		}
	}
	
	/**
	 * Sorts the given files by name (not path).
	 * 
	 * @param files
	 * @return sorted files
	 */
	private File[] sort(File[] files) {
		List sorted = new ArrayList(files.length + 2);
		for (int i = 0; i < files.length; i++) {
			sorted.add(files[i]);
		}
		
		Collections.sort(sorted, Util.filesorter);
		return (File[]) sorted.toArray(new File[sorted.size()]);
	}
}
