/*******************************************************************************
 *  Copyright (c) 2023 Christoph Läubrich and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.bnd;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.internal.core.builders.CompilerFlags;
import org.eclipse.pde.internal.core.builders.ErrorReporter;
import org.eclipse.pde.internal.core.builders.PDEMarkerFactory;

import aQute.bnd.properties.IRegion;
import aQute.bnd.properties.LineType;
import aQute.bnd.properties.PropertiesLineReader;
import aQute.service.reporter.Report;
import aQute.service.reporter.Report.Location;

public class BndErrorReporter extends ErrorReporter {

	private BndDocument bndDocument;
	private final Report report;

	public BndErrorReporter(IProject project, Report report, IFile file) {
		super(file);
		this.report = report;
		IDocument document = createDocument(fFile);
		if (document != null) {
			bndDocument = new BndDocument(document);
		}
	}

	@Override
	protected void validate(IProgressMonitor monitor) {
		if (report != null) {
			for (String warn : report.getWarnings()) {
				reportProblem(warn, CompilerFlags.WARNING, report.getLocation(warn));
			}
			for (String err : report.getErrors()) {
				reportProblem(err, CompilerFlags.ERROR, report.getLocation(err));
			}
		}
	}

	private void reportProblem(String err, int severity, Location location) {
		int line = -1;
		if (location != null) {
			if (location.line > 0) {
				line = location.line;
			} else if (location.header != null && bndDocument != null) {
				try {
					PropertiesLineReader reader = new PropertiesLineReader(bndDocument);
					LineType type = reader.next();
					while (type != LineType.eof) {
						if (type == LineType.entry) {
							String key = reader.key();
							if (location.header.equals(key)) {
								IRegion region = reader.region();
								line = bndDocument.getLineOfOffset(region.getOffset()) + 1;
								break;
							}
						}
						type = reader.next();
					}
				} catch (Exception e) {
					// can't do anything here then...
				}
			}
		}
		report(err, line, severity, PDEMarkerFactory.CAT_FATAL);
	}

}
