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
package org.eclipse.pde.core.bundle;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IWritable;
import org.eclipse.pde.core.plugin.IPluginBase;
/**
 * A model object that represents the content of the fragment.xml
 * file.
 * <p>
 * <b>Note:</b> This interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 */
public interface IBundle extends IWritable {
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
	
	String [] VALID_KEYS = {
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
		KEY_IMPORT_PACKAGE
	};
	
	String [] COMMA_SEPARATED_KEYS = { 
			KEY_CATEGORY,
			KEY_CLASSPATH,
					KEY_NATIVE_CODE,
					KEY_EXECUTION_ENV,
					KEY_DYNAMIC_IMPORT_PACKAGE,
					KEY_EXPORT_PACKAGE,
					KEY_IMPORT_PACKAGE };
	
	String getHeader(String headerKey);
	void setHeader(String headerKey, String headerValue) throws CoreException;
	// load the bundle from the plug-in (migrate)
	void load(IPluginBase plugin, IProgressMonitor monitor);
}