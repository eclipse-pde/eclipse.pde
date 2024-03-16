/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.problems.ApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IComponentDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.search.IMetadata;
import org.osgi.framework.Version;

public class MissingRefReportConverter extends UseReportConverter {

	class MissingRefVisitor {
		public List<Report> reports;

		public void visitScan() {
			reports = new ArrayList<>();
		}

		public boolean visitComponent(IComponentDescriptor targetComponent) {
			currentreport = new Report();
			currentreport.name = composeName(targetComponent.getId(), targetComponent.getVersion());
			reports.add(currentreport);
			return true;
		}

		/**
		 * Builds the name for the component
		 *
		 * @param id id of the component
		 * @param version version of the component, can be <code>null</code>
		 * @return string name
		 */
		protected String composeName(String id, String version) {
			String versionName = version;
			if (version == null) {
				versionName = Version.emptyVersion.toString();
			}
			StringBuilder buffer = new StringBuilder(3 + id.length() + versionName.length());
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
				StringBuilder buffer = new StringBuilder();
				buffer.append(HTML_HEADER);
				buffer.append(OPEN_HTML).append(OPEN_HEAD).append(CONTENT_TYPE_META);
				buffer.append(REF_STYLE);
				buffer.append(REF_SCRIPT);
				buffer.append(OPEN_TITLE).append(getProblemTitle(report.name)).append(CLOSE_TITLE);
				buffer.append(CLOSE_HEAD);
				buffer.append(OPEN_BODY);
				buffer.append(OPEN_H3).append(getProblemTitle(report.name)).append(CLOSE_H3);
				buffer.append(getProblemSummary(report));

				StringBuilder typeProblems = new StringBuilder();
				StringBuilder methodProblems = new StringBuilder();
				StringBuilder fieldProblems = new StringBuilder();
				report.apiProblems.forEach((key, types) -> {
					switch (key.intValue())
						{
						case IApiProblem.API_USE_SCAN_TYPE_PROBLEM -> typeProblems.append(getProblemTable(types));
						case IApiProblem.API_USE_SCAN_METHOD_PROBLEM -> methodProblems.append(getProblemTable(types));
						case IApiProblem.API_USE_SCAN_FIELD_PROBLEM -> fieldProblems.append(getProblemTable(types));
						default -> { /**/ }
						}
				});
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

				try (PrintWriter writer = new PrintWriter(
						new OutputStreamWriter(new FileOutputStream(originhtml), StandardCharsets.UTF_8))) {
					writer.println(buffer.toString());
					writer.flush();
				}
			} catch (IOException ioe) {
				throw new Exception(NLS.bind(SearchMessages.ioexception_writing_html_file, originhtml.getAbsolutePath()));
			}
		}

