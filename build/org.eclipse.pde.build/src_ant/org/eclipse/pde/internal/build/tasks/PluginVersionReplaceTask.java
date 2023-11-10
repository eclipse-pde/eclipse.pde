/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.tasks;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/** 
 * Internal task.
 * This task aims at replacing the generic ids used into a plugin.xml by another value.
 * @since 3.0
 */
public class PluginVersionReplaceTask extends Task {
	private static final String PLUGIN_START_TAG = "<plugin"; //$NON-NLS-1$
	private static final String FRAGMENT_START_TAG = "<fragment"; //$NON-NLS-1$
	private static final String COMMENT_START_TAG = "<!--"; //$NON-NLS-1$
	private static final String COMMENT_END_TAG = "-->"; //$NON-NLS-1$
	private static final String VERSION = "version";//$NON-NLS-1$
	private static final String BACKSLASH = "\""; //$NON-NLS-1$

	//Path of the file where we are replacing the values
	private String pluginFilePath;
	private boolean plugin = true;
	private String newVersion;

	/**
	 * The location of a fragment.xml or plugin.xml file 
	 */
	public void setPluginFilePath(String path) {
		pluginFilePath = path;
	}

	/**
	 * Set the new version.
	 * @param qualifier the version that will be set in the manifest file.
	 */
	public void setVersionNumber(String qualifier) {
		newVersion = qualifier;
	}

	/**
	 * Set the type of the file.
	 * @param input 
	 */
	public void setInput(String input) {
		if (input.equalsIgnoreCase("fragment.xml")) //$NON-NLS-1$
			plugin = false;
	}

	@Override
	public void execute() {
		StringBuffer buffer = null;
		try {
			buffer = readFile(new File(pluginFilePath));
		} catch (IOException e) {
			throw new BuildException(e);
		}

		//Find the word plugin or fragment
		int startPlugin = scan(buffer, 0, plugin ? PLUGIN_START_TAG : FRAGMENT_START_TAG);
		int startComment = scan(buffer, 0, COMMENT_START_TAG);
		int endComment = startComment > -1 ? scan(buffer, startComment, COMMENT_END_TAG) : -1;

		while (startComment != -1 && startPlugin > startComment && startPlugin < endComment) {
			startPlugin = scan(buffer, endComment, plugin ? PLUGIN_START_TAG : FRAGMENT_START_TAG);
			startComment = scan(buffer, endComment, COMMENT_START_TAG);
			endComment = startComment > -1 ? scan(buffer, startComment, COMMENT_END_TAG) : -1;
		}

		if (startPlugin == -1)
			return;

		int endPlugin = scan(buffer, startPlugin + 1, ">"); //$NON-NLS-1$

		//Find the version tag in the plugin header
		boolean versionFound = false;
		while (!versionFound) {
			int versionAttr = scan(buffer, startPlugin, VERSION);
			if (versionAttr == -1 || versionAttr > endPlugin)
				return;
			if (!Character.isWhitespace(buffer.charAt(versionAttr - 1))) {
				startPlugin = versionAttr + VERSION.length();
				continue;
			}
			//Verify that the word version found is the actual attribute
			int endVersionWord = versionAttr + VERSION.length();
			while (Character.isWhitespace(buffer.charAt(endVersionWord)) && endVersionWord < endPlugin) {
				endVersionWord++;
			}
			if (endVersionWord > endPlugin) //version has not been found 
				return;

			if (buffer.charAt(endVersionWord) != '=') {
				startPlugin = endVersionWord;
				continue;
			}

			//Version has been found, extract the version id and replace it
			int startVersionId = scan(buffer, versionAttr + 1, BACKSLASH);
			int endVersionId = scan(buffer, startVersionId + 1, BACKSLASH);

			buffer.replace(startVersionId + 1, endVersionId, newVersion);
			versionFound = true;
		}
		try (OutputStreamWriter w = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(pluginFilePath)), StandardCharsets.UTF_8)) {
			w.write(buffer.toString());
		} catch (FileNotFoundException e) {
			// ignore
		} catch (IOException e) {
			throw new BuildException(e);
		}
	}

	private int scan(StringBuffer buf, int start, String targetName) {
		return scan(buf, start, new String[] {targetName});
	}

	private int scan(StringBuffer buf, int start, String[] targets) {
		for (int i = start; i < buf.length(); i++) {
			for (String target2 : targets) {
				if (i < buf.length() - target2.length()) {
					String match = buf.substring(i, i + target2.length());
					if (target2.equalsIgnoreCase(match))
						return i;
				}
			}
		}
		return -1;
	}

	private StringBuffer readFile(File targetName) throws IOException {
		return new StringBuffer(Files.readString(targetName.toPath()));
	}
}
