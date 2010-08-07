package org.petero.droidfish;

/** Settings controlling PGN import/export */
public class PGNOptions {
	public static class Viewer {
		public boolean variations;
		public boolean comments;
		public boolean nag;
	}
	public static class Import {
		public boolean variations;
		public boolean comments;
		public boolean nag;
	}
	public static class Export {
		public boolean variations;
		public boolean comments;
		public boolean nag;
		public boolean playerAction;
		public boolean clockInfo;
	}

	public Viewer view;
	public Import imp;
	public Export exp;

	public PGNOptions() {
		view = new Viewer();
		imp = new Import();
		exp = new Export();
	}

	public PGNOptions(PGNOptions other) {
		view = new Viewer();
		imp = new Import();
		exp = new Export(); 
		view.variations  = other.view.variations;
		view.comments    = other.view.comments;
		view.nag         = other.view.nag;
		imp.variations   = other.imp.variations;
		imp.comments     = other.imp.comments;
		imp.nag          = other.imp.nag;
		exp.variations   = other.exp.variations;
		exp.comments     = other.exp.comments;
		exp.nag          = other.exp.nag;
		exp.playerAction = other.exp.playerAction;
		exp.clockInfo    = other.exp.clockInfo;
	}
}