		private StringBuilder getProblemRow(StringBuilder type, String header) {
			StringBuilder buffer = new StringBuilder();
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

		private StringBuilder getProblemTable(TreeMap<String, List<IApiProblem>> types) {
			StringBuilder buffer = new StringBuilder();
			buffer.append("<table width=\"100%\" border=\"0\" cellspacing=\"1\" cellpadding=\"6\">\n"); //$NON-NLS-1$
			String tname = null;
			List<IApiProblem> pbs = null;
			for (Entry<String, List<IApiProblem>> entry : types.entrySet()) {
				tname = entry.getKey();
				pbs = entry.getValue();
				buffer.append("<tr align=\"left\"> \n"); //$NON-NLS-1$
				buffer.append("<td colspan=\"1\" bgcolor=\"#CCCCCC\">").append(OPEN_B).append(tname).append(CLOSE_B).append(CLOSE_TD); //$NON-NLS-1$
				buffer.append(CLOSE_TR);
				Collections.sort(pbs, missingcompare);
				for (IApiProblem pb : pbs) {
					buffer.append(OPEN_TR);
					buffer.append("<td align=\"left\" width=\"75%\">").append(pb.getMessage()).append(CLOSE_TD); //$NON-NLS-1$
					buffer.append(CLOSE_TR);
				}
			}
			buffer.append(CLOSE_TABLE);
			return buffer;
		}

		private Object getProblemSummary(Report report) {
			StringBuilder buffer = new StringBuilder();
			buffer.append(OPEN_H4).append(SearchMessages.MissingRefReportConverter_Summary).append(CLOSE_H4);
			buffer.append(OPEN_P).append(NLS.bind(SearchMessages.MissingRefReportConverter_SummaryDesc, new String[] {
					report.name, Integer.toString(report.apiProblems.size()) })).append(CLOSE_P);
			return buffer.toString();
		}

		/**
		 * Returns the HTML markup for the problems table header.
		 *
		 * @return the default references table header
		 */
		String getProblemsTableHeader(String sectionname, String type) {
			StringBuilder buffer = new StringBuilder();
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
		 * @return the page title
		 */
		protected String getProblemTitle(String bundle) {
			return NLS.bind(SearchMessages.MissingRefReportConverter_ProblemTitle, bundle);
		}

		public void addToCurrentReport(List<IApiProblem> apiProblems) {
			currentreport.add(apiProblems);
		}
	}

	private String xmlLocation = null;
	private String htmlLocation = null;
	private File reportsRoot = null;
	private final File htmlIndex = null;
	Report currentreport = null;

	static final Comparator<Object> missingcompare = (o1, o2) -> {
		if (o1 instanceof String && o2 instanceof String) {
			return ((String) o1).compareTo((String) o2);
		}
		if (o1 instanceof ApiProblem && o2 instanceof ApiProblem) {
			return ((ApiProblem) o1).getMessage().compareTo(((ApiProblem) o2).getMessage());
		}
		return 0;
	};

	/**
	 * Root item describing the use of one component
	 */
	static class Report {
		String name = null;
		TreeMap<Integer, TreeMap<String, List<IApiProblem>>> apiProblems = new TreeMap<>();
		int typeProblems = 0;
		int methodProblems = 0;
		int fieldProblems = 0;

		public void add(List<IApiProblem> apipbs) {
			for (IApiProblem pb : apipbs) {
				Integer key = pb.getKind();
				TreeMap<String, List<IApiProblem>> types = this.apiProblems.get(key);
				if (types == null) {
					types = new TreeMap<>(missingcompare);
					this.apiProblems.put(key, types);
				}
				String tname = pb.getTypeName();
				List<IApiProblem> list = types.get(tname);
				if (list == null) {
					list = new ArrayList<>();
					types.put(tname, list);
				}
				list.add(pb);
				switch (pb.getKind()) {
					case 1 -> ++typeProblems;
					case 2 -> ++methodProblems;
					case 3 -> ++fieldProblems;
					default -> { /**/ }
				}
			}
		}

		public int getTotal() {
			return typeProblems + methodProblems + fieldProblems;
		}
	}

	/**
	 * Constructor
	 *
	 * @param htmlroot the folder root where the HTML reports should be written
	 * @param xmlroot the folder root where the current API use scan output is
	 *            located
	 */
	public MissingRefReportConverter(String htmlroot, String xmlroot) {
		super(htmlroot, xmlroot, null, null);
		this.xmlLocation = xmlroot;
		this.htmlLocation = htmlroot;
	}

	@Override
	public void convert(String xslt, IProgressMonitor monitor) throws Exception {
		File htmlRoot = new File(this.htmlLocation);
		if (!htmlRoot.exists()) {
			if (!htmlRoot.mkdirs()) {
				throw new Exception(NLS.bind(SearchMessages.could_not_create_file, this.htmlLocation));
			}
		} else {
			htmlRoot.mkdirs();
		}
		File lreportsRoot = getReportsRoot();
		if (!lreportsRoot.exists() || !lreportsRoot.isDirectory()) {
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
		List<Report> result = parse();
		if (ApiPlugin.DEBUG_USE_REPORT_CONVERTER) {
			System.out.println("done in: " + (System.currentTimeMillis() - start) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println("Sorting reports and writing index..."); //$NON-NLS-1$
			start = System.currentTimeMillis();
		}
		writeIndexPage(result);
	}

	/**
	 * Writes the main index file for the reports
	 *
	 * @param result a list of {@link Report} objects returns from the use scan
	 *            parser
	 */
	@Override
	protected void writeIndexPage(List<?> result) throws Exception {
		Collections.sort(result, (o1, o2) -> ((Report) o1).name.compareTo(((Report) o2).name));

		try {
			File reportIndex = new File(getHtmlLocation(), "index.html"); //$NON-NLS-1$
			if (!reportIndex.exists()) {
				reportIndex.createNewFile();
			}
			// setReportIndex(reportIndex);

			StringBuilder buffer = new StringBuilder();
			buffer.append(HTML_HEADER);
			buffer.append(OPEN_HTML).append(OPEN_HEAD).append(CONTENT_TYPE_META);
			writeMetadataHeaders(buffer);
			buffer.append(OPEN_TITLE).append(getIndexTitle()).append(CLOSE_TITLE);
			buffer.append(CLOSE_HEAD);
			buffer.append(OPEN_BODY);
			buffer.append(OPEN_H3).append(getIndexTitle()).append(CLOSE_H3);

			writeMetadataSummary(buffer);

			getFilteredCount();
			writeFilterCount(buffer);

			buffer.append(OPEN_H4).append(SearchMessages.MissingRefReportConverter_AddlBundleInfo).append(CLOSE_H4);
			// if(hasMissing()) {
			// buffer.append(OPEN_P);
			// buffer.append(NLS.bind(SearchMessages.UseReportConverter_missing_bundles_prevented_scan,
			//						new String[] {" <a href=\"./missing.html\">", "</a>"})); //$NON-NLS-1$ //$NON-NLS-2$
			// buffer.append(CLOSE_P);
			// }
			buffer.append(OPEN_P);
			buffer.append(NLS.bind(SearchMessages.MissingRefReportConverter_NotSearched, new String[] {
					"<a href=\"./not_searched.html\">", "</a></p>\n" })); //$NON-NLS-1$//$NON-NLS-2$
			if (!result.isEmpty()) {
				buffer.append(getProblemSummaryTable());
				if (!result.isEmpty()) {
					for (Object obj : result) {
						if (obj instanceof Report report) {
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

			// write the file
			try (PrintWriter writer = new PrintWriter(
					new OutputStreamWriter(new FileOutputStream(reportIndex), StandardCharsets.UTF_8))) {
				writer.print(buffer.toString());
				writer.flush();
			}
		} catch (IOException e) {
			throw new Exception(NLS.bind(SearchMessages.ioexception_writing_html_file, getReportIndex().getAbsolutePath()));
		}
	}

	@Override
	protected String getNoReportsInformation() {
		StringBuilder buffer = new StringBuilder();
		buffer.append(OPEN_P).append(BR).append(SearchMessages.no_use_scan_ref_problems).append(CLOSE_P);
		return buffer.toString();
	}

	/**
	 * Returns the HTML markup for one entry in the problem summary table.
	 *
	 * @return a single reference table entry
	 */
	private Object getReferenceTableEntry(Report report, String link) {
		StringBuilder buffer = new StringBuilder();
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

	private StringBuilder getProblemSummaryTable() {
		StringBuilder buffer = new StringBuilder();
		buffer.append(OPEN_H4).append(SearchMessages.MissingRefReportConverter_ProblemSummaryTitle).append(CLOSE_H4);
		buffer.append(OPEN_P).append(SearchMessages.MissingRefReportConverter_ProblemSummary).append(CLOSE_P);
		buffer.append("<table border=\"1\" width=\"80%\">\n"); //$NON-NLS-1$
		buffer.append(OPEN_TR);
		buffer.append("\t<td bgcolor=\"").append(REFERENCES_TABLE_HEADER_COLOUR).append("\" width=\"25%\" title=\""); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append(SearchMessages.MissingRefReportConverter_ProblemTable_ColumnBundleTooltip).append("\"\">"); //$NON-NLS-1$
		buffer.append(OPEN_B).append(SearchMessages.MissingRefReportConverter_ProblemTable_ColumnBundle).append(CLOSE_B).append(CLOSE_TD);
		// version header
		buffer.append("\t<td bgcolor=\"").append(REFERENCES_TABLE_HEADER_COLOUR).append("\" align=\"center\" width=\"15%\" title=\""); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append(SearchMessages.MissingRefReportConverter_ProblemTable_ColumnVersionTooltip).append("\"\">"); //$NON-NLS-1$
		buffer.append(OPEN_B).append(SearchMessages.MissingRefReportConverter_ProblemTable_ColumnVersion).append(CLOSE_B).append(CLOSE_TD);
		// Missing Types
		buffer.append("\t<td bgcolor=\"").append(REFERENCES_TABLE_HEADER_COLOUR).append("\" align=\"center\" width=\"15%\" title=\""); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append(SearchMessages.MissingRefReportConverter_ProblemTable_ColumnMissingTypesTooltip).append("\">"); //$NON-NLS-1$
		buffer.append(OPEN_B).append(SearchMessages.MissingRefReportConverter_ProblemTable_ColumnMissingTypes).append(CLOSE_B).append(CLOSE_TD);
		// Missing Methods
		buffer.append("\t<td bgcolor=\"").append(REFERENCES_TABLE_HEADER_COLOUR).append("\" align=\"center\" width=\"15%\" title=\""); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append(SearchMessages.MissingRefReportConverter_ProblemTable_ColumnMissingMethodsTooltip).append("\">"); //$NON-NLS-1$
		buffer.append(OPEN_B).append(SearchMessages.MissingRefReportConverter_ProblemTable_ColumnMissingMethods).append(CLOSE_B).append(CLOSE_TD);
		// Missing Fields
		buffer.append("\t<td bgcolor=\"").append(REFERENCES_TABLE_HEADER_COLOUR).append("\" align=\"center\" width=\"15%\" title=\""); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append(SearchMessages.MissingRefReportConverter_ProblemTable_ColumnMissingFieldsTooltip).append("\">"); //$NON-NLS-1$
		buffer.append(OPEN_B).append(SearchMessages.MissingRefReportConverter_ProblemTable_ColumnMissingFields).append(CLOSE_B).append(CLOSE_TD);
		// Total
		buffer.append("\t<td bgcolor=\"").append(REFERENCES_TABLE_HEADER_COLOUR).append("\" align=\"center\" width=\"15%\" title=\""); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append(SearchMessages.MissingRefReportConverter_ProblemTable_ColumnTotalTooltip).append("\">"); //$NON-NLS-1$
		buffer.append(OPEN_B).append(SearchMessages.MissingRefReportConverter_ProblemTable_ColumnTotal).append(CLOSE_B).append(CLOSE_TD);
		return buffer;
	}

	@Override
	void writeMetadataSummary(StringBuilder buffer) throws Exception {
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

	@Override
	IMetadata getMetadata() throws Exception {
		File xmlFile = new File(getReportsRoot(), "meta" + XML_EXTENSION); //$NON-NLS-1$
		if (!xmlFile.exists()) {
			// try looking in the default 'xml' directory as a raw report root
			// might have been specified
			xmlFile = new File(getReportsRoot() + File.separator + "xml", "meta" + XML_EXTENSION); //$NON-NLS-1$//$NON-NLS-2$
		}
		return MissingRefMetadata.getMetadata(xmlFile);

	}

	@Override
	protected String getIndexTitle() {
		return SearchMessages.MissingRefReportConverter_ReportTitle;
	}

	@Override
	protected void writeMetadataHeaders(StringBuilder buffer) {
		buffer.append("<meta name=\"").append("description").append("\" content=\"").append(SearchMessages.MissingRefReportConverter_IndexMetaTag).append("\">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	/**
	 * Parse the XML directories and report.xml and generate HTML for them
	 */
	protected List<Report> parse() throws Exception {
		MissingRefParser lparser = new MissingRefParser();
		MissingRefVisitor visitor = new MissingRefVisitor();
		lparser.parse(getXmlLocation(), visitor);
		return visitor.reports;
	}

	@Override
	protected String getHtmlLocation() {
		return this.htmlLocation;
	}

	@Override
	protected String getXmlLocation() {
		return this.xmlLocation;
	}

	@Override
	protected File getReportsRoot() {
		if (this.reportsRoot == null) {
			this.reportsRoot = new File(getXmlLocation());
		}
		return this.reportsRoot;
	}

	@Override
	public File getReportIndex() {
		// TODO remove if
		if (htmlIndex == null) {
			return new File(htmlLocation);
		} else {
			return htmlIndex;
		}
	}
}
