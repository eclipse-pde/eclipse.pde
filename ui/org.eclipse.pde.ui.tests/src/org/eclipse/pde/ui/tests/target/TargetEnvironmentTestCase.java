/*******************************************************************************
 *  Copyright (c) 2006, 2018 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.target;

import java.util.Dictionary;
import java.util.Properties;
import junit.framework.TestCase;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.osgi.framework.Constants;

public class TargetEnvironmentTestCase extends TestCase {

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
		Dictionary<String, String> dictionary = TargetPlatformHelper.getTargetEnvironment();
		assertEquals(6, dictionary.size());
	}

	public void testDictionaryOS() {
		Dictionary<String, String> dictionary = TargetPlatformHelper.getTargetEnvironment();
		assertEquals(Platform.getOS(), dictionary.get("osgi.os"));
	}

	public void testDictionaryWS() {
		Dictionary<String, String> dictionary = TargetPlatformHelper.getTargetEnvironment();
		assertEquals(Platform.getWS(), dictionary.get("osgi.ws"));
	}

	public void testDictionaryArch() {
		Dictionary<String, String> dictionary = TargetPlatformHelper.getTargetEnvironment();
		assertEquals(Platform.getOSArch(), dictionary.get("osgi.arch"));
	}

	public void testDictionaryNL() {
		Dictionary<String, String> dictionary = TargetPlatformHelper.getTargetEnvironment();
		assertEquals(Platform.getNL(), dictionary.get("osgi.nl"));
	}

	public void testResolveOptional() {
		Dictionary<String, String> dictionary = TargetPlatformHelper.getTargetEnvironment();
		assertTrue("true".equals(dictionary.get("osgi.resolveOptional")));
	}

	/**
	 * Tests that the OSGi state for the PDE models has the correct properties set, based on known execution environments
	 */
	@SuppressWarnings("deprecation")
	public void testStateEEProperties() {
		Dictionary<?, ?>[] platformProps = TargetPlatformHelper.getState().getPlatformProperties();

		String[] profiles = TargetPlatformHelper.getKnownExecutionEnvironments();
		for (String profile : profiles) {
			IExecutionEnvironment environment = JavaRuntime.getExecutionEnvironmentsManager().getEnvironment(profile);
			if (environment != null) {
				Properties profileProps = environment.getProfileProperties();
				if (profileProps != null) {
					// If we have profile properties for an execution environment, ensure that they were added to the state
					String systemPackages = profileProps.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES);
					if (systemPackages != null){
						boolean foundSystemPackage = false;
						for (Dictionary<?, ?> platformProp : platformProps) {
							if (systemPackages.equals(platformProp.get(Constants.FRAMEWORK_SYSTEMPACKAGES))){
								foundSystemPackage = true;
								break;
							}
						}
						if (!foundSystemPackage){
							fail("The system packages property for EE " + profile + " was not found in the state's propeties");
						}
					}
					String ee = profileProps.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT);
					if (ee != null){
						boolean foundEE = false;
						for (Dictionary<?, ?> platformProp : platformProps) {
							if (ee.equals(platformProp.get(Constants.FRAMEWORK_EXECUTIONENVIRONMENT))){
								foundEE = true;
								break;
							}
						}
						if (!foundEE){
							fail("The framework EE property for EE " + profile + " was not found in the state's propeties");
						}
					}
				}
			}
		}
	}

}
