/*******************************************************************************
 * Copyright (c) 2010, 2021 IBM Corporation and others.
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

package org.eclipse.pde.internal.build.tasks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.types.resources.Files;
import org.apache.tools.ant.types.resources.Union;

public class CompileErrorTask extends Task {
	private static final Object LOCK = new Object();
	private static final String NEW_LINE = System.lineSeparator();
	private static final String ANT_PREFIX = "${"; //$NON-NLS-1$

	private final Files problemFiles = new Files();
	private String logFile = null;
	private String bundle = null;

	@Override
	public void execute() {
		if (logFile == null || logFile.startsWith(ANT_PREFIX) || problemFiles.size() == 0) {
			return;
		}

		Union union = new Union(problemFiles);
		String[] prereqFiles = union.list();
		List<String> problems = new ArrayList<>();
		for (String prereqFile : prereqFiles) {
			File file = new File(prereqFile);
			try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
				String line = reader.readLine();
				if (line != null) {
					problems.add(line);
				}
			} catch (IOException e) {
				// 
			}
		}

		if (problems.size() > 0) {
			File log = new File(logFile);
			if (!log.getParentFile().exists()) {
				log.getParentFile().mkdirs();
			}
			synchronized (LOCK) {
				try (FileWriter writer = new FileWriter(log, true)) {
					writer.write(bundle + ": the following prerequisites contain compile errors" + NEW_LINE); //$NON-NLS-1$
					for (String problem : problems) {
						writer.write("\t"); //$NON-NLS-1$
						writer.write(problem);
						writer.write(NEW_LINE);
					}
				} catch (IOException e) {
					// 
				}

			}
		}

	}

	public void setBundle(String bundle) {
		this.bundle = bundle;
	}

	public void setLog(String logFile) {
		this.logFile = logFile;
	}

	public PatternSet.NameEntry createInclude() {
		return problemFiles.createInclude();
	}

}
