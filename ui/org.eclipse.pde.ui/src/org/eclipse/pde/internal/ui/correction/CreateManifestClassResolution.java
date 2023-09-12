/*******************************************************************************
 *  Copyright (c) 2005, 2019 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.plugin.JavaAttributeValue;
import org.eclipse.pde.internal.ui.util.PDEJavaHelperUI;
import org.eclipse.pde.internal.ui.util.TextUtil;

public class CreateManifestClassResolution extends AbstractManifestMarkerResolution {

	private final String fHeader;

	public CreateManifestClassResolution(int type, String headerName, IMarker marker) {
		super(type, marker);
		fHeader = headerName;
	}

	@Override
	protected void createChange(BundleModel model) {
		IManifestHeader header = model.getBundle().getManifestHeader(fHeader);

		String name = TextUtil.trimNonAlphaChars(header.getValue()).replace('$', '.');
		IProject project = model.getUnderlyingResource().getProject();

		IPluginModelBase modelBase = PluginRegistry.findModel(project);
		if (modelBase == null)
			return;

		JavaAttributeValue value = new JavaAttributeValue(project, modelBase, null, name);
		name = PDEJavaHelperUI.createClass(name, project, value, true);
		if (name != null && !name.equals(header.getValue()))
			header.setValue(name);
	}

	@Override
	public String getLabel() {
		return NLS.bind(PDEUIMessages.CreateManifestClassResolution_label, fHeader);
	}

}
