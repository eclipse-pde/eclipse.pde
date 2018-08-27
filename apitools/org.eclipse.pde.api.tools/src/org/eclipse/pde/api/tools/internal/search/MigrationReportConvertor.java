/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
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
	 *
	 * @param htmlroot
	 * @param xmlroot
	 * @param topatterns
	 * @param frompatterns
	 */
	public MigrationReportConvertor(String htmlroot, String xmlroot, String[] topatterns, String[] frompatterns) {
		super(htmlroot, xmlroot, topatterns, frompatterns);
	}

	@Override
	protected String getIndexTitle() {
		return SearchMessages.MigrationReportConvertor_bundle_migration_information;
	}

	protected String getIndexHeader() {
		return getIndexTitle();
	}

	@Override
	protected String getReferencedTypeTitle(String bundle) {
		return NLS.bind(SearchMessages.MigrationReportConvertor_type_with_unresolved_refs, bundle);
	}

	protected String getReferencedTypeHeader(String bundle) {
		return getReferencedTypeTitle(bundle);
	}

	@Override
	protected String getAdditionalReferencedTypeInformation() {
		return SearchMessages.MigrationReportConvertor_table_shows_unresolved;
	}

	@Override
	protected String getTypeTitle(String typename) {
		return NLS.bind(SearchMessages.MigrationReportConvertor_type_migration_information, Signature.getSimpleName(typename));
	}

	protected String getTypeHeader(String typename) {
		return getTypeTitle(typename);
	}

	@Override
	protected String getTypeDetailsHeader() {
		return SearchMessages.MigrationReportConvertor_migration_details;
	}

	@Override
	protected String getTypeDetails() {
		return SearchMessages.MigrationReportConvertor_click_table_entry;
	}

	@Override
	protected String getTypeCountSummary(String typename, CountGroup counts, int membercount) {
		StringBuilder buffer = new StringBuilder();
		buffer.append(OPEN_H4).append(SearchMessages.UseReportConverter_summary).append(CLOSE_H4);
		buffer.append(OPEN_P).append(NLS.bind(SearchMessages.MigrationReportConvertor_member_has_unresolved_refs, new String[] {
				typename, Integer.toString(counts.getTotalRefCount()),
				Integer.toString(membercount) })).append(CLOSE_P);
		return buffer.toString();
	}

	@Override
	protected String getAdditionalIndexInfo(boolean hasreports) {
		if (hasreports) {
			StringBuilder buffer = new StringBuilder();
			buffer.append(SearchMessages.MigrationReportConvertor_bundles_have_references);
			return buffer.toString();
		}
		return null;
	}

	@Override
	protected String getNoReportsInformation() {
		StringBuilder buffer = new StringBuilder();
		buffer.append(OPEN_P).append(BR).append(SearchMessages.MigrationReportConvertor_no_reported_migration_problems).append(CLOSE_P);
		return buffer.toString();
	}
}
