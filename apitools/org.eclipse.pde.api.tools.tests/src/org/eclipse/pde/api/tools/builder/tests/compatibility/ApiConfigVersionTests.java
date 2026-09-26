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
package org.eclipse.pde.api.tools.builder.tests.compatibility;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.pde.api.tools.internal.ApiConfigParser;
import org.eclipse.pde.api.tools.internal.ApiConfigSettings;
import org.eclipse.pde.api.tools.internal.IApiCoreConstants;

import junit.framework.TestCase;

/**
 * Tests for .apiconfig file integration with version checking
 */
public class ApiConfigVersionTests extends TestCase {
	
	public ApiConfigVersionTests(String name) {
		super(name);
	}
	
	/**
	 * Test that ApiConfigSettings can be created and have correct defaults
	 */
	public void testDefaultSettings() {
		ApiConfigSettings settings = new ApiConfigSettings();
		
		assertNotNull("Settings should not be null", settings);
		assertEquals("Default major increment should be MAJOR+1", 
				ApiConfigSettings.VersionSegment.MAJOR, 
				settings.getMajorVersionIncrement().getTargetSegment());
		assertEquals("Default major increment amount should be 1", 
				1, 
				settings.getMajorVersionIncrement().getIncrementAmount());
		
		assertEquals("Default minor increment should be MINOR+1", 
				ApiConfigSettings.VersionSegment.MINOR, 
				settings.getMinorVersionIncrement().getTargetSegment());
		assertEquals("Default minor increment amount should be 1", 
				1, 
				settings.getMinorVersionIncrement().getIncrementAmount());
		
		assertEquals("Default micro increment should be MICRO+1", 
				ApiConfigSettings.VersionSegment.MICRO, 
				settings.getMicroVersionIncrement().getTargetSegment());
		assertEquals("Default micro increment amount should be 1", 
				1, 
				settings.getMicroVersionIncrement().getIncrementAmount());
	}
	
	/**
	 * Test that ApiConfigSettings can be configured with custom increments
	 */
	public void testCustomIncrements() {
		ApiConfigSettings settings = new ApiConfigSettings();
		
		// Configure major to increment minor instead (like Eclipse Platform)
		ApiConfigSettings.VersionIncrementRule majorRule = 
			new ApiConfigSettings.VersionIncrementRule(ApiConfigSettings.VersionSegment.MINOR, 1);
		settings.setMajorVersionIncrement(majorRule);
		
		// Configure micro to increment by 100
		ApiConfigSettings.VersionIncrementRule microRule = 
			new ApiConfigSettings.VersionIncrementRule(ApiConfigSettings.VersionSegment.MICRO, 100);
		settings.setMicroVersionIncrement(microRule);
		
		assertEquals("Major should increment MINOR+1", 
				ApiConfigSettings.VersionSegment.MINOR, 
				settings.getMajorVersionIncrement().getTargetSegment());
		
		assertEquals("Micro should increment MICRO+100", 
				100, 
				settings.getMicroVersionIncrement().getIncrementAmount());
	}
	
	/**
	 * Test that error modes can be configured
	 */
	public void testErrorModes() {
		ApiConfigSettings settings = new ApiConfigSettings();
		
		settings.setMajorVersionError(ApiConfigSettings.ErrorMode.FILTER);
		settings.setMinorVersionError(ApiConfigSettings.ErrorMode.WARNING);
		settings.setMicroVersionError(ApiConfigSettings.ErrorMode.IGNORE);
		
		assertEquals("Major error mode should be FILTER", 
				ApiConfigSettings.ErrorMode.FILTER, 
				settings.getMajorVersionError());
		assertEquals("Minor error mode should be WARNING", 
				ApiConfigSettings.ErrorMode.WARNING, 
				settings.getMinorVersionError());
		assertEquals("Micro error mode should be IGNORE", 
				ApiConfigSettings.ErrorMode.IGNORE, 
				settings.getMicroVersionError());
	}
	
	/**
	 * Test that .apiconfig constant is defined
	 */
	public void testApiConfigConstant() {
		assertNotNull("API_CONFIG_FILE_NAME constant should be defined", 
				IApiCoreConstants.API_CONFIG_FILE_NAME);
		assertEquals(".apiconfig file name should be correct", 
				".apiconfig", 
				IApiCoreConstants.API_CONFIG_FILE_NAME);
	}
}
