/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.tasks;

import org.eclipse.osgi.util.NLS;

/**
 * All externalized messages found in the ant tasks.
 */
public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.pde.api.tools.internal.tasks.messages"; //$NON-NLS-1$

	public static String deltaReportTask_footer;
	public static String deltaReportTask_header;
	public static String deltaReportTask_entry;
	public static String deltaReportTask_componentEntry;
	public static String deltaReportTask_endComponentEntry;
	public static String deltaReportTask_entry_major_version;
	public static String deltaReportTask_entry_minor_version;
	public static String deltaReportTask_missingXmlFileLocation;
	public static String deltaReportTask_couldNotCreateSAXParser;

	public static String fullReportTask_bundlesheader;
	public static String fullReportTask_bundlesentry_even;
	public static String fullReportTask_bundlesentry_odd;
	public static String fullReportTask_bundlesfooter;

	public static String W3C_page_footer;
	public static String fullReportTask_apiproblemheader;
	public static String fullReportTask_apiproblemsummary;

	public static String fullReportTask_categoryheader;
	public static String fullReportTask_categoryfooter;
	public static String fullReportTask_problementry_even_error;
	public static String fullReportTask_problementry_even_warning;
	public static String fullReportTask_problementry_odd_error;
	public static String fullReportTask_problementry_odd_warning;
	public static String fullReportTask_categoryseparator;
	public static String fullReportTask_category_no_elements;
	
	public static String fullReportTask_indexheader;
	public static String fullReportTask_indexfooter;
	public static String fullReportTask_indexsummary_even;
	public static String fullReportTask_indexsummary_odd;
	
	public static String missing_xml_files_location;
	public static String invalid_directory_name;
	public static String could_not_create_sax_parser;
	public static String could_not_create_file;
	public static String fullReportTask_nonApiBundleSummary;
	public static String fullReportTask_apiBundleSummary;
	public static String ioexception_writing_html_file;
	public static String fullReportTask_compatibility_header;
	public static String fullReportTask_api_usage_header;
	public static String fullReportTask_bundle_version_header;

	public static String printArguments;
	public static String errorInComparison;
	public static String errorCreatingParentReportFile;
	public static String errorCreatingReportDirectory;
	public static String directoryIsEmpty;
	public static String fileDoesnotExist;
	public static String couldNotDelete;
	public static String couldNotCreate;
	public static String couldNotUnzip;
	public static String couldNotUntar;

	public static String api_generation_printArguments;
	public static String api_generation_printArguments2;
	public static String api_generation_projectLocationNotADirectory;
	public static String api_generation_targetFolderNotADirectory;
	public static String api_generation_invalidBinaryLocation;

	public static String ApiUseDBTask_connection_could_not_be_established;

	public static String ApiUseDBTask_driver_class_not_found;

	public static String ApiUseDBTask_driver_instantiation_exception;

	public static String ApiUseDBTask_illegal_access_loading_driver;

	public static String ApiUseDBTask_sql_connection_exception;

	public static String ApiUseReportConversionTask_api;

	public static String ApiUseReportConversionTask_back_to_bundle_index;

	public static String ApiUseReportConversionTask_bundle_list_header;

	public static String ApiUseReportConversionTask_coreexception_writing_html_file;

	public static String ApiUseReportConversionTask_field;

	public static String ApiUseReportConversionTask_internal;

	public static String ApiUseReportConversionTask_internal_permissable;

	public static String ApiUseReportConversionTask_method;

	public static String ApiUseReportConversionTask_no_bundles;

	public static String ApiUseReportConversionTask_no_xslt_found;

	public static String ApiUseReportConversionTask_not_searched_component_list;

	public static String ApiUseReportConversionTask_origin_html_header;

	public static String ApiUseReportConversionTask_origin_summary_count_link;

	public static String ApiUseReportConversionTask_origin_summary_header;

	public static String ApiUseReportConversionTask_origin_summary_table_entry;

	public static String ApiUseReportConversionTask_origin_summary_table_entry_bold;

	public static String ApiUseReportConversionTask_other;

	public static String ApiUseReportConversionTask_referee_index_entry;

	public static String ApiUseReportConversionTask_referee_index_header;

	public static String ApiUseReportConversionTask_search_html_index_file_header;

	public static String ApiUseReportConversionTask_table_end;

	public static String ApiUseReportConversionTask_that_were_not_searched;

	public static String ApiUseReportConversionTask_type;

	public static String ApiUseReportConversionTask_visibility;

	public static String ApiUseReportConversionTask_with_no_api_description;

	public static String ApiUseReportConversionTask_xslt_file_not_valid;

	public static String ApiUseTask_missing_arguments;

	public static String ApiUseTask_search_engine_problem;

	public static String comparison_invalidRegularExpression;

	public static String DatabaseTask_missing_db_connect_arguments;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
