package com.julianh06.bettermenubuttons.config;

public enum ButtonPosition {
	VANILLA,
	LEFT_OF_MENU,
	RIGHT_OF_MENU,
	LEFT_ROW_1,
	RIGHT_ROW_1,
	LEFT_ROW_2,
	RIGHT_ROW_2,
	LEFT_ROW_3,
	RIGHT_ROW_3,
	LEFT_ROW_4,
	RIGHT_ROW_4,
	LEFT_ROW_5,
	RIGHT_ROW_5,
	ICON_SLOT_1,
	ICON_SLOT_2,
	ICON_SLOT_3,
	ICON_SLOT_4,
	ICON_SLOT_5,
	BELOW_MENU,
	TOP_LEFT,
	TOP_RIGHT,
	BOTTOM_LEFT,
	BOTTOM_RIGHT,
	HIDDEN;

	public static ButtonPosition row(int row, boolean left) {
		return switch (row) {
			case 0 -> left ? LEFT_ROW_1 : RIGHT_ROW_1;
			case 1 -> left ? LEFT_ROW_2 : RIGHT_ROW_2;
			case 2 -> left ? LEFT_ROW_3 : RIGHT_ROW_3;
			case 3 -> left ? LEFT_ROW_4 : RIGHT_ROW_4;
			default -> left ? LEFT_ROW_5 : RIGHT_ROW_5;
		};
	}

	public boolean isSide() {
		return this.isLeftSide() || this.isRightSide();
	}

	public boolean isLeftSide() {
		return switch (this) {
			case LEFT_OF_MENU, LEFT_ROW_1, LEFT_ROW_2, LEFT_ROW_3, LEFT_ROW_4, LEFT_ROW_5 -> true;
			default -> false;
		};
	}

	public boolean isRightSide() {
		return switch (this) {
			case RIGHT_OF_MENU, RIGHT_ROW_1, RIGHT_ROW_2, RIGHT_ROW_3, RIGHT_ROW_4, RIGHT_ROW_5 -> true;
			default -> false;
		};
	}

	public boolean isCorner() {
		return this.isLeftCorner() || this.isRightCorner();
	}

	public boolean isLeftCorner() {
		return this == TOP_LEFT || this == BOTTOM_LEFT;
	}

	public boolean isRightCorner() {
		return this == TOP_RIGHT || this == BOTTOM_RIGHT;
	}
}