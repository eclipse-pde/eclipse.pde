package a.api;

/**
 * Test consumer that exercises OSGi visibility and JDT access rules.
 * <p>
 * Bundle A imports: {@code b.api}, {@code g.api}. The {@code b.api.MyObject}
 * type has a rich type hierarchy ({@code extends c.api.MyObject implements
 * d.api.Processor}) that requires transitive dependencies on the compilation
 * classpath for type hierarchy resolution.
 * <p>
 * <b>Accessible API usage:</b> Methods inherited from the transitive type
 * hierarchy are called through {@code b.api.MyObject} ({@code K_ACCESSIBLE}),
 * exercising the compiler's type resolution without producing forbidden
 * markers. This models the real-world scenario from issue #2195 where
 * {@code IWidgetValueProperty extends IValueProperty} required the databinding
 * bundle on the classpath.
 * <p>
 * <b>Forbidden references:</b> Direct references to types from non-imported
 * packages produce forbidden reference markers, validating that PDE's access
 * rules correctly enforce OSGi module layer visibility at compile time.
 */
public class AClass {
	// ---- Directly imported bundles (K_ACCESSIBLE for exported packages) ----

	// b.api.MyObject extends c.api.MyObject implements d.api.Processor.
	// The compiler needs transitive types (c.api.*, d.api.*) for type
	// hierarchy resolution — these are present as transitive forbidden deps.
	public b.api.MyObject service = new b.api.MyObject();

	// Calls inherited methods from c.api.Configurable via c.api.MyObject.
	// No markers: accessed through b.api.MyObject (K_ACCESSIBLE).
	public boolean configureAndVerify() {
		service.configure("production");
		return service.isConfigured();
	}

	// Inherited from c.api.MyObject — compiler resolves c.api.MyObject
	// from the transitive forbidden classpath entry.
	public Object retrieveConfig() {
		return service.getConfig();
	}

	// From d.api.Processor interface (implemented by b.api.MyObject).
	// Compiler resolves d.api.Processor from transitive forbidden entry.
	public Object processItem(String input) {
		return service.process(input);
	}

	// b.api method using d.api.MyObject in its signature — compiler
	// resolves d.api.MyObject from the transitive forbidden classpath entry.
	public Object processData() {
		return service.processData(null);
	}

	// b.api method returning g.api.MyObject (G is reexported by B).
	// g.api is directly imported by A, so no marker.
	public g.api.MyObject createViaService() {
		return service.createService();
	}

	// B's b.internal is NOT exported → caught by EXCLUDE_ALL → forbidden
	public Object objectFromB_forbidden = new b.internal.MyObject();

	// G exports g.api, A imports g.api → K_ACCESSIBLE → no marker
	public g.api.MyObject gService = new g.api.MyObject();
	// g.api.MyObject.describe() internally uses h.api (optional dep of G)
	public String description = gService.describe();

	// G's g.internal is NOT exported → caught by EXCLUDE_ALL → forbidden
	public Object objectFromG_forbidden = new g.internal.MyObject();

	// ---- Transitive dependencies (all-forbidden access rules) ----
	// Direct references to forbidden types produce forbidden reference markers.
	// At OSGi runtime, A's classloader cannot load these types (§3.9.4).

	// C: Required by B (Require-Bundle: C, visibility:=private §3.13.1)
	public Object objectFromC_forbidden1 = new c.api.MyObject();
	public Object objectFromC_forbidden2 = new c.internal.MyObject();

	// D: Imported by B (Import-Package: d.api) — never re-exports §3.6.4
	public Object objectFromD_forbidden1 = new d.api.MyObject();
	public Object objectFromD_forbidden2 = new d.internal.MyObject();

	// E: Optionally required by B (Require-Bundle: E;optional §3.7.5)
	public Object objectFromE_forbidden1 = new e.api.MyObject();
	public Object objectFromE_forbidden2 = new e.internal.MyObject();

	// F: Optionally imported by B (Import-Package: f.api;optional)
	public Object objectFromF_forbidden1 = new f.api.MyObject();
	public Object objectFromF_forbidden2 = new f.internal.MyObject();

	// H: Optionally imported by G (Import-Package: h.api;optional)
	public Object objectFromH_forbidden1 = new h.api.MyObject();
	public Object objectFromH_forbidden2 = new h.internal.MyObject();
}
