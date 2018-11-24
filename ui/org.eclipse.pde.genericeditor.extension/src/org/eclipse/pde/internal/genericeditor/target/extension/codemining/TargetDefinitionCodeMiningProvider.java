/********************************************************************************
 * Copyright (c) 2018 vogella GmbH and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lars Vogel (vogella GmbH) - initial API and implementation
 *     Alexander Fedorov <alexander.fedorov@arsysop.ru> - Bug 534758
 ********************************************************************************/
package org.eclipse.pde.internal.genericeditor.target.extension.codemining;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.xml.stream.XMLStreamException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.codemining.AbstractCodeMiningProvider;
import org.eclipse.jface.text.codemining.ICodeMining;
import org.eclipse.pde.internal.genericeditor.target.extension.model.Node;
import org.eclipse.pde.internal.genericeditor.target.extension.model.xml.Parser;

public class TargetDefinitionCodeMiningProvider extends AbstractCodeMiningProvider {

	@Override
	public CompletableFuture<List<? extends ICodeMining>> provideCodeMinings(ITextViewer viewer,
			IProgressMonitor monitor) {
		return CompletableFuture.supplyAsync(() -> {
			List<ICodeMining> minings = new ArrayList<>();
			IDocument document = viewer.getDocument();
			try {
				fillCodeMinings(document, minings);
			} catch (BadLocationException e) {
				// Caught with empty mining
			}
			return minings;
		});
	}

	void fillCodeMinings(IDocument document, List<ICodeMining> minings) throws BadLocationException {
		int line = 0;
		try {
			Parser parser = Parser.getDefault();
			parser.parse(document);
			Node target = parser.getRootNode();
			if (target != null) {
				line = document.getLineOfOffset(target.getOffsetStart());
				minings.add(new TargetDefinitionActivationCodeMining(line, document, this, null));
			} else {
				minings.add(new TargetDefinitionActivationCodeMining(line, document, this,
						Messages.TargetDefinitionCodeMiningProvider_e_format_invalid));
			}
		} catch (XMLStreamException e) {
			minings.add(new TargetDefinitionActivationCodeMining(line, document, this,
					Messages.TargetDefinitionCodeMiningProvider_e_format_invalid));
		}
	}

}
