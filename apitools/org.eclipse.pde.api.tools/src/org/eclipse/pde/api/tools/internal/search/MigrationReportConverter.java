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

import com.ibm.icu.text.MessageFormat;


/**
 * 
 */
public class MigrationReportConverter extends UseReportConverter {

	/**
	 * Constructs a new HTML report generator.
	 * 
	 * @param htmlroot
	 * @param xmlroot
	 */
	public MigrationReportConverter(String htmlroot, String xmlroot) {
		super(htmlroot, xmlroot);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseReportConverter#getIndexPageHeader(boolean)
	 */
	protected String getIndexPageHeader(boolean results) {
		if(results) {
			return SearchMessages.MigrationReportConverter_search_html_index_file_header;
		} else {
			StringBuffer buf = new StringBuffer(SearchMessages.ApiUseReportConverter_no_usage_header);
			buf.append('\n');
			buf.append(SearchMessages.ApiUseReportConverter_no_bundle_have_usage);
			return buf.toString();
		}		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseReportConverter#getRefereeReportHeader(org.eclipse.pde.api.tools.internal.search.UseReportConverter.Report)
	 */
	protected String getRefereeReportHeader(Report report) {
		return MessageFormat.format(SearchMessages.MigrationReportConverter_referee_index_header, new String[] {report.referee.getName(), report.alternate});		
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseReportConverter#getOriginEntryHeader(org.eclipse.pde.api.tools.internal.search.UseReportConverter.Report, java.io.File)
	 */
	protected String getOriginEntryHeader(Report report, File origin) {
		return MessageFormat.format(SearchMessages.MigrationReportConverter_origin_html_header, new String[] {origin.getName(), report.referee.getName(), report.alternate});
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseReportConverter#getOriginSummary(org.eclipse.pde.api.tools.internal.search.UseReportConverter.Report, java.io.File, org.eclipse.pde.api.tools.internal.search.UseReportConverter.CountGroup)
	 */
	protected String getOriginSummary(Report report, File origin, CountGroup counts) {
		return MessageFormat.format(SearchMessages.MigrationReportConverter_origin_summary_header,  
				new String[] {origin.getName(), Integer.toString(counts.getTotalRefCount()), report.referee.getName(), report.alternate});
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseReportConverter#getNotSearchedXSLPath()
	 */
	protected String getNotSearchedXSLPath() {
		return "/notanalyzed.xsl"; //$NON-NLS-1$
	}

}
