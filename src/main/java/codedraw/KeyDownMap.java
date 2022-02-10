package codedraw;

import codedraw.events.KeyDownEventArgs;

import java.awt.event.KeyEvent;
import java.util.HashMap;

class KeyDownMap {
	public KeyDownMap(Event<KeyDownEventArgs> keyDownEvent) {
		this.event = keyDownEvent;
	}

	private final Event<KeyDownEventArgs> event;
	private final HashMap<Integer, Boolean> map = new HashMap<>();

	public void keyPress(KeyEvent keyEvent) {
		Integer keyCode = keyEvent.getExtendedKeyCode();

		if (!isKeyAlreadyPressed(keyCode)) {
			map.put(keyCode, true);
			event.invoke(new KeyDownEventArgs(keyEvent));
		}
	}

	public void keyRelease(KeyEvent keyEvent) {
		map.put(keyEvent.getExtendedKeyCode(), false);
	}

	private boolean isKeyAlreadyPressed(Integer extendedKeyCode) {
		return map.getOrDefault(extendedKeyCode, false);
	}
}