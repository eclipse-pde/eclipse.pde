/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira N�brega <rafael.oliveira@gmail.com> - bug 223738
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	public static String DSAddItemAction_addProperties;
	public static String DSAddItemAction_addProperty;
	public static String DSAddItemAction_addProvide;
	public static String DSAddItemAction_addService;
	public static String DSAddItemAction_addComponent;

	public static String DSComponentDetails_mainSectionTitle;
	public static String DSComponentDetails_mainSectionDescription;
	public static String DSComponentDetails_nameEntry;
	public static String DSComponentDetails_factoryEntry;
	public static String DSComponentDetails_enabledLabel;
	public static String DSComponentDetails_immediateLabel;
	public static String DSComponentDetails_browse;
	
	public static String DSCreationOperation_title;

	public static String DSFileWizardPage_description;
	public static String DSFileWizardPage_title;
	
	public static String DSImplementationDetails_title;
	public static String DSImplementationDetails_description;
	public static String DSImplementationDetails_classEntry;
	public static String DSImplementationDetails_browse;
	public static String DSImplementationDetails_selectType;

	public static String DSPage_pageId;
	public static String DSPage_errorTitle;
	public static String DSPage_errorMessage;
	public static String DSPage_formTitle;

	public static String DSPage_title;

	public static String DSPropertiesDetails_sectionTitle;
	public static String DSPropertiesDetails_sectionDescription;
	public static String DSPropertiesDetails_entry;
	public static String DSPropertiesDetails_browse;
	public static String DSPropertiesDetails_dialogTitle;
	public static String DSPropertiesDetails_dialogMessage;

	public static String DSPropertyDetails_mainSectionText;
	public static String DSPropertyDetails_mainSectionDescription;
	public static String DSPropertyDetails_nameEntry;
	public static String DSPropertyDetails_valueEntry;
	public static String DSPropertyDetails_typeEntry;
	public static String DSPropertyDetails_bodyLabel;

	public static String DSProvideDetails_mainSectionText;
	public static String DSProvideDetails_mainSectionDesc;
	public static String DSProvideDetails_interface;
	public static String DSProvideDetails_browse;
	public static String DSProvideDetails_selectType;

	public static String DSServiceDetails_sectionTitle;
	public static String DSServiceDetails_sectionDescription;
	public static String DSServiceDetails_serviceFactoryLabel;

	public static String DSSourcePage_partName;

	public static String DSMasterTreeSection_addService;
	public static String DSMasterTreeSection_addProperty;
	public static String DSMasterTreeSection_addProperties;
	public static String DSMasterTreeSection_addReference;
	public static String DSMasterTreeSection_addProvide;

	public static String DSMasterTreeSection_up;
	public static String DSMasterTreeSection_down;
	public static String DSMasterTreeSection_remove;
	public static String DSMasterTreeSection_client_text;
	public static String DSMasterTreeSection_client_description;

	public static String DSReferenceDetails_title;
	public static String DSReferenceDetails_description;
	public static String DSReferenceDetails_bindEntry;
	public static String DSReferenceDetails_unbindEntry;
	public static String DSReferenceDetails_policeLabel;
	public static String DSReferenceDetails_nameEntry;
	public static String DSReferenceDetails_interfaceEntry;
	public static String DSReferenceDetails_cardinalityLabel;
	public static String DSReferenceDetails_targetEntry;
	public static String DSReferenceDetails_browse;
	public static String DSReferenceDetails_selectType;

	public static String DSRemoveItemAction_actionText;
	
	
	

	private static final String BUNDLE_NAME = "org.eclipse.pde.internal.ds.ui.messages"; //$NON-NLS-1$

	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

}
