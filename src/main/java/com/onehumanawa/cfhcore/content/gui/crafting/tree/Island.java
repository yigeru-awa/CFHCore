package com.onehumanawa.cfhcore.content.gui.crafting.tree;

import java.util.List;

public final class Island {
	public final String a;
	public final String b;
	public boolean open;

	public Island(String first, String second) {
		boolean swap = first.compareTo(second) > 0;
		this.a = swap ? second : first;
		this.b = swap ? first : second;
	}

	public String top() {
		return TreePaths.common(this.a, this.b);
	}

	public List<String> bottoms() {
		String top = this.top();
		if (this.a.equals(top)) {
			return List.of(this.b);
		} else {
			return this.b.equals(top) ? List.of(this.a) : List.of(this.a, this.b);
		}
	}

	public boolean hides(String path) {
		return hides(this.top(), this.bottoms(), path);
	}

	static boolean hides(String top, List<String> bottoms, String path) {
		if (!TreePaths.under(top, path)) {
			return false;
		} else {
			for (String bottom : bottoms) {
				if (TreePaths.at(bottom, path)) {
					return false;
				}
			}

			return true;
		}
	}

	public String id() {
		return "island\n" + this.a + "\n\n" + this.b;
	}

	public boolean sameEnds(Island other) {
		return this.a.equals(other.a) && this.b.equals(other.b);
	}

	public Island.Relation to(Island other) {
		String top = this.top();
		String otherTop = other.top();
		List<String> bottoms = this.bottoms();
		List<String> otherBottoms = other.bottoms();
		if (!top.equals(otherTop)) {
			if (TreePaths.under(top, otherTop)) {
				return above(top, bottoms, otherTop, otherBottoms) ? Island.Relation.AROUND : (apart(bottoms, otherTop) ? Island.Relation.APART : Island.Relation.CROSSING);
			} else if (TreePaths.under(otherTop, top)) {
				return above(otherTop, otherBottoms, top, bottoms) ? Island.Relation.INSIDE : (apart(otherBottoms, top) ? Island.Relation.APART : Island.Relation.CROSSING);
			} else {
				return Island.Relation.APART;
			}
		} else {
			boolean otherInside = covers(otherBottoms, bottoms, otherTop);
			boolean thisInside = covers(bottoms, otherBottoms, top);
			return otherInside && thisInside
				? Island.Relation.SAME
				: (thisInside ? Island.Relation.INSIDE : (otherInside ? Island.Relation.AROUND : Island.Relation.CROSSING));
		}
	}

	private static boolean apart(List<String> upperBottoms, String lowerTop) {
		for (String bottom : upperBottoms) {
			if (TreePaths.at(bottom, lowerTop)) {
				return true;
			}
		}

		return false;
	}

	private static boolean above(String upperTop, List<String> upperBottoms, String lowerTop, List<String> lowerBottoms) {
		return !apart(upperBottoms, lowerTop) && covers(lowerBottoms, upperBottoms, lowerTop);
	}

	private static boolean covers(List<String> by, List<String> ends, String under) {
		for (String end : ends) {
			if (TreePaths.under(under, end)) {
				boolean covered = false;

				for (String cover : by) {
					if (TreePaths.at(cover, end)) {
						covered = true;
						break;
					}
				}

				if (!covered) {
					return false;
				}
			}
		}

		return true;
	}

	public static enum Relation {
		APART,
		INSIDE,
		AROUND,
		SAME,
		CROSSING;
	}
}
