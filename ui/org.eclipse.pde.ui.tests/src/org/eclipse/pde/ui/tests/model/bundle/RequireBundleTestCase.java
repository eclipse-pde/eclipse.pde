package org.eclipse.pde.ui.tests.model.bundle;

import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.RequireBundleHeader;
import org.eclipse.pde.internal.core.text.bundle.RequireBundleObject;
import org.eclipse.text.edits.TextEdit;
import org.osgi.framework.Constants;

import junit.framework.Test;
import junit.framework.TestSuite;

public class RequireBundleTestCase extends MultiLineHeaderTestCase {
	
	public RequireBundleTestCase() {
		super(Constants.REQUIRE_BUNDLE);
	}
	
	public static Test suite() {
		return new TestSuite(RequireBundleTestCase.class);
	}

	public void testAddRequireBundleHeader() throws Exception {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		fDocument.set(buffer.toString());
		load(true);
		fModel.getBundle().setHeader(Constants.REQUIRE_BUNDLE, "com.example.abc");
		
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.REQUIRE_BUNDLE);
		assertNotNull(header);	
		assertEquals("Require-Bundle: com.example.abc\n", header.write());
		
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);
		
		ops[0].apply(fDocument);
		assertEquals(buffer.toString() + header.write(), fDocument.get());
	}
	
	public void testRemoveExistingRequireBundleHeader() throws Exception {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Require-Bundle: com.example.abc\n");
		fDocument.set(buffer.toString());
		load(true);
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.REQUIRE_BUNDLE);
		assertNotNull(header);
		
		((RequireBundleHeader)header).removeBundle("com.example.abc");
		
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);
		
		ops[0].apply(fDocument);
		assertEquals(4, fDocument.getNumberOfLines());	
	}
	
	public void testAddBundle() throws Exception {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Require-Bundle: com.example.abc\n");
		fDocument.set(buffer.toString());
		load(true);
		
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.REQUIRE_BUNDLE);
		((RequireBundleHeader)header).addBundle("com.example.core");
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);
		
		ops[0].apply(fDocument);
		assertEquals(6, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(5));
		
		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineLength(3) + fDocument.getLineLength(4);
		
		StringBuffer expected = new StringBuffer("Require-Bundle: com.example.abc,\n");
		expected.append(" com.example.core\n");
		assertEquals(expected.toString(), fDocument.get(pos, length));
	}
	
	public void testAddMultipleBundles() throws Exception {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Require-Bundle: com.example.abc\n");
		fDocument.set(buffer.toString());
		load(true);
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.REQUIRE_BUNDLE);
		((RequireBundleHeader)header).addBundle("com.example.core");
		((RequireBundleHeader)header).addBundle("com.example.ui");
		((RequireBundleHeader)header).addBundle("com.example");
		
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);
		
		ops[0].apply(fDocument);
		assertEquals(8, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(7));
		
		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineOffset(7) - fDocument.getLineOffset(3);
		StringBuffer expected = new StringBuffer("Require-Bundle: com.example.abc,\n");
		expected.append(" com.example.core,\n");
		expected.append(" com.example.ui,\n");
		expected.append(" com.example\n");
		assertEquals(expected.toString(), fDocument.get(pos, length));		
	}
	
	public void testRemoveBundle() throws Exception {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Require-Bundle: com.example.abc,\n");
		buffer.append(" com.example.core,\n");
		buffer.append(" com.example.ui\n");
		fDocument.set(buffer.toString());
		load(true);
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.REQUIRE_BUNDLE);
		((RequireBundleHeader)header).removeBundle("com.example.core");
		
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);
		
		ops[0].apply(fDocument);
		assertEquals(6, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(5));
		
		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineOffset(5) - fDocument.getLineOffset(3);
		StringBuffer expected = new StringBuffer("Require-Bundle: com.example.abc,\n");
		expected.append(" com.example.ui\n");
		assertEquals(expected.toString(), fDocument.get(pos, length));			
	}
	
	public void testRemoveMultipleBundles() throws Exception {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Require-Bundle: com.example.abc,\n");
		buffer.append(" com.example.core,\n");
		buffer.append(" com.example.ui\n");
		fDocument.set(buffer.toString());
		load(true);
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.REQUIRE_BUNDLE);
		((RequireBundleHeader)header).removeBundle("com.example.core");
		((RequireBundleHeader)header).removeBundle("com.example.abc");
		
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);
		
		ops[0].apply(fDocument);
		assertEquals(5, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(4));
		
		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineLength(3);		
		assertEquals("Require-Bundle: com.example.ui\n", fDocument.get(pos, length));			
	}
	
	public void testReadOptionalBundle() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Require-Bundle: com.example.abc;resolution:=optional\n");
		fDocument.set(buffer.toString());
		load();
		
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.REQUIRE_BUNDLE);
		assertNotNull(header);
		
		RequireBundleObject bundle = getBundle((RequireBundleHeader)header, "com.example.abc");
		assertNotNull(bundle);
		assertTrue(bundle.isOptional());
	}
	
	public void testReadBundleWithVersion() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Require-Bundle: org.eclipse.osgi;bundle-version=\"3.2.0\"\n");
		fDocument.set(buffer.toString());
		load();
		
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.REQUIRE_BUNDLE);
		assertNotNull(header);
		
		RequireBundleObject bundle = getBundle((RequireBundleHeader)header, "org.eclipse.osgi");
		assertNotNull(bundle);
		assertEquals("3.2.0", bundle.getVersion());		
	}
	
	public void testMakeBundleOptional() throws Exception {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Require-Bundle: org.eclipse.osgi\n");
		fDocument.set(buffer.toString());
		load(true);
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.REQUIRE_BUNDLE);
		assertNotNull(header);

		RequireBundleObject bundle = getBundle((RequireBundleHeader)header, "org.eclipse.osgi");
		assertNotNull(bundle);
		bundle.setOptional(true);
		
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);
		
		ops[0].apply(fDocument);
		assertEquals(5, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(4));
		
		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineLength(3);		
		assertEquals("Require-Bundle: org.eclipse.osgi;resolution:=optional\n", fDocument.get(pos, length));			
	}
	
	public void testRemoveOptionalDirective() throws Exception {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Require-Bundle: org.eclipse.osgi;resolution:=optional\n");
		fDocument.set(buffer.toString());
		load(true);
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.REQUIRE_BUNDLE);
		assertNotNull(header);

		RequireBundleObject object = getBundle((RequireBundleHeader)header, "org.eclipse.osgi");
		assertNotNull(object);
		object.setOptional(false);
		
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);
		
		ops[0].apply(fDocument);
		assertEquals(5, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(4));
		
		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineLength(3);		
		assertEquals("Require-Bundle: org.eclipse.osgi\n", fDocument.get(pos, length));			
	}
	
	public void testAddVersionToBundle() throws Exception {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Require-Bundle: org.eclipse.osgi\n");
		fDocument.set(buffer.toString());
		load(true);
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.REQUIRE_BUNDLE);
		assertNotNull(header);

		RequireBundleObject object = getBundle((RequireBundleHeader)header, "org.eclipse.osgi");
		assertNotNull(object);
		object.setVersion("3.2.0");
		
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);
		
		ops[0].apply(fDocument);
		assertEquals(5, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(4));
		
		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineLength(3);		
		assertEquals("Require-Bundle: org.eclipse.osgi;bundle-version=\"3.2.0\"\n", fDocument.get(pos, length));					
	}
	
	public void testRemoveVersionFromBundle() throws Exception {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Require-Bundle: org.eclipse.osgi\n");
		fDocument.set(buffer.toString());
		load(true);
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.REQUIRE_BUNDLE);
		assertNotNull(header);

		RequireBundleObject object = getBundle((RequireBundleHeader)header, "org.eclipse.osgi");
		assertNotNull(object);
		object.setVersion(null);
		
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);
		
		ops[0].apply(fDocument);
		assertEquals(5, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(4));
		
		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineLength(3);		
		assertEquals("Require-Bundle: org.eclipse.osgi\n", fDocument.get(pos, length));					
	}
	
	public void testAddBundleWithWindowsDelimiter() throws Exception {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\r\n");
		buffer.append("Bundle-ManifestVersion: 2\r\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\r\n");
		buffer.append("Require-Bundle: com.example.abc\r\n");
		fDocument.set(buffer.toString());
		load(true);
		
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.REQUIRE_BUNDLE);
		((RequireBundleHeader)header).addBundle("com.example.core");
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);
		
		ops[0].apply(fDocument);
		assertEquals(6, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(5));
		
		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineLength(3) + fDocument.getLineLength(4);
		
		StringBuffer expected = new StringBuffer("Require-Bundle: com.example.abc,\r\n");
		expected.append(" com.example.core\r\n");
		assertEquals(expected.toString(), fDocument.get(pos, length));		
	}
	
	public void testRemoveBundleWithWindowsDelimiter() throws Exception {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\r\n");
		buffer.append("Bundle-ManifestVersion: 2\r\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\r\n");
		buffer.append("Require-Bundle: com.example.abc,\r\n");
		buffer.append(" com.example.core,\r\n");
		buffer.append(" com.example.ui\r\n");
		fDocument.set(buffer.toString());
		load(true);
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.REQUIRE_BUNDLE);
		((RequireBundleHeader)header).removeBundle("com.example.core");
		
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);
		
		ops[0].apply(fDocument);
		assertEquals(6, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(5));
		
		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineOffset(5) - fDocument.getLineOffset(3);
		StringBuffer expected = new StringBuffer("Require-Bundle: com.example.abc,\r\n");
		expected.append(" com.example.ui\r\n");
		assertEquals(expected.toString(), fDocument.get(pos, length));					
	}
	
	public void testPreserveSpacing() throws Exception {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Require-Bundle: \n");
		buffer.append(" com.example.abc\n");
		fDocument.set(buffer.toString());
		load(true);
		
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.REQUIRE_BUNDLE);
		((RequireBundleHeader)header).addBundle("com.example.core");
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);
		
		ops[0].apply(fDocument);
		assertEquals(7, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(6));
		
		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineOffset(6) - fDocument.getLineOffset(3);
		
		StringBuffer expected = new StringBuffer("Require-Bundle: \n");
		expected.append(" com.example.abc,\n");
		expected.append(" com.example.core\n");
		assertEquals(expected.toString(), fDocument.get(pos, length));
		
	}
	
	public void testReadBundleReExport() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Require-Bundle: com.example.abc;visibility:=reexport\n");
		fDocument.set(buffer.toString());
		load();
		
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.REQUIRE_BUNDLE);
		assertNotNull(header);
		
		RequireBundleObject bundle = getBundle((RequireBundleHeader)header, "com.example.abc");
		assertNotNull(bundle);
		assertTrue(bundle.isReexported());
	}
	
	public void testMakeBundleReExport() throws Exception{
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Require-Bundle: org.eclipse.osgi\n");
		fDocument.set(buffer.toString());
		load(true);
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.REQUIRE_BUNDLE);
		assertNotNull(header);

		RequireBundleObject bundle = getBundle((RequireBundleHeader)header, "org.eclipse.osgi");
		assertNotNull(bundle);
		bundle.setReexported(true);
		
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);
		
		ops[0].apply(fDocument);
		assertEquals(5, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(4));
		
		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineLength(3);		
		assertEquals("Require-Bundle: org.eclipse.osgi;visibility:=reexport\n", fDocument.get(pos, length));			
	}
	
	public void testRemoveReExport() throws Exception {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Require-Bundle: org.eclipse.osgi;visibility:=reexport\n");
		fDocument.set(buffer.toString());
		load(true);
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.REQUIRE_BUNDLE);
		assertNotNull(header);

		RequireBundleObject object = getBundle((RequireBundleHeader)header, "org.eclipse.osgi");
		assertNotNull(object);
		object.setReexported(false);
		
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);
		
		ops[0].apply(fDocument);
		assertEquals(5, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(4));
		
		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineLength(3);		
		assertEquals("Require-Bundle: org.eclipse.osgi\n", fDocument.get(pos, length));			
	}
	
	private static RequireBundleObject getBundle(RequireBundleHeader header, String id) {
		RequireBundleObject[] bundles = header.getRequiredBundles();
		for (int i = 0; i < bundles.length; i++)
			if (bundles[i].getId().equals(id))
				return bundles[i];
		return null;
	}

}
