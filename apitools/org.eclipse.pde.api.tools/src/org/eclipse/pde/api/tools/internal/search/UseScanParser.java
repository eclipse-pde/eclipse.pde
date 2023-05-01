/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.search;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
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

		// type of file being analyzed - type reference, method reference, field
		// reference
		private int type = 0;

		/**
		 * Constructor
		 *
		 * @param type one of IReference.T_TYPE_REFERENCE,
		 *            IReference.T_METHOD_REFERENCE,
		 *            IReference.T_FIELD_REFERENCE
		 */
		public ReferenceHandler(int type) {
			this.type = type;
		}

		@Override
		public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
			processElement(uri, localName, name, attributes, type);
		}

	}

	protected String[] getIdVersion(String value) {
		int index = value.indexOf(' ');
		if (index > 0) {
			String id = value.substring(0, index);
			String version = value.substring(index + 1);
			if (version.startsWith("(")) { //$NON-NLS-1$
				version = version.substring(1);
				if (version.endsWith(")")) { //$NON-NLS-1$
					version = version.substring(0, version.length() - 1);
				}
			}
			return new String[] { id, version };
		}
		return new String[] { value, null };
	}

	/**
	 * Process the XML element described by the URI, local name, name and
	 * attributes
	 *
	 * @param uri the URI of the XML element
	 * @param localName the local name of the XML element
	 * @param name the name of the XML element
	 * @param attributes the attribute listing for the XML element
	 * @param type the type of the XML file. One of:
	 *            {@link IReference#T_TYPE_REFERENCE},
	 *            {@link IReference#T_METHOD_REFERENCE} or
	 *            {@link IReference#T_FIELD_REFERENCE}
	 * @throws SAXException
	 */
	protected void processElement(String uri, String localName, String name, Attributes attributes, int type) throws SAXException {
		if (name != null) {
			switch (name) {
				case IApiXmlConstants.REFERENCES -> {
				// Check that the current target component and referencing component
				// match what is in the file
				String target = attributes.getValue(IApiXmlConstants.ATTR_REFEREE);
				String[] idv = getIdVersion(target);
				IComponentDescriptor tcomp = Factory.componentDescriptor(idv[0], idv[1]);
				if (!tcomp.equals(this.targetComponent)) {
					System.out.println("WARNING: The referee in the xml file (" + tcomp + ") does not match the directory name (" + this.targetComponent + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
				String source = attributes.getValue(IApiXmlConstants.ATTR_ORIGIN);
				idv = getIdVersion(source);
				IComponentDescriptor sourceComponent = Factory.componentDescriptor(idv[0], idv[1]);
				if (!sourceComponent.equals(this.referencingComponent)) {
					System.out.println("WARNING: The origin in the xml file (" + sourceComponent + ") does not match the directory name (" + this.referencingComponent + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
				// Track the current reference visibility
				String visString = attributes.getValue(IApiXmlConstants.ATTR_REFERENCE_VISIBILITY);
				try {
					int vis = Integer.parseInt(visString);
					enterVisibility(vis);
				} catch (NumberFormatException e) {
					// TODO:
					enterVisibility(-1);
					System.out.println("Internal error: invalid visibility: " + visString); //$NON-NLS-1$
				}
			}
			case IApiXmlConstants.ELEMENT_TARGET -> {
				String qName = attributes.getValue(IApiXmlConstants.ATTR_TYPE);
				String memberName = attributes.getValue(IApiXmlConstants.ATTR_MEMBER_NAME);
				String signature = attributes.getValue(IApiXmlConstants.ATTR_SIGNATURE);
				IMemberDescriptor member = switch (type)
					{
					case IReference.T_TYPE_REFERENCE -> Factory.typeDescriptor(qName);
					case IReference.T_METHOD_REFERENCE -> Factory.methodDescriptor(qName, memberName, signature);
					case IReference.T_FIELD_REFERENCE -> Factory.fieldDescriptor(qName, memberName);
					default -> null;
					};
				enterTargetMember(member);
			}
			case IApiXmlConstants.REFERENCE_KIND -> {
				String value = attributes.getValue(IApiXmlConstants.ATTR_KIND);
				if (value != null) {
					try {
						enterReferenceKind(Integer.parseInt(value));
					} catch (NumberFormatException e) {
						// ERROR
						System.out.println(NLS.bind("Internal error: invalid reference kind: {0}", value)); //$NON-NLS-1$
					}
				}
			}
			case IApiXmlConstants.ATTR_REFERENCE -> {
				String qName = attributes.getValue(IApiXmlConstants.ATTR_TYPE);
				if (qName != null) {
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
					String flags = attributes.getValue(IApiXmlConstants.ATTR_FLAGS);
					try {
						int num = Integer.parseInt(line);
						int flgs = 0;
						if (flags != null) {
							flgs = Integer.parseInt(flags);
						}
						setReference(Factory.referenceDescriptor(referencingComponent, origin, num, targetComponent, targetMember, referenceKind, flgs, visibility, parseMessages(attributes)));
					} catch (NumberFormatException e) {
						// TODO:
						System.out.println("Internal error: invalid line number: " + line); //$NON-NLS-1$
					}
				} else {
					System.out.println(NLS.bind("Element {0} is missing type attribute and will be skipped", targetMember.getName())); //$NON-NLS-1$
				}
			}
			default -> { /**/ }
			}
		}
	}

	/**
	 * Parses the problem messages from the attributes
	 *
	 * @param attribs
	 * @return the messages or an empty array never <code>null</code>
	 * @since 1.1
	 */
	protected String[] parseMessages(Attributes attribs) {
		String msgs = attribs.getValue(IApiXmlConstants.ELEMENT_PROBLEM_MESSAGE_ARGUMENTS);
		String[] messages = null;
		if (msgs != null) {
			messages = msgs.split("\\,"); //$NON-NLS-1$
		}
		return messages;
	}

	/**
	 * Resolves references from an API use scan rooted at the specified location
	 * in the file system in the given baseline.
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
		localmonitor.subTask(SearchMessages.UseReportConverter_collecting_dir_info);
		File[] referees = getDirectories(reportsRoot);
		localmonitor.split(1);
		File[] origins = null;
		File[] xmlfiles = null;
		localmonitor.setWorkRemaining(referees.length);
		visitor.visitScan();
		try {
			SAXParser parser = getParser();
			// Treat each top level directory as a producer component
			for (File referee : referees) {
				if (referee.isDirectory()) {
					String[] idv = getIdVersion(referee.getName());
					IComponentDescriptor tcomp = Factory.componentDescriptor(idv[0], idv[1]);
					enterTargetComponent(tcomp);
					if (visitReferencingComponent) {

						// If the visitor returned true, treat sub-directories
						// as consumer components
						origins = getDirectories(referee);
						origins = sort(origins); // sort to visit in determined
													// order
						for (File origin : origins) {
							if (origin.isDirectory()) {
								idv = getIdVersion(origin.getName());
								IComponentDescriptor rcomp = Factory.componentDescriptor(idv[0], idv[1]);
								enterReferencingComponent(rcomp);
								if (visitMembers) {

									// If the visitor returned true, open all
									// xml files in the directory and process
									// them to find members
									localmonitor.subTask(NLS.bind(SearchMessages.UseScanParser_analyzing_references, new String[] { origin.getName() }));
									xmlfiles = Util.getAllFiles(origin, pathname -> pathname.isDirectory() || pathname.getName().endsWith(".xml")); //$NON-NLS-1$
									if (xmlfiles != null && xmlfiles.length > 0) {
										xmlfiles = sort(xmlfiles); // sort to
																	// visit in
																	// determined
																	// order
										for (File xmlfile : xmlfiles) {
											ReferenceHandler handler = new ReferenceHandler(getTypeFromFileName(xmlfile));
											try (InputStream inputFile = new FileInputStream(xmlfile.getAbsoluteFile());) {
												parser.parse(inputFile, handler);
											} catch (SAXException | IOException e) {
												ApiPlugin.log(e);
											}
										}
									}
									endMember();
								}
								endReferencingComponent();
							}
						}
					}
					localmonitor.split(1);
					endComponent();
				}
			}
		} finally {
			visitor.endVisitScan();
		}
	}

	/**
	 * Returns a parser
	 *
	 * @return default parser
	 * @throws Exception forwarded general exception that can be trapped in Ant
	 *             builds
	 */
	SAXParser getParser() throws Exception {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		try {
			return factory.newSAXParser();
		} catch (ParserConfigurationException pce) {
			throw new Exception(SearchMessages.UseReportConverter_pce_error_getting_parser, pce);
		} catch (SAXException se) {
			throw new Exception(SearchMessages.UseReportConverter_se_error_parser_handle, se);
		}
	}

	/**
	 * @return the referencingComponent or <code>null</code>
	 */
	protected IComponentDescriptor getReferencingComponent() {
		return referencingComponent;
	}

	/**
	 * @return the targetComponent or <code>null</code>
	 */
	protected IComponentDescriptor getTargetComponent() {
		return targetComponent;
	}

	/**
	 * @return the targetMember or <code>null</code>
	 */
	protected IMemberDescriptor getTargetMember() {
		return targetMember;
	}

	/**
	 * @return the referenceKind
	 */
	protected int getReferenceKind() {
		return referenceKind;
	}

	/**
	 * @return the visibility
	 */
	protected int getVisibility() {
		return visibility;
	}

	/**
	 * Returns all the child directories from the given directory
	 *
	 * @param file
	 * @return
	 */
	File[] getDirectories(File file) {
		File[] directories = file.listFiles((FileFilter) pathname -> pathname.isDirectory() && !pathname.isHidden());
		return directories;
	}

	/**
	 * Returns the {@link IReference} type from the file name
	 *
	 * @param xmlfile
	 * @return the type from the file name
	 */
	private int getTypeFromFileName(File xmlfile) {
		if (xmlfile.getName().indexOf(XmlReferenceDescriptorWriter.TYPE_REFERENCES) > -1) {
			return IReference.T_TYPE_REFERENCE;
		}
		if (xmlfile.getName().indexOf(XmlReferenceDescriptorWriter.METHOD_REFERENCES) > -1) {
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
			endComponent();

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
				visitReferences = visitor.visitMember(targetMember);
			}
		}
	}

	public void enterReferenceKind(int refKind) {
		referenceKind = refKind;
	}

	public void setReference(IReferenceDescriptor reference) {
		if (visitReferencingComponent && visitMembers && visitReferences) {
			visitor.visitReference(reference);
		}
	}

	private void endMember() {
		if (targetMember != null) {
			if (visitReferencingComponent && visitMembers) {
				visitor.endVisitMember(targetMember);
			}
			targetMember = null;
		}
	}

	private void endReferencingComponent() {
		if (referencingComponent != null) {
			if (visitReferencingComponent) {
				visitor.endVisitReferencingComponent(referencingComponent);
			}
			referencingComponent = null;
		}
	}

	private void endComponent() {
		if (targetComponent != null) {
			visitor.endVisitComponent(targetComponent);
			targetComponent = null;
		}
	}

	/**
	 * Sorts the given files by name (not path).
	 *
	 * @param files
	 * @return sorted files
	 */
	File[] sort(File[] files) {
		List<File> sorted = new ArrayList<>(files.length + 2);
		Collections.addAll(sorted, files);

		Collections.sort(sorted, Util.filesorter);
		return sorted.toArray(new File[sorted.size()]);
	}
}
