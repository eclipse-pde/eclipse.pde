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
 *     Lucas Bullen (Red Hat Inc.) - [Bug 522317] Support environment arguments tags in Generic TP editor
 *******************************************************************************/
package org.eclipse.pde.internal.genericeditor.target.extension.reconciler.presentation;

import static org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants.ARCH_TAG;
import static org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants.ARTIFACT_ID_TAG;
import static org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants.DEPENDENCIES_TAG;
import static org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants.DEPENDENCY_TAG;
import static org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants.ENVIRONMENT_TAG;
import static org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants.GROUP_ID_TAG;
import static org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants.LAUNCHER_ARGS_TAG;
import static org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants.LOCATIONS_TAG;
import static org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants.LOCATION_TAG;
import static org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants.NL_TAG;
import static org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants.OS_TAG;
import static org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants.PROGRAM_ARGS_TAG;
import static org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants.REPOSITORY_TAG;
import static org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants.TARGET_JRE_TAG;
import static org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants.TARGET_TAG;
import static org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants.TYPE_TAG;
import static org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants.UNIT_TAG;
import static org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants.VERSION_TAG;
import static org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants.VM_ARGS_TAG;
import static org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants.WS_TAG;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;
import org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants;
import org.eclipse.ui.PlatformUI;

/**
 * Word rule feeding the tags of a target definition to be highlighted
 */
public class TargetPlatformTagRule extends WordRule {

	private static final String[] TAGS = new String[] { LOCATIONS_TAG, LOCATION_TAG, TARGET_TAG, UNIT_TAG,
			REPOSITORY_TAG, TARGET_JRE_TAG, LAUNCHER_ARGS_TAG, VM_ARGS_TAG, PROGRAM_ARGS_TAG, ENVIRONMENT_TAG, OS_TAG,
			WS_TAG, ARCH_TAG, NL_TAG, DEPENDENCIES_TAG, DEPENDENCY_TAG, VERSION_TAG, TYPE_TAG, GROUP_ID_TAG,
			ARTIFACT_ID_TAG, ITargetConstants.IMPLICITDEPENDENCIES_TAG, ITargetConstants.PLUGIN_TAG };

	private final IToken tagToken = new Token(
			new TextAttribute(PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry()
					.get(IGETEColorConstants.P_TAG)));

	public TargetPlatformTagRule() {
		super(new AlphanumericDetector());
		for (String tag : TAGS) {
			this.addWord(tag, tagToken);
		}
	}

	@Override
	public IToken evaluate(ICharacterScanner scanner) {
		int c = scanner.read();
		if (c != '<') {
			scanner.unread();
			return fDefaultToken;
		} else {
			c = scanner.read();
			if (c == '/') {
				while (Character.isWhitespace(scanner.read())) {
				}
				scanner.unread();
				return super.evaluate(scanner);
			} else if (c == '>') { // handle special '<>' case
				return tagToken;
			}

			scanner.unread();
		}
		if (c == '>') {
			scanner.unread();
			return tagToken;
		}

		while (Character.isWhitespace(scanner.read())) {
		}
		scanner.unread();

		return super.evaluate(scanner);
	}

}
