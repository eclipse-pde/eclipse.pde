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
package org.eclipse.pde.api.tools.util.tests;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.pde.api.tools.internal.ApiConfigParser;
import org.eclipse.pde.api.tools.internal.ApiConfigSettings;
import org.eclipse.pde.api.tools.internal.ApiConfigSettings.ErrorMode;
import org.eclipse.pde.api.tools.internal.ApiConfigSettings.VersionSegment;

import junit.framework.TestCase;

/**
 * Tests for ApiConfigParser
 */
public class ApiConfigParserTests extends TestCase {
	
	public ApiConfigParserTests(String name) {
		super(name);
	}
	
	/**
	 * Test parsing an empty config file
	 */
	public void testParseEmptyConfig() throws IOException {
		String config = "";
		ApiConfigSettings settings = parseConfig(config);
		
		assertNotNull("Settings should not be null", settings);
		// Should have default values
		assertEquals(VersionSegment.MAJOR, settings.getMajorVersionIncrement().getTargetSegment());
		assertEquals(1, settings.getMajorVersionIncrement().getIncrementAmount());
	}
	
	/**
	 * Test parsing comments and empty lines
	 */
	public void testParseWithComments() throws IOException {
		String config = "# This is a comment\n" +
				"\n" +
				"# Another comment\n" +
				"major.version.increment = minor+1\n" +
				"\n" +
				"# More comments\n";
		
		ApiConfigSettings settings = parseConfig(config);
		assertNotNull(settings);
		assertEquals(VersionSegment.MINOR, settings.getMajorVersionIncrement().getTargetSegment());
		assertEquals(1, settings.getMajorVersionIncrement().getIncrementAmount());
	}
	
	/**
	 * Test parsing version increment with same segment
	 */
	public void testParseMicroIncrement100() throws IOException {
		String config = "micro.version.increment = micro+100\n";
		
		ApiConfigSettings settings = parseConfig(config);
		assertNotNull(settings);
		assertEquals(VersionSegment.MICRO, settings.getMicroVersionIncrement().getTargetSegment());
		assertEquals(100, settings.getMicroVersionIncrement().getIncrementAmount());
	}
	
	/**
	 * Test parsing version increment with different segment
	 */
	public void testParseMajorToMinorIncrement() throws IOException {
		String config = "major.version.increment = minor+1\n";
		
		ApiConfigSettings settings = parseConfig(config);
		assertNotNull(settings);
		assertEquals(VersionSegment.MINOR, settings.getMajorVersionIncrement().getTargetSegment());
		assertEquals(1, settings.getMajorVersionIncrement().getIncrementAmount());
	}
	
	/**
	 * Test parsing multiple version increments
	 */
	public void testParseMultipleIncrements() throws IOException {
		String config = "major.version.increment = minor+1\n" +
				"minor.version.increment = minor+5\n" +
				"micro.version.increment = micro+100\n";
		
		ApiConfigSettings settings = parseConfig(config);
		assertNotNull(settings);
		
		assertEquals(VersionSegment.MINOR, settings.getMajorVersionIncrement().getTargetSegment());
		assertEquals(1, settings.getMajorVersionIncrement().getIncrementAmount());
		
		assertEquals(VersionSegment.MINOR, settings.getMinorVersionIncrement().getTargetSegment());
		assertEquals(5, settings.getMinorVersionIncrement().getIncrementAmount());
		
		assertEquals(VersionSegment.MICRO, settings.getMicroVersionIncrement().getTargetSegment());
		assertEquals(100, settings.getMicroVersionIncrement().getIncrementAmount());
	}
	
	/**
	 * Test parsing error mode settings
	 */
	public void testParseErrorModes() throws IOException {
		String config = "major.version.error = filter\n" +
				"minor.version.error = warning\n" +
				"micro.version.error = ignore\n";
		
		ApiConfigSettings settings = parseConfig(config);
		assertNotNull(settings);
		
		assertEquals(ErrorMode.FILTER, settings.getMajorVersionError());
		assertEquals(ErrorMode.WARNING, settings.getMinorVersionError());
		assertEquals(ErrorMode.IGNORE, settings.getMicroVersionError());
	}
	
	/**
	 * Test parsing complete configuration
	 */
	public void testParseCompleteConfig() throws IOException {
		String config = "# Eclipse Platform API configuration\n" +
				"# We don't use major version increments\n" +
				"major.version.increment = minor+1\n" +
				"major.version.error = filter\n" +
				"\n" +
				"# Micro increments by 100\n" +
				"micro.version.increment = micro+100\n" +
				"micro.version.error = error\n";
		
		ApiConfigSettings settings = parseConfig(config);
		assertNotNull(settings);
		
		assertEquals(VersionSegment.MINOR, settings.getMajorVersionIncrement().getTargetSegment());
		assertEquals(1, settings.getMajorVersionIncrement().getIncrementAmount());
		assertEquals(ErrorMode.FILTER, settings.getMajorVersionError());
		
		assertEquals(VersionSegment.MICRO, settings.getMicroVersionIncrement().getTargetSegment());
		assertEquals(100, settings.getMicroVersionIncrement().getIncrementAmount());
		assertEquals(ErrorMode.ERROR, settings.getMicroVersionError());
	}
	
	/**
	 * Test parsing with whitespace variations
	 */
	public void testParseWithWhitespace() throws IOException {
		String config = "  major.version.increment  =  minor + 1  \n" +
				"minor.version.increment=micro+100\n";
		
		ApiConfigSettings settings = parseConfig(config);
		assertNotNull(settings);
		
		assertEquals(VersionSegment.MINOR, settings.getMajorVersionIncrement().getTargetSegment());
		assertEquals(1, settings.getMajorVersionIncrement().getIncrementAmount());
		
		assertEquals(VersionSegment.MICRO, settings.getMinorVersionIncrement().getTargetSegment());
		assertEquals(100, settings.getMinorVersionIncrement().getIncrementAmount());
	}
	
	/**
	 * Test parsing with invalid lines (should be ignored)
	 */
	public void testParseWithInvalidLines() throws IOException {
		String config = "major.version.increment = minor+1\n" +
				"invalid line without equals\n" +
				"minor.version.increment = minor+5\n";
		
		ApiConfigSettings settings = parseConfig(config);
		assertNotNull(settings);
		
		// Valid lines should be parsed
		assertEquals(VersionSegment.MINOR, settings.getMajorVersionIncrement().getTargetSegment());
		assertEquals(1, settings.getMajorVersionIncrement().getIncrementAmount());
		
		assertEquals(VersionSegment.MINOR, settings.getMinorVersionIncrement().getTargetSegment());
		assertEquals(5, settings.getMinorVersionIncrement().getIncrementAmount());
	}
	
	/**
	 * Helper method to parse a config string
	 */
	private ApiConfigSettings parseConfig(String config) throws IOException {
		try (InputStream is = new ByteArrayInputStream(config.getBytes(StandardCharsets.UTF_8))) {
			return ApiConfigParser.parse(is);
		}
	}
}
