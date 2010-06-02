/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.publisher.compatibility;

import org.eclipse.equinox.p2.metadata.MetadataFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.p2.metadata.ITouchpointData;
import org.eclipse.equinox.p2.publisher.AbstractPublisherAction;
import org.eclipse.equinox.p2.publisher.actions.ITouchpointAdvice;
import org.eclipse.equinox.p2.publisher.actions.RootFilesAdvice;
import org.eclipse.osgi.service.environment.Constants;

public class RootFileTouchpointAdvice extends RootFilesAdvice implements ITouchpointAdvice {

	private final ProductFile product;

	public RootFileTouchpointAdvice(ProductFile product, File root, File[] includedFiles, File[] excludedFiles, String configSpec) {
		super(root, includedFiles, excludedFiles, configSpec);
		this.product = product;
	}

	public ITouchpointData getTouchpointData(ITouchpointData existingData) {
		String[] config = AbstractPublisherAction.parseConfigSpec(getConfigSpec());
		String os = config[1];

		String launcherName = product != null ? product.getLauncherName() : null;
		if (launcherName == null)
			launcherName = "eclipse"; //$NON-NLS-1$

		File root = getRoot();
		File launcherFile = new File(root, launcherName);
		if (Constants.OS_MACOSX.equals(os)) {
			launcherFile = new File(root, launcherName + ".app/Contents/MacOS/" + launcherName); //$NON-NLS-1$
			if (!launcherFile.exists()) {
				String capitalized = launcherName.substring(0, 1).toUpperCase() + launcherName.substring(1, launcherName.length());
				launcherFile = new File(root, capitalized + ".app/Contents/MacOS/" + launcherName); //$NON-NLS-1$
			}
		} else if (Constants.OS_WIN32.equals(os) && !launcherFile.exists()) {
			launcherFile = new File(root, launcherName + ".exe"); //$NON-NLS-1$				
		}

		String configInstruction = null;
		if (launcherFile.exists()) {
			configInstruction = "setLauncherName(name:" + launcherName + ");"; //$NON-NLS-1$ //$NON-NLS-2$
			if (Constants.OS_MACOSX.equals(os)) {
				Path path = new Path(launcherFile.getAbsolutePath());
				File appFolder = path.removeLastSegments(3).toFile();
				configInstruction += "chmod(targetDir:${installFolder}/" + appFolder.getName() + "/Contents/MacOS/, targetFile:" + launcherFile.getName() + ", permissions:755);"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			} else if (!Constants.OS_WIN32.equals(os)) {
				configInstruction += "chmod(targetDir:${installFolder}, targetFile:" + launcherFile.getName() + ", permissions:755);"; //$NON-NLS-1$ //$NON-NLS-2$				
			}

			Map newInstructions = new HashMap();
			newInstructions.put("configure", MetadataFactory.createTouchpointInstruction(configInstruction, "org.eclipse.equinox.p2.touchpoint.eclipse.setLauncherName")); //$NON-NLS-1$ //$NON-NLS-2$ 
			return MetadataFactory.mergeTouchpointData(existingData, newInstructions);
		}
		return existingData;
	}
}
