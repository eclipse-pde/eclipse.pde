/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.build.tasks;

import java.io.*;
import java.util.*;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.types.resources.Files;
import org.apache.tools.ant.types.resources.Union;

public class CompileErrorTask extends Task {
	private static final Object LOCK = new Object();
	private static final String NEW_LINE = System.getProperty("line.separator"); //$NON-NLS-1$
	private static final String ANT_PREFIX = "${"; //$NON-NLS-1$

	private final Files problemFiles = new Files();
	private String logFile = null;
	private String bundle = null;

	public void execute() {
		if (logFile == null || logFile.startsWith(ANT_PREFIX) || problemFiles.size() == 0)
			return;

		Union union = new Union(problemFiles);
		String[] prereqFiles = union.list();
		List problems = new ArrayList();
		BufferedReader reader = null;
		for (int i = 0; i < prereqFiles.length; i++) {
			File file = new File(prereqFiles[i]);
			try {
				reader = new BufferedReader(new FileReader(file));
				String line = reader.readLine();
				if (line != null)
					problems.add(line);
			} catch (IOException e) {
				// 
			} finally {
				close(reader);
			}
		}

		if (problems.size() > 0) {
			File log = new File(logFile);
			if (!log.getParentFile().exists())
				log.getParentFile().mkdirs();
			synchronized (LOCK) {
				FileWriter writer = null;
				try {
					writer = new FileWriter(log, true);
					writer.write(bundle + ": the following prerequisites contain compile errors" + NEW_LINE); //$NON-NLS-1$
					for (Iterator iterator = problems.iterator(); iterator.hasNext();) {
						writer.write("\t"); //$NON-NLS-1$
						writer.write((String) iterator.next());
						writer.write(NEW_LINE);
					}
				} catch (IOException e) {
					// 
				} finally {
					close(writer);
				}

			}
		}

	}

	private void close(Object o) {
		if (o == null)
			return;
		try {
			if (o instanceof Reader)
				((Reader) o).close();
			if (o instanceof Writer)
				((Writer) o).close();
		} catch (IOException e) {
			// ignore
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
