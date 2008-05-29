public class X extends Y {
	public boolean equals(Object obj) {
		return obj instanceof X;
	}
	public int hashCode() {
		return 0;
	}
}
