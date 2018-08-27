/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc. and others
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

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;
import org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants;
import org.eclipse.ui.PlatformUI;

/**
 * Rule to match the attributes of a tag
 */
public class TargetPlatformAttributeRule extends WordRule {

	private static final String attributes[] = new String[] { ITargetConstants.TARGET_NAME_ATTR,
			ITargetConstants.UNIT_VERSION_ATTR, ITargetConstants.UNIT_ID_ATTR,
			ITargetConstants.LOCATION_INCLUDE_PLATFORMS_ATTR, ITargetConstants.LOCATION_INCLUDE_MODE_ATTR,
			ITargetConstants.LOCATION_TYPE_ATTR, ITargetConstants.REPOSITORY_LOCATION_ATTR,
			ITargetConstants.TARGET_JRE_PATH_ATTR, ITargetConstants.TARGET_SEQ_NO_ATTR,
			ITargetConstants.LOCATION_INCLUDE_CONFIG_PHASE_ATTR, ITargetConstants.LOCATION_INCLUDE_SOURCE_ATTR };
	private IToken attributeToken = new Token(
			new TextAttribute(PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry()
					.get(IGETEColorConstants.P_ATTRIBUTE)));

	public TargetPlatformAttributeRule() {
		super(new AlphanumericDetector());
		for (String att : attributes) {
			this.addWord(att, attributeToken);
		}
	}

}
