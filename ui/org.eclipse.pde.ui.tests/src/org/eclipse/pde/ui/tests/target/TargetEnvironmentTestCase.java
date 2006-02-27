/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.target;

import java.util.Dictionary;

import org.eclipse.core.runtime.Platform;
import org.eclipse.pde.internal.core.ExecutionEnvironmentAnalyzer;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.osgi.framework.Constants;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TargetEnvironmentTestCase extends TestCase {

	public static Test suite() {
		return new TestSuite(TargetEnvironmentTestCase.class);
	}
	
	public void testOS() {
		assertEquals(Platform.getOS(), TargetPlatform.getOS());
	}
	
	public void testWS() {
		assertEquals(Platform.getWS(), TargetPlatform.getWS());
	}
	
	public void testArch() {
		assertEquals(Platform.getOSArch(), TargetPlatform.getOSArch());
	}
	
	public void testNL() {
		assertEquals(Platform.getNL(), TargetPlatform.getNL());
	}
	
	public void testEnvironmentDictionarySize() {
		Dictionary dictionary = TargetPlatform.getTargetEnvironment();
		assertEquals(5, dictionary.size());
	}
	
	public void testDictionaryOS() {
		Dictionary dictionary = TargetPlatform.getTargetEnvironment();
		assertEquals(Platform.getOS(), dictionary.get("osgi.os"));
	}

	public void testDictionaryWS() {
		Dictionary dictionary = TargetPlatform.getTargetEnvironment();
		assertEquals(Platform.getWS(), dictionary.get("osgi.ws"));
	}
	
	public void testDictionaryArch() {
		Dictionary dictionary = TargetPlatform.getTargetEnvironment();
		assertEquals(Platform.getOSArch(), dictionary.get("osgi.arch"));
	}
	
	public void testDictionaryNL() {
		Dictionary dictionary = TargetPlatform.getTargetEnvironment();
		assertEquals(Platform.getNL(), dictionary.get("osgi.nl"));
	}
	
	public void testResolveOptional() {
		Dictionary dictionary = TargetPlatform.getTargetEnvironment();
		assertTrue("true".equals(dictionary.get("osgi.resolveOptional")));		
	}
	
	public void testStateDictionaryNumber() {
		Dictionary[] dictionaries = TargetPlatform.getState().getPlatformProperties();
		String[] envs = ExecutionEnvironmentAnalyzer.getKnownExecutionEnvironments();
		assertEquals(envs.length, dictionaries.length);
	}
	
	public void testStateDictionaryLength() {
		Dictionary[] dictionaries = TargetPlatform.getState().getPlatformProperties();
		Dictionary dictionary = TargetPlatform.getTargetEnvironment();
		for (int i = 0; i < dictionaries.length; i++) 
			assertTrue(dictionary.size() + 2 <= dictionaries[i].size());
	}

	public void testSystemPackages() {
		Dictionary[] dictionaries = TargetPlatform.getState().getPlatformProperties();
		for (int i = 0; i < dictionaries.length; i++) 
			assertNotNull(dictionaries[i].get(Constants.FRAMEWORK_SYSTEMPACKAGES));
	}

	public void testExecutionEnvironment() {
		Dictionary[] dictionaries = TargetPlatform.getState().getPlatformProperties();
		for (int i = 0; i < dictionaries.length; i++) 
			assertNotNull(dictionaries[i].get(Constants.FRAMEWORK_EXECUTIONENVIRONMENT));
	}

}
