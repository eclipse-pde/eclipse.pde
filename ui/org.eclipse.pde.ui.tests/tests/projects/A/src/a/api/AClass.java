package a.api;

/**
 * Test class that references types from multiple bundles to exercise OSGi
 * visibility and JDT access rules.
 * <p>
 * Bundle A declares: {@code Import-Package: b.api, g.api}
 * <p>
 * With the correct PDE behavior (PR #2218), all bundles in the transitive
 * dependency closure are present on the compilation classpath:
 * <ul>
 * <li><b>Directly imported bundles</b> (B, G): exported packages are
 * {@code K_ACCESSIBLE}, non-exported packages caught by EXCLUDE_ALL_RULE
 * ({@code K_NON_ACCESSIBLE})</li>
 * <li><b>Transitive dependencies</b> (C, D, E, F, H): all-forbidden access
 * rules (single {@code **&#47;* K_NON_ACCESSIBLE} rule) — compiler can
 * resolve types but direct usage produces forbidden reference warnings
 * (configured via {@code forbiddenReference=warning})</li>
 * </ul>
 * <p>
 * At <b>OSGi runtime</b>, only directly imported packages (b.api, g.api)
 * would be accessible to A's classloader (OSGi Core R8 §3.9.4). The JDT
 * access rules enforce this visibility at compile time.
 */
public class AClass {
	// ---- Directly imported bundles (K_ACCESSIBLE for exported packages) ----

	// B exports b.api, A imports b.api → K_ACCESSIBLE → no marker
	public Object objectFromB_allowed = new b.api.MyObject();
	// B's b.internal is NOT exported → caught by EXCLUDE_ALL → forbidden warning
	public Object objectFromB_forbidden = new b.internal.MyObject();

	// G exports g.api, A imports g.api → K_ACCESSIBLE → no marker
	public Object objectFromG_allowed = new g.api.MyObject();
	// G's g.internal is NOT exported → caught by EXCLUDE_ALL → forbidden warning
	public Object objectFromG_forbidden = new g.internal.MyObject();

	// ---- Transitive dependencies (all-forbidden access rules) ----
	// These types CAN be resolved by the compiler (on classpath for type
	// hierarchy validation), but produce forbidden reference warnings because
	// their entries have only **/* K_NON_ACCESSIBLE access rules.
	// At OSGi runtime, A's classloader cannot load any of these types (§3.9.4).

	// C: Required by B (Require-Bundle: C, default visibility:=private §3.13.1)
	public Object objectFromC_forbidden1 = new c.api.MyObject();
	public Object objectFromC_forbidden2 = new c.internal.MyObject();

	// D: Package imported by B (Import-Package: d.api) — never re-exports §3.6.4
	public Object objectFromD_forbidden1 = new d.api.MyObject();
	public Object objectFromD_forbidden2 = new d.internal.MyObject();

	// E: Optionally required by B (Require-Bundle: E;resolution:=optional §3.7.5)
	public Object objectFromE_forbidden1 = new e.api.MyObject();
	public Object objectFromE_forbidden2 = new e.internal.MyObject();

	// F: Optionally imported by B (Import-Package: f.api;resolution:=optional)
	public Object objectFromF_forbidden1 = new f.api.MyObject();
	public Object objectFromF_forbidden2 = new f.internal.MyObject();

	// H: Optionally imported by G (Import-Package: h.api;resolution:=optional)
	public Object objectFromH_forbidden1 = new h.api.MyObject();
	public Object objectFromH_forbidden2 = new h.internal.MyObject();
}
