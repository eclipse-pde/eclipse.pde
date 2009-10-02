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

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.Signature;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.IApiXmlConstants;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IComponentDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IFieldDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.util.Signatures;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This class converts a collection of API use report XML files
 * from a given location to a corresponding collection of
 * HTML in a given location
 * 
 * @since 1.0.1
 */
public class UseReportConverter extends HTMLConvertor {

	/**
	 * Use visitor to write the reports
	 */
	class Visitor extends UseScanVisitor {
		
		ArrayList reports = new ArrayList();
		Report currentreport = null;
		Type currenttype = null;
		Member currentmember = null;
		HashMap keys = new HashMap();
		
		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.internal.search.UseScanVisitor#visitComponent(org.eclipse.pde.api.tools.internal.provisional.descriptors.IComponentDescriptor)
		 */
		public boolean visitComponent(IComponentDescriptor target) {
			this.currentreport = new Report();
			this.currentreport.name = composeName(target.getId(), target.getVersion());
			this.reports.add(this.currentreport);
			return true;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.internal.search.UseScanVisitor#endVisit(org.eclipse.pde.api.tools.internal.provisional.descriptors.IComponentDescriptor)
		 */
		public void endVisit(IComponentDescriptor target) {
			try {
				long start = 0;
				if(DEBUG) {
					System.out.println("Writing report for bundle: "+target.getId()); //$NON-NLS-1$
					start = System.currentTimeMillis();
				}
				writeReferencedMemberPage(this.currentreport);
				if(DEBUG) {
					System.out.println("done in: "+(System.currentTimeMillis()-start)+ " ms"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			catch(Exception e) {
				ApiPlugin.log(e);
			}
			finally {
				//clear any children as we have written them out - keep the report object to write a sorted index page
				this.currentreport.children.clear();
				this.keys.clear();
			}
		}

		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.internal.search.UseScanVisitor#visitMember(org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor)
		 */
		public boolean visitMember(IMemberDescriptor referencedMember) {
			IReferenceTypeDescriptor desc = null;
			switch(referencedMember.getElementType()) {
				case IElementDescriptor.TYPE: {
					desc = (IReferenceTypeDescriptor) referencedMember;
					break;
				}
				case IElementDescriptor.METHOD:
				case IElementDescriptor.FIELD: {
					desc = referencedMember.getEnclosingType();
					break;
				}
			}
			if(desc == null) {
				return false;
			}
			this.currenttype = (Type) this.keys.get(desc);
			if(this.currenttype == null) {
				this.currenttype = new Type(desc);
				this.keys.put(desc, this.currenttype);
			}
			TreeMap map = (TreeMap) this.currentreport.children.get(this.currenttype);
			if(map == null) {
				map = new TreeMap(compare);
				this.currentreport.children.put(this.currenttype, map);
			}
			this.currentmember = (Member) map.get(referencedMember);
			if(this.currentmember == null) {
				this.currentmember = new Member(referencedMember);
				map.put(referencedMember, this.currentmember);
			}
			return true;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.internal.search.UseScanVisitor#visitReference(int, org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor, int, int)
		 */
		public void visitReference(int refKind, IMemberDescriptor fromMember, int lineNumber, int visibility) {
			String refname = org.eclipse.pde.api.tools.internal.builder.Reference.getReferenceText(refKind);
			ArrayList refs = (ArrayList) this.currentmember.children.get(refname);
			if(refs == null) {
				refs = new ArrayList();
				this.currentmember.children.put(refname, refs);
			}
			refs.add(new Reference(fromMember, lineNumber, visibility));
			switch(fromMember.getElementType()) {
				case IElementDescriptor.TYPE: {
					switch(visibility) {
						case VisibilityModifiers.API: {
							this.currentmember.counts.total_api_type_count++;
							this.currenttype.counts.total_api_type_count++;
							this.currentreport.counts.total_api_type_count++;
							break;
						}
						case VisibilityModifiers.PRIVATE: {
							this.currentmember.counts.total_private_type_count++;
							this.currenttype.counts.total_private_type_count++;
							this.currentreport.counts.total_private_type_count++;
							break;
						}
						case VisibilityModifiers.PRIVATE_PERMISSIBLE: {
							this.currentmember.counts.total_permissable_type_count++;
							this.currenttype.counts.total_permissable_type_count++;
							this.currentreport.counts.total_permissable_type_count++;
							break;
						}
						case FRAGMENT_PERMISSIBLE: {
							this.currentmember.counts.total_fragment_permissible_type_count++;
							this.currenttype.counts.total_fragment_permissible_type_count++;
							this.currentreport.counts.total_fragment_permissible_type_count++;
							break;
						}
						default: {
							this.currentmember.counts.total_other_type_count++;
							this.currenttype.counts.total_other_type_count++;
							this.currentreport.counts.total_other_type_count++;
							break;
						}
					}
					break;
				}
				case IElementDescriptor.METHOD: {
					switch(visibility) {
						case VisibilityModifiers.API: {
							this.currentmember.counts.total_api_method_count++;
							this.currenttype.counts.total_api_method_count++;
							this.currentreport.counts.total_api_method_count++;
							break;
						}
						case VisibilityModifiers.PRIVATE: {
							this.currentmember.counts.total_private_method_count++;
							this.currenttype.counts.total_private_method_count++;
							this.currentreport.counts.total_private_method_count++;
							break;
						}
						case VisibilityModifiers.PRIVATE_PERMISSIBLE: {
							this.currentmember.counts.total_permissable_method_count++;
							this.currenttype.counts.total_permissable_method_count++;
							this.currentreport.counts.total_permissable_method_count++;
							break;
						}
						case FRAGMENT_PERMISSIBLE: {
							this.currentmember.counts.total_fragment_permissible_method_count++;
							this.currenttype.counts.total_fragment_permissible_method_count++;
							this.currentreport.counts.total_fragment_permissible_method_count++;
							break;
						}
						default: {
							this.currentmember.counts.total_other_method_count++;
							this.currenttype.counts.total_other_method_count++;
							this.currentreport.counts.total_other_method_count++;
							break;
						}
					}
					break;
				}
				case IElementDescriptor.FIELD: {
					switch(visibility) {
						case VisibilityModifiers.API: {
							this.currentmember.counts.total_api_field_count++;
							this.currenttype.counts.total_api_field_count++;
							this.currentreport.counts.total_api_field_count++;
							break;
						}
						case VisibilityModifiers.PRIVATE: {
							this.currentmember.counts.total_private_field_count++;
							this.currenttype.counts.total_private_field_count++;
							this.currentreport.counts.total_private_field_count++;
							break;
						}
						case VisibilityModifiers.PRIVATE_PERMISSIBLE: {
							this.currentmember.counts.total_permissable_field_count++;
							this.currenttype.counts.total_permissable_field_count++;
							this.currentreport.counts.total_permissable_field_count++;
							break;
						}
						case FRAGMENT_PERMISSIBLE: {
							this.currentmember.counts.total_fragment_permissible_field_count++;
							this.currenttype.counts.total_fragment_permissible_field_count++;
							this.currentreport.counts.total_fragment_permissible_field_count++;
							break;
						}
						default: {
							this.currentmember.counts.total_other_field_count++;
							this.currenttype.counts.total_other_field_count++;
							this.currentreport.counts.total_other_field_count++;
							break;
						}
					}
					break;
				}
			}
		}
	}
	
	/**
	 * Comparator for use report items
	 */
	static Comparator compare = new Comparator() {
		public int compare(Object o1, Object o2) {
			if(o1 instanceof String && o2 instanceof String) {
				return ((String)o1).compareTo((String)o2);
			}
			if(o1 instanceof Type && o2 instanceof Type) {
				return compare(((Type)o1).desc, ((Type)o2).desc);
			}
			if(o1 instanceof IReferenceTypeDescriptor && o2 instanceof IReferenceTypeDescriptor) {
				return ((IReferenceTypeDescriptor)o1).getQualifiedName().compareTo(((IReferenceTypeDescriptor)o2).getQualifiedName());
			}
			if(o1 instanceof IMethodDescriptor && o2 instanceof IMethodDescriptor) {
				try {
					return Signatures.getQualifiedMethodSignature((IMethodDescriptor)o1).compareTo(Signatures.getQualifiedMethodSignature((IMethodDescriptor)o2));
				}
				catch(CoreException ce) {
					return  -1;
				}
			}
			if(o1 instanceof IFieldDescriptor && o2 instanceof IFieldDescriptor) {
				try {
					return Signatures.getQualifiedFieldSignature((IFieldDescriptor)o1).compareTo(Signatures.getQualifiedFieldSignature((IFieldDescriptor)o2));
				}
				catch(CoreException ce) {
					return -1;
				}
			}
			return -1;
		};
	};
	
	/**
	 * Root item describing the use of one component
	 */
	static class Report {
		String name = null;
		TreeMap children = new TreeMap(compare);
		CountGroup counts = new CountGroup();
	}
	
	/**
	 * Describes a type, used to key a collection of {@link Member}s
	 */
	static class Type {
		IElementDescriptor desc = null;
		CountGroup counts = new CountGroup();
		public Type(IElementDescriptor desc) {
			this.desc = desc;
		}
	}
	
	/**
	 * Describes a member that is being used
	 */
	static class Member {
		IElementDescriptor descriptor = null;
		TreeMap children = new TreeMap(compare);
		CountGroup counts = new CountGroup();
		public Member(IElementDescriptor desc) {
			this.descriptor = desc;
		}
	}
	
	/**
	 * Describes a reference from a given descriptor
	 */
	static class Reference {
		IElementDescriptor desc = null;
		int line = -1, vis = -1;
		public Reference(IElementDescriptor desc, int line, int vis) {
			this.desc = desc;
			this.line = line;
			this.vis = vis;
		}
	}
	
	/**
	 * A group of counters to origin meta-data
	 */
	static final class CountGroup {
		int total_api_field_count = 0;
		int total_private_field_count = 0;
		int total_permissable_field_count = 0;
		int total_fragment_permissible_field_count = 0;
		int total_other_field_count = 0;
		int total_api_method_count = 0;
		int total_private_method_count = 0;
		int total_permissable_method_count = 0;
		int total_fragment_permissible_method_count = 0;
		int total_other_method_count = 0;
		int total_api_type_count = 0;
		int total_private_type_count = 0;
		int total_permissable_type_count = 0;
		int total_fragment_permissible_type_count = 0;
		int total_other_type_count = 0;
		
		public int getTotalRefCount() {
			return total_api_field_count +
					total_api_method_count +
					total_api_type_count +
					total_other_field_count +
					total_other_method_count +
					total_other_type_count +
					total_private_field_count +
					total_private_method_count +
					total_private_type_count +
					total_permissable_field_count +
					total_permissable_method_count + 
					total_permissable_type_count +
					total_fragment_permissible_field_count +
					total_fragment_permissible_method_count +
					total_fragment_permissible_type_count;
		}
		
		public int getTotalApiRefCount() {
			return total_api_field_count + total_api_method_count + total_api_type_count;
		}
		
		public int getTotalInternalRefCount() {
			return total_private_field_count + total_private_method_count + total_private_type_count;
		}
		
		public int getTotalOtherRefCount() {
			return total_other_field_count + total_other_method_count + total_other_type_count;
		}
		
		public int getTotalPermissableRefCount() {
			return total_permissable_field_count + total_permissable_method_count + total_permissable_type_count;
		}
		
		public int getTotalFragmentPermissibleRefCount() {
			return total_fragment_permissible_field_count + total_fragment_permissible_method_count + total_fragment_permissible_type_count;
		}
	}
	
	/**
	 * Handler for parsing the not_searched.xml file to output a summary or 
	 * missing required bundles
	 */
	static final class MissingHandler extends DefaultHandler {
		List missing = new ArrayList();
		static String pattern = "Require-Bundle:"; //$NON-NLS-1$
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if(IApiXmlConstants.ELEMENT_COMPONENT.equals(qName)) {
				String value = attributes.getValue("details"); //$NON-NLS-1$
				StringTokenizer tokenizer = new StringTokenizer(value, "<>"); //$NON-NLS-1$
				int index = -1;
				while(tokenizer.hasMoreTokens()) {
					value = tokenizer.nextToken();
					index = value.indexOf(pattern);
					if(index > -1) {
						missing.add(value.replaceAll(pattern, Util.EMPTY_STRING));
					}
				}
			}
		}
	}
	
	/**
	 * Visibility constant indicating an element has host-fragment level of visibility.
	 *  i.e. fragments have {@link #PRIVATE_PERMISSIBLE}-like access to the internals of their host.
	 *  
	 *  @since 1.0.1
	 */
	public static final int FRAGMENT_PERMISSIBLE = 0x0000005;
	/**
	 * Default XSLT file name
	 */
	private static final String DEFAULT_XSLT = "/references.xsl"; //$NON-NLS-1$
	/**
	 * Colour white for normal / permissible references
	 */
	static final String NORMAL_REFS_COLOUR = "#FFFFFF"; //$NON-NLS-1$
	/**
	 * Colour red for internal references
	 */
	 static final String INTERNAL_REFS_COLOUR = "#F6CECE"; //$NON-NLS-1$
	/**
	 * Style HTML bits for a page that shows references
	 */
	static final String REF_STYLE;
	/**
	 * The script block used to show an expanding table of references
	 */
	static final String REF_SCRIPT;
	
	static {
		StringBuffer buffer = new StringBuffer();
		buffer.append("<style type=\"text/css\">\n"); //$NON-NLS-1$
		buffer.append("\t.main {\t\tfont-family:Arial, Helvetica, sans-serif;\n\t}\n"); //$NON-NLS-1$
		buffer.append("\t.main h3 {\n\t\tfont-family:Arial, Helvetica, sans-serif;\n\t\t\background-color:#FFFFFF;\n\t\tfont-size:14px;\n\t\tmargin:0.1em;\n\t}\n"); //$NON-NLS-1$
		buffer.append("\t.main h4 {\n\t\tbackground-color:#CCCCCC;\n\t\tmargin:0.15em;\n\t}\n"); //$NON-NLS-1$
		buffer.append("\ta.typeslnk {\n\t\tfont-family:Arial, Helvetica, sans-serif;\n\t\ttext-decoration:none;\n\t\tmargin-left:0.25em;\n\t}\n"); //$NON-NLS-1$
		buffer.append("\ta.typeslnk:hover {\n\t\ttext-decoration:underline;\n\t}\n"); //$NON-NLS-1$
		buffer.append("\ta.kindslnk {\n\t\tfont-family:Arial, Helvetica, sans-serif;\n\t\ttext-decoration:none;\n\t\tmargin-left:0.25em;\n\t}\n"); //$NON-NLS-1$
		buffer.append("\t.types {\n\t\tdisplay:none;\n\t\tmargin-bottom:0.25em;\n\t\tmargin-top:0.25em;\n\t\tmargin-right:0.25em;\n\t\tmargin-left:0.75em;\n\t}\n"); //$NON-NLS-1$
		buffer.append("</style>\n"); //$NON-NLS-1$
		REF_STYLE = buffer.toString();
		
		buffer = new StringBuffer();
		buffer.append("<script type=\"text/javascript\">\n\tfunction expand(location) {\n\t\tif(document.getElementById) {\n\t\t\tvar childhtml = location.firstChild;\n\t\t\tif(!childhtml.innerHTML) {\n\t\t\t\tchildhtml = childhtml.nextSibling;\n\t\t\t}\n\t\t\tchildhtml.innerHTML = childhtml.innerHTML == '[+] ' ? '[-] ' : '[+] ';\n\t\t\tvar parent = location.parentNode;\n\t\t\tchildhtml = parent.nextSibling.style ? parent.nextSibling : parent.nextSibling.nextSibling;\n\t\t\tchildhtml.style.display = childhtml.style.display == 'block' ? 'none' : 'block';\n\t\t}\n\t}\n</script>\n"); //$NON-NLS-1$
		buffer.append("<noscript>\n\t<style type=\"text/css\">\n\t\t.types {display:block;}\n\t\t.kinds{display:block;}\n\t</style>\n</noscript>\n"); //$NON-NLS-1$
		REF_SCRIPT = buffer.toString();
	}
	
	/**
	 * Method used for initializing tracing in the report converter
	 */
	public static void setDebug(boolean debugValue) {
		DEBUG = debugValue || Util.DEBUG;
	}
	
	/**
	 * Constant used for controlling tracing in the report converter
	 */
	protected static boolean DEBUG = Util.DEBUG;
	
	private File htmlRoot = null;
	private File reportsRoot = null;
	private String xmlLocation = null;
	private String htmlLocation = null;
	private File htmlIndex = null;
	SAXParser parser = null;
	private boolean hasmissing = false;
	
	/**
	 * Constructor
	 * @param htmlroot the folder root where the HTML reports should be written
	 * @param xmlroot the folder root where the current API use scan output is located
	 */
	public UseReportConverter(String htmlroot, String xmlroot) {
		this.xmlLocation = xmlroot;
		this.htmlLocation = htmlroot;
	}
	
	/**
	 * Runs the converter on the given locations
	 */
	public void convert(String xslt, IProgressMonitor monitor) throws Exception {
		if (this.htmlLocation == null) {
			return;
		}
		SubMonitor localmonitor = SubMonitor.convert(monitor, SearchMessages.UseReportConverter_preparing_report_metadata, 8);
		try {
			localmonitor.setTaskName(SearchMessages.UseReportConverter_preparing_html_root);
			Util.updateMonitor(localmonitor, 1);
			this.htmlRoot = new File(this.htmlLocation);
			if (!this.htmlRoot.exists()) {
				if (!this.htmlRoot.mkdirs()) {
					throw new Exception(NLS.bind(SearchMessages.could_not_create_file, this.htmlLocation));
				}
			}
			else {
				this.htmlRoot.mkdirs();
			}
			localmonitor.setTaskName(SearchMessages.UseReportConverter_preparing_xml_root);
			Util.updateMonitor(localmonitor, 1);
			if (this.xmlLocation == null) {
				throw new Exception(SearchMessages.missing_xml_files_location);
			}
			this.reportsRoot = new File(this.xmlLocation);
			if (!this.reportsRoot.exists() || !this.reportsRoot.isDirectory()) {
				throw new Exception(NLS.bind(SearchMessages.invalid_directory_name, this.xmlLocation));
			}
			
			localmonitor.setTaskName(SearchMessages.UseReportConverter_preparing_xslt_file);
			Util.updateMonitor(localmonitor, 1);
			File xsltFile = null;
			if(xslt != null) {
				// we will use the default XSLT transform from the ant jar when this is null
				xsltFile = new File(xslt);
				if(!xsltFile.exists() || !xsltFile.isFile()) {
					throw new Exception(SearchMessages.UseReportConverter_xslt_file_not_valid);
				}
			}
			long start = 0;
			if(DEBUG) {
				start = System.currentTimeMillis();
			}
			localmonitor.setTaskName(SearchMessages.UseReportConverter_writing_not_searched);
			writeMissingBundlesPage(this.htmlRoot);
			writeNotSearchedPage(this.htmlRoot);
			Util.updateMonitor(localmonitor, 1);
			if(DEBUG) {
				System.out.println("done in: "+(System.currentTimeMillis()-start)+ " ms"); //$NON-NLS-1$ //$NON-NLS-2$
				System.out.println("Parsing use scan..."); //$NON-NLS-1$
				start = System.currentTimeMillis();
			}
			localmonitor.setTaskName(SearchMessages.UseReportConverter_parsing_use_scan);
			UseScanParser parser = new UseScanParser();
			Visitor convertor = new Visitor();
			parser.parse(xmlLocation, localmonitor.newChild(5), convertor);
			Util.updateMonitor(localmonitor, 1);
			if(DEBUG) {
				System.out.println("done in: "+(System.currentTimeMillis()-start)+ " ms"); //$NON-NLS-1$ //$NON-NLS-2$
				System.out.println("Sorting reports and writing index..."); //$NON-NLS-1$
				start = System.currentTimeMillis();
			}
			localmonitor.setTaskName(SearchMessages.UseReportConverter_writing_root_index);
			Collections.sort(convertor.reports, new Comparator() {
				public int compare(Object o1, Object o2) {
					return ((Report)o1).name.compareTo(((Report)o2).name);
				}
			});
			writeIndexPage(convertor.reports, this.htmlRoot);
			Util.updateMonitor(localmonitor, 1);
			if(DEBUG) {
				System.out.println("done in: "+(System.currentTimeMillis()-start)+ " ms"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		finally {
			if(localmonitor != null) {
				localmonitor.done();
			}
		}
	}
	
	/**
	 * Returns the handle to the default parser, caches the handle once it has been created
	 * @return the handle to the default parser
	 * @throws Exception forwarded general exception that can be trapped in Ant builds
	 */
	SAXParser getParser() throws Exception {
		if(this.parser == null) {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			try {
				this.parser = factory.newSAXParser();
			} catch (ParserConfigurationException pce) {
				throw new Exception(SearchMessages.UseReportConverter_pce_error_getting_parser, pce);
			} catch (SAXException se) {
				throw new Exception(SearchMessages.UseReportConverter_se_error_parser_handle, se);
			}
			if (this.parser == null) {
				throw new Exception(SearchMessages.could_not_create_sax_parser);
			}
		}
		return this.parser;
	}
	
	/**
	 * Builds the name for the component
	 * @param id
	 * @param version
	 * @return
	 */
	protected String composeName(String id, String version) {
		StringBuffer buffer = new StringBuffer(3+id.length()+version.length());
		buffer.append(id).append(" (").append(version).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
		return buffer.toString();
	}
	
	/**
	 * @return the index.html file created from the report conversion or <code>null</code>
	 * if the conversion failed
	 */
	public File getReportIndex() {
		return htmlIndex;
	}
	
	/**
	 * Applies the given XSLT to the given XML to produce HTML in the given file
	 * @param xsltfile
	 * @param xmlfile
	 * @param htmloutput
	 * @throws TransformerException
	 */
	protected void applyXSLT(File xsltFile, File xmlfile, File htmloutput) throws TransformerException, Exception {
		Source xslt = null;
		if (xsltFile != null) {
			xslt = new StreamSource(xsltFile);
		} else {
			InputStream defaultXsltInputStream = UseReportConverter.class.getResourceAsStream(DEFAULT_XSLT);
			if (defaultXsltInputStream != null) {
				xslt = new StreamSource(new BufferedInputStream(defaultXsltInputStream));
			}
		}
		if(xslt == null) {
			throw new Exception(SearchMessages.UseReportConverter_no_xstl_specified);
		}
		applyXSLT(xslt, xmlfile, htmloutput);
	}
	
	/**
	 * Applies the given XSLT source to the given XML file outputting to the given HTML file
	 * @param xslt
	 * @param xmlfile
	 * @param htmlfile
	 * @throws TransformerException
	 */
	protected void applyXSLT(Source xslt, File xmlfile, File htmlfile) throws TransformerException {
		Source xml = new StreamSource(xmlfile);
		Result html = new StreamResult(htmlfile);
		TransformerFactory factory = TransformerFactory.newInstance();
		Transformer former = factory.newTransformer(xslt);
		former.transform(xml, html);
	}
	
	/**
	 * Transforms the given set of xml files with the given XSLT and places the result into a
	 * corresponding HTML file
	 * @param xmlfiles
	 * @param xsltFile
	 * @param html
	 */
	protected void tranformXml(File[] xmlfiles, File xsltFile) {
		File html = null;
		for (int i = 0; i < xmlfiles.length; i++) {
			try {
				File htmlroot = new File(this.htmlLocation, getHTMLFileLocation(this.reportsRoot, xmlfiles[i]));
				if(!htmlroot.exists()) {
					htmlroot.mkdirs();
				}
				html = new File(getNameFromXMLFilename(xmlfiles[i]));
				applyXSLT(xsltFile, xmlfiles[i], html);
			}
			catch(TransformerException te) {}
			catch (Exception e) {
				ApiPlugin.log(e);
			}
		}
	}
	
	/**
	 * Gets the HTML path to write out the transformed XML file to
	 * @param reportroot
	 * @param xmlfile
	 * @return
	 */
	protected String getHTMLFileLocation(File reportroot, File xmlfile) {
		IPath xml = new Path(xmlfile.getPath());
		IPath report = new Path(reportroot.getPath());
		int segments = xml.matchingFirstSegments(report);
		if(segments > 0) {
			if(xml.getDevice() != null) {
				xml = xml.setDevice(null);
			}
			IPath html = xml.removeFirstSegments(segments);
			return html.removeLastSegments(1).toOSString();
		}
		return null;
	}
	
	/**
	 * Returns the name to use for the corresponding HTML file
	 * from the given XML file
	 * @param xmlFile
	 * @return the HTML name to use
	 */
	protected String getNameFromXMLFilename(File xmlFile) {
		String fileName = xmlFile.getAbsolutePath();
		int index = fileName.lastIndexOf('.');
		StringBuffer buffer = new StringBuffer();
		buffer.append(fileName.substring(this.reportsRoot.getAbsolutePath().length(), index)).append(HTML_EXTENSION); 
		File htmlFile = new File(this.htmlLocation, String.valueOf(buffer));
		return htmlFile.getAbsolutePath();
	}
	
	/**
	 * Returns the collection of missing bundle names
	 * @param missingfile
	 * @return the collection of missing bundle names
	 * @throws Exception
	 */
	protected String[] getMissingBundles(File missingfile) throws Exception {
		MissingHandler handler = new MissingHandler();
		getParser().parse(missingfile, handler);
		return (String[]) handler.missing.toArray(new String[handler.missing.size()]); 
	}
	
	/**
	 * Returns the sentence describing the purpose / reason of the missing bundles
	 * @return a blurb describing the table of missing bundles
	 */
	protected String getMissingBundlesHeader() {
		return SearchMessages.UseReportConverter_reported_missing_bundles;
	}
	
	/**
	 * Writes out a summary of the missing required bundles
	 * @param htmlroot
	 */
	protected void writeMissingBundlesPage(File htmlroot) throws Exception {
		File missing = null;
		PrintWriter writer = null;
		try {
			String filename = "missing"; //$NON-NLS-1$
			missing = new File(htmlroot, filename+HTML_EXTENSION); 
			if(!missing.exists()) {
				missing.createNewFile();
			}
			
			File file = new File(this.reportsRoot, "not_searched.xml"); //$NON-NLS-1$
			TreeSet sorted = new TreeSet(Util.componentsorter);
			if (file.exists()) {
				String[] missingBundles = getMissingBundles(file); 
				this.hasmissing = missingBundles.length > 0;
				for (int i = 0; i < missingBundles.length; i++) {
					sorted.add(missingBundles[i]);
				}
			}
			StringBuffer buffer = new StringBuffer();
			buffer.append(HTML_HEADER);
			buffer.append(OPEN_HTML).append(OPEN_HEAD).append(CONTENT_TYPE_META);
			buffer.append(OPEN_TITLE).append(SearchMessages.UseReportConverter_missing_required).append(CLOSE_TITLE); 
			buffer.append(CLOSE_HEAD); 
			buffer.append(OPEN_BODY); 
			buffer.append(OPEN_H3).append(SearchMessages.UseReportConverter_missing_required).append(CLOSE_H3);
			
			if(sorted.isEmpty()) {
				buffer.append(SearchMessages.UseReportConverter_no_required_missing).append(BR);
			}
			else {
				buffer.append(OPEN_P).append(getMissingBundlesHeader()).append(CLOSE_P); 
				buffer.append("<table border=\"1\" width=\"50%\">\n"); //$NON-NLS-1$
				buffer.append(OPEN_TR).append("<td bgcolor=\"#CC9933\" width=\"38%\">").append(OPEN_B).append(SearchMessages.UseReportConverter_required_bundles).append(CLOSE_B).append(CLOSE_TD).append(CLOSE_TR); //$NON-NLS-1$ 
			}
			String value = null;
			for (Iterator iter = sorted.iterator(); iter.hasNext();) {
				value = (String) iter.next();
				buffer.append(OPEN_TR).append(OPEN_TD).append(value).append(CLOSE_TD).append(CLOSE_TR);  
			}
			buffer.append(CLOSE_TABLE); 
			buffer.append(BR).append("<a href=\"not_searched.html\">").append(SearchMessages.UseReportConverter_back_to_not_searched).append(CLOSE_A); //$NON-NLS-1$ 
			buffer.append(W3C_FOOTER);
			
			//write file
			FileWriter fileWriter = new FileWriter(missing);
			writer = new PrintWriter(new BufferedWriter(fileWriter));
			writer.println(buffer.toString());
			writer.flush();
		}
		catch(IOException ioe) {
			throw new Exception(NLS.bind(SearchMessages.ioexception_writing_html_file, missing.getAbsolutePath()));
		}
		finally {
			if(writer != null) {
				writer.close();
			}
		}
	}
	
	/**
	 * Writes out the file of components that were not searched: either because they appeared in an exclude list
	 * or they have no .api_description file
	 * 
	 * @param htmlroot
	 */
	void writeNotSearchedPage(File htmlroot) throws Exception {
		File originhtml = null;
		try {
			String filename = "not_searched"; //$NON-NLS-1$
			originhtml = new File(htmlroot, filename+HTML_EXTENSION); 
			if(!originhtml.exists()) {
				originhtml.createNewFile();
			}
			File xml = new File(this.reportsRoot, filename+XML_EXTENSION); 
			InputStream defaultXsltInputStream = UseReportConverter.class.getResourceAsStream(getNotSearchedXSLPath()); 
			Source xslt = null;
			if (defaultXsltInputStream != null) {
				xslt = new StreamSource(new BufferedInputStream(defaultXsltInputStream));
			}
			if(xslt == null) {
				throw new Exception(SearchMessages.UseReportConverter_no_xstl_specified);
			}
			if (xml.exists()) {
				applyXSLT(xslt, xml, originhtml);
			}
		}
		catch(IOException ioe) {
			throw new Exception(NLS.bind(SearchMessages.ioexception_writing_html_file, originhtml.getAbsolutePath()));
		}
		catch (TransformerException te) {
			throw new Exception(SearchMessages.UseReportConverter_te_applying_xslt_skipped, te);
		}
		catch (CoreException e) {
			throw new Exception(NLS.bind(SearchMessages.UseReportConverter_coreexception_writing_html_file, originhtml.getAbsolutePath()));
		}
	}
	
	/**
	 * Returns path of XSL file to use when generating "not searched" information.
	 * 
	 * @return path to the XSL file
	 */
	String getNotSearchedXSLPath() {
		return "/notsearched.xsl"; //$NON-NLS-1$
	}
	
	/**
	 * Writes the referenced member index page
	 * @param report
	 */
	void writeReferencedMemberPage(Report report) throws Exception {
		PrintWriter writer = null;
		File originhtml = null;
		try {
			File htmlroot = new File(this.htmlLocation, report.name);
			if(!htmlroot.exists()) {
				htmlroot.mkdirs();
			}
			originhtml = new File(htmlroot, "index.html"); //$NON-NLS-1$
			if(!originhtml.exists()) {
				originhtml.createNewFile();
			}
			StringBuffer buffer = new StringBuffer();
			buffer.append(HTML_HEADER);
			buffer.append(OPEN_HTML).append(OPEN_HEAD).append(CONTENT_TYPE_META);
			buffer.append(REF_STYLE);
			buffer.append(REF_SCRIPT);
			buffer.append(OPEN_TITLE).append(NLS.bind(SearchMessages.UseReportConverter_types_used_in, report.name)).append(CLOSE_TITLE); 
			buffer.append(CLOSE_HEAD); 
			buffer.append(OPEN_BODY); 
			buffer.append(OPEN_H3).append(NLS.bind(SearchMessages.UseReportConverter_types_used_in, report.name)).append(CLOSE_H3); 
			buffer.append(getTerminologySection());
			buffer.append(getReferencesTableHeader(SearchMessages.UseReportConverter_referenced_type));
			CountGroup counts = null;
			String link = null;
			Entry entry = null;
			File typefile = null;
			TreeMap map = null;
			Type type = null;
			for (Iterator iter = report.children.entrySet().iterator(); iter.hasNext();) {
				entry = (Entry) iter.next();
				map = (TreeMap) entry.getValue();
				type = (Type) entry.getKey();
				counts = type.counts;
				
				String fqname = Signatures.getQualifiedTypeSignature((IReferenceTypeDescriptor) type.desc);
				typefile = new File(htmlroot, fqname+HTML_EXTENSION); 
				if(!typefile.exists()) {
					typefile.createNewFile();
				}
				link = extractLinkFrom(htmlroot, typefile.getAbsolutePath());
				buffer.append(getReferenceTableEntry(counts, link, fqname));
				writeTypePage(map, type, typefile, fqname);
			}
			buffer.append(CLOSE_TABLE); 
			buffer.append(OPEN_P).append("<a href=\"../index.html\">").append(SearchMessages.UseReportConverter_back_to_bundle_index).append(CLOSE_A).append(CLOSE_P); //$NON-NLS-1$ 
			buffer.append(W3C_FOOTER);
			
			FileWriter fileWriter = new FileWriter(originhtml);
			writer = new PrintWriter(new BufferedWriter(fileWriter));
			writer.println(buffer.toString());
			writer.flush();
		}
		catch(IOException ioe) {
			throw new Exception(NLS.bind(SearchMessages.ioexception_writing_html_file, originhtml.getAbsolutePath()));
		}
		finally {
			if (writer != null) {
				writer.close();
			}
		}
	}

	/**
	 * Writes the page that displays all of the members used in a type
	 * @param map
	 * @param type
	 * @param typefile
	 * @param typename
	 * @throws Exception
	 */
	void writeTypePage(TreeMap map, Type type, File typefile, String typename) throws Exception {
		PrintWriter writer = null;
		try {
			StringBuffer buffer = new StringBuffer();
			buffer.append(HTML_HEADER);
			buffer.append(OPEN_HTML).append(OPEN_HEAD).append(CONTENT_TYPE_META);
			buffer.append(REF_STYLE);
			buffer.append(REF_SCRIPT);
			buffer.append(OPEN_TITLE).append(SearchMessages.UseReportConverter_bundle_usage_information).append(CLOSE_TITLE); 
			buffer.append(CLOSE_HEAD); 
			buffer.append(OPEN_BODY); 
			buffer.append(OPEN_H3).append(NLS.bind(SearchMessages.UseReportConverter_usage_details, Signature.getSimpleName(typename))).append(CLOSE_H3); 
			buffer.append(getTypeCountSummary(typename, type.counts, map.size()));
			buffer.append(OPEN_H4).append(SearchMessages.UseReportConverter_reference_details).append(CLOSE_H4); 
			buffer.append("<table width=\"50%\" border=\"1\">\n"); //$NON-NLS-1$
			buffer.append(OPEN_P).append(SearchMessages.UseReportConverter_click_an_entry_to_see_details).append(CLOSE_P); 
			buffer.append("<div align=\"left\" class=\"main\">"); //$NON-NLS-1$
			buffer.append("<table border=\"1\" width=\"70%\">\n"); //$NON-NLS-1$
			buffer.append(OPEN_TR); 
			buffer.append("<td bgcolor=\"#CC9933\"><b>").append(SearchMessages.UseReportConverter_member).append("</b></td>\n"); //$NON-NLS-1$ //$NON-NLS-2$ 
			buffer.append(CLOSE_TR); 
			Entry entry = null;
			IElementDescriptor desc = null;
			Member mem = null;
			for (Iterator iter = map.entrySet().iterator(); iter.hasNext();) {
				entry = (Entry) iter.next();
				desc = (IElementDescriptor)entry.getKey();
				mem = (Member) entry.getValue();
				buffer.append(OPEN_TR); 
				buffer.append("<td align=\"left\">\n"); //$NON-NLS-1$
				buffer.append(OPEN_B); 
				buffer.append("<a href=\"javascript:void(0)\" class=\"typeslnk\" onclick=\"expand(this)\">\n"); //$NON-NLS-1$
				buffer.append("<span>[+] </span>").append(getDisplayName(desc, false)).append("\n");  //$NON-NLS-1$//$NON-NLS-2$
				buffer.append(CLOSE_A).append(CLOSE_B);
				buffer.append(getReferencesTable(mem)).append("\n"); //$NON-NLS-1$
				buffer.append(CLOSE_TR); 
			}
			buffer.append(CLOSE_TABLE);
			buffer.append(CLOSE_DIV); 
			buffer.append(OPEN_P).append("<a href=\"index.html\">").append(SearchMessages.UseReportConverter_back_to_bundle_index).append(CLOSE_A).append(CLOSE_P); //$NON-NLS-1$ 
			buffer.append(W3C_FOOTER);
			
			//write the file
			FileWriter fileWriter = new FileWriter(typefile);
			writer = new PrintWriter(new BufferedWriter(fileWriter));
			writer.print(buffer.toString());
			writer.flush();
		}
		catch(IOException ioe) {
			throw new Exception(NLS.bind(SearchMessages.ioexception_writing_html_file, typefile.getAbsolutePath()));
		}
		finally {
			if(writer != null) {
				writer.close();
			}
		}
	}
	
	/**
	 * Returns the nested table of references
	 * @return the nested table of references as a string
	 */
	String getReferencesTable(Member member) {
		StringBuffer buffer = new StringBuffer();
		Entry entry = null;
		buffer.append("<div colspan=\"6\" class=\"types\">\n"); //$NON-NLS-1$
		buffer.append("<table width=\"100%\" border=\"0\">\n"); //$NON-NLS-1$
		ArrayList refs = null;
		Reference ref = null;
		for (Iterator iter = member.children.entrySet().iterator(); iter.hasNext();) {
			entry = (Entry) iter.next();
			buffer.append("<tr align=\"left\"> \n"); //$NON-NLS-1$
			buffer.append("<td colspan=\"3\" bgcolor=\"#CCCCCC\">").append(OPEN_B).append(entry.getKey()).append(CLOSE_B).append(CLOSE_TD); //$NON-NLS-1$
			buffer.append(CLOSE_TR);
			buffer.append("<tr bgcolor=\"#CC9933\">"); //$NON-NLS-1$
			buffer.append("<td align=\"left\" width=\"92%\">").append(OPEN_B).append(SearchMessages.UseReportConverter_reference_location).append(CLOSE_B).append(CLOSE_TD); //$NON-NLS-1$ 
			buffer.append("<td align=\"center\" width=\"8%\">").append(OPEN_B).append(SearchMessages.UseReportConverter_line_number).append(CLOSE_B).append(CLOSE_TD); //$NON-NLS-1$ 
			buffer.append("<td align=\"center\" width=\"8%\">").append(OPEN_B).append(SearchMessages.UseReportConverter_reference_kind).append(CLOSE_B).append(CLOSE_TD); //$NON-NLS-1$ 
			buffer.append(CLOSE_TR); 
			refs = (ArrayList) entry.getValue();
			Collections.sort(refs, compare);
			for (Iterator iter2 = refs.iterator(); iter2.hasNext();) {
				ref = (Reference) iter2.next();
				try {
					String name = getDisplayName(ref.desc, true);
					buffer.append(OPEN_TR);
					buffer.append(OPEN_TD).append(name).append(CLOSE_TD); 
					buffer.append("<td align=\"center\">").append(ref.line).append(CLOSE_TD); //$NON-NLS-1$ 
					buffer.append("<td align=\"center\">").append(VisibilityModifiers.getVisibilityName(ref.vis)).append(CLOSE_TD); //$NON-NLS-1$ 
					buffer.append(CLOSE_TR); 
				}
				catch(CoreException ce) {
					ApiPlugin.log(ce);
				}
			}
		}
		buffer.append(CLOSE_TABLE); 
		buffer.append(CLOSE_DIV); 
		return buffer.toString();
	}
	
	/**
	 * Returns the name to display for the given {@link IElementDescriptor} which can be qualified or not
	 * @param desc
	 * @param qualified
	 * @return the (un)-qualified name to display for the given {@link IElementDescriptor}
	 * @throws CoreException
	 */
	String getDisplayName(IElementDescriptor desc, boolean qualified) throws CoreException {
		String displayname = null;
		switch(desc.getElementType()) {
			case IElementDescriptor.TYPE: {
				IReferenceTypeDescriptor rtype = (IReferenceTypeDescriptor) desc;
				displayname = Signatures.getTypeSignature(rtype.getSignature(), rtype.getGenericSignature(), qualified);
				break;
			}
			case IElementDescriptor.METHOD: {
				IMethodDescriptor method = (IMethodDescriptor)desc;
				if(qualified) {
					displayname = Signatures.getQualifiedMethodSignature(method);
				}
				else {
					displayname = Signatures.getMethodSignature(method);
				}
				break;
			}
			case IElementDescriptor.FIELD: {
				IFieldDescriptor field = (IFieldDescriptor) desc;
				if(qualified) {
					displayname = Signatures.getQualifiedFieldSignature(field);
				}
				else {
					displayname = field.getName();
				}
				break;
			}
		}
		return displayname;
	}
	
	/**
	 * Extracts underlying link text from the given absolute filename based off the root file
	 * @param root
	 * @param fileName
	 * @return link text pruned via the given root file
	 */
	String extractLinkFrom(File root, String fileName) {
		StringBuffer buffer = new StringBuffer();
		String substring = fileName.substring(root.getAbsolutePath().length()).replace('\\', '/');
		buffer.append('.');
		if(substring.charAt(0) != '/') {
			buffer.append('/');
		}
		buffer.append(substring);
		return String.valueOf(buffer);
	}
	
	/**
	 * Writes the main index file for the reports
	 * @param reportsRoot
	 */
	void writeIndexPage(List sortedreports, File reportsRoot) throws Exception {
		PrintWriter writer = null;
		try {
			htmlIndex = new File(this.htmlLocation, "index.html"); //$NON-NLS-1$
			if(!htmlIndex.exists()) {
				htmlIndex.createNewFile();
			}
			StringBuffer buffer = new StringBuffer();
			buffer.append(HTML_HEADER);
			buffer.append(OPEN_HTML).append(OPEN_HEAD).append(CONTENT_TYPE_META);
			buffer.append(OPEN_TITLE).append(SearchMessages.UseReportConverter_bundle_usage_information).append(CLOSE_TITLE); 
			buffer.append(CLOSE_HEAD); 
			buffer.append(OPEN_BODY); 
			buffer.append(OPEN_H3).append(SearchMessages.UseReportConverter_bundle_usage_information).append(CLOSE_H3); 
			buffer.append(OPEN_H4).append(SearchMessages.UseReportConvertor_additional_infos_section).append(CLOSE_H4); 
			if(this.hasmissing) {
				buffer.append(OPEN_P); 
				buffer.append(NLS.bind(SearchMessages.UseReportConverter_missing_bundles_prevented_scan, 
						new String[] {" <a href=\"./missing.html\">", "</a>"})); //$NON-NLS-1$ //$NON-NLS-2$
				buffer.append(CLOSE_P); 
			}
			buffer.append(OPEN_P); 
			buffer.append(NLS.bind(SearchMessages.UseReportConverter_bundles_that_were_not_searched, new String[] {"<a href=\"./not_searched.html\">", "</a></p>\n"}));  //$NON-NLS-1$//$NON-NLS-2$ 
			if(getAdditionalIndexInfo() != null) {
				buffer.append(getAdditionalIndexInfo());
			}
			if(sortedreports.size() > 0) {
				buffer.append(getTerminologySection());
				buffer.append(getReferencesTableHeader(SearchMessages.UseReportConverter_bundle));
				if(sortedreports.size() > 0) {
					Report report = null;
					File refereehtml = null;
					String link = null;
					for(Iterator iter = sortedreports.iterator(); iter.hasNext();) {
						report = (Report) iter.next();
						if(report != null) {
							refereehtml = new File(this.reportsRoot, report.name+File.separator+"index.html"); //$NON-NLS-1$
							link = extractLinkFrom(this.reportsRoot, refereehtml.getAbsolutePath());
							buffer.append(getReferenceTableEntry(report.counts, link, report.name));
						}
					}
					buffer.append(CLOSE_TABLE); 
				}
			}
			else {
				buffer.append(OPEN_P).append(BR).append(SearchMessages.UseReportConverter_no_reported_usage).append(CLOSE_P); 
			}
			buffer.append(W3C_FOOTER);
			buffer.append(CLOSE_BODY).append(CLOSE_HTML);  
			
			//write the file
			FileWriter fileWriter = new FileWriter(htmlIndex);
			writer = new PrintWriter(new BufferedWriter(fileWriter));
			writer.print(buffer.toString());
			writer.flush();
		} catch (IOException e) {
			throw new Exception(NLS.bind(SearchMessages.ioexception_writing_html_file, htmlIndex.getAbsolutePath()));
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
	}
	
	/**
	 * Returns the HTML markup for the default references table header.
	 * Where the first column contains the linked item and the following five columns are 
	 * API, Internal, Permissible, Fragment-Permissible and Other reference counts respectively
	 * @param columnname
	 * @return the default references table header
	 */
	String getReferencesTableHeader(String columnname) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("<table border=\"1\" width=\"70%\">\n"); //$NON-NLS-1$
		buffer.append(OPEN_TR); 
		buffer.append("\t<td bgcolor=\"#CC9933\" width=\"38%\"><b>").append(columnname).append(CLOSE_B).append(CLOSE_TD); //$NON-NLS-1$ 
		buffer.append("\t<td bgcolor=\"#CC9933\" align=\"center\" width=\"8%\">").append(OPEN_B).append(SearchMessages.UseReportConverter_api_references).append(CLOSE_B).append(CLOSE_TD);  //$NON-NLS-1$ 
		buffer.append("\t<td bgcolor=\"#CC9933\" align=\"center\" width=\"8%\">").append(OPEN_B).append(SearchMessages.UseReportConverter_internal_references).append(CLOSE_B).append(CLOSE_TD);  //$NON-NLS-1$ 
		buffer.append("\t<td bgcolor=\"#CC9933\" align=\"center\" width=\"8%\">").append(OPEN_B).append(SearchMessages.UseReportConverter_internal_permissible_references).append(CLOSE_B).append(CLOSE_TD); //$NON-NLS-1$ 
		buffer.append("\t<td bgcolor=\"#CC9933\" align=\"center\" width=\"8%\">").append(OPEN_B).append(SearchMessages.UseReportConverter_fragment_permissible_references).append(CLOSE_B).append(CLOSE_TD);  //$NON-NLS-1$ 
		buffer.append("\t<td bgcolor=\"#CC9933\" align=\"center\" width=\"8%\">").append(OPEN_B).append(SearchMessages.UseReportConverter_other_references).append(CLOSE_B).append(CLOSE_TD);  //$NON-NLS-1$ 
		buffer.append(CLOSE_TR); 
		return buffer.toString();
	}
	
	/**
	 * Returns the HTML markup for one entry in the default references table.
	 * Where the first column contains the linked item and the following five columns are 
	 * API, Internal, Permissible, Fragment-Permissible and Other reference counts respectively
	 * @param counts
	 * @param link
	 * @param linktext
	 * @return a single reference table entry
	 */
	String getReferenceTableEntry(CountGroup counts, String link, String linktext) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("<tr bgcolor=\"").append((counts.getTotalInternalRefCount() > 0 ? INTERNAL_REFS_COLOUR : NORMAL_REFS_COLOUR)).append("\">\n");  //$NON-NLS-1$//$NON-NLS-2$
		buffer.append("\t<td><a href=\"").append(link).append("\">").append(linktext).append("</a>").append(CLOSE_TD); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		buffer.append("\t<td align=\"center\">").append(counts.getTotalApiRefCount()).append(CLOSE_TD); //$NON-NLS-1$ 
		buffer.append("\t<td align=\"center\">").append(counts.getTotalInternalRefCount()).append(CLOSE_TD); //$NON-NLS-1$ 
		buffer.append("\t<td align=\"center\">").append(counts.getTotalPermissableRefCount()).append(CLOSE_TD); //$NON-NLS-1$ 
		buffer.append("\t<td align=\"center\">").append(counts.getTotalFragmentPermissibleRefCount()).append(CLOSE_TD); //$NON-NLS-1$ 
		buffer.append("\t<td align=\"center\">").append(counts.getTotalOtherRefCount()).append(CLOSE_TD); //$NON-NLS-1$ 
		buffer.append(CLOSE_TR); 
		return buffer.toString();
	}
	
	/**
	 * Returns the terminology section
	 * @return the terminology section
	 */
	protected String getTerminologySection() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(OPEN_H4).append(SearchMessages.UseReportConverter_terminology).append(CLOSE_H4); 
		buffer.append(OPEN_OL); 
		buffer.append(OPEN_LI).append(SearchMessages.UseReportConverter_api_ref_description).append(CLOSE_LI);  
		buffer.append(OPEN_LI).append(SearchMessages.UseReportConverter_internal_ref_description).append(CLOSE_LI);  
		buffer.append(OPEN_LI).append(SearchMessages.UseReportConverter_permissible_ref_description).append(CLOSE_LI);  
		buffer.append(OPEN_LI).append(SearchMessages.UseReportConverter_fragment_ref_description).append(CLOSE_LI);  
		buffer.append(OPEN_LI).append(SearchMessages.UseReportConverter_other_ref_description).append(CLOSE_LI);  
		buffer.append(CLOSE_OL); 
		buffer.append(OPEN_P).append(SearchMessages.UseReportConverter_inlined_description).append(CLOSE_P); 
		return buffer.toString();
	}
	
	/**
	 * Allows additional infos to be added to the HTML at the top of the report page
	 * @return
	 */
	protected String getAdditionalIndexInfo() {
		return null;
	}
	
	/**
	 * Returns HTML summary for references from a specific component.
	 * 
	 * @param typename
	 * @param counts
	 * @return HTML as a string
	 */
	protected String getTypeCountSummary(String typename, CountGroup counts, int membercount) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(OPEN_H4).append(SearchMessages.UseReportConverter_summary).append(CLOSE_H4); 
		buffer.append(OPEN_P).append(NLS.bind(SearchMessages.UseReportConverter___has_total_refs, new String[] {typename, Integer.toString(counts.getTotalRefCount()), Integer.toString(membercount)})).append(CLOSE_P);  
		return buffer.toString();
	}
}
