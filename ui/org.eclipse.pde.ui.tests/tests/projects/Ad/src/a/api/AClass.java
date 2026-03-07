package a.api;

/**
 * Test class that references only exported API from direct dependencies,
 * plus both public and x-internal packages from bundle X.
 * <p>
 * Bundle Ad declares:
 * {@code Import-Package: b.api, g.api, x.api, x.internal}
 * <p>
 * Expected markers:
 * <ul>
 * <li>Lines 26, 29: b.api.MyObject, g.api.MyObject → {@code K_ACCESSIBLE}
 * → <b>no markers</b></li>
 * <li>Line 32: x.api.MyObject → {@code K_ACCESSIBLE} (normal export)
 * → <b>no marker</b></li>
 * <li>Line 35: x.internal.MyObject → {@code K_DISCOURAGED}
 * ({@code x-internal:=true} on Export-Package) → <b>discouraged access
 * warning</b></li>
 * </ul>
 * <p>
 * No transitive dependency types are referenced — Ad only uses directly
 * imported packages. This validates that exported API from direct
 * dependencies produces zero markers, and that x-internal packages produce
 * discouraged (not forbidden) markers.
 */
public class AClass {
	// ---- Directly imported bundles: exported API only ----

	// B exports b.api, Ad imports b.api → K_ACCESSIBLE → no marker
	public Object objectFromB_allowed = new b.api.MyObject();

	// G exports g.api, Ad imports g.api → K_ACCESSIBLE → no marker
	public Object objectFromG_allowed = new g.api.MyObject();

	// X exports x.api (normal), Ad imports x.api → K_ACCESSIBLE → no marker
	public Object objectFromX_allowed = new x.api.MyObject();

	// X exports x.internal with x-internal:=true, Ad imports x.internal
	// → K_DISCOURAGED → discouraged access warning
	public Object objectFromX_discouraged = new x.internal.MyObject();
}
