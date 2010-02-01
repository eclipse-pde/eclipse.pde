/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.build;

import org.eclipse.pde.internal.core.exports.FeatureExportInfo;
import org.eclipse.pde.internal.core.exports.FeatureExportOperation;
import org.eclipse.pde.internal.ui.PDEUIMessages;

/**
 * This class remains for internal compatibility - see bug 301178.
 */
public class FeatureExportJob extends FeatureExportOperation {

	public FeatureExportJob(FeatureExportInfo info) {
		super(info, PDEUIMessages.FeatureExportJob_name);
	}

}
