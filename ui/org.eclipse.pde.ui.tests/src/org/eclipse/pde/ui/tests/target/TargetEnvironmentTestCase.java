/*******************************************************************************
 *  Copyright (c) 2006, 2012 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.target;

import java.util.Dictionary;
import java.util.Properties;
import junit.framework.*;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.osgi.framework.Constants;

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
		Dictionary dictionary = TargetPlatformHelper.getTargetEnvironment();
		assertEquals(6, dictionary.size());
	}

	public void testDictionaryOS() {
		Dictionary dictionary = TargetPlatformHelper.getTargetEnvironment();
		assertEquals(Platform.getOS(), dictionary.get("osgi.os"));
	}

	public void testDictionaryWS() {
		Dictionary dictionary = TargetPlatformHelper.getTargetEnvironment();
		assertEquals(Platform.getWS(), dictionary.get("osgi.ws"));
	}

	public void testDictionaryArch() {
		Dictionary dictionary = TargetPlatformHelper.getTargetEnvironment();
		assertEquals(Platform.getOSArch(), dictionary.get("osgi.arch"));
	}

	public void testDictionaryNL() {
		Dictionary dictionary = TargetPlatformHelper.getTargetEnvironment();
		assertEquals(Platform.getNL(), dictionary.get("osgi.nl"));
	}

	public void testResolveOptional() {
		Dictionary dictionary = TargetPlatformHelper.getTargetEnvironment();
		assertTrue("true".equals(dictionary.get("osgi.resolveOptional")));
	}
	
	/**
	 * Tests that the OSGi state for the PDE models has the correct properties set, based on known execution environments
	 */
	public void testStateEEProperties() {
		Dictionary[] platformProps = TargetPlatformHelper.getState().getPlatformProperties();

		String[] profiles = TargetPlatformHelper.getKnownExecutionEnvironments();
		for (int i = 0; i < profiles.length; i++) {
			IExecutionEnvironment environment = JavaRuntime.getExecutionEnvironmentsManager().getEnvironment(profiles[i]);
			if (environment != null) {
				Properties profileProps = environment.getProfileProperties();
				if (profileProps != null) {
					// If we have profile properties for an execution environment, ensure that they were added to the state
					String systemPackages = profileProps.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES);
					if (systemPackages != null){
						boolean foundSystemPackage = false;
						for (int j = 0; j < platformProps.length; j++) {
							if (systemPackages.equals(platformProps[j].get(Constants.FRAMEWORK_SYSTEMPACKAGES))){
								foundSystemPackage = true;
								break;
							}
						}
						if (!foundSystemPackage){
							fail("The system packages property for EE " + profiles[i] + " was not found in the state's propeties");
						}
					}
					String ee = profileProps.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT);
					if (ee != null){
						boolean foundEE = false;
						for (int j = 0; j < platformProps.length; j++) {
							if (ee.equals(platformProps[j].get(Constants.FRAMEWORK_EXECUTIONENVIRONMENT))){
								foundEE = true;
								break;
							}
						}
						if (!foundEE){
							fail("The framework EE property for EE " + profiles[i] + " was not found in the state's propeties");
						}
					}
				}
			}
		}
	}

}
