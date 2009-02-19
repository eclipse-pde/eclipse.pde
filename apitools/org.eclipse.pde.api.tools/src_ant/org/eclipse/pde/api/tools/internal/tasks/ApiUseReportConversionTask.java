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
package org.eclipse.pde.api.tools.internal.tasks;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

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

import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.api.tools.internal.IApiXmlConstants;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.search.XMLApiSearchReporter;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.ibm.icu.text.MessageFormat;

/**
 * Default task for converting the XML output from the apitooling.apiuse ant task
 * to HTML
 * 
 * @since 1.0.0
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class ApiUseReportConversionTask extends CommonUtilsTask {

	private static final String DEFAULT_XSLT = "/references.xsl"; //$NON-NLS-1$

	/**
	 * Default handler to collect a total reference count
	 */
	static final class UseDefaultHandler extends DefaultHandler {

		private Report lreport = null;
		private int type = 0;
		private CountGroup counts = null;
		
		/**
		 * Constructor
		 */
		public UseDefaultHandler(Report report, int type, CountGroup counts) {
			this.lreport = report;
			this.type = type;
			this.counts = counts;
		}
		
		/* (non-Javadoc)
		 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
		 */
		public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
			if(IApiXmlConstants.REFERENCES.equals(name)) {
				String vis = attributes.getValue(IApiXmlConstants.ATTR_REFERENCE_VISIBILITY);
				String value = attributes.getValue(IApiXmlConstants.ATTR_REFERENCE_COUNT);
				int count = Integer.parseInt(value);
				switch(Integer.parseInt(vis)) {
					case VisibilityModifiers.API: {
						switch(type) {
							case IReference.T_TYPE_REFERENCE: {
								counts.total_api_type_count = count;
								lreport.counts.total_api_type_count += count;
								break;
							}
							case IReference.T_METHOD_REFERENCE: {
								counts.total_api_method_count = count;
								lreport.counts.total_api_method_count += count;
								break;
							}
							case IReference.T_FIELD_REFERENCE: {
								counts.total_api_field_count = count;
								lreport.counts.total_api_field_count += count;
								break;
							}
						}
						break;
					}
					case VisibilityModifiers.PRIVATE: {
						switch(type) {
							case IReference.T_TYPE_REFERENCE: {
								counts.total_private_type_count = count;
								lreport.counts.total_private_type_count += count;
								break;
							}
							case IReference.T_METHOD_REFERENCE: {
								counts.total_private_method_count = count;
								lreport.counts.total_private_method_count += count;
								break;
							}
							case IReference.T_FIELD_REFERENCE: {
								counts.total_private_field_count = count;
								lreport.counts.total_private_field_count += count;
								break;
							}
						}
						break;
					}
					case VisibilityModifiers.PRIVATE_PERMISSIBLE: {
						switch(type) {
							case IReference.T_TYPE_REFERENCE: {
								counts.total_permissable_type_count = count;
								lreport.counts.total_permissable_type_count += count;
								break;
							}
							case IReference.T_METHOD_REFERENCE: {
								counts.total_permissable_method_count = count;
								lreport.counts.total_permissable_method_count += count;
								break;
							}
							case IReference.T_FIELD_REFERENCE: {
								counts.total_permissable_field_count = count;
								lreport.counts.total_permissable_field_count += count;
								break;
							}
						}
						break;
					}
					case VisibilityModifiers.ALL_VISIBILITIES: {
						switch(type) {
							case IReference.T_TYPE_REFERENCE: {
								counts.total_other_type_count = count;
								lreport.counts.total_other_type_count += count;
								break;
							}
							case IReference.T_METHOD_REFERENCE: {
								counts.total_other_method_count = count;
								lreport.counts.total_other_method_count += count;
								break;
							}
							case IReference.T_FIELD_REFERENCE: {
								counts.total_other_field_count = count;
								lreport.counts.total_other_field_count += count;
								break;
							}
						}
						break;
					}
					case VisibilityModifiers.FRAGMENT_PERMISSIBLE: {
						switch(type) {
						case IReference.T_TYPE_REFERENCE: {
							counts.total_fragment_permissible_type_count = count;
							lreport.counts.total_fragment_permissible_type_count += count;
							break;
						}
						case IReference.T_METHOD_REFERENCE: {
							counts.total_fragment_permissible_method_count = count;
							lreport.counts.total_fragment_permissible_method_count += count;
							break;
						}
						case IReference.T_FIELD_REFERENCE: {
							counts.total_fragment_permissible_field_count = count;
							lreport.counts.total_fragment_permissible_field_count += count;
							break;
						}
					}
						break;
					}
				}
			}
		}
	}
	
	/**
	 * A group of counters to origin meta-data
	 */
	private final class CountGroup {
		private int total_api_field_count = 0;
		private int total_private_field_count = 0;
		private int total_permissable_field_count = 0;
		private int total_fragment_permissible_field_count = 0;
		private int total_other_field_count = 0;
		private int total_api_method_count = 0;
		private int total_private_method_count = 0;
		private int total_permissable_method_count = 0;
		private int total_fragment_permissible_method_count = 0;
		private int total_other_method_count = 0;
		private int total_api_type_count = 0;
		private int total_private_type_count = 0;
		private int total_permissable_type_count = 0;
		private int total_fragment_permissible_type_count = 0;
		private int total_other_type_count = 0;
		
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
	 * Describes one project with references
	 */
	private final class Report {
		private File referee = null;
		private TreeMap origintorefslist = new TreeMap(filesorter);
		private TreeMap origintocountgroup = new TreeMap(filesorter);
		private CountGroup counts = new CountGroup();
	}

	HashSet reports = null;
	private File htmlRoot = null;
	private File reportsRoot = null;
	private String xmlReportsLocation = null;
	private String htmlReportsLocation = null;
	private String xsltFileLocation = null;
	
	/**
	 * Set the debug value.
	 * <p>The possible values are: <code>true</code>, <code>false</code></p>
	 * <p>Default is <code>false</code>.</p>
	 *
	 * @param debugValue the given debug value
	 */
	public void setDebug(String debugValue) {
		this.debug = Boolean.toString(true).equals(debugValue); 
	}
	/**
	 * Set the location where the html reports are generated.
	 * 
	 * <p>This is optional. If not set, the html files are created in the same folder as the
	 * xml files.</p>
	 * <p>The location is set using an absolute path.</p>
	 * 
	 * @param htmlFilesLocation the given the location where the html reports are generated
	 */
	public void setHtmlFiles(String htmlFilesLocation) {
		this.htmlReportsLocation = htmlFilesLocation;
	}
	/**
	 * Set the location where the xml reports are retrieved.
	 * 
	 * <p>The location is set using an absolute path.</p>
	 *
	 * @param xmlFilesLocation the given location to retrieve the xml reports
	 */
	public void setXmlFiles(String xmlFilesLocation) {
		this.xmlReportsLocation = xmlFilesLocation;
	}
	
	/**
	 * Sets the location of the XSLT file to use in the conversion of the XML
	 * the HTML.
	 * 
	 * <p>This is optional. If none is specified, then a default one is used.</p>
	 * 
	 * <p>The location is an absolute path.</p>
	 * 
	 * @param xsltFileLocation
	 */
	public void setXSLTFile(String xsltFileLocation) {
		this.xsltFileLocation = xsltFileLocation;
	}
	
	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
	public void execute() throws BuildException {
		if (this.debug) {
			System.out.println("XML report location: " + this.xmlReportsLocation); //$NON-NLS-1$
			System.out.println("HTML report location: " + this.htmlReportsLocation); //$NON-NLS-1$
			if (this.xsltFileLocation == null) {
				System.out.println("No XSLT file specified: using default"); //$NON-NLS-1$}
			} else {
				System.out.println("XSLT file location: " + this.xsltFileLocation); //$NON-NLS-1$}
			}
		}
		File xsltFile = null;
		if(this.xsltFileLocation != null) {
			// we will use the default XSLT transform from the ant jar when this is null
			xsltFile = new File(this.xsltFileLocation);
			if(!xsltFile.exists() || !xsltFile.isFile()) {
				throw new BuildException(Messages.ApiUseReportConversionTask_xslt_file_not_valid);
			}
		}
		if (this.xmlReportsLocation == null) {
			throw new BuildException(Messages.missing_xml_files_location);
		}
		this.reportsRoot = new File(this.xmlReportsLocation);
		if (!this.reportsRoot.exists() || !this.reportsRoot.isDirectory()) {
			throw new BuildException(Messages.bind(Messages.invalid_directory_name, this.xmlReportsLocation));
		}
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser parser = null;
		try {
			parser = factory.newSAXParser();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
		if (parser == null) {
			throw new BuildException(Messages.could_not_create_sax_parser);
		}

		if (this.htmlReportsLocation == null) {
			this.htmlReportsLocation = this.xmlReportsLocation;
		}
		this.htmlRoot = new File(this.htmlReportsLocation);
		if (!this.htmlRoot.exists()) {
			if (!this.htmlRoot.mkdirs()) {
				throw new BuildException(Messages.bind(Messages.could_not_create_file, this.htmlReportsLocation));
			}
		}
		else {
			scrubReportLocation(this.htmlRoot);
			this.htmlRoot.mkdirs();
		}
		long start = 0;
		if(this.debug) {
			System.out.println("Preparing to write indexes..."); //$NON-NLS-1$
			start = System.currentTimeMillis();
		}
		File[] referees = getDirectories(this.reportsRoot);
		this.reports = new HashSet(referees.length);
		Report report = null;
		File[] origins = null;
		File[] xmlfiles = null;
		UseDefaultHandler handler = null;
		CountGroup counts = null;
		for (int i = 0; i < referees.length; i++) {
			report = new Report();
			report.referee = referees[i];
			origins = getDirectories(referees[i]);
			for (int j = 0; j < origins.length; j++) {
				xmlfiles = Util.getAllFiles(origins[j], new FileFilter() {
					public boolean accept(File pathname) {
						return pathname.isDirectory() || pathname.getName().endsWith(".xml"); //$NON-NLS-1$
					}
				});
				if(xmlfiles != null) {
					report.origintorefslist.put(origins[j], xmlfiles);
				}
				counts = new CountGroup();
				report.origintocountgroup.put(origins[j], counts);
				for (int k = 0; k < xmlfiles.length; k++) {
					try {
						handler = new UseDefaultHandler(report, getTypeFromFileName(xmlfiles[k]), counts);
						parser.parse(xmlfiles[k], handler);
					} 
					catch (SAXException e) {}
					catch (IOException e) {}
				}
			}
			this.reports.add(report);
		}
		ArrayList sortedreports = new ArrayList(this.reports); 
		Collections.sort(sortedreports, new Comparator() {
			public int compare(Object o1, Object o2) {
				if(o1 instanceof Report && o2 instanceof Report) {
					return ((Report)o1).referee.getName().compareTo(((Report)o2).referee.getName());
				}
				return 0;
			}
		});
		if(this.debug) {
			System.out.println("done in: "+(System.currentTimeMillis()-start)+ " ms"); //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println("Writing not searched index..."); //$NON-NLS-1$
			start = System.currentTimeMillis();
		}
		writeNotSearched(htmlRoot);
		if(this.debug) {
			System.out.println("done in: "+(System.currentTimeMillis()-start)+ " ms"); //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println("Writing root index.html..."); //$NON-NLS-1$
			start = System.currentTimeMillis();
		}
		writeIndexFile(sortedreports, htmlRoot);
		if(this.debug) {
			System.out.println("done in: "+(System.currentTimeMillis()-start)+ " ms"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		//dump the reports
		TreeMap originstorefs = null;
		File origin = null;
		for(Iterator iter = sortedreports.iterator(); iter.hasNext();) {
			report = (Report) iter.next();
			if(this.debug) {
				start = System.currentTimeMillis();
				System.out.println("Writing report for "+report.referee.getName()+"..."); //$NON-NLS-1$ //$NON-NLS-2$
			}
			writeRefereeIndex(report);
			originstorefs = report.origintorefslist;
			for(Iterator iter2 = originstorefs.keySet().iterator(); iter2.hasNext();) {
				origin = (File) iter2.next();
				writeOriginEntry(report, xmlfiles, origin, (CountGroup) report.origintocountgroup.get(origin));
				xmlfiles = (File[]) originstorefs.get(origin);
				tranformXml(xmlfiles, xsltFile);
			}
			if(this.debug) {
				System.out.println("done in: "+(System.currentTimeMillis()-start)+ " ms"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}
	
	/**
	 * Writes out the file of components that were not searched: either because they appeared in an exclude list
	 * or they have no .api_description file
	 * 
	 * @param htmlroot
	 */
	private void writeNotSearched(File htmlroot) {
		PrintWriter writer = null;
		File originhtml = null;
		try {
			String filename = "not_searched"; //$NON-NLS-1$
			originhtml = new File(htmlroot, filename+".html"); //$NON-NLS-1$
			if(!originhtml.exists()) {
				originhtml.createNewFile();
			}
			FileWriter fileWriter = new FileWriter(originhtml);
			writer = new PrintWriter(new BufferedWriter(fileWriter));
			writer.println(MessageFormat.format(Messages.ApiUseReportConversionTask_bundle_list_header, new String[] {Messages.ApiUseReportConversionTask_that_were_not_searched}));
			writeComponentList(writer, filename);
			writeTableEnd(writer);
			writeBackToBundleIndex(writer, "./index"); //$NON-NLS-1$
			writeW3Footer(writer);
		}
		catch(IOException ioe) {
			throw new BuildException(Messages.bind(Messages.ioexception_writing_html_file, originhtml.getAbsolutePath()));
		}
		catch (CoreException e) {
			throw new BuildException(Messages.bind(Messages.ApiUseReportConversionTask_coreexception_writing_html_file, originhtml.getAbsolutePath()));
		}
		finally {
			if (writer != null) {
				writer.close();
			}
		}
	}
	
	/**
	 * Writes out a raw list of {@link org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent}
	 * @param writer
	 * @param filename
	 */
	private void writeComponentList(PrintWriter writer, String filename) throws CoreException {
		File xml = new File(this.reportsRoot, filename+".xml"); //$NON-NLS-1$
		if(!xml.exists()) {
			writer.println(Messages.ApiUseReportConversionTask_no_bundles);
		}
		else {
			Element root = Util.parseDocument(Util.getFileContentAsString(xml));
			NodeList components = root.getElementsByTagName(IApiXmlConstants.ELEMENT_COMPONENT);
			Element component = null;
			String id = null, nodesc = null, excluded = null, resolveerrors = null;
			for (int i = 0; i < components.getLength(); i++) {
				component = (Element) components.item(i);
				id = component.getAttribute(IApiXmlConstants.ATTR_ID);
				nodesc = component.getAttribute(ApiUseTask.NO_API_DESCRIPTION);
				excluded = component.getAttribute(ApiUseTask.EXCLUDED);
				resolveerrors = component.getAttribute(ApiUseTask.RESOLUTION_ERRORS);
				if(!"".equals(id)) { //$NON-NLS-1$
					writer.println(MessageFormat.format(Messages.ApiUseReportConversionTask_not_searched_component_list, 
									new String[] {id, nodesc, excluded, resolveerrors}));
				}
			}
		}
	}
	
	/**
	 * Writes the referee index
	 * @param report
	 */
	private void writeRefereeIndex(Report report) {
		PrintWriter writer = null;
		File originhtml = null;
		try {
			File htmlroot = new File(this.htmlReportsLocation, getHTMLFileLocation(this.reportsRoot, report.referee));
			if(!htmlroot.exists()) {
				htmlroot.mkdirs();
			}
			String refereetext = report.referee.getName();
			File root = new File(htmlroot, report.referee.getName());
			if(!root.exists()) {
				root.mkdir();
			}
			originhtml = new File(root, refereetext+".html"); //$NON-NLS-1$
			if(!originhtml.exists()) {
				originhtml.createNewFile();
			}
			FileWriter fileWriter = new FileWriter(originhtml);
			writer = new PrintWriter(new BufferedWriter(fileWriter));
			writer.println(MessageFormat.format(Messages.ApiUseReportConversionTask_referee_index_header, new String[] {report.referee.getName()}));
			writeRefereeIndexEntries(writer, report);
			writeTableEnd(writer);
			writeBackToBundleIndex(writer, "../index"); //$NON-NLS-1$
			writeW3Footer(writer);
		}
		catch(IOException ioe) {
			throw new BuildException(Messages.bind(Messages.ioexception_writing_html_file, originhtml.getAbsolutePath()));
		}
		finally {
			if (writer != null) {
				writer.close();
			}
		}
	}
	
	/**
	 * Writes out all the index entries for the referee of the given report
	 * @param writer
	 * @param report
	 */
	private void writeRefereeIndexEntries(PrintWriter writer, Report report) {
		TreeMap map = report.origintocountgroup;
		File origin = null;
		CountGroup counts = null;
		String link = null;
		File summary = null;
		for(Iterator iter = map.keySet().iterator(); iter.hasNext();) {
			origin = (File) iter.next();
			counts = (CountGroup) map.get(origin);
			summary = new File(origin, origin.getName()+".html"); //$NON-NLS-1$
			link = extractLinkFrom(report.referee, summary.getAbsolutePath());
			writer.println(MessageFormat.format(Messages.ApiUseReportConversionTask_referee_index_entry, 
					new String[] {link, 
						origin.getName(),
						Integer.toString(counts.getTotalApiRefCount()),
						Integer.toString(counts.getTotalInternalRefCount()),
						Integer.toString(counts.getTotalPermissableRefCount()),
						Integer.toString(counts.getTotalFragmentPermissibleRefCount()),
						Integer.toString(counts.getTotalOtherRefCount())}));
		}
	}
	
	/**
	 * Writes an origin index file in the corresponding origin directory
	 * @param report
	 * @param xmlfiles
	 * @param origin
	 * @param counts
	 */
	private void writeOriginEntry(Report report, File[] xmlfiles, File origin, CountGroup counts) {
		PrintWriter writer = null;
		File originhtml = null;
		try {
			File htmlroot = new File(this.htmlReportsLocation, getHTMLFileLocation(this.reportsRoot, origin));
			if(!htmlroot.exists()) {
				htmlroot.mkdirs();
			}
			String origintext = origin.getName();
			File root = new File(htmlroot, origin.getName());
			if(!root.exists()) {
				root.mkdir();
			}
			originhtml = new File(root, origintext+".html"); //$NON-NLS-1$
			if(!originhtml.exists()) {
				originhtml.createNewFile();
			}
			FileWriter fileWriter = new FileWriter(originhtml);
			writer = new PrintWriter(new BufferedWriter(fileWriter));
			writer.println(MessageFormat.format(Messages.ApiUseReportConversionTask_origin_html_header, new String[] {origin.getName(), report.referee.getName()}));
			writeOriginSummary(writer, report, origin, counts);
			writeBackToBundleIndex(writer, "../"+report.referee.getName()); //$NON-NLS-1$
			writeW3Footer(writer);
			writer.flush();
		}
		catch(IOException ioe) {
			throw new BuildException(Messages.bind(Messages.ioexception_writing_html_file, originhtml.getAbsolutePath()));
		}
		finally {
			if (writer != null) {
				writer.close();
			}
		}
	}
	
	/**
	 * Writes a convenience 'Back' link on the summary page
	 * @param writer
	 * @param indexname
	 */
	private void writeBackToBundleIndex(PrintWriter writer, String indexname) {
		writer.println(MessageFormat.format(Messages.ApiUseReportConversionTask_back_to_bundle_index, 
				new String[] {indexname+".html"})); //$NON-NLS-1$
	}
	
	/**
	 * Writes out one individual origin index entry
	 * @param writer
	 * @param origin
	 * @param counts
	 */
	private void writeOriginSummary(PrintWriter writer, Report report, File origin, CountGroup counts) {
		writer.println(MessageFormat.format(Messages.ApiUseReportConversionTask_origin_summary_header, 
				new String[] {origin.getName(), Integer.toString(counts.getTotalRefCount()), report.referee.getName()}));
		writer.println(MessageFormat.format(Messages.ApiUseReportConversionTask_origin_summary_table_entry_bold, 
				new String[]{Messages.ApiUseReportConversionTask_visibility, 
					Messages.ApiUseReportConversionTask_type, 
					Messages.ApiUseReportConversionTask_method, 
					Messages.ApiUseReportConversionTask_field}));
		writeOriginSummaryEntry(writer, 
				origin, 
				Messages.ApiUseReportConversionTask_api, 
				VisibilityModifiers.API,
				counts.total_api_type_count, 
				counts.total_api_method_count, 
				counts.total_api_field_count);
		writeOriginSummaryEntry(writer, 
				origin, 
				Messages.ApiUseReportConversionTask_internal, 
				VisibilityModifiers.PRIVATE,
				counts.total_private_type_count, 
				counts.total_private_method_count, 
				counts.total_private_field_count);
		writeOriginSummaryEntry(writer, 
				origin, 
				Messages.ApiUseReportConversionTask_internal_permissable, 
				VisibilityModifiers.PRIVATE_PERMISSIBLE,
				counts.total_permissable_type_count, 
				counts.total_permissable_method_count, 
				counts.total_permissable_field_count);
		writeOriginSummaryEntry(writer, 
				origin, 
				Messages.ApiUseReportConversionTask_fragment_permissible, 
				VisibilityModifiers.FRAGMENT_PERMISSIBLE,
				counts.total_fragment_permissible_type_count, 
				counts.total_fragment_permissible_method_count, 
				counts.total_fragment_permissible_field_count);
		writeOriginSummaryEntry(writer, 
				origin, 
				Messages.ApiUseReportConversionTask_other, 
				VisibilityModifiers.ALL_VISIBILITIES,
				counts.total_other_type_count, 
				counts.total_other_method_count, 
				counts.total_other_field_count);
		writeTableEnd(writer);
	}
	
	/**
	 * Writes one entry for an origin summary
	 * @param writer
	 * @param origin
	 * @param vis
	 * @param vismodifier
	 * @param typecount
	 * @param methodcount
	 * @param fieldcount
	 */
	private void writeOriginSummaryEntry(PrintWriter writer, File origin, String vis, int vismodifier, int typecount, int methodcount, int fieldcount) {
		writer.println(MessageFormat.format(Messages.ApiUseReportConversionTask_origin_summary_table_entry,  
				new String[]{
					vis, 
					getOriginSummaryCountLink(vismodifier, "type", origin, typecount),  //$NON-NLS-1$
					getOriginSummaryCountLink(vismodifier, "method", origin, methodcount),  //$NON-NLS-1$
					getOriginSummaryCountLink(vismodifier, "field", origin, fieldcount)})); //$NON-NLS-1$
	}
	
	/**
	 * Dumps out a link on the number of references, or just the number if the reference count is zero
	 * @param vis
	 * @param type
	 * @param origin
	 * @param count
	 * @return a link or not
	 */
	private String getOriginSummaryCountLink(int vis, String type, File origin, int count) {
		if(count == 0) {
			return Integer.toString(count);
		}
		String vname = VisibilityModifiers.getVisibilityName(vis);
		File linked = new File(origin, vname+File.separator+type+"_references.html"); //$NON-NLS-1$
		String link = extractLinkFrom(origin, linked.getAbsolutePath());
		return MessageFormat.format(Messages.ApiUseReportConversionTask_origin_summary_count_link, 
				new String[] {link, Integer.toString(count)}); 
	}
	
	/**
	 * Extracts underlying link text from the given absolute filename based off the root file
	 * @param root
	 * @param fileName
	 * @return link text pruned via the given root file
	 */
	private String extractLinkFrom(File root, String fileName) {
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
	 * Writes table end HTML 
	 * @param writer
	 */
	private void writeTableEnd(PrintWriter writer) {
		writer.println(Messages.ApiUseReportConversionTask_table_end);
	}
	
	/**
	 * Returns the {@link IReference} type from the file name
	 * @param xmlfile
	 * @return the type from the file name
	 */
	private int getTypeFromFileName(File xmlfile) {
		if(xmlfile.getName().indexOf(XMLApiSearchReporter.TYPE_REFERENCES) > -1) {
			return IReference.T_TYPE_REFERENCE;
		}
		if(xmlfile.getName().indexOf(XMLApiSearchReporter.METHOD_REFERENCES) > -1) {
			return IReference.T_METHOD_REFERENCE;
		}
		return IReference.T_FIELD_REFERENCE;
	}
	
	/**
	 * Transforms the given set of xml files with the given XSLT and places the result into a
	 * corresponding HTML file
	 * @param xmlfiles
	 * @param xsltFile
	 * @param html
	 */
	private void tranformXml(File[] xmlfiles, File xsltFile) {
		File html = null;
		for (int i = 0; i < xmlfiles.length; i++) {
			try {
				File htmlroot = new File(this.htmlReportsLocation, getHTMLFileLocation(this.reportsRoot, xmlfiles[i]));
				if(!htmlroot.exists()) {
					htmlroot.mkdirs();
				}
				html = new File(getNameFromXMLFilename(xmlfiles[i]));
				applyXSLT(xsltFile, xmlfiles[i], html);
			}
			catch(TransformerException te) {}
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
	 * Gets the HTML path to write out the transformed XML file to
	 * @param reportroot
	 * @param xmlfile
	 * @return
	 */
	private String getHTMLFileLocation(File reportroot, File xmlfile) {
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
	 * Applies the given XSLT to the given XML to produce HTML in the given file
	 * @param xsltfile
	 * @param xmlfile
	 * @param htmloutput
	 * @throws TransformerException
	 */
	private void applyXSLT(File xsltFile, File xmlfile, File htmloutput) throws TransformerException {
		Source xml = new StreamSource(xmlfile);
		Source xslt = null;
		if (xsltFile != null) {
			xslt = new StreamSource(xsltFile);
		} else {
			InputStream defaultXsltInputStream = CommonUtilsTask.class.getResourceAsStream(DEFAULT_XSLT);
			if (defaultXsltInputStream != null) {
				xslt = new StreamSource(new BufferedInputStream(defaultXsltInputStream));
			}
		}
		if(xslt == null) {
			throw new BuildException(Messages.ApiUseReportConversionTask_no_xslt_found);
		}
		Result html = new StreamResult(htmloutput);
		TransformerFactory factory = TransformerFactory.newInstance();
		Transformer former = factory.newTransformer(xslt);
		former.transform(xml, html);
	}
	
	/**
	 * Returns the name to use for the corresponding HTML file
	 * from the given XML file
	 * @param xmlFile
	 * @return the HTML name to use
	 */
	private String getNameFromXMLFilename(File xmlFile) {
		String fileName = xmlFile.getAbsolutePath();
		int index = fileName.lastIndexOf('.');
		StringBuffer buffer = new StringBuffer();
		buffer.append(fileName.substring(this.reportsRoot.getAbsolutePath().length(), index)).append(".html"); //$NON-NLS-1$
		File htmlFile = new File(this.htmlReportsLocation, String.valueOf(buffer));
		return htmlFile.getAbsolutePath();
	}
	
	/**
	 * Writes the standard W3 footer for each page
	 * @param writer
	 */
	private void writeW3Footer(PrintWriter writer) {
		writer.println(Messages.W3C_page_footer);
	}
	
	/**
	 * Writes the main index file for the reports
	 * @param reportsRoot
	 */
	private void writeIndexFile(List sortedreports, File reportsRoot) {
		PrintWriter writer = null;
		File index = null;
		try {
			index = new File(this.htmlReportsLocation, "index.html"); //$NON-NLS-1$
			if(!index.exists()) {
				index.createNewFile();
			}
			FileWriter fileWriter = new FileWriter(index);
			writer = new PrintWriter(new BufferedWriter(fileWriter));
			writer.println(Messages.ApiUseReportConversionTask_search_html_index_file_header);
			Report report = null;
			for(Iterator iter = sortedreports.iterator(); iter.hasNext();) {
				report = (Report) iter.next();
				if(report != null) {
					writeIndexEntry(writer, report);
				}
			}
			writeTableEnd(writer);
			writeW3Footer(writer);
			writer.flush();
		} catch (IOException e) {
			throw new BuildException(Messages.bind(Messages.ioexception_writing_html_file, index.getAbsolutePath()));
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
	}
	
	/**
	 * Writes a single index file entry
	 * @param writer
	 * @param report
	 * @throws IOException
	 */
	private void writeIndexEntry(PrintWriter writer, Report report) throws IOException {
		File refereehtml = new File(report.referee, report.referee.getName()+".html"); //$NON-NLS-1$
		String link = extractLinkFrom(this.reportsRoot, refereehtml.getAbsolutePath());
		writer.println(MessageFormat.format(Messages.ApiUseReportConversionTask_referee_index_entry, 
				new String[] {
					link,
					report.referee.getName(),
					Integer.toString(report.counts.getTotalApiRefCount()),
					Integer.toString(report.counts.getTotalInternalRefCount()),
					Integer.toString(report.counts.getTotalPermissableRefCount()),
					Integer.toString(report.counts.getTotalFragmentPermissibleRefCount()),
					Integer.toString(report.counts.getTotalOtherRefCount())}));
	}
	
}
