/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation and others.
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.IApiCoreConstants;
import org.eclipse.pde.api.tools.internal.problems.ApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IComponentDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.search.IMetadata;
import org.osgi.framework.Version;

public class MissingRefReportConverter extends UseReportConverter {

	class MissingRefVisitor {
		public List reports;

		public void visitScan() {
			reports = new ArrayList();
		}

		public boolean visitComponent(IComponentDescriptor targetComponent) {
			currentreport = new Report();
			currentreport.name = composeName(targetComponent.getId(), targetComponent.getVersion());
			reports.add(currentreport);
			return true;
		}

		/**
		 * Builds the name for the component
		 * @param id id of the component
		 * @param version version of the component, can be <code>null</code>
		 * @return string name
		 */
		protected String composeName(String id, String version) {
			String versionName = version;
			if (version == null) {
				versionName = Version.emptyVersion.toString();
			}
			StringBuffer buffer = new StringBuffer(3 + id.length() + versionName.length());
			buffer.append(id).append(" (").append(versionName).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
			return buffer.toString();
		}

		public void endVisitComponent() {
			try {
				writeIndexFileForComponent(currentreport);
			} catch (Exception e) {
				ApiPlugin.log(e);
			}
		}

		private void writeIndexFileForComponent(Report report) throws Exception {
			PrintWriter writer = null;
			File originhtml = null;
			try {
				File htmlroot = new File(getHtmlLocation(), report.name);
				if (!htmlroot.exists()) {
					htmlroot.mkdirs();
				}
				originhtml = new File(htmlroot, "index.html"); //$NON-NLS-1$
				if (!originhtml.exists()) {
					originhtml.createNewFile();
				}
				StringBuffer buffer = new StringBuffer();
				buffer.append(HTML_HEADER);
				buffer.append(OPEN_HTML).append(OPEN_HEAD).append(CONTENT_TYPE_META);
				buffer.append(REF_STYLE);
				buffer.append(REF_SCRIPT);
				buffer.append(OPEN_TITLE).append(getProblemTitle(report.name)).append(CLOSE_TITLE);
				buffer.append(CLOSE_HEAD);
				buffer.append(OPEN_BODY);
				buffer.append(OPEN_H3).append(getProblemTitle(report.name)).append(CLOSE_H3);
				buffer.append(getProblemSummary(report));

				StringBuffer typeProblems = new StringBuffer();
				StringBuffer methodProblems = new StringBuffer();
				StringBuffer fieldProblems = new StringBuffer();
				Entry entry = null;
				Integer key = null;
				TreeMap types = null;
				for (Iterator iterator = report.apiProblems.entrySet().iterator(); iterator.hasNext();) {
					entry = (Entry) iterator.next();
					key = (Integer) entry.getKey();
					types = (TreeMap) entry.getValue();
					switch (key.intValue()) {
						case IApiProblem.API_USE_SCAN_TYPE_PROBLEM :
							typeProblems.append(getProblemTable(types));
							break;
						case IApiProblem.API_USE_SCAN_METHOD_PROBLEM :
							methodProblems.append(getProblemTable(types));
							break;
						case IApiProblem.API_USE_SCAN_FIELD_PROBLEM :
							fieldProblems.append(getProblemTable(types));
					}
				}
				buffer.append(getProblemsTableHeader(SearchMessages.MissingRefReportConverter_ProblemDetails, SearchMessages.MissingRefReportConverter_ProblemTypes)); 
				if (typeProblems.length() > 0) {
					buffer.append(getProblemRow(typeProblems, SearchMessages.MissingRefReportConverter_Type));
				}
				if (methodProblems.length() > 0) {
					buffer.append(getProblemRow(methodProblems, SearchMessages.MissingRefReportConverter_Method));
				}
				if (fieldProblems.length() > 0) {
					buffer.append(getProblemRow(fieldProblems, SearchMessages.MissingRefReportConverter_Field)); 
				}
				buffer.append(CLOSE_TABLE);

				buffer.append(OPEN_P).append("<a href=\"../index.html\">").append(SearchMessages.MissingRefReportConverter_BackToIndex).append(CLOSE_A).append(CLOSE_P); //$NON-NLS-1$ 
				buffer.append(W3C_FOOTER);
				buffer.append(CLOSE_BODY);

				writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(originhtml), IApiCoreConstants.UTF_8));;
				writer.println(buffer.toString());
				writer.flush();
			} catch (IOException ioe) {
				throw new Exception(NLS.bind(SearchMessages.ioexception_writing_html_file, originhtml.getAbsolutePath()));
			} finally {
				if (writer != null) {
					writer.close();
				}

			}
		}

		private StringBuffer getProblemRow(StringBuffer type, String header) {
			StringBuffer buffer = new StringBuffer();
			buffer.append(OPEN_TR);
			buffer.append("<td align=\"left\">\n"); //$NON-NLS-1$
			buffer.append(OPEN_B);
			buffer.append("<a href=\"javascript:void(0)\" class=\"typeslnk\" onclick=\"expand(this)\" title=\""); //$NON-NLS-1$
			buffer.append(header).append("\">\n"); //$NON-NLS-1$
			buffer.append("<span>[+] </span>").append(header).append("\n"); //$NON-NLS-1$//$NON-NLS-2$
			buffer.append(CLOSE_A).append(CLOSE_B);
			buffer.append("<div colspan=\"6\" class=\"types\">\n"); //$NON-NLS-1$
			buffer.append(type).append("\n"); //$NON-NLS-1$
			buffer.append(CLOSE_DIV);
			buffer.append(CLOSE_TR);
			return buffer;
		}

		private StringBuffer getProblemTable(TreeMap types) {
			StringBuffer buffer = new StringBuffer();
			buffer.append("<table width=\"100%\" border=\"0\" cellspacing=\"1\" cellpadding=\"6\">\n"); //$NON-NLS-1$
			Entry entry = null;
			String tname = null;
			ArrayList pbs = null;
			for (Iterator i = types.entrySet().iterator(); i.hasNext();) {
				entry = (Entry) i.next();
				tname = (String) entry.getKey();
				pbs = (ArrayList) entry.getValue();
				buffer.append("<tr align=\"left\"> \n"); //$NON-NLS-1$
				buffer.append("<td colspan=\"1\" bgcolor=\"#CCCCCC\">").append(OPEN_B).append(tname).append(CLOSE_B).append(CLOSE_TD); //$NON-NLS-1$
				buffer.append(CLOSE_TR);
				ApiProblem pb = null;
				Collections.sort(pbs, compare);
				for (Iterator i2 = pbs.iterator(); i2.hasNext();) {
					pb = (ApiProblem) i2.next();
					buffer.append(OPEN_TR);
					buffer.append("<td align=\"left\" width=\"75%\">").append(pb.getMessage()).append(CLOSE_TD); //$NON-NLS-1$ 
					buffer.append(CLOSE_TR);
				}
			}
			buffer.append(CLOSE_TABLE);
			return buffer;
		}

		private Object getProblemSummary(Report report) {
			StringBuffer buffer = new StringBuffer();
			buffer.append(OPEN_H4).append(SearchMessages.MissingRefReportConverter_Summary).append(CLOSE_H4);
			buffer.append(OPEN_P).append(NLS.bind(SearchMessages.MissingRefReportConverter_SummaryDesc, new String[] {report.name, Integer.toString(report.apiProblems.size())})).append(CLOSE_P);
			return buffer.toString();
		}

		/**
		 * Returns the HTML markup for the problems table header.
		 * @param sectionname
		 * @param type
		 * @return the default references table header
		 */
		String getProblemsTableHeader(String sectionname, String type) {
			StringBuffer buffer = new StringBuffer();
			buffer.append(OPEN_H4).append(sectionname).append(CLOSE_H4);
			buffer.append(OPEN_P).append(SearchMessages.MissingRefReportConverter_ProblemTableHeader).append(CLOSE_P);
			buffer.append("<div align=\"left\" class=\"main\">"); //$NON-NLS-1$
			buffer.append("<table border=\"1\" width=\"80%\">\n"); //$NON-NLS-1$

			buffer.append(OPEN_TR);
			buffer.append("<td bgcolor=\"").append(REFERENCES_TABLE_HEADER_COLOUR).append("\">").append(OPEN_B).append(type).append("</b></td>\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
			buffer.append(CLOSE_TR);

			return buffer.toString();
		}

		/**
		 * @param bundle
		 * @return the page title 
		 */
		protected String getProblemTitle(String bundle) {
			return NLS.bind(SearchMessages.MissingRefReportConverter_ProblemTitle, bundle);
		}

		public void addToCurrentReport(List apiProblems) {
			currentreport.add(apiProblems);
		}
	};

	private String xmlLocation = null;
	private String htmlLocation = null;
	private File reportsRoot = null;
	private File htmlIndex = null;
	Report currentreport = null;

	static final Comparator compare = new Comparator() {
		public int compare(Object o1, Object o2) {
			if(o1 instanceof String && o2 instanceof String) {
				return ((String)o1).compareTo((String)o2);
			}
			if(o1 instanceof ApiProblem && o2 instanceof ApiProblem) {
				return ((ApiProblem)o1).getMessage().compareTo(((ApiProblem)o2).getMessage());
			}
			return 0;
		}
	};
	
	/**
	 * Root item describing the use of one component
	 */
	static class Report {
		String name = null;
		TreeMap apiProblems = new TreeMap();
		int typeProblems = 0;
		int methodProblems = 0;
		int fieldProblems = 0;

		public void add(List apiProblems) {
			ApiProblem pb = null;
			ArrayList list = null;
			TreeMap types = null;
			for (Iterator i = apiProblems.iterator(); i.hasNext();) {
				pb = (ApiProblem) i.next();
				Integer key = new Integer(pb.getKind());
				types = (TreeMap) this.apiProblems.get(key);
				if(types == null) {
					types = new TreeMap(compare);
					this.apiProblems.put(key, types);
				}
				String tname = pb.getTypeName();
				list = (ArrayList) types.get(tname);
				if(list == null) {
					list = new ArrayList();
					types.put(tname, list);
				}
				list.add(pb);
				switch (pb.getKind()) {
					case 1 :
						++typeProblems;
						break;
					case 2 :
						++methodProblems;
						break;
					case 3 :
						++fieldProblems;
						break;
				}
			}
		}

		public int getTotal() {
			return typeProblems + methodProblems + fieldProblems;
		}
	}

	/**
	 * Constructor
	 * @param htmlroot the folder root where the HTML reports should be written
	 * @param xmlroot the folder root where the current API use scan output is located
	 */
	public MissingRefReportConverter(String htmlroot, String xmlroot) {
		super(htmlroot, xmlroot, null, null);
		this.xmlLocation = xmlroot;
		this.htmlLocation = htmlroot;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseReportConverter#convert(java.lang.String, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void convert(String xslt, IProgressMonitor monitor) throws Exception {
		File htmlRoot = new File(this.htmlLocation);
		if (!htmlRoot.exists()) {
			if (!htmlRoot.mkdirs()) {
				throw new Exception(NLS.bind(SearchMessages.could_not_create_file, this.htmlLocation));
			}
		} else {
			htmlRoot.mkdirs();
		}
		File reportsRoot = getReportsRoot();
		if (!reportsRoot.exists() || !reportsRoot.isDirectory()) {
			throw new Exception(NLS.bind(SearchMessages.invalid_directory_name, this.xmlLocation));
		}

		long start = 0;
		if (ApiPlugin.DEBUG_USE_REPORT_CONVERTER) {
			start = System.currentTimeMillis();
		}
		writeNotSearchedPage(htmlRoot);

		if (ApiPlugin.DEBUG_USE_REPORT_CONVERTER) {
			System.out.println("done in: " + (System.currentTimeMillis() - start) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println("Parsing use scan..."); //$NON-NLS-1$
			start = System.currentTimeMillis();
		}
		List result = parse();
		if (ApiPlugin.DEBUG_USE_REPORT_CONVERTER) {
			System.out.println("done in: " + (System.currentTimeMillis() - start) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println("Sorting reports and writing index..."); //$NON-NLS-1$
			start = System.currentTimeMillis();
		}
		writeIndexPage(result);
	}
	
	/**
	 * Writes the main index file for the reports
	 * @param result a list of {@link Report} objects returns from the use scan parser
	 */
	void writeIndexPage(List result) throws Exception {
		Collections.sort(result, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((Report) o1).name.compareTo(((Report) o2).name);
			}
		});

		PrintWriter writer = null;
		try {
			File reportIndex = new File(getHtmlLocation(), "index.html"); //$NON-NLS-1$
			if (!reportIndex.exists()) {
				reportIndex.createNewFile();
			}
//			setReportIndex(reportIndex);

			StringBuffer buffer = new StringBuffer();
			buffer.append(HTML_HEADER);
			buffer.append(OPEN_HTML).append(OPEN_HEAD).append(CONTENT_TYPE_META);
			writeMetadataHeaders(buffer);
			buffer.append(OPEN_TITLE).append(getIndexTitle()).append(CLOSE_TITLE);
			buffer.append(CLOSE_HEAD);
			buffer.append(OPEN_BODY);
			buffer.append(OPEN_H3).append(getIndexTitle()).append(CLOSE_H3);

			writeMetadataSummary(buffer);

			buffer.append(OPEN_H4).append(SearchMessages.MissingRefReportConverter_AddlBundleInfo).append(CLOSE_H4);
//			if(hasMissing()) {
//				buffer.append(OPEN_P); 
//				buffer.append(NLS.bind(SearchMessages.UseReportConverter_missing_bundles_prevented_scan, 
//						new String[] {" <a href=\"./missing.html\">", "</a>"})); //$NON-NLS-1$ //$NON-NLS-2$
//				buffer.append(CLOSE_P); 
//			}
			buffer.append(OPEN_P);
			buffer.append(NLS.bind(SearchMessages.MissingRefReportConverter_NotSearched, new String[] {"<a href=\"./not_searched.html\">", "</a></p>\n"}));  //$NON-NLS-1$//$NON-NLS-2$
			if (result.size() > 0) {
				buffer.append(getProblemSummaryTable());
				if (result.size() > 0) {
					Report report = null;
					for (Iterator iter = result.iterator(); iter.hasNext();) {
						report = (Report) iter.next();
						if (report != null) {
							File refereehtml = new File(getReportsRoot(), report.name + File.separator + "index.html"); //$NON-NLS-1$
							String link = extractLinkFrom(getReportsRoot(), refereehtml.getAbsolutePath());
							buffer.append(getReferenceTableEntry(report, link));
						}
					}
					buffer.append(CLOSE_TABLE);
				}
			} else {
				buffer.append(getNoReportsInformation()); 
			}
			buffer.append(W3C_FOOTER);
			buffer.append(CLOSE_BODY).append(CLOSE_HTML);

			//write the file
			writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(reportIndex), IApiCoreConstants.UTF_8));
			writer.print(buffer.toString());
			writer.flush();
		} catch (IOException e) {
			throw new Exception(NLS.bind(SearchMessages.ioexception_writing_html_file, getReportIndex().getAbsolutePath()));
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseReportConverter#getNoReportsInformation()
	 */
	protected String getNoReportsInformation() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(OPEN_P).append(BR).append(SearchMessages.no_use_scan_ref_problems).append(CLOSE_P); 
		return buffer.toString();
	}
	
	/**
	 * Returns the HTML markup for one entry in the problem summary table.
	 * @param report
	 * @param link
	 * @return a single reference table entry
	 */
	private Object getReferenceTableEntry(Report report, String link) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(OPEN_TR);
		buffer.append("<td><b><a href=\"").append(link).append("\">").append(getBundleOnlyName(report.name)).append("</a>").append(CLOSE_B).append(CLOSE_TD); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		buffer.append("\t<td align=\"left\">").append(getVersion(report.name)).append(CLOSE_TD); //$NON-NLS-1$
		buffer.append("\t<td align=\"center\">").append(report.typeProblems).append(CLOSE_TD); //$NON-NLS-1$
		buffer.append("\t<td align=\"center\">").append(report.methodProblems).append(CLOSE_TD); //$NON-NLS-1$ 
		buffer.append("\t<td align=\"center\">").append(report.fieldProblems).append(CLOSE_TD); //$NON-NLS-1$ 
		buffer.append("\t<td align=\"center\">").append(report.getTotal()).append(CLOSE_TD); //$NON-NLS-1$ 
		buffer.append(CLOSE_TR);
		return buffer.toString();
	}

	private StringBuffer getProblemSummaryTable() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(OPEN_H4).append(SearchMessages.MissingRefReportConverter_ProblemSummaryTitle).append(CLOSE_H4);
		buffer.append(OPEN_P).append(SearchMessages.MissingRefReportConverter_ProblemSummary).append(CLOSE_P);
		buffer.append("<table border=\"1\" width=\"80%\">\n"); //$NON-NLS-1$
		buffer.append(OPEN_TR);
		buffer.append("\t<td bgcolor=\"").append(REFERENCES_TABLE_HEADER_COLOUR).append("\" width=\"25%\" title=\""); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append(SearchMessages.MissingRefReportConverter_ProblemTable_ColumnBundleTooltip).append("\"\">");  //$NON-NLS-1$
		buffer.append(OPEN_B).append(SearchMessages.MissingRefReportConverter_ProblemTable_ColumnBundle).append(CLOSE_B).append(CLOSE_TD);
		//version header
		buffer.append("\t<td bgcolor=\"").append(REFERENCES_TABLE_HEADER_COLOUR).append("\" align=\"center\" width=\"15%\" title=\""); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append(SearchMessages.MissingRefReportConverter_ProblemTable_ColumnVersionTooltip).append("\"\">");  //$NON-NLS-1$
		buffer.append(OPEN_B).append(SearchMessages.MissingRefReportConverter_ProblemTable_ColumnVersion).append(CLOSE_B).append(CLOSE_TD);
		//Missing Types
		buffer.append("\t<td bgcolor=\"").append(REFERENCES_TABLE_HEADER_COLOUR).append("\" align=\"center\" width=\"15%\" title=\""); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append(SearchMessages.MissingRefReportConverter_ProblemTable_ColumnMissingTypesTooltip).append("\">");  //$NON-NLS-1$
		buffer.append(OPEN_B).append(SearchMessages.MissingRefReportConverter_ProblemTable_ColumnMissingTypes).append(CLOSE_B).append(CLOSE_TD);
		//Missing Methods
		buffer.append("\t<td bgcolor=\"").append(REFERENCES_TABLE_HEADER_COLOUR).append("\" align=\"center\" width=\"15%\" title=\""); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append(SearchMessages.MissingRefReportConverter_ProblemTable_ColumnMissingMethodsTooltip).append("\">");  //$NON-NLS-1$
		buffer.append(OPEN_B).append(SearchMessages.MissingRefReportConverter_ProblemTable_ColumnMissingMethods).append(CLOSE_B).append(CLOSE_TD);
		//Missing Fields
		buffer.append("\t<td bgcolor=\"").append(REFERENCES_TABLE_HEADER_COLOUR).append("\" align=\"center\" width=\"15%\" title=\""); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append(SearchMessages.MissingRefReportConverter_ProblemTable_ColumnMissingFieldsTooltip).append("\">");  //$NON-NLS-1$
		buffer.append(OPEN_B).append(SearchMessages.MissingRefReportConverter_ProblemTable_ColumnMissingFields).append(CLOSE_B).append(CLOSE_TD);
		//Total
		buffer.append("\t<td bgcolor=\"").append(REFERENCES_TABLE_HEADER_COLOUR).append("\" align=\"center\" width=\"15%\" title=\""); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append(SearchMessages.MissingRefReportConverter_ProblemTable_ColumnTotalTooltip).append("\">");  //$NON-NLS-1$
		buffer.append(OPEN_B).append(SearchMessages.MissingRefReportConverter_ProblemTable_ColumnTotal).append(CLOSE_B).append(CLOSE_TD);
		return buffer;
	}

	void writeMetadataSummary(StringBuffer buffer) throws Exception {
		MissingRefMetadata metadata = (MissingRefMetadata) getMetadata();
		buffer.append(OPEN_H4).append(SearchMessages.MissingRefReportConverter_MetadataTitle).append(CLOSE_H4);
		buffer.append("<table border=\"0px\" title=\"").append(SearchMessages.MissingRefReportConverter_MetadataTableTitle).append("\"width=\"50%\">"); //$NON-NLS-1$ //$NON-NLS-2$ 

		buffer.append(OPEN_TR);
		buffer.append(openTD(14)).append(SearchMessages.MissingRefReportConverter_ReportDate).append(CLOSE_TD);
		buffer.append(openTD(36)).append(metadata.getRunAtDate()).append(CLOSE_TD);
		buffer.append(CLOSE_TR);

		buffer.append(OPEN_TR);
		buffer.append(openTD(14)).append(SearchMessages.MissingRefReportConverter_ProfileLocation).append(CLOSE_TD);
		String value = metadata.getProfile();
		buffer.append(openTD(36)).append((value != null ? value : SearchMessages.MissingRefReportConverter_NONE)).append(CLOSE_TD);
		buffer.append(CLOSE_TR);

		buffer.append(OPEN_TR);
		buffer.append(openTD(14)).append(SearchMessages.MissingRefReportConverter_ReportLocation).append(CLOSE_TD);
		value = metadata.getReportLocation();
		buffer.append(openTD(36)).append((value != null ? value : SearchMessages.MissingRefReportConverter_NONE)).append(CLOSE_TD);
		buffer.append(CLOSE_TR);

		buffer.append(OPEN_TR);
		buffer.append(openTD(14)).append(SearchMessages.MissingRefReportConverter_ApiUseScanLocations).append(CLOSE_TD);
		value = metadata.getApiUseScans();
		buffer.append(openTD(36)).append((value != null ? value : SearchMessages.MissingRefReportConverter_NONE)).append(CLOSE_TD);
		buffer.append(CLOSE_TR);

		buffer.append(CLOSE_TD);
		buffer.append(CLOSE_TR);
		buffer.append(CLOSE_TABLE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseReportConverter#getMetadata()
	 */
	IMetadata getMetadata() throws Exception {
		File xmlFile = new File(getReportsRoot(), "meta" + XML_EXTENSION); //$NON-NLS-1$
		if (!xmlFile.exists()) {
			//try looking in the default 'xml' directory as a raw report root
			//might have been specified
			xmlFile = new File(getReportsRoot() + File.separator + "xml", "meta" + XML_EXTENSION); //$NON-NLS-1$//$NON-NLS-2$
		}
		return MissingRefMetadata.getMetadata(xmlFile);

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseReportConverter#getIndexTitle()
	 */
	protected String getIndexTitle() {
		return SearchMessages.MissingRefReportConverter_ReportTitle;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseReportConverter#writeMetadataHeaders(java.lang.StringBuffer)
	 */
	protected void writeMetadataHeaders(StringBuffer buffer) {
		buffer.append("<meta name=\"").append("description").append("\" content=\"").append(SearchMessages.MissingRefReportConverter_IndexMetaTag).append("\">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
	}

	/**
	 * Parse the xml directories and report.xml and generate html for them
	 */
	protected List parse() throws Exception {
		MissingRefParser parser = new MissingRefParser();
		MissingRefVisitor visitor = new MissingRefVisitor();
		parser.parse(getXmlLocation(), visitor);
		return visitor.reports;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseReportConverter#getHtmlLocation()
	 */
	protected String getHtmlLocation() {
		return this.htmlLocation;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseReportConverter#getXmlLocation()
	 */
	protected String getXmlLocation() {
		return this.xmlLocation;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseReportConverter#getReportsRoot()
	 */
	protected File getReportsRoot() {
		if (this.reportsRoot == null) {
			this.reportsRoot = new File(getXmlLocation());
		}
		return this.reportsRoot;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseReportConverter#getReportIndex()
	 */
	public File getReportIndex() {
		//TODO remove if
		if (htmlIndex == null) {
			return new File(htmlLocation);
		}
		else {
			return htmlIndex;
		}
	}
}
