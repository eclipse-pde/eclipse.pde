/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.ApiConfigSettings.ErrorMode;
import org.eclipse.pde.api.tools.internal.ApiConfigSettings.VersionIncrementRule;
import org.eclipse.pde.api.tools.internal.ApiConfigSettings.VersionSegment;

/**
 * Parser for .apiconfig files
 * 
 * Format:
 * - Lines starting with # are comments
 * - Empty lines are ignored
 * - Settings are in key=value format
 * - Version increment format: segment.version.increment = target_segment+amount
 *   Example: major.version.increment = minor+1
 * - Error mode format: segment.version.error = error|warning|ignore|filter
 *   Example: major.version.error = filter
 * 
 * @since 1.2
 */
public class ApiConfigParser {
	
	private static final Pattern INCREMENT_PATTERN = Pattern.compile("(major|minor|micro)\\s*\\+\\s*(\\d+)");
	
	/**
	 * Parse an .apiconfig file from a project
	 * 
	 * @param project the project to search for .apiconfig file
	 * @return parsed settings, or null if file doesn't exist
	 * @throws CoreException if there's an error reading the file
	 */
	public static ApiConfigSettings parseFromProject(IProject project) throws CoreException {
		if (project == null || !project.exists()) {
			return null;
		}
		
		IFile configFile = project.getFile(IApiCoreConstants.API_CONFIG_FILE_NAME);
		if (!configFile.exists()) {
			return null;
		}
		
		try (InputStream is = configFile.getContents()) {
			return parse(is);
		} catch (IOException e) {
			throw new CoreException(
					org.eclipse.core.runtime.Status.error("Error reading .apiconfig file", e));
		}
	}
	
	/**
	 * Parse an .apiconfig file from a File
	 * 
	 * @param configFile the .apiconfig file
	 * @return parsed settings, or null if file doesn't exist
	 * @throws IOException if there's an error reading the file
	 */
	public static ApiConfigSettings parseFromFile(File configFile) throws IOException {
		if (configFile == null || !configFile.exists()) {
			return null;
		}
		
		try (InputStream is = new FileInputStream(configFile)) {
			return parse(is);
		}
	}
	
	/**
	 * Parse an .apiconfig file from an InputStream
	 * 
	 * @param is the input stream
	 * @return parsed settings
	 * @throws IOException if there's an error reading the file
	 */
	public static ApiConfigSettings parse(InputStream is) throws IOException {
		ApiConfigSettings settings = new ApiConfigSettings();
		
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
			String line;
			int lineNumber = 0;
			
			while ((line = reader.readLine()) != null) {
				lineNumber++;
				line = line.trim();
				
				// Skip comments and empty lines
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				
				// Parse key=value pairs
				int equalsIndex = line.indexOf('=');
				if (equalsIndex < 0) {
					// Invalid line, skip
					continue;
				}
				
				String key = line.substring(0, equalsIndex).trim();
				String value = line.substring(equalsIndex + 1).trim();
				
				try {
					parseKeyValue(settings, key, value);
				} catch (IllegalArgumentException e) {
					// Log warning but continue parsing
					System.err.println("Warning: Invalid value at line " + lineNumber + ": " + e.getMessage());
				}
			}
		}
		
		return settings;
	}
	
	private static void parseKeyValue(ApiConfigSettings settings, String key, String value) {
		switch (key) {
			case "major.version.increment":
				settings.setMajorVersionIncrement(parseIncrementRule(value));
				break;
			case "minor.version.increment":
				settings.setMinorVersionIncrement(parseIncrementRule(value));
				break;
			case "micro.version.increment":
				settings.setMicroVersionIncrement(parseIncrementRule(value));
				break;
			case "major.version.error":
				settings.setMajorVersionError(parseErrorMode(value));
				break;
			case "minor.version.error":
				settings.setMinorVersionError(parseErrorMode(value));
				break;
			case "micro.version.error":
				settings.setMicroVersionError(parseErrorMode(value));
				break;
			default:
				// Unknown key, ignore
				break;
		}
	}
	
	private static VersionIncrementRule parseIncrementRule(String value) {
		Matcher matcher = INCREMENT_PATTERN.matcher(value.toLowerCase());
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Invalid increment format: " + value + 
					". Expected format: segment+amount (e.g., minor+1, micro+100)");
		}
		
		String segmentStr = matcher.group(1);
		int amount = Integer.parseInt(matcher.group(2));
		
		VersionSegment segment = VersionSegment.valueOf(segmentStr.toUpperCase());
		return new VersionIncrementRule(segment, amount);
	}
	
	private static ErrorMode parseErrorMode(String value) {
		try {
			return ErrorMode.valueOf(value.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid error mode: " + value + 
					". Expected: error, warning, ignore, or filter");
		}
	}
}
