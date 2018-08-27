/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
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
 * Class used to decode what is in an API tool build state
 */
public class DecodeBuildState {

	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("Usage: <path to build state>"); //$NON-NLS-1$
			return;
		}
		String fileName = args[0];
		File file = new File(fileName);
		if (!file.exists()) {
			System.err.println("Build state file : " + fileName + " doesn't exist"); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}
		BuildState state = null;
		try (DataInputStream inputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
			state = BuildState.read(inputStream);
		} catch (FileNotFoundException e) {
			ApiPlugin.log(e);
		} catch (IOException e) {
			ApiPlugin.log(e);
		}
		if (state != null) {
			printBuildState(state);
		}
	}

	private static void printBuildState(BuildState state) {
		StringWriter stringWriter = new StringWriter();
		try (PrintWriter writer = new PrintWriter(stringWriter)) {
			writer.println("Breaking changes"); //$NON-NLS-1$
			IDelta[] breakingChanges = state.getBreakingChanges();
			int length = breakingChanges.length;
			if (length != 0) {
				for (int i = 0; i < length; i++) {
					IDelta delta = breakingChanges[i];
					writer.println(delta);
					writer.println(delta.getMessage());
				}
			} else {
				writer.println("No breaking changes"); //$NON-NLS-1$
			}
			writer.println("Compatible changes"); //$NON-NLS-1$
			IDelta[] compatibleChanges = state.getCompatibleChanges();
			length = compatibleChanges.length;
			if (length != 0) {
				for (int i = 0; i < length; i++) {
					IDelta delta = compatibleChanges[i];
					writer.println(delta);
					writer.println(delta.getMessage());
				}
			} else {
				writer.println("No compatible changes"); //$NON-NLS-1$
			}
			writer.flush();
		}
		System.out.println("Build state:" + String.valueOf(stringWriter.getBuffer())); //$NON-NLS-1$
	}


}
