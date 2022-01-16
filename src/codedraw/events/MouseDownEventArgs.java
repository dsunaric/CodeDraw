package codedraw.events;

import java.awt.event.MouseEvent;

public class MouseDownEventArgs {
	public MouseDownEventArgs(MouseEvent e) {
		this.e = e;
	}

	private MouseEvent e;

	public int getX() {
		return e.getX();
	}

	public int getY() {
		return e.getY();
	}

	public MouseButton getMouseButton() {
		return MouseButton.values()[e.getButton() - 1];
	}
}