package org.petero.droidfish;

/** Settings controlling PGN import/export */
public class PGNOptions {
	public static class Import {
		public boolean variations;
		public boolean comments;
		public boolean nag;
	}
	public static class Export {
		public boolean variations;
		public boolean comments;
		public boolean nag;
		public boolean userCmd;
		public boolean clockInfo;
	}

	public Import imp;
	public Export exp;

	public PGNOptions() {
		imp = new Import();
		exp = new Export();
	}
}
