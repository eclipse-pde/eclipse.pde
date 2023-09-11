/*******************************************************************************
 * Copyright (c) 2016, 2022 Red Hat Inc. and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Sopot Cela (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.pde.internal.genericeditor.target.extension.reconciler.presentation;

import static org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants.INCLUDE_DEPENDENCY_DEPTH;
import static org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants.INCLUDE_DEPENDENCY_SCOPES;
import static org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants.LOCATION_INCLUDE_CONFIG_PHASE_ATTR;
import static org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants.LOCATION_INCLUDE_MODE_ATTR;
import static org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants.LOCATION_INCLUDE_PLATFORMS_ATTR;
import static org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants.LOCATION_INCLUDE_SOURCE_ATTR;
import static org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants.LOCATION_TYPE_ATTR;
import static org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants.MISSING_MANIFEST;
import static org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants.REPOSITORY_LOCATION_ATTR;
import static org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants.TARGET_JRE_PATH_ATTR;
import static org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants.TARGET_NAME_ATTR;
import static org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants.TARGET_SEQ_NO_ATTR;
import static org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants.UNIT_ID_ATTR;
import static org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants.UNIT_VERSION_ATTR;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;
import org.eclipse.ui.PlatformUI;

/**
 * Rule to match the attributes of a tag
 */
public class TargetPlatformAttributeRule extends WordRule {

	private static final String[] ATTRIBUTES = new String[] { TARGET_NAME_ATTR, UNIT_VERSION_ATTR, UNIT_ID_ATTR,
			LOCATION_INCLUDE_PLATFORMS_ATTR, LOCATION_INCLUDE_MODE_ATTR, LOCATION_TYPE_ATTR, REPOSITORY_LOCATION_ATTR,
			TARGET_JRE_PATH_ATTR, TARGET_SEQ_NO_ATTR, LOCATION_INCLUDE_CONFIG_PHASE_ATTR, LOCATION_INCLUDE_SOURCE_ATTR,
			INCLUDE_DEPENDENCY_DEPTH, INCLUDE_DEPENDENCY_SCOPES, MISSING_MANIFEST };
	private final IToken attributeToken = new Token(
			new TextAttribute(PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry()
					.get(IGETEColorConstants.P_ATTRIBUTE)));

	public TargetPlatformAttributeRule() {
		super(new AlphanumericDetector());
		for (String att : ATTRIBUTES) {
			this.addWord(att, attributeToken);
		}
	}

}
