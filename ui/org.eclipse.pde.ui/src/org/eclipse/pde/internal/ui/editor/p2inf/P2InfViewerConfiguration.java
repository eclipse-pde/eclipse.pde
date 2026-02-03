/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.p2inf;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.pde.internal.ui.editor.build.BuildSourceViewerConfiguration;
import org.eclipse.pde.internal.ui.editor.contentassist.TypePackageCompletionProcessor;
import org.eclipse.pde.internal.ui.editor.text.IColorManager;

/**
 * Viewer configuration for the p2.inf editor. Reuses all syntax highlighting
 * from {@link BuildSourceViewerConfiguration} (properties-file grammar) and
 * only swaps in the p2.inf-specific content-assist processor.
 */
public class P2InfViewerConfiguration extends BuildSourceViewerConfiguration {

	public P2InfViewerConfiguration(IColorManager colorManager, IPreferenceStore store, PDESourcePage sourcePage) {
		super(colorManager, store, sourcePage);
	}

	@Override
	protected TypePackageCompletionProcessor createContentAssistProcessor() {
		return new P2InfContentAssistProcessor(fSourcePage);
	}

}
