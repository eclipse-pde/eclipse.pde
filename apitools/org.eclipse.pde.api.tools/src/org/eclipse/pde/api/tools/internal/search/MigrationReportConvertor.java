/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.search;

import org.eclipse.jdt.core.Signature;
import org.eclipse.osgi.util.NLS;

/**
 * Report converter specialization for migration reports
 * 
 * @since 1.0.1
 */
public class MigrationReportConvertor extends UseReportConverter {

	/**
	 * Constructor
	 * @param htmlroot
	 * @param xmlroot
	 * @param topatterns
	 * @param frompatterns
	 */
	public MigrationReportConvertor(String htmlroot, String xmlroot, String[] topatterns, String[] frompatterns) {
		super(htmlroot, xmlroot, topatterns, frompatterns);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseReportConverter#getIndexTitle()
	 */
	protected String getIndexTitle() {
		return SearchMessages.MigrationReportConvertor_bundle_migration_information;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseReportConverter#getIndexHeader()
	 */
	protected String getIndexHeader() {
		return getIndexTitle();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseReportConverter#getReferencedTypeTitle(java.lang.String)
	 */
	protected String getReferencedTypeTitle(String bundle) {
		return NLS.bind(SearchMessages.MigrationReportConvertor_type_with_unresolved_refs, bundle);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseReportConverter#getReferencedTypeHeader(java.lang.String)
	 */
	protected String getReferencedTypeHeader(String bundle) {
		return getReferencedTypeTitle(bundle);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseReportConverter#getAdditionalReferencedTypeInformation()
	 */
	protected String getAdditionalReferencedTypeInformation() {
		return SearchMessages.MigrationReportConvertor_table_shows_unresolved;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseReportConverter#getTypeTitle(java.lang.String)
	 */
	protected String getTypeTitle(String typename) {
		return NLS.bind(SearchMessages.MigrationReportConvertor_type_migration_information, Signature.getSimpleName(typename));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseReportConverter#getTypeHeader(java.lang.String)
	 */
	protected String getTypeHeader(String typename) {
		return getTypeTitle(typename);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseReportConverter#getTypeDetailsHeader()
	 */
	protected String getTypeDetailsHeader() {
		return SearchMessages.MigrationReportConvertor_migration_details;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseReportConverter#getTypeDetails()
	 */
	protected String getTypeDetails() {
		return SearchMessages.MigrationReportConvertor_click_table_entry;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseReportConverter#getTypeCountSummary(java.lang.String, org.eclipse.pde.api.tools.internal.search.UseReportConverter.CountGroup, int)
	 */
	protected String getTypeCountSummary(String typename, CountGroup counts, int membercount) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(OPEN_H4).append(SearchMessages.UseReportConverter_summary).append(CLOSE_H4); 
		buffer.append(OPEN_P).append(NLS.bind(SearchMessages.MigrationReportConvertor_member_has_unresolved_refs, new String[] {typename, Integer.toString(counts.getTotalRefCount()), Integer.toString(membercount)})).append(CLOSE_P);  
		return buffer.toString();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseReportConverter#getAdditionalIndexInfo(boolean)
	 */
	protected String getAdditionalIndexInfo(boolean hasreports) {
		if(hasreports) {
			StringBuffer buffer = new StringBuffer();
			buffer.append(SearchMessages.MigrationReportConvertor_bundles_have_references);
			return buffer.toString();
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseReportConverter#getNoReportsInformation()
	 */
	protected String getNoReportsInformation() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(OPEN_P).append(BR).append(SearchMessages.MigrationReportConvertor_no_reported_migration_problems).append(CLOSE_P); 
		return buffer.toString();
	}
}
