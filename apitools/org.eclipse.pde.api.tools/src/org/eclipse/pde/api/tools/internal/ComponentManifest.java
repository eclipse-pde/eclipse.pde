/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IRequiredComponentDescription;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/**
 * Reads headers from an API component's manifest file.
 * 
 * @since 1.0.0
 */
public class ComponentManifest {
	
	private String fId;
	private String fName;
	private String fVersion;
	private String fEnvironment;
	private IRequiredComponentDescription[] fRequired;
		
	/**
	 * Constructs a new component manifest.
	 */
	ComponentManifest() {
	}
	
	/**
	 * Constructs and returns new component manifest on the given input stream
	 * that represents a bundle manifest file ("MANIFEST.MF").
	 * 
	 * @param inputStream manifest file input stream
	 * @throws CoreException 
	 * @throws CoreException if unable to build the component from the file
	 */	
	public static ComponentManifest fromBundleManifest(InputStream stream) throws CoreException {
		ComponentManifest manifest = new ComponentManifest();
		manifest.initFromBundleManifest(stream);
		return manifest;
	}

	/**
	 * Initializes this component manifest from the given manifest file.
	 * 
	 * @param manifest input stream to manifest file
	 * @throws CoreException if unable to read the file or missing required headers
	 */
	private void initFromBundleManifest(InputStream manifest) throws CoreException {
		try {
			Map headers = ManifestElement.parseBundleManifest(manifest, null);
			fId = getValue(Constants.BUNDLE_SYMBOLICNAME, headers);
			fName = getValue(Constants.BUNDLE_NAME, headers);
			fVersion = getValue(Constants.BUNDLE_VERSION, headers);
			fEnvironment = getValue(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, headers);
			
			String value = (String) headers.get(Constants.REQUIRE_BUNDLE);
			if (value == null) {
				fRequired = new IRequiredComponentDescription[0];
			} else {
				ManifestElement[] elements = ManifestElement.parseHeader(Constants.REQUIRE_BUNDLE, value);
				fRequired = new IRequiredComponentDescription[elements.length];
				for (int i = 0; i < elements.length; i++) {
					ManifestElement element = elements[i];
					fRequired[i] = new RequiredComponentDescription(
							element.getValue(),
							new BundleVersionRange(element.getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE)),
							isOptional(element));
				}
			}

		} catch (IOException e) {
			abort("Invalid API component manifest", e); //$NON-NLS-1$
		} catch (BundleException e) {
			abort("Invalid API component manifest", e); //$NON-NLS-1$
		}
	}

	private boolean isOptional(ManifestElement element) {
		return Constants.RESOLUTION_OPTIONAL.equals(element.getDirective(Constants.RESOLUTION_DIRECTIVE))
		|| "true".equals(element.getAttribute(Constants.RESOLUTION_OPTIONAL)); //$NON-NLS-1$
	}

	/**
	 * Throws a core exception with the given message and underlying exception,
	 * if any.
	 * 
	 * @param message error message
	 * @param e underlying exception or <code>null</code>
	 * @throws CoreException
	 */
	private void abort(String message, Throwable e) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID, message, e));
	}
	
	/**
	 * Returns a manifest element value for the specified header in the given map
	 * of raw header values.
	 * 
	 * @param header header name
	 * @param headers map of header values
	 * @return manifest element
	 * @exception BundleException, CoreException if missing or invalid
	 */
	private String getValue(String header, Map headers) throws BundleException, CoreException {
		String value = (String) headers.get(header);
		if (value != null) {
			ManifestElement[] elements = ManifestElement.parseHeader(header, value);
			if (elements == null) {
				abort("Manifest missing required header: " + header, null); //$NON-NLS-1$
			} else if (elements.length == 1) {
				return elements[0].getValue();
			} else {
				abort("Manifest has invalid header: " + header, null); //$NON-NLS-1$
			}
		}
		return null;
	}
	
	/**
	 * Returns a human readable name for this component.
	 * 
	 * @return component name
	 */
	public String getName() {
		return fName;
	}
	
	/**
	 * Returns this component's unique identifier.
	 * 
	 * @return unique component identifier
	 */
	public String getId() {
		return fId;
	}
	
	/**
	 * Returns this component's version identifier.
	 * 
	 * @return component version
	 */
	public String getVersion() {
		return fVersion;
	}
	
	/**
	 * Returns the execution environment required by this component for building
	 * and running. An execution environment is represented by a unique identifier
	 * as defined by OSGi - for example "J2SE-1.4" or "CDC-1.0/Foundation-1.0".
	 * 
	 * @return execution environment identifier
	 */
	public String getExecutionEnvironment() {
		return fEnvironment;
	}
	
	/**
	 * Returns a collection of descriptions of components required by this
	 * component, or an empty collection if none.
	 * 
	 * @return required component descriptions, possibly empty
	 */
	public IRequiredComponentDescription[] getRequiredComponents() {
		return fRequired;
	}
}
