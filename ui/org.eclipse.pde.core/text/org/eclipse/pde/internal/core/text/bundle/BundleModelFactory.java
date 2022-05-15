/*******************************************************************************
 *  Copyright (c) 2005, 2022 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text.bundle;

import org.eclipse.jface.text.TextUtilities;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundleModelFactory;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.osgi.framework.Constants;

public class BundleModelFactory implements IBundleModelFactory {

	private final IBundleModel fModel;

	public BundleModelFactory(IBundleModel model) {
		fModel = model;
	}

	@Override
	public IManifestHeader createHeader() {
		return null;
	}

	@Override
	public IManifestHeader createHeader(String key, String value) {
		ManifestHeader header = null;
		IBundle bundle = fModel.getBundle();
		String newLine;
		if (fModel instanceof BundleModel) {
			newLine = TextUtilities.getDefaultLineDelimiter(((BundleModel) fModel).getDocument());
		} else {
			// use consistent line endings, see bug 506157
			newLine = "\n"; //$NON-NLS-1$
		}

		if (key.equalsIgnoreCase(Constants.BUNDLE_ACTIVATOR)) {
			header = new BundleActivatorHeader(key, value, bundle, newLine);
		} else if (key.equalsIgnoreCase(Constants.BUNDLE_LOCALIZATION)) {
			header = new BundleLocalizationHeader(key, value, bundle, newLine);
		} else if (key.equalsIgnoreCase(Constants.BUNDLE_NAME)) {
			header = new BundleNameHeader(key, value, bundle, newLine);
		} else if (key.equalsIgnoreCase(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT)) {
			header = new RequiredExecutionEnvironmentHeader(key, value, bundle, newLine);
		} else if (key.equalsIgnoreCase(Constants.BUNDLE_SYMBOLICNAME)) {
			header = new BundleSymbolicNameHeader(key, value, bundle, newLine);
		} else if (key.equalsIgnoreCase(Constants.BUNDLE_VENDOR)) {
			header = new BundleVendorHeader(key, value, bundle, newLine);
		} else if (key.equalsIgnoreCase(Constants.BUNDLE_VERSION)) {
			header = new BundleVersionHeader(key, value, bundle, newLine);
		} else if (key.equalsIgnoreCase(Constants.BUNDLE_CLASSPATH)) {
			header = new BundleClasspathHeader(key, value, bundle, newLine);
		} else if (key.equalsIgnoreCase(ICoreConstants.ECLIPSE_LAZYSTART) || key.equalsIgnoreCase(ICoreConstants.ECLIPSE_AUTOSTART)) {
			header = new LazyStartHeader(key, value, bundle, newLine);
		} else if (key.equalsIgnoreCase(Constants.EXPORT_PACKAGE) || key.equalsIgnoreCase(ICoreConstants.PROVIDE_PACKAGE)) {
			header = new ExportPackageHeader(key, value, bundle, newLine);
		} else if (key.equalsIgnoreCase(Constants.FRAGMENT_HOST)) {
			header = new FragmentHostHeader(key, value, bundle, newLine);
		} else if (key.equalsIgnoreCase(Constants.IMPORT_PACKAGE)) {
			header = new ImportPackageHeader(key, value, bundle, newLine);
		} else if (key.equalsIgnoreCase(Constants.REQUIRE_BUNDLE)) {
			header = new RequireBundleHeader(key, value, bundle, newLine);
		} else if (key.equalsIgnoreCase(Constants.BUNDLE_ACTIVATIONPOLICY)) {
			header = new BundleActivationPolicyHeader(key, value, bundle, newLine);
		} else {
			header = new ManifestHeader(key, value, bundle, newLine);
		}
		return header;
	}

}
