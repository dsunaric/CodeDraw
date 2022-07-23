package codedraw;

import codedraw.drawing.Image;
import codedraw.events.EventScanner;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

class CodeDrawGUI implements AutoCloseable {
	public static CodeDrawGUI createWindow(int width, int height) {
		CodeDrawGUI gui = new CodeDrawGUI(width, height);
		JFrame frame = gui.frame;

		frame.pack();
		gui.jFrameCorrector = new JFrameCorrector(frame, frame.getPreferredSize());
		frame.setLocationByPlatform(true);

		return gui.finishConstructor();
	}

	public static CodeDrawGUI createBorderlessWindow(int width, int height) {
		CodeDrawGUI gui = new CodeDrawGUI(width, height);
		JFrame frame = gui.frame;

		frame.setSize(width, height);
		frame.setUndecorated(true);
		frame.setLocationByPlatform(true);

		return gui.finishConstructor();
	}

	public static CodeDrawGUI createFullscreen(Screen screen) {
		if (!screen.canAttachGUI()) throw Screen.createWindowAlreadyAttachedException(screen);
		CodeDrawGUI gui = new CodeDrawGUI(screen.getWidth(), screen.getHeight());
		JFrame frame = gui.frame;

		gui.screen = screen;
		frame.setUndecorated(true);
		screen.attachGUI(frame);

		return gui.finishConstructor();
	}

	private CodeDrawGUI(int width, int height) {
		if (width < 1) throw createParameterGreaterThanZeroException("width");
		if (height < 1) throw createParameterGreaterThanZeroException("height");

		frame = new JFrame();
		panel = new CanvasPanel(width, height);

		frame.setContentPane(panel);
		frame.setIconImage(codeDrawIcon);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		setTitle("CodeDraw");
		setCursorStyle(CursorStyle.DEFAULT);
		setInstantDraw(false);
	}

	private CodeDrawGUI finishConstructor() {
		frame.setLayout(null);
		frame.setResizable(false);
		frame.setVisible(true);
		frame.toFront();
		eventHandler = new EventHandler(frame, panel);
		return this;
	}

	private final JFrame frame;
	private final CanvasPanel panel;
	private EventHandler eventHandler;
	private CursorStyle cursorStyle;

	private JFrameCorrector jFrameCorrector;
	private Screen screen;

	private boolean isInstantDraw = false;
	private boolean isClosed = false;

	public EventScanner getEventScanner() {
		checkIsClosed();
		return eventHandler.getEventScanner();
	}

	public boolean isInstantDraw() {
		checkIsClosed();
		return isInstantDraw;
	}

	public void setInstantDraw(boolean isInstantDraw) {
		checkIsClosed();
		this.isInstantDraw = isInstantDraw;
	}

	public boolean isAlwaysOnTop() {
		checkIsClosed();
		return frame.isAlwaysOnTop();
	}

	public void setAlwaysOnTop(boolean isAlwaysOnTop) {
		checkIsClosed();
		frame.setAlwaysOnTop(isAlwaysOnTop);
	}

	public Point getCanvasPosition() {
		checkIsClosed();
		return eventHandler.getCanvasPosition();
	}

	public void setCanvasPosition(Point newCanvasPosition) {
		checkIsClosed();
		eventHandler.setCanvasPosition(newCanvasPosition);
	}

	public Point getWindowPosition() {
		checkIsClosed();
		return eventHandler.getWindowPosition();
	}

	public void setWindowPosition(Point newWindowPosition) {
		checkIsClosed();
		eventHandler.setWindowPosition(newWindowPosition);
	}

	public String getTitle() {
		checkIsClosed();
		return frame.getTitle();
	}

	public void setTitle(String title) {
		checkIsClosed();
		frame.setTitle(title);
	}

	public CursorStyle getCursorStyle() {
		checkIsClosed();
		return cursorStyle;
	}

	public void setCursorStyle(CursorStyle cursorStyle) {
		checkIsClosed();
		this.cursorStyle = cursorStyle;
		frame.setCursor(cursorStyle.getCursor());
	}

	public void show(Image image, long waitMilliseconds) {
		checkIsClosed();
		panel.render(image, waitMilliseconds, isInstantDraw);
	}

	public boolean isClosed() {
		return isClosed;
	}

	public void close(boolean terminateOnLastClose) {
		if (!isClosed) {
			isClosed = true;
			if (jFrameCorrector != null) jFrameCorrector.stop();
			if (screen != null) screen.detachGUI(frame);
			frame.dispose();
			eventHandler.dispose(terminateOnLastClose);
		}
	}

	@Override
	public void close() {
		close(false);
	}

	private void checkIsClosed() {
		if (this.isClosed())
			throw new RuntimeException(
				"This CodeDraw window has already been closed. " +
				"The methods associated with the graphical user interface can no longer be used."
			);
	}

	private static IllegalArgumentException createParameterGreaterThanZeroException(String parameterName) {
		return new IllegalArgumentException("The parameter '" + parameterName + "' must be greater than zero.");
	}

	private static final BufferedImage codeDrawIcon = getCodeDrawIcon();
	private static BufferedImage getCodeDrawIcon() {
		return Image.fromBase64String(
			"iVBORw0KGgoAAAANSUhEUgAAABAAAAAQBAMAAADt3eJSAAAABGdBTUEAALGPC/xh" +
				"BQAAAA9QTFRF/59o/0RHY2Np/8iZAAAAQFvSagAAAAlwSFlzAAAOwgAADsIBFShK" +
				"gAAAADtJREFUGNNtyMENACAMQlF0AruBMoKM4P47eaAeTPovkIe/9k4Mb4/w4TJ1" +
				"0kJyJrAGJUDbAMmAI1QBFz7NBPgXRK/qAAAAAElFTkSuQmCC"
		).toBufferedImage();
	}
}