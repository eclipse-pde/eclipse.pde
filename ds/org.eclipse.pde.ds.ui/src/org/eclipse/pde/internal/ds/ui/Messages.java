/*******************************************************************************
 * Copyright (c) 2008, 2013 Code 9 Corporation and others.
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
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira Nobrega <rafael.oliveira@gmail.com> - bug 223738
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	public static String DSComponentDetails_nameEntry;
	public static String DSComponentDetails_activateEntry;
	public static String DSComponentDetails_activateTooltip;
	public static String DSComponentDetails_deactivateEntry;
	public static String DSComponentDetails_deactivateTooltip;
	public static String DSComponentDetails_modifiedEntry;
	public static String DSComponentDetails_modifiedTooltip;
	public static String DSComponentDetails_factoryEntry;
	public static String DSComponentDetails_configurationPolicy;

	public static String DSCreationOperation_title;

	public static String DSFileWizardPage_description;
	public static String DSFileWizardPage_title;
	public static String DSFileWizardPage_group;
	public static String DSFileWizardPage_component_name;
	public static String DSFileWizardPage_implementation_class;
	public static String DSFileWizardPage_browse;
	public static String DSFileWizardPage_ComponentNeedsClass;
	public static String DSFileWizardPage_ComponentNeedsFileName;
	public static String DSFileWizardPage_ComponentNeedsName;
	public static String DSFileWizardPage_ExampleComponentName;
	public static String DSFileWizardPage_selectType;

	public static String DSNewWizard_title;

	public static String DSImplementationDetails_classEntry;
	public static String DSImplementationDetails_browse;

	public static String DSPropertiesDetails_entry;
	public static String DSPropertiesDetails_browse;
	public static String DSPropertiesDetails_dialogTitle;
	public static String DSPropertiesDetails_dialogMessage;

	public static String DSPropertyDetails_nameEntry;
	public static String DSPropertyDetails_typeEntry;

	public static String DSProvideDetails_interface;
	public static String DSProvideDetails_browse;
	public static String DSProvideDetails_selectType;

	public static String DSSourcePage_partName;

	public static String DSReferenceDetails_bindEntry;
	public static String DSReferenceDetails_unbindEntry;
	public static String DSReferenceDetails_policeLabel;
	public static String DSReferenceDetails_nameEntry;
	public static String DSReferenceDetails_interfaceEntry;
	public static String DSReferenceDetails_cardinalityLabel;
	public static String DSReferenceDetails_targetEntry;
	public static String DSReferenceDetails_browse;
	public static String DSReferenceDetails_selectType;


	public static String DSSimpPage_title;

	public static String DSServicesPage_title;

	public static String DSSection_title;
	public static String DSSection_description;

	public static String DSOptionsSection_title;
	public static String DSOptionsSection_description;

	public static String DSReferenceSection_title;
	public static String DSReferenceSection_description;
	public static String DSReferenceSection_add;
	public static String DSReferenceSection_remove;
	public static String DSReferenceSection_edit;
	public static String DSReferenceSection_up;
	public static String DSReferenceSection_down;

	public static String DSProvideSection_title;
	public static String DSProvideSection_description;
	public static String DSProvideSection_add;
	public static String DSProvideSection_remove;
	public static String DSProvideSection_edit;

	public static String DSEditReferenceDialog_dialog_title;

	public static String DSEditProvideDialog_dialog_title;

	public static String DSPropertiesSection_title;
	public static String DSPropertiesSection_description;
	public static String DSPropertiesSection_addProperties;
	public static String DSPropertiesSection_addProperty;
	public static String DSPropertiesSection_remove;
	public static String DSPropertiesSection_edit;
	public static String DSPropertiesSection_up;
	public static String DSPropertiesSection_down;

	public static String DSEditPropertiesDialog_dialog_title;

	public static String DSEditPropertyDialog_dialog_title;
	public static String DSEditPropertyDialog_dialogMessage;
	public static String DSEditPropertyDialog_valuesLabel;

	public static String DSServiceComponentSection_immediateButtonMessage;
	public static String DSServiceComponentSection_enabledButtonMessage;

	public static String DSService_title;




	private static final String BUNDLE_NAME = "org.eclipse.pde.internal.ds.ui.messages"; //$NON-NLS-1$

	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

}
