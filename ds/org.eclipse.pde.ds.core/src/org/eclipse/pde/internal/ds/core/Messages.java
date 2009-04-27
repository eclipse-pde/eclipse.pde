/*******************************************************************************
 * Copyright (c) 2008, 2009 EclipseSource Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     EclipseSource Corporation - initial API and implementation
 *     Rafael Oliveira Nóbrega <rafael.oliveira@gmail.com> - bug 230232
 *******************************************************************************/

package org.eclipse.pde.internal.ds.core;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	private static final String BUNDLE_NAME = "org.eclipse.pde.internal.ds.core.messages"; //$NON-NLS-1$

	public static String DSErrorReporter_cannotFindJavaType;
	public static String DSErrorReporter_requiredElement;
	public static String DSErrorReporter_requiredAttribute;
	public static String DSErrorReporter_attrValue;
	public static String DSErrorReporter_emptyAttrValue;
	public static String DSErrorReporter_duplicateReferenceName;
	public static String DSErrorReporter_requiredDefaultConstructor;
	public static String DSErrorReporter_invalidTarget;
	public static String DSErrorReporter_unimplementedProvidedInterface;
	public static String DSErrorReporter_singleAndMultipleAttrValue;
	public static String DSErrorReporter_emptyPropertyValue;
	public static String DSErrorReporter_invalidImmediateValue;
	public static String DSErrorReporter_invalidConfigurationPolicyValue;
	public static String DSErrorReporter_invalidImmediateValueFactory;
	public static String DSErrorReporter_duplicatedInterface;
	public static String DSErrorReporter_cannotFindProperties;
	public static String DSErrorReporter_invalidCardinalityValue;
	public static String DSErrorReporter_invalidPolicyValue;
	public static String DSErrorReporter_propertyTypeCastException;
	public static String DSErrorReporter_illegalServiceFactory;
	public static String DSErrorReporter_illegalServiceFactory_Immediate;
	public static String DSErrorReporter_illegalEmptyService;
	
	public static String DSBuilder_verifying;
	public static String DSBuilder_updating;

	

	

	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

}
