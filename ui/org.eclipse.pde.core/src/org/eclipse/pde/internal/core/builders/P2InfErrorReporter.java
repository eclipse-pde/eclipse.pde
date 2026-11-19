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
package org.eclipse.pde.internal.core.builders;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.builders.IncrementalErrorReporter.VirtualMarker;
import org.eclipse.pde.internal.core.text.p2inf.P2InfEntry;
import org.eclipse.pde.internal.core.text.p2inf.P2InfTextModel;

/**
 * Validates a p2.inf file and reports the problems as markers. The validations
 * are executed in order of priority:
 * <ol>
 * <li>spelling of the keys and presence of numeric indices</li>
 * </ol>
 * The severity of the problems is configured with
 * {@link CompilerFlags#P_P2INF}.
 */
public class P2InfErrorReporter extends ErrorReporter {

	private IDocument fDocument;

	public P2InfErrorReporter(IFile file) {
		super(file);
	}

	@Override
	protected void validate(IProgressMonitor monitor) {
		fDocument = createDocument(fFile);
		if (fDocument == null) {
			return;
		}
		P2InfTextModel model = new P2InfTextModel(fDocument, false);
		try {
			model.load();
		} catch (CoreException e) {
			PDECore.log(e);
			return;
		}
		if (!model.isLoaded()) {
			return;
		}
		for (P2InfEntry entry : model.getEntries()) {
			if (monitor.isCanceled()) {
				return;
			}
			// further validations (id, namespace/name) will only run for
			// entries whose key is well-formed
			validateKey(entry);
		}
	}

	/**
	 * First priority: the spelling of the key and the numeric indices.
	 *
	 * @return <code>true</code> if the key is valid
	 */
	private boolean validateKey(P2InfEntry entry) {
		P2InfGrammar.KeyProblem problem = P2InfGrammar.check(entry.getName());
		if (problem == null) {
			return true;
		}
		String message;
		if (problem.kind() == P2InfGrammar.KeyProblem.Kind.MISSING_INDEX) {
			message = NLS.bind(PDECoreMessages.P2InfErrorReporter_missingIndex, problem.key(), problem.suggestion());
		} else if (problem.suggestion() != null) {
			message = NLS.bind(PDECoreMessages.P2InfErrorReporter_unknownKeySuggestion, problem.key(),
					problem.suggestion());
		} else {
			message = NLS.bind(PDECoreMessages.P2InfErrorReporter_unknownKey, problem.key());
		}
		report(message, getLine(entry));
		return false;
	}

	private void report(String message, int line) {
		VirtualMarker marker = report(message, line, CompilerFlags.P_P2INF, PDEMarkerFactory.M_ONLY_CONFIG_SEV,
				PDEMarkerFactory.CAT_OTHER);
		if (marker != null) {
			marker.setAttribute(PDEMarkerFactory.compilerKey, CompilerFlags.P_P2INF);
		}
	}

	private int getLine(P2InfEntry entry) {
		if (entry.getOffset() < 0) {
			return 1;
		}
		try {
			return fDocument.getLineOfOffset(entry.getOffset()) + 1;
		} catch (BadLocationException e) {
			return 1;
		}
	}
}
