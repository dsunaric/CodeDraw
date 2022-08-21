package codedraw.drawing;

import codedraw.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;

/**
 * Image represents an image that can be used within the CodeDraw library.
 * Empty images can be created via the constructor.
 * <pre>{@code
 * CodeDrawImage image = new CodeDrawImage(400, 600);
 * }</pre>
 * External images can be loaded via static methods.
 * <pre>{@code
 * CodeDrawImage image = CodeDrawImage.fromFile("/directory/filename.png");
 * }</pre>
 */
public class Image {
	/**
	 * Loads an image from the file system.
	 * Supported image formats:
	 *      .jpg or .jpeg (JPEG), .bmp (Bitmap), .gif (Graphics Interchange Format),
	 *      .png (Portable Network Graphic) and .wbmp (Wireless Application Protocol Bitmap Format).
	 * {@link ImageIO#read(File)} is used to read images from the file system.
	 * @param pathToImage A string that points to an image file.
	 * @return An image.
	 */
	public static Image fromFile(String pathToImage) {
		if (pathToImage == null) throw createParameterNullException("pathToImage");

		try {
			return new Image(ImageIO.read(new File(pathToImage)));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Loads an image file from the internet.
	 * This function might be very slow depending on speed of the network connection
	 * or the availability of the server.
	 * <pre>{@code
	 * CodeDrawImage image = CodeDrawImage.fromUrl("https://exmaple.com/example-image.png");
	 * }</pre>
	 * Supported image formats:
	 *      .jpg or .jpeg (JPEG), .bmp (Bitmap), .gif (Graphics Interchange Format),
	 *      .png (Portable Network Graphic) and .wbmp (Wireless Application Protocol Bitmap Format).
	 * {@link ImageIO#read(URL)} is used to load images from the network.
	 * @param url Link to the image file.
	 * @return An image.
	 */
	public static Image fromUrl(String url) {
		if (url == null) throw createParameterNullException("url");

		try {
			return new Image(ImageIO.read(new URL(url)));
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Loads CodeDrawImages from the resource folder.
	 * <pre>{@code
	 * CodeDrawImage image = CodeDrawImage.fromResource("./my_image.png");
	 * }</pre>
	 * Supported image formats:
	 *      .jpg or .jpeg (JPEG), .bmp (Bitmap), .gif (Graphics Interchange Format),
	 *      .png (Portable Network Graphic) and .wbmp (Wireless Application Protocol Bitmap Format).
	 * {@link ImageIO#read(URL)} is used to read images from the resource folder.
	 * @param resourceName Path to the resource from the root of the resource folder.
	 * @return An image.
	 */
	public static Image fromResource(String resourceName) {
		if (resourceName == null) throw createParameterNullException("resourceName");

		URL url = Thread.currentThread().getContextClassLoader().getResource(resourceName);
		if (url == null) {
			//url = CodeDrawImage.class.getClassLoader().getResource(resourceName);
		}
		if (url == null) throw new RuntimeException();
		try {
			return new Image(ImageIO.read(url));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Converts a Base64 string into a CodeDrawImage.
	 * Supported image formats:
	 *      .jpg or .jpeg (JPEG), .bmp (Bitmap), .gif (Graphics Interchange Format),
	 *      .png (Portable Network Graphic) and .wbmp (Wireless Application Protocol Bitmap Format).
	 * {@link ImageIO#read(InputStream)} and {@link Base64.Decoder#decode(String)} are used to convert the image.
	 * @param base64 a Base64 string
	 * @return a CodeDrawImage
	 */
	public static Image fromBase64String(String base64) {
		if (base64 == null) throw createParameterNullException("base64");

		try {
			return new Image(ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(base64))));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Creates a CodeDraw image with an up-scaled resolution.
	 * The upscaling factor depends on the highest resolution of all monitors on the current machine.
	 * The pixel grid coordinates will be the same as a regular CodeDrawImage
	 * but round edges and fractional coordinates will be more precisely drawn.
	 * @param width The width of the CodeDrawImage.
	 * @param height The height of the CodeDrawImage.
	 * @return a CodeDrawImage
	 */
	public static Image fromDPIAwareSize(int width, int height) {
		if (width < 1) throw createParameterMustBeGreaterThanZeroException("width");
		if (height < 1) throw createParameterMustBeGreaterThanZeroException("height");

		AffineTransform max = getMaximumDPIFromAllScreens();
		return new Image(width, height, upscale(max.getScaleX()), upscale(max.getScaleY()), Palette.WHITE);
	}

	/**
	 * Saves the image to the specified location using the specified image format.
	 * Supported formats are all values in {@link ImageFormat}.
	 * The formats {@link ImageFormat#JPG}, {@link ImageFormat#JPEG} and {@link ImageFormat#BMP} do not support transparency.
	 * {@link ImageIO#write(RenderedImage, String, File)} and {@link File#File(String)} are used to read the image from the file system.
	 * Read their documentation for more details.
	 * @param image a CodeDrawImage
	 * @param pathToImage The location where the image should be saved.
	 * @param format The format the image should be saved in.
	 *               As a default, choose {@link ImageFormat#PNG} and make sure that the file ends with ".png".
	 */
	public static void save(Image image, String pathToImage, ImageFormat format) {
		if (image == null) throw createParameterNullException("image");
		if (pathToImage == null) throw createParameterNullException("pathToImage");
		if (format == null) throw createParameterNullException("format");

		try {
			boolean result = ImageIO.write(
					image.toBufferedImage(format.supportsTransparency() ? BufferedImageType.INT_ARGB : BufferedImageType.INT_RGB),
					format.getFormatName(),
					new File(pathToImage)
			);
			if (!result) throw new RuntimeException("Could not save image, because no appropriate writer has been found in ImageIO.");
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Creates a new image from the given image that only contains the specified section.
	 * @param source The image from which a new smaller image should be created from.
	 * @param x The start point of the cutout.
	 * @param y The start point of the cutout.
	 * @param width The width of the cutout.
	 * @param height The height of the cutout.
	 * @return A new image cutout from the specified section.
	 */
	public static Image crop(Image source, int x, int y, int width, int height) {
		if (source == null) throw createParameterNullException("source");
		if (x < 0 || source.width <= x) throw new RuntimeException("");
		if (y < 0 || source.height <= y) throw new RuntimeException("");
		if (x + width > source.width) throw new RuntimeException();
		if (y + height > source.height) throw new RuntimeException();

		Image result = new Image(width, height, source.xScale, source.yScale, Palette.TRANSPARENT);
		result.drawImage(-x, -y, source);
		return result;
	}

	/**
	 *
	 * @param source
	 * @param scale
	 * @return
	 */
	public static Image scale(Image source, double scale) {
		if (source == null) throw createParameterNullException("source");
		if (scale <= 0) throw new RuntimeException();

		return scale(source, scale, Interpolation.BICUBIC);
	}

	/**
	 *
	 * @param source
	 * @param scale
	 * @param interpolation
	 * @return
	 */
	public static Image scale(Image source, double scale, Interpolation interpolation) {
		if (source == null) throw createParameterNullException("source");
		if (scale <= 0) throw new RuntimeException();
		if (interpolation == null) throw createParameterNullException("interpolation");

		int width = (int)(source.width * scale);
		int height = (int)(source.height * scale);

		if (width == 0) throw new RuntimeException("The scale is too small and would create an image with a width of 0px.");
		if (height == 0) throw new RuntimeException("The scale is too small and would create an image with a height of 0px.");

		Image result = new Image(width, height, source.xScale, source.yScale, Palette.TRANSPARENT);
		result.drawImage(0, 0, width, height, source, interpolation);
		return result;
	}

	/**
	 * Rotates the image clockwise.
	 * @param image The image to rotate.
	 * @return A copy of the image that is rotated 90° clockwise.
	 */
	public static Image rotateClockwise(Image image) {
		if (image == null) throw createParameterNullException("image");

		return rotate(image, Matrix2D.IDENTITY.rotate(Math.PI / 2).translate(image.width, 0));
	}

	/**
	 * Rotates the image counter-clockwise.
	 * @param image The image to rotate.
	 * @return A copy of the image that is rotated 90° counter-clockwise.
	 */
	public static Image rotateCounterClockwise(Image image) {
		if (image == null) throw createParameterNullException("image");

		return rotate(image, Matrix2D.IDENTITY.rotate(-Math.PI / 2).translate(0, image.height));
	}

	private static Image rotate(Image image, Matrix2D transformation) {
		Image result = new Image(image.height, image.width, image.yScale, image.xScale, Palette.TRANSPARENT);

		result.setTransformation(transformation);
		result.drawImage(0, 0, image);
		result.setTransformationToIdentity();

		return result;
	}

	/**
	 * Mirrors the image horizontally.
	 * @param image The image to mirror.
	 * @return A copy of the image that is mirrored along the horizontal axis.
	 */
	public static Image mirrorHorizontally(Image image) {
		if (image == null) throw createParameterNullException("image");

		return mirror(image, Matrix2D.IDENTITY.scale(1, -1).translate(0, image.height));
	}

	/**
	 * Mirrors the image vertically.
	 * @param image The image to mirror.
	 * @return A copy of the image that is mirrored along the vertical axis.
	 */
	public static Image mirrorVertically(Image image) {
		if (image == null) throw createParameterNullException("image");

		return mirror(image, Matrix2D.IDENTITY.scale(-1, 1).translate(image.width, 0));
	}

	private static Image mirror(Image image, Matrix2D transformation) {
		Image result = new Image(image.width, image.height, image.xScale, image.yScale, Palette.TRANSPARENT);

		result.setTransformation(transformation);
		result.drawImage(0, 0, image);
		result.setTransformationToIdentity();

		return result;
	}

	/**
	 * Creates a copy of the CodeDrawImage supplied as a parameter.
	 * The configuration of the CodeDrawImage will not be copied.
	 * All parameters of the result will be set to their default value.
	 * DPI aware scaling will also be ignored.
	 * @param image Image that is to be copied.
	 */
	public Image(Image image) {
		this(checkParameterNull(image, "image").getWidth(), image.getHeight(), image.xScale, image.yScale, Palette.TRANSPARENT);
		drawImageInternal(0, 0, image.getWidth(), image.getHeight(), image, Interpolation.NEAREST_NEIGHBOR);
	}

	/**
	 * Creates a CodeDrawImage from the Image supplied as a parameter.
	 * The contents of the supplied image will be copied to create this new CodeDrawImage instance.
	 * @param image Image that is to be converted to a CodeDrawImage.
	 */
	public Image(java.awt.Image image) {
		this(checkParameterNull(image, "image").getWidth(null), image.getHeight(null), 1, 1, Palette.TRANSPARENT);
		drawImageInternal(0, 0, image.getWidth(null), image.getHeight(null), image, Interpolation.BICUBIC);
	}

	/**
	 * Creates a white CodeDrawImage of the specified size.
	 * @param width The width of the CodeDrawImage.
	 * @param height The height of the CodeDrawImage.
	 */
	public Image(int width, int height) {
		this(width, height, Palette.WHITE);
	}

	public Image(int width, int height, Color backgroundColor) {
		this(width, height, 1, 1, backgroundColor);
	}

	private Image(int width, int height, int xScale, int yScale, Color backgroundColor) {
		if (width < 1) throw createParameterMustBeGreaterThanZeroException("width");
		if (height < 1) throw createParameterMustBeGreaterThanZeroException("height");
		if (xScale < 1) throw createParameterMustBeGreaterThanZeroException("xScale");
		if (yScale < 1) throw createParameterMustBeGreaterThanZeroException("yScale");
		if (backgroundColor == null) throw createParameterNullException("backgroundColor");
		this.width = width;
		this.height = height;
		this.xScale = xScale;
		this.yScale = yScale;

		image = new BufferedImage(width * xScale, height * yScale, BufferedImage.TYPE_INT_ARGB);
		g = image.createGraphics();

		setRenderingHint(RHAlphaInterpolation.QUALITY);
		setRenderingHint(RHColorRendering.QUALITY);
		setRenderingHint(RHRendering.QUALITY);
		setRenderingHint(RHDithering.ENABLE);
		setRenderingHint(RHFractionalMetrics.ON); // tested
		setRenderingHint(RHResolutionVariant.DEFAULT); // unknown

		setColor(Palette.BLACK);
		setLineWidth(1);
		setAntiAliased(true);
		setCorner(Corner.SHARP);
		setTransformationToIdentity();
		setAlphaComposition(AlphaComposition.SET_VALUE);
		clearInternal(backgroundColor);
		setAlphaComposition(AlphaComposition.DRAW_OVER);
	}

	private BufferedImage image;
	private Graphics2D g;
	private int width;
	private int height;
	private int xScale;
	private int yScale;

	private double lineWidth = 1;
	private Corner corner = Corner.SHARP;
	private boolean isAntiAliased = true;
	private TextFormat textFormat = new TextFormat();
	private Matrix2D transformation = Matrix2D.IDENTITY;
	private AlphaComposition alphaComposition = AlphaComposition.DRAW_OVER;

	/**
	 * This value cannot be changed once set via the constructor.
	 * @return the width of the canvas in pixel.
	 */
	public int getWidth() { return width; }

	/**
	 * This value cannot be changed once set via the constructor.
	 * @return the height of the canvas in pixel.
	 */
	public int getHeight() { return height; }

	/**
	 * Defines the color that is used for drawing all shapes.
	 * @return the drawing color of this CodeDraw window.
	 */
	public Color getColor() {
		return g.getColor();
	}

	/**
	 * Defines the color that is used for drawing all shapes.
	 * Use {@link Palette} for a large selection of colors.
	 * @param color Sets the drawing color of this CodeDraw window.
	 */
	public void setColor(Color color) {
		if (color == null) throw createParameterNullException("color");

		g.setColor(color);
	}

	/**
	 * Defines the width or thickness of drawn shapes and lines.
	 * Must be greater than zero.
	 * @return the lineWidth of this CodeDraw window.
	 */
	public double getLineWidth() { return lineWidth; }

	/**
	 * Defines the width or thickness of drawn shapes and lines.
	 * Must be greater than zero.
	 * @param lineWidth Sets the lineWidth of this CodeDraw window.
	 */
	public void setLineWidth(double lineWidth) {
		if (lineWidth <= 0) throw createParameterMustBeGreaterThanZeroException("lineWidth");

		this.lineWidth = lineWidth;
		updateBrush();
	}

	/**
	 * Defines how the corners of drawn shapes should look.
	 * See {@link Corner} for details.
	 * @return the corner style of this CodeDraw window.
	 */
	public Corner getCorner() { return corner; }

	/**
	 * Defines how the corners of drawn shapes should look.
	 * See {@link Corner} for details.
	 * @param corner Sets the corner style of this CodeDraw window.
	 */
	public void setCorner(Corner corner) {
		if (corner == null) throw createParameterNullException("corner");

		this.corner = corner;
		updateBrush();
	}

	/**
	 * Defines the styling of drawn text.
	 * See also {@link #drawText(double, double, String)} on how the styling is applied.
	 * @return the text formatting options of this CodeDraw window.
	 */
	public TextFormat getTextFormat() {
		return textFormat;
	}

	/**
	 * Defines the styling of drawn text.
	 * See also {@link #drawText(double, double, String)} on how the styling is applied.
	 * @param textFormat Sets the text formatting options of this CodeDraw window.
	 */
	public void setTextFormat(TextFormat textFormat){
		if (textFormat == null) throw createParameterNullException("textFormat");

		this.textFormat = textFormat;
	}

	/**
	 * Defines whether draw text, drawn shapes and filled shapes are anti-aliased.
	 * See <a href="https://en.wikipedia.org/wiki/Spatial_anti-aliasing">Wikipedia Spatial Anti-aliasing</a>
	 * @return Whether this CodeDraw window anti aliases.
	 */
	public boolean isAntiAliased() { return isAntiAliased; }

	/**
	 * Defines whether drawn text, drawn shapes and filled shapes are anti-aliased.
	 * See <a href="https://en.wikipedia.org/wiki/Spatial_anti-aliasing">Wikipedia Spatial Anti-aliasing</a>
	 * @param isAntiAliased Sets whether this CodeDraw window anti aliases.
	 */
	public void setAntiAliased(boolean isAntiAliased) {
		this.isAntiAliased = isAntiAliased;
		if (isAntiAliased) {
			setRenderingHint(RHAntiAliasing.ON);
			setRenderingHint(RHTextAntiAliasing.ON);
			setRenderingHint(RHStrokeControl.PURE);
		}
		else {
			setRenderingHint(RHAntiAliasing.OFF);
			setRenderingHint(RHTextAntiAliasing.OFF);
			/* Pure strokes look better when anti-aliasing is on
			   but certain vertical lines can disappear when anti-aliasing is off.
			   With normalize non-anti-aliased lines don't change. */
			setRenderingHint(RHStrokeControl.NORMALIZE);
		}
	}

	private void setRenderingHint(RenderingHintValue hint) {
		RenderingHintValue.applyHint(g, hint);
	}

	private void setRenderingHint(Interpolation interpolation) {
		RenderingHintValue.applyHint(g, interpolation);
	}

	private void updateBrush() {
		g.setStroke(new BasicStroke((float)lineWidth, getCap(corner), getJoin(corner)));
	}

	private static int getCap(Corner corner) {
		switch (corner) {
			case SHARP: return BasicStroke.CAP_SQUARE;
			case ROUND: return BasicStroke.CAP_ROUND;
			case BEVEL: return BasicStroke.CAP_BUTT;
			default: throw new RuntimeException("Unknown corner type.");
		}
	}

	private static int getJoin(Corner corner) {
		switch (corner) {
			case SHARP: return BasicStroke.JOIN_MITER;
			case ROUND: return BasicStroke.JOIN_ROUND;
			case BEVEL: return BasicStroke.JOIN_BEVEL;
			default: throw new RuntimeException("Unknown corner type.");
		}
	}

	public Matrix2D getTransformation() {
		return transformation;
	}

	public void setTransformation(Matrix2D transformation) {
		this.transformation = transformation;
		this.g.setTransform(transformation.scale(xScale, yScale).toAffineTransform());
	}

	public void setTransformationToIdentity() {
		setTransformation(Matrix2D.IDENTITY);
	}

	public AlphaComposition getAlphaComposition() {
		return alphaComposition;
	}

	public void setAlphaComposition(AlphaComposition alphaComposition) {
		this.alphaComposition = alphaComposition;
		g.setComposite(alphaComposition == AlphaComposition.DRAW_OVER ? AlphaComposite.SrcOver : AlphaComposite.Src);
	}

	/**
	 * Draws the text at the specified (x, y) coordinate.
	 * Formatting options can be set via the TextFormat object.
	 * See {@link #getTextFormat()}, {@link #setTextFormat(TextFormat)} and the {@link TextFormat} class.
	 * Multiple lines can be drawn by including newline characters in the text parameter.
	 * If not specified otherwise in the TextFormat object the (x, y) coordinates will be in the top left corner of the text.
	 * @param x The distance in pixel from the left side of the canvas.
	 * @param y The distance in pixel from the top side of the canvas.
	 * @param text The text or string to be drawn.
	 */
	public void drawText(double x, double y, String text) {
		if (text == null) throw createParameterNullException("text");

		beforeDrawing();
		g.setFont(textFormat.toFont());
		TextFormat.drawText(g, x, y, text, textFormat);
		afterDrawing();
	}

	/**
	 * Draws a point which is exactly 1x1 pixel in size.
	 * Ignores any transformation set with {@link #setTransformation(Matrix2D)}.
	 * @param x The distance in pixel from the left side of the canvas.
	 * @param y The distance in pixel from the top side of the canvas.
	 */
	public void drawPixel(double x, double y) {
		beforeDrawing();
		if (0 <= x && x < image.getWidth() && 0 <= y && y < image.getHeight()) {
			image.setRGB((int) (x * xScale), (int) (y * yScale), getColor().getRGB());
		}
		afterDrawing();
	}

	/**
	 * Draws a point which changes size depending on the {@link #getLineWidth()}
	 * @param centerX The distance in pixel from the left side of the canvas to the center of the point.
	 * @param centerY The distance in pixel from the top side of the canvas to the center of the point.
	 */
	public void drawPoint(double centerX, double centerY) {
		beforeDrawing();
		g.fill(createEllipse(centerX, centerY, getLineWidth(), getLineWidth()));
		afterDrawing();
	}

	/**
	 * Draws a straight line between the start point and end point.
	 * The line width can be changed with {@link #setLineWidth(double)}.
	 * The corners can be changed with {@link #setCorner(Corner)}. For details see the {@link Corner} class.
	 * @param startX The distance in pixel from the left side of the canvas to the start of the line.
	 * @param startY The distance in pixel from the top side of the canvas to the start of the line.
	 * @param endX The distance in pixel from the left side of the canvas to the end of the line.
	 * @param endY The distance in pixel from the top side of the canvas to the end of the line.
	 */
	public void drawLine(double startX, double startY, double endX, double endY) {
		beforeDrawing();
		g.draw(createLine(startX, startY, endX, endY));
		afterDrawing();
	}

	/**
	 * Draws a quadratic Bézier curve. See: <a href="https://en.wikipedia.org/wiki/B%C3%A9zier_curve">Wikipedia Bezier Curve</a>
	 * The start and end of the curve will be precisely where startX/Y and endX/Y are specified.
	 * The controlX/Y parameter specifies in what way the curve will be bent.
	 * The line width can be changed with {@link #setLineWidth(double)}.
	 * The corners can be changed with {@link #setCorner(Corner)}. For details see the {@link Corner} class.
	 * @param startX The distance in pixel from the left side of the canvas to the start of the curve.
	 * @param startY The distance in pixel from the top side of the canvas to the start of the curve.
	 * @param controlX Defines the way the curve bends in the x direction.
	 * @param controlY Defines the way the curve bends in the y direction.
	 * @param endX The distance in pixel from the left side of the canvas to the end of the curve.
	 * @param endY The distance in pixel from the top side of the canvas to the end of the curve.
	 */
	public void drawCurve(double startX, double startY, double controlX, double controlY, double endX, double endY) {
		beforeDrawing();
		g.draw(createCurve(startX, startY, controlX, controlY, endX, endY));
		afterDrawing();
	}

	/**
	 * Draws a cubic Bézier curve. See <a href="https://en.wikipedia.org/wiki/B%C3%A9zier_curve">Wikipedia Bezier Curve</a>
	 * The start and end of the curve will be precisely where startX/Y and endX/Y are specified.
	 * The control1X/Y and control2X/Y parameter specify in what way the curve will be bent.
	 * The line width can be changed with {@link #setLineWidth(double)}.
	 * The corners can be changed with {@link #setCorner(Corner)}. For details see the {@link Corner} class.
	 * @param startX The distance in pixel from the left side of the canvas to the start of the curve.
	 * @param startY The distance in pixel from the top side of the canvas to the start of the curve.
	 * @param control1X Defines the way the curve bends in the x direction.
	 * @param control1Y Defines the way the curve bends in the y direction.
	 * @param control2X Defines the way the curve bends in the x direction.
	 * @param control2Y Defines the way the curve bends in the y direction.
	 * @param endX The distance in pixel from the left side of the canvas to the end of the curve.
	 * @param endY The distance in pixel from the top side of the canvas to the end of the curve.
	 */
	public void drawBezier(double startX, double startY, double control1X, double control1Y, double control2X, double control2Y, double endX, double endY) {
		beforeDrawing();
		g.draw(createBezierCurve(startX, startY, control1X, control1Y, control2X, control2Y, endX, endY));
		afterDrawing();
	}

	/**
	 * Draws the outline of a square.
	 * The line width can be changed with {@link #setLineWidth(double)}.
	 * The corners can be changed with {@link #setCorner(Corner)}. For details see the {@link Corner} class.
	 * @param x The distance in pixel from the left side of the canvas to the left size of the square.
	 * @param y The distance in pixel from the top side of the canvas to the top side of the square.
	 * @param sideLength The width and the height of the square in pixel.
	 */
	public void drawSquare(double x, double y, double sideLength) {
		if (sideLength < 0) throw createParameterMustBeGreaterOrEqualToZeroException("sideLength");

		beforeDrawing();
		g.draw(createDrawRectangle(x, y, sideLength, sideLength));
		afterDrawing();
	}

	/**
	 * Draws a filled square.
	 * The corners can be changed with {@link #setCorner(Corner)}. For details see the {@link Corner} class.
	 * @param x The distance in pixel from the left side of the canvas to the left size of the square.
	 * @param y The distance in pixel from the top side of the canvas to the top side of the square.
	 * @param sideLength The width and the height of the square in pixel.
	 */
	public void fillSquare(double x, double y, double sideLength) {
		if (sideLength < 0) throw createParameterMustBeGreaterOrEqualToZeroException("sideLength");

		beforeDrawing();
		g.fill(createFillRectangle(x, y, sideLength, sideLength));
		afterDrawing();
	}

	/**
	 * Draws the outline of a rectangle.
	 * The line width can be changed with {@link #setLineWidth(double)}.
	 * The corners can be changed with {@link #setCorner(Corner)}. For details see the {@link Corner} class.
	 * @param x The distance in pixel from the left side of the canvas to the left size of the rectangle.
	 * @param y The distance in pixel from the top side of the canvas to the top side of the rectangle.
	 * @param height The height of the rectangle in pixel.
	 * @param width The width of the rectangle in pixel.
	 */
	public void drawRectangle(double x, double y, double width, double height) {
		if (width < 0) throw createParameterMustBeGreaterOrEqualToZeroException("width");
		if (height < 0) throw createParameterMustBeGreaterOrEqualToZeroException("height");

		beforeDrawing();
		g.draw(createDrawRectangle(x, y, width, height));
		afterDrawing();
	}

	/**
	 * Draws a filled rectangle.
	 * The corners can be changed with {@link #setCorner(Corner)}. For details see the {@link Corner} class.
	 * @param x The distance in pixel from the left side of the canvas to the left size of the rectangle.
	 * @param y The distance in pixel from the top side of the canvas to the top side of the rectangle.
	 * @param height The height of the rectangle in pixel.
	 * @param width The width of the rectangle in pixel.
	 */
	public void fillRectangle(double x, double y, double width, double height) {
		if (width < 0) throw createParameterMustBeGreaterOrEqualToZeroException("width");
		if (height < 0) throw createParameterMustBeGreaterOrEqualToZeroException("height");

		beforeDrawing();
		g.fill(createFillRectangle(x, y, width, height));
		afterDrawing();
	}

	/**
	 * Draws the outline of a circle.
	 * The center of the circle will be at the specified (centerX, centerY) coordinate.
	 * The line width can be changed with {@link #setLineWidth(double)}.
	 * @param centerX The distance in pixel from the left side of the canvas to the center of the circle.
	 * @param centerY The distance in pixel from the top side of the canvas to the center of the circle.
	 * @param radius The radius of the circle in pixel.
	 */
	public void drawCircle(double centerX, double centerY, double radius) {
		if (radius < 0) throw createParameterMustBeGreaterOrEqualToZeroException("radius");

		beforeDrawing();
		g.draw(createEllipse(centerX, centerY, radius, radius));
		afterDrawing();
	}

	/**
	 * Draws a filled circle.
	 * The center of the circle will be at the specified (centerX, centerY) coordinate.
	 * @param centerX The distance in pixel from the left side of the canvas to the center of the circle.
	 * @param centerY The distance in pixel from the top side of the canvas to the center of the circle.
	 * @param radius The radius of the circle in pixel.
	 */
	public void fillCircle(double centerX, double centerY, double radius) {
		if (radius < 0) throw createParameterMustBeGreaterOrEqualToZeroException("radius");

		beforeDrawing();
		g.fill(createEllipse(centerX, centerY, radius, radius));
		afterDrawing();
	}

	/**
	 * Draws the outline of an ellipse.
	 * The center of the ellipse will be at the specified (centerX, centerY) coordinate.
	 * The line width can be changed with {@link #setLineWidth(double)}.
	 * @param centerX The distance in pixel from the left side of the canvas to the center of the ellipse.
	 * @param centerY The distance in pixel from the top side of the canvas to the center of the ellipse.
	 * @param horizontalRadius The horizontal radius of the ellipse in pixel. The width of the ellipse is 2 * horizontalRadius.
	 * @param verticalRadius The vertical radius of the ellipse in pixel. The height of the ellipse is 2 * verticalRadius.
	 */
	public void drawEllipse(double centerX, double centerY, double horizontalRadius, double verticalRadius) {
		if (horizontalRadius < 0) throw createParameterMustBeGreaterOrEqualToZeroException("horizontalRadius");
		if (verticalRadius < 0) throw createParameterMustBeGreaterOrEqualToZeroException("verticalRadius");

		beforeDrawing();
		g.draw(createEllipse(centerX, centerY, horizontalRadius, verticalRadius));
		afterDrawing();
	}

	/**
	 * Draws a filled ellipse.
	 * The center of the ellipse will be at the specified (centerX, centerY) coordinate.
	 * @param centerX The distance in pixel from the left side of the canvas to the center of the ellipse.
	 * @param centerY The distance in pixel from the top side of the canvas to the center of the ellipse.
	 * @param horizontalRadius The horizontal radius of the ellipse in pixel. The width of the ellipse is 2 * horizontalRadius.
	 * @param verticalRadius The vertical radius of the ellipse in pixel. The height of the ellipse is 2 * verticalRadius.
	 */
	public void fillEllipse(double centerX, double centerY, double horizontalRadius, double verticalRadius) {
		if (horizontalRadius < 0) throw createParameterMustBeGreaterOrEqualToZeroException("horizontalRadius");
		if (verticalRadius < 0) throw createParameterMustBeGreaterOrEqualToZeroException("verticalRadius");

		beforeDrawing();
		g.fill(createEllipse(centerX, centerY, horizontalRadius, verticalRadius));
		afterDrawing();
	}

	/**
	 * Draws the outline of an arc with the center being the (centerX, centerY) coordinates.
	 * The arc starts at the 3 o'clock position offset by the startRadians parameter.
	 * The total length of the arc is defined by the sweepRadians parameter.
	 * The line width can be changed with {@link #setLineWidth(double)}.<br>
	 * You can use {@link Math#toRadians(double)} to specify the angle in degrees.
	 * <pre>{@code
	 * cd.drawArc(200, 200, 50, Math.toRadians(90), Math.toRadians(180));
	 * }</pre>
	 * The line width can be changed with {@link #setLineWidth(double)}.
	 * The corners can be changed with {@link #setCorner(Corner)}. For details see the {@link Corner} class.
	 * @param centerX The distance in pixel from the left side of the canvas to the center of the arc.
	 * @param centerY The distance in pixel from the top side of the canvas to the center of the arc.
	 * @param radius The radius of the arc in pixel.
	 * @param startRadians The starting angle in radians. A 0 radians angle would be interpreted as starting at 3 o'clock going clockwise.
	 * @param sweepRadians The length of the arc in radians from the start angle in a clockwise direction.
	 */
	public void drawArc(double centerX, double centerY, double radius, double startRadians, double sweepRadians) {
		if (radius < 0) throw createParameterMustBeGreaterOrEqualToZeroException("radius");

		beforeDrawing();
		g.draw(createArc(centerX, centerY, radius, radius, startRadians, sweepRadians));
		afterDrawing();
	}

	/**
	 * Draws the outline of an arc with the center being the (centerX, centerY) coordinates.
	 * The width is 2 * horizontalRadius and the height is 2 * verticalRadius.
	 * The arc starts at the 3 o'clock position offset by the startRadians parameter.
	 * The total length of the arc is defined by the sweepRadians parameter.
	 * The line width can be changed with {@link #setLineWidth(double)}.<br>
	 * You can use {@link Math#toRadians(double)} to specify the angle in degrees.
	 * <pre>{@code
	 * cd.drawArc(200, 200, 50, 50, Math.toRadians(90), Math.toRadians(180));
	 * }</pre>
	 * The line width can be changed with {@link #setLineWidth(double)}.
	 * The corners can be changed with {@link #setCorner(Corner)}. For details see the {@link Corner} class.
	 * @param centerX The distance in pixel from the left side of the canvas to the center of the arc.
	 * @param centerY The distance in pixel from the top side of the canvas to the center of the arc.
	 * @param horizontalRadius The horizontal radius of the arc in pixel. The width of the arc is 2 * horizontalRadius.
	 * @param verticalRadius The vertical radius of the arc in pixel. The height of the arc is 2 * verticalRadius.
	 * @param startRadians The starting angle in radians. A 0 radians angle would be interpreted as starting at 3 o'clock going clockwise.
	 * @param sweepRadians The length of the arc in radians from the start angle in a clockwise direction.
	 */
	public void drawArc(double centerX, double centerY, double horizontalRadius, double verticalRadius, double startRadians, double sweepRadians) {
		if (horizontalRadius < 0) throw createParameterMustBeGreaterOrEqualToZeroException("horizontalRadius");
		if (verticalRadius < 0) throw createParameterMustBeGreaterOrEqualToZeroException("verticalRadius");

		beforeDrawing();
		g.draw(createArc(centerX, centerY, horizontalRadius, verticalRadius, startRadians, sweepRadians));
		afterDrawing();
	}

	/**
	 * Draws the outline of a pie with the center being the (centerX, centerY) coordinates.
	 * The pie starts at the 3 o'clock position offset by the startRadians parameter.
	 * The total length of the pie is defined by the sweepRadians parameter.
	 * The line width can be changed with {@link #setLineWidth(double)}.<br>
	 * You can use {@link Math#toRadians(double)} to specify the angle in degrees.
	 * <pre>{@code
	 * cd.drawPie(200, 200, 50, Math.toRadians(90), Math.toRadians(180));
	 * }</pre>
	 * The line width can be changed with {@link #setLineWidth(double)}.
	 * The corners can be changed with {@link #setCorner(Corner)}. For details see the {@link Corner} class.
	 * @param centerX The distance in pixel from the left side of the canvas to the center of the pie.
	 * @param centerY The distance in pixel from the top side of the canvas to the center of the pie.
	 * @param radius The radius of the pie in pixel.
	 * @param startRadians The starting angle in radians. A 0 radians angle would be interpreted as starting at 3 o'clock going clockwise.
	 * @param sweepRadians The length of the pie in radians from the start angle in a clockwise direction.
	 */
	public void drawPie(double centerX, double centerY, double radius, double startRadians, double sweepRadians) {
		if (radius < 0) throw createParameterMustBeGreaterOrEqualToZeroException("radius");

		beforeDrawing();
		g.draw(createPie(centerX, centerY, radius, radius, startRadians, sweepRadians));
		afterDrawing();
	}

	/**
	 * Draws the outline of a pie with the center being the (centerX, centerY) coordinates.
	 * The width is 2 * horizontalRadius and the height is the 2 * verticalRadius.
	 * The pie starts at the 3 o'clock position offset by the startRadians parameter.
	 * The total length of the pie is defined by the sweepRadians parameter.
	 * The line width can be changed with {@link #setLineWidth(double)}.<br>
	 * You can use {@link Math#toRadians(double)} to specify the angle in degrees.
	 * <pre>{@code
	 * cd.drawPie(200, 200, 50, 50, Math.toRadians(90), Math.toRadians(180));
	 * }</pre>
	 * The line width can be changed with {@link #setLineWidth(double)}.
	 * The corners can be changed with {@link #setCorner(Corner)}. For details see the {@link Corner} class.
	 * @param centerX The distance in pixel from the left side of the canvas to the center of the pie.
	 * @param centerY The distance in pixel from the top side of the canvas to the center of the pie.
	 * @param horizontalRadius The horizontal radius of the pie in pixel. The width of the pie is 2 * horizontalRadius.
	 * @param verticalRadius The vertical radius of the pie in pixel. The height of the pie is 2 * verticalRadius.
	 * @param startRadians The starting angle in radians. A 0 radians angle would be interpreted as starting at 3 o'clock going clockwise.
	 * @param sweepRadians The length of the pie in radians from the start angle in a clockwise direction.
	 */
	public void drawPie(double centerX, double centerY, double horizontalRadius, double verticalRadius, double startRadians, double sweepRadians) {
		if (horizontalRadius < 0) throw createParameterMustBeGreaterOrEqualToZeroException("horizontalRadius");
		if (verticalRadius < 0) throw createParameterMustBeGreaterOrEqualToZeroException("verticalRadius");

		beforeDrawing();
		g.draw(createPie(centerX, centerY, horizontalRadius, verticalRadius, startRadians, sweepRadians));
		afterDrawing();
	}

	/**
	 * Draws a filled pie with the center being the (centerX, centerY) coordinates.
	 * The pie starts at the 3 o'clock position offset by the startRadians parameter.
	 * The total length of the pie is defined by the sweepRadians parameter.<br>
	 * You can use {@link Math#toRadians(double)} to specify the angle in degrees.
	 * <pre>{@code
	 * cd.fillPie(200, 200, 50, Math.toRadians(90), Math.toRadians(180));
	 * }</pre>
	 * @param centerX The distance in pixel from the left side of the canvas to the center of the pie.
	 * @param centerY The distance in pixel from the top side of the canvas to the center of the pie.
	 * @param radius The radius of the pie in pixel.
	 * @param startRadians The starting angle in radians. A 0 radians angle would be interpreted as starting at 3 o'clock going clockwise.
	 * @param sweepRadians The length of the pie in radians from the start angle in a clockwise direction.
	 */
	public void fillPie(double centerX, double centerY, double radius, double startRadians, double sweepRadians) {
		if (radius < 0) throw createParameterMustBeGreaterOrEqualToZeroException("radius");

		beforeDrawing();
		g.fill(createPie(centerX, centerY, radius, radius, startRadians, sweepRadians));
		afterDrawing();
	}

	/**
	 * Draws a filled pie with the center being the (centerX, centerY) coordinates.
	 * The width is 2 * horizontalRadius and the height is the 2 * verticalRadius.
	 * The pie starts at the 3 o'clock position offset by the startRadians parameter.
	 * The total length of the pie is defined by the sweepRadians parameter.<br>
	 * You can use {@link Math#toRadians(double)} to specify the angle in degrees.
	 * <pre>{@code
	 * cd.fillPie(200, 200, 50, 50, Math.toRadians(90), Math.toRadians(180));
	 * }</pre>
	 * @param centerX The distance in pixel from the left side of the canvas to the center of the pie.
	 * @param centerY The distance in pixel from the top side of the canvas to the center of the pie.
	 * @param horizontalRadius The horizontal radius of the pie in pixel. The width of the pie is 2 * horizontalRadius.
	 * @param verticalRadius The vertical radius of the pie in pixel. The height of the pie is 2 * verticalRadius.
	 * @param startRadians The starting angle in radians. A 0 radians angle would be interpreted as starting at 3 o'clock going clockwise.
	 * @param sweepRadians The length of the pie in radians from the start angle in a clockwise direction.
	 */
	public void fillPie(double centerX, double centerY, double horizontalRadius, double verticalRadius, double startRadians, double sweepRadians) {
		if (horizontalRadius < 0) throw createParameterMustBeGreaterOrEqualToZeroException("horizontalRadius");
		if (verticalRadius < 0) throw createParameterMustBeGreaterOrEqualToZeroException("verticalRadius");

		beforeDrawing();
		g.fill(createPie(centerX, centerY, horizontalRadius, verticalRadius, startRadians, sweepRadians));
		afterDrawing();
	}

	/**
	 * Draws the outline of a triangle.
	 * The line width can be changed with {@link #setLineWidth(double)}.
	 * The corners can be changed with {@link #setCorner(Corner)}. For details see the {@link Corner} class.
	 * @param x1 The distance in pixel from the left side of the canvas to the first corner of the triangle.
	 * @param y1 The distance in pixel from the top side of the canvas to the first corner of the triangle.
	 * @param x2 The distance in pixel from the left side of the canvas to the second corner of the triangle.
	 * @param y2 The distance in pixel from the top side of the canvas to the second corner of the triangle.
	 * @param x3 The distance in pixel from the left side of the canvas to the third corner of the triangle.
	 * @param y3 The distance in pixel from the top side of the canvas to the third corner of the triangle.
	 */
	public void drawTriangle(double x1, double y1, double x2, double y2, double x3, double y3) {
		beforeDrawing();
		g.draw(createTriangle(x1, y1, x2, y2, x3, y3));
		afterDrawing();
	}

	/**
	 * Draws a filled triangle.
	 * @param x1 The distance in pixel from the left side of the canvas to the first corner of the triangle.
	 * @param y1 The distance in pixel from the top side of the canvas to the first corner of the triangle.
	 * @param x2 The distance in pixel from the left side of the canvas to the second corner of the triangle.
	 * @param y2 The distance in pixel from the top side of the canvas to the second corner of the triangle.
	 * @param x3 The distance in pixel from the left side of the canvas to the third corner of the triangle.
	 * @param y3 The distance in pixel from the top side of the canvas to the third corner of the triangle.
	 */
	public void fillTriangle(double x1, double y1, double x2, double y2, double x3, double y3) {
		beforeDrawing();
		g.fill(createTriangle(x1, y1, x2, y2, x3, y3));
		afterDrawing();
	}

	/**
	 * Draws the outline of a polygon.
	 * This function must be called with an even number of parameters.
	 * Each parameter pair (x, y) represents a corner of the polygon.
	 * Must be called with at least two points as parameter.
	 * Each point passed to drawPolygon will be connected to the following points and
	 * the last point will be connected to the first point.
	 * <pre>{@code
	 * cd.drawPolygon(
	 *     200, 100,
	 *     100, 200,
	 *     300, 200
	 * );
	 * }</pre>
	 * The line width can be changed with {@link #setLineWidth(double)}.
	 * The corners can be changed with {@link #setCorner(Corner)}. For details see the {@link Corner} class.
	 * @param points An even number of doubles. Each pair represents one corner of the polygon.
	 */
	public void drawPolygon(double... points) {
		if (isInvalidPolygonCount(points)) throw createPolygonCountException(points, "drawPolygon");

		beforeDrawing();
		g.draw(createPolygon(points));
		afterDrawing();
	}

	/**
	 * Draws a filled polygon.
	 * This function must be called with an even number of parameters.
	 * Each parameter pair (x, y) represents a corner of the polygon.
	 * Must be called with at least two points as parameter.
	 * Each point passed to drawPolygon will be connected to the following points and
	 * the last point will be connected to the first point.
	 * If you pass only two arguments to fillPolygon nothing will be drawn.
	 * <pre>{@code
	 * cd.fillPolygon(
	 *     200, 100,
	 *     100, 200,
	 *     300, 200
	 * );
	 * }</pre>
	 * @param points An even number of doubles. Each pair represents one corner of the polygon.
	 */
	public void fillPolygon(double... points) {
		if (isInvalidPolygonCount(points)) throw createPolygonCountException(points, "fillPolygon");

		beforeDrawing();
		g.fill(createPolygon(points));
		afterDrawing();
	}


	public Path fillPathStartingAt(double x, double y) {
		return new Path(g, x, y, true);
	}

	public Path drawPathStartingAt(double x, double y) {
		return new Path(g, x, y, false);
	}

	private void drawImageInternal(double x, double y, double width, double height, java.awt.Image image, Interpolation interpolation) {
		setRenderingHint(interpolation);
		g.drawImage(image, (int)x, (int)y, (int)width, (int)height, null);
	}

	private void drawImageInternal(double x, double y, double width, double height, Image image, Interpolation interpolation) {
		drawImageInternal(x, y, width, height, image.image, interpolation);
	}

	/**
	 * Draws an image at the specified (x, y) coordinate.
	 * The width and height of the image will be used to draw the image.
	 * @param x The distance in pixel from the left side of the canvas to the left side of the image.
	 * @param y The distance in pixel from the top side of the canvas to the top side of the image.
	 * @param image Any image.
	 */
	public void drawImage(double x, double y, Image image) {
		if (image == null) throw createParameterNullException("image");

		beforeDrawing();
		drawImageInternal(x, y, image.getWidth(), image.getHeight(), image, Interpolation.NEAREST_NEIGHBOR);
		afterDrawing();
	}

	/**
	 * Draws an image at the specified (x, y) coordinate.
	 * The image will be rescaled to fit within the width and height given as parameters.
	 * @param x The distance in pixel from the left side of the canvas to the left side of the image.
	 * @param y The distance in pixel from the top side of the canvas to the top side of the image.
	 * @param width The width of the image on the canvas.
	 * @param height The height of the image on the canvas.
	 * @param image Any image.
	 */
	public void drawImage(double x, double y, double width, double height, Image image) {
		if (width < 0) throw createParameterMustBeGreaterOrEqualToZeroException("width");
		if (height < 0) throw createParameterMustBeGreaterOrEqualToZeroException("height");
		if (image == null) throw createParameterNullException("image");

		beforeDrawing();
		drawImageInternal(x, y, width, height, image, Interpolation.BICUBIC);
		afterDrawing();
	}

	/**
	 * Draws an image at the specified (x, y) coordinate.
	 * The image will be rescaled to fit within the width and height given as parameters.
	 * @param x The distance in pixel from the left side of the canvas to the left side of the image.
	 * @param y The distance in pixel from the top side of the canvas to the top side of the image.
	 * @param width The width of the image on the canvas.
	 * @param height The height of the image on the canvas.
	 * @param image Any image.
	 * @param interpolation Defines the way the images is interpolated when scaled. See {@link Interpolation}.
	 */
	public void drawImage(double x, double y, double width, double height, Image image, Interpolation interpolation) {
		if (width < 0) throw createParameterMustBeGreaterOrEqualToZeroException("width");
		if (height < 0) throw createParameterMustBeGreaterOrEqualToZeroException("height");
		if (image == null) throw createParameterNullException("image");

		beforeDrawing();
		drawImageInternal(x, y, width, height, image, interpolation);
		afterDrawing();
	}

	/**
	 * Colors the whole canvas in white.
	 */
	public void clear() {
		clear(Palette.WHITE);
	}

	/**
	 * Colors the whole canvas in the color given as a parameter.
	 * @param color The color the canvas will be colored in.
	 */
	public void clear(Color color) {
		if (color == null) throw createParameterNullException("color");

		beforeDrawing();

		clearInternal(color);

		afterDrawing();
	}

	private void clearInternal(Color color) {
		Color c = getColor();
		Matrix2D m = getTransformation();
		setTransformationToIdentity();
		setColor(color);
		g.fill(createSharpRectangle(0, 0, getWidth(), getHeight()));
		setColor(c);
		setTransformation(m);
	}

	/**
	 * Copies this CodeDrawImage onto the graphics object.
	 * Also applies an interpolation rendering hint if the graphics target is an instance of Graphics2D.
	 * @param target a graphics object.
	 * @param interpolation Defines the way the images is interpolated when drawn onto the graphics object. See {@link Interpolation}.
	 */
	public void copyTo(Graphics target, Interpolation interpolation) {
		if (target == null) throw createParameterNullException("target");
		if (interpolation == null) throw createParameterNullException("interpolation");

		if (target instanceof Graphics2D) {
			RenderingHintValue.applyHint((Graphics2D) target, interpolation);
		}

		Color c = target.getColor();
		target.setColor(Palette.WHITE);
		target.drawRect(0, 0, getWidth(), getHeight());
		target.drawImage(image, 0, 0, getWidth(), getHeight(), Palette.WHITE, null);
		target.setColor(c);
	}

	/**
	 * Creates a copy of this CodeDrawImage in the form of a BufferedImage.
	 * @param type Defines the way the image is encoded in memory.
	 * @return a BufferedImage.
	 */
	public BufferedImage toBufferedImage(BufferedImageType type) {
		BufferedImage result = new BufferedImage(width, height, type.getType());
		Graphics2D g = result.createGraphics();
		g.drawImage(image, 0, 0, width, height, Palette.WHITE, null);
		g.dispose();
		return result;
	}

	/**
	 * Creates a copy of this CodeDrawImage in the form of a BufferedImage.
	 * The BufferedImage type INT_ARGB is chosen as a default.
	 * @return a BufferedImage.
	 */
	public BufferedImage toBufferedImage() {
		return toBufferedImage(BufferedImageType.INT_ARGB);
	}

	protected void beforeDrawing() { }

	protected void afterDrawing() { }

	private static Line2D createLine(double startX, double startY, double endX, double endY) {
		return new Line2D.Double(
				startX, startY,
				endX, endY
		);
	}

	private static QuadCurve2D createCurve(double startX, double startY, double controlX, double controlY, double endX, double endY) {
		return new QuadCurve2D.Double(
				startX, startY,
				controlX, controlY,
				endX, endY
		);
	}

	private static CubicCurve2D createBezierCurve(double startX, double startY, double control1X, double control1Y, double control2X, double control2Y, double endX, double endY) {
		return new CubicCurve2D.Double(
				startX, startY,
				control1X, control1Y,
				control2X, control2Y,
				endX, endY
		);
	}

	private Shape createFillRectangle(double x, double y, double width, double height) {
		if (corner == Corner.SHARP) {
			return createSharpRectangle(x, y, width, height);
		}
		else if (corner == Corner.ROUND) {
			return createRoundRectangle(x, y, width, height);
		}
		else {
			return createBevelRectangle(x, y, width, height);
		}
	}

	private Shape createDrawRectangle(double x, double y, double width, double height) {
		return createSharpRectangle(x, y, width, height);
	}

	private static Rectangle2D createSharpRectangle(double x, double y, double width, double height) {
		return new Rectangle2D.Double(
				x, y,
				width, height
		);
	}

	private RoundRectangle2D createRoundRectangle(double x, double y, double width, double height) {
		return new RoundRectangle2D.Double(
				x, y,
				width, height,
				lineWidth, lineWidth
		);
	}

	private Path2D createBevelRectangle(double x, double y, double width, double height) {
		double lw = lineWidth / 2;
		return createPolygon(
				x + lw, y,
				x + width - lw, y,
				x + width, y + lw,
				x + width, y + height - lw,
				x + width - lw, y + height,
				x + lw, y + height,
				x, y + height - lw,
				x, y + lw
		);
	}

	private static Ellipse2D createEllipse(double x, double y, double horizontalRadius, double verticalRadius) {
		return new Ellipse2D.Double(
				x - horizontalRadius, y - verticalRadius,
				2 * horizontalRadius, 2 * verticalRadius
		);
	}

	private static Arc2D createArc(double x, double y, double horizontalRadius, double verticalRadius, double startRadians, double sweepRadians) {
		return new Arc2D.Double(
				x - horizontalRadius,
				y - verticalRadius,
				2 * horizontalRadius,
				2 * verticalRadius,
				transformStart(startRadians),
				transformSweep(sweepRadians),
				Arc2D.OPEN
		);
	}

	private static Arc2D createPie(double x, double y, double horizontalRadius, double verticalRadius, double startRadians, double sweepRadians) {
		return new Arc2D.Double(
				x - horizontalRadius,
				y - verticalRadius,
				2 * horizontalRadius,
				2 * verticalRadius,
				transformStart(startRadians),
				transformSweep(sweepRadians),
				Arc2D.PIE
		);
	}

	private static Path2D createTriangle(double x1, double y1, double x2, double y2, double x3, double y3) {
		return createPolygon(
				x1, y1,
				x2, y2,
				x3, y3
		);
	}

	private static Path2D createPolygon(double... doubles) {
		Path2D.Double result = new Path2D.Double();

		result.moveTo(doubles[0], doubles[1]);
		for (int i = 2; i < doubles.length; i += 2) {
			result.lineTo(doubles[i], doubles[i + 1]);
		}
		result.closePath();

		return result;
	}

	private static double transformStart(double startRadians) {
		return Math.toDegrees(-startRadians);
	}

	private static double transformSweep(double sweepRadians) {
		return Math.toDegrees(-sweepRadians);
	}

	private static int upscale(double scale) {
		return Math.max(1, (int)Math.ceil(scale));
	}

	private static AffineTransform getMaximumDPIFromAllScreens() {
		AffineTransform max = new AffineTransform();

		for (GraphicsDevice device : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
			for (GraphicsConfiguration configuration : device.getConfigurations()) {
				max = maxAffineTransformScale(max, configuration.getDefaultTransform());
			}
		}

		return max;
	}

	private static AffineTransform maxAffineTransformScale(AffineTransform a, AffineTransform b) {
		AffineTransform result = new AffineTransform();
		result.setToScale(
				Math.max(a.getScaleX(), b.getScaleX()),
				Math.max(a.getScaleY(), b.getScaleY())
		);
		return result;
	}

	private static IllegalArgumentException createParameterNullException(String parameterName) {
		return new IllegalArgumentException("The parameter " + parameterName + " cannot be null.");
	}

	private static IllegalArgumentException createParameterMustBeGreaterThanZeroException(String parameterName) {
		return new IllegalArgumentException("The parameter " + parameterName + " must be greater than zero.");
	}

	private static IllegalArgumentException createParameterMustBeGreaterOrEqualToZeroException(String parameterName) {
		return new IllegalArgumentException("The parameter " + parameterName + " must be equal or greater than zero.");
	}

	private static boolean isInvalidPolygonCount(double[] polygonParameter) {
		return polygonParameter.length < 4 || (polygonParameter.length & 1) == 1;
	}

	private static IllegalArgumentException createPolygonCountException(double[] polygonParameter, String methodName) {
		if (polygonParameter.length < 4) {
			return new IllegalArgumentException("You must pass at least 4 arguments to " + methodName + ". A polygon must have at least two points (2 arguments for each point).");
		}
		else if ((polygonParameter.length & 1) == 1) {
			return new IllegalArgumentException(methodName + " must be called with an even number of arguments. Each argument pair represents the x and y coordinate of on point of the polygon.");
		}
		else {
			throw new RuntimeException();
		}
	}

	private static <T> T checkParameterNull(T parameter, String parameterName) {
		if (parameter == null)
			throw new IllegalArgumentException("The parameter " + parameterName + " cannot be null.");
		else
			return parameter;
	}
}
