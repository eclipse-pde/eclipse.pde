/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core.osgi.bundle;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.IWritable;
import org.eclipse.pde.core.plugin.IPluginBase;
/**
 * A model object that represents the content of the fragment.xml file.
 * <p>
 * <b>Note:</b> This interface is part of an interim API that is still under
 * development and expected to change significantly before reaching stability.
 * It is being made available at this early stage to solicit feedback from
 * pioneering adopters on the understanding that any code that uses this API
 * will almost certainly be broken (repeatedly) as the API evolves.
 * </p>
 */
public interface IBundle extends IWritable {
	// Standard OSGi header keys
	String KEY_ACTIVATOR = "Bundle-Activator";
	String KEY_CATEGORY = "Bundle-Category";
	String KEY_CLASSPATH = "Bundle-ClassPath";
	String KEY_CONTACT_ADDRESS = "Bundle-ContactAddress";
	String KEY_COPYRIGHT = "Bundle-Copyright";
	String KEY_DESC = "Bundle-Description";
	String KEY_DOC_URL = "Bundle-DocURL";
	String KEY_NAME = "Bundle-Name";
	String KEY_NATIVE_CODE = "Bundle-NativeCode";
	String KEY_EXECUTION_ENV = "Bundle-RequiredExecutionEnvironment";
	String KEY_UPDATE_LOCATION = "Bundle-UpdateLocation";
	String KEY_VENDOR = "Bundle-Vendor";
	String KEY_VERSION = "Bundle-Version";
	String KEY_DYNAMIC_IMPORT_PACKAGE = "DynamicImport-Package";
	String KEY_EXPORT_PACKAGE = "Export-Package";
	String KEY_IMPORT_PACKAGE = "Import-Package";
	
	// Equinox addenda
	String PERFECT_MATCH = "perfect";
	String EQUIVALENT_MATCH = "equivalent";
	String COMPATIBLE_MATCH = "compatible";
	String GREATERTHANOREQUAL_MATCH = "greaterthan-or-equal";

	String KEY_SYMBOLIC_NAME = "Bundle-SymbolicName";
	String KEY_PROVIDE_PACKAGE = "Provide-Package";
	String KEY_REQUIRE_BUNDLE = "Require-Bundle";
	String KEY_FRAGMENT_HOST = "Fragment-Host";

	String KEY_LEGACY = "Legacy"; //$NON-NLS-1$
	
	String COMPATIBILITY_ACTIVATOR = "org.eclipse.core.runtime.compatibility.PluginActivator";

	String[] VALID_KEYS =
		{
			KEY_ACTIVATOR,
			KEY_CATEGORY,
			KEY_CLASSPATH,
			KEY_CONTACT_ADDRESS,
			KEY_COPYRIGHT,
			KEY_DESC,
			KEY_DOC_URL,
			KEY_NAME,
			KEY_NATIVE_CODE,
			KEY_EXECUTION_ENV,
			KEY_UPDATE_LOCATION,
			KEY_VENDOR,
			KEY_VERSION,
			KEY_DYNAMIC_IMPORT_PACKAGE,
			KEY_EXPORT_PACKAGE,
			KEY_IMPORT_PACKAGE,
			KEY_PROVIDE_PACKAGE,
			KEY_REQUIRE_BUNDLE,
			KEY_FRAGMENT_HOST,
			KEY_SYMBOLIC_NAME,
			KEY_LEGACY
			};

	String[] COMMA_SEPARATED_KEYS =
		{
			KEY_CATEGORY,
			KEY_CLASSPATH,
			KEY_NATIVE_CODE,
			KEY_EXECUTION_ENV,
			KEY_DYNAMIC_IMPORT_PACKAGE,
			KEY_EXPORT_PACKAGE,
			KEY_IMPORT_PACKAGE,
			KEY_PROVIDE_PACKAGE,
			KEY_REQUIRE_BUNDLE };

	String getHeader(String headerKey);
	void setHeader(String headerKey, String headerValue) throws CoreException;
	// load the bundle from the plug-in (migrate)
	void load(IPluginBase plugin, IProgressMonitor monitor);
	boolean isValid();
}