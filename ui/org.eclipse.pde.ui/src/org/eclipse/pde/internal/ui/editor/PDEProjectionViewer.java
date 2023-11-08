/*******************************************************************************
 *  Copyright (c) 2006, 2015 IBM Corporation and others.
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

package org.eclipse.pde.internal.ui.editor;

import org.eclipse.jface.text.information.IInformationPresenter;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.pde.internal.ui.editor.text.ChangeAwareSourceViewerConfiguration;
import org.eclipse.swt.widgets.Composite;

/**
 * PDEProjectionViewer
 *
 */
public class PDEProjectionViewer extends ProjectionViewer {

	/**
	 * Text operation code for requesting the quick outline for the current input.
	 */
	public static final int QUICK_OUTLINE = 513;

	private IInformationPresenter fOutlinePresenter;

	private final boolean fIsQuickOutlineEnabled;

	/**
	 * @param parent
	 * @param ruler
	 * @param overviewRuler
	 * @param showsAnnotationOverview
	 * @param styles
	 */
	public PDEProjectionViewer(Composite parent, IVerticalRuler ruler, IOverviewRuler overviewRuler, boolean showsAnnotationOverview, int styles, boolean isQuickOutlineEnabled) {
		super(parent, ruler, overviewRuler, showsAnnotationOverview, styles);

		fIsQuickOutlineEnabled = isQuickOutlineEnabled;
	}

	@Override
	public void doOperation(int operation) {
		// Ensure underlying text widget is defined
		if ((getTextWidget() == null) || getTextWidget().isDisposed()) {
			return;
		}
		// Handle quick outline operation
		if (operation == QUICK_OUTLINE) {
			if (fOutlinePresenter != null) {
				fOutlinePresenter.showInformation();
			}
			return;
		}
		// Handle default operations
		super.doOperation(operation);
	}

	@Override
	public boolean canDoOperation(int operation) {
		// Verify quick outline operation
		if (operation == QUICK_OUTLINE) {
			if (fOutlinePresenter == null) {
				return false;
			}
			return true;
		}
		// Verfify default operations
		return super.canDoOperation(operation);
	}

	@Override
	public void configure(SourceViewerConfiguration configuration) {
		// Ensure underlying text widget is defined
		if ((getTextWidget() == null) || getTextWidget().isDisposed()) {
			return;
		}
		// Configure default operations
		super.configure(configuration);
		// Configure quick outline operation for the source viewer only if the
		// given source viewer supports it
		if (fIsQuickOutlineEnabled && configuration instanceof ChangeAwareSourceViewerConfiguration sourceConfiguration) {
			fOutlinePresenter = sourceConfiguration.getOutlinePresenter(this);
			if (fOutlinePresenter != null) {
				fOutlinePresenter.install(this);
			}
		}
	}

	@Override
	public void unconfigure() {
		// Unconfigure quick outline operation
		if (fOutlinePresenter != null) {
			fOutlinePresenter.uninstall();
			fOutlinePresenter = null;
		}
		// Unconfigure default operations
		super.unconfigure();
	}

}
