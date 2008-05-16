/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.tests.util;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.eclipse.pde.api.tools.internal.builder.BuildState;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;

/**
 * Class used to decode what is in an api tool build state
 */
public class DecodeBuildState {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("Usage: <path to build state>");
			return;
		}
		String fileName = args[0];
		File file = new File(fileName);
		if (!file.exists()) {
			System.err.println("Build state file : " + fileName + " doesn't exist");
			return;
		}
		BuildState state = null;
		DataInputStream inputStream = null;
		try {
			inputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
			state = BuildState.read(inputStream);
		} catch (FileNotFoundException e) {
			ApiPlugin.log(e);
		} catch (IOException e) {
			ApiPlugin.log(e);
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
		if (state != null) printBuildState(state);
	}

	private static void printBuildState(BuildState state) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter writer = new PrintWriter(stringWriter);
		writer.println("Breaking changes");
		IDelta[] breakingChanges = state.getBreakingChanges();
		int length = breakingChanges.length;
		if (length != 0) {
			for (int i = 0; i < length; i++) {
				IDelta delta = breakingChanges[i];
				writer.println(delta);
				writer.println(delta.getMessage());
			}
		} else {
			writer.println("No breaking changes");
		}
		writer.println("Compatible changes");
		IDelta[] compatibleChanges = state.getCompatibleChanges();
		length = compatibleChanges.length;
		if (length != 0) {
			for (int i = 0; i < length; i++) {
				IDelta delta = compatibleChanges[i];
				writer.println(delta);
				writer.println(delta.getMessage());
			}
		} else {
			writer.println("No compatible changes");
		}
		writer.flush();
		writer.close();
		System.out.println("Build state:" + String.valueOf(stringWriter.getBuffer()));
	}

	
}
