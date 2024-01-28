import codedraw.*;
import nu.pattern.OpenCV;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;

public class Video {
	public static void main(String[] args) throws IOException {
		// Load the OpenCV library
		OpenCV.loadLocally();

		// Path to the input video file
		String videoPath = "vid.mp4";
		String outputDir = "output";

		// Create a VideoCapture object
		VideoCapture videoCapture = new VideoCapture(videoPath);

		// Check if the VideoCapture object is opened successfully
		if (!videoCapture.isOpened()) {
			System.out.println("Error: Couldn't open the video file.");
			return;
		}

		CodeDraw fs = new CodeDraw((int) videoCapture.get(Videoio.CAP_PROP_FRAME_WIDTH),
				(int) videoCapture.get(Videoio.CAP_PROP_FRAME_HEIGHT));
		// Process each frame in the video
		Mat frame = new Mat();
		while (videoCapture.read(frame)) {


			codedraw.Image image = new codedraw.Image(mat2awtImg(frame));

			fs.drawImage(0, 0, image);

			fs.show();
		}

		// Release the VideoCapture object
		videoCapture.release();
	}

	public static Image mat2awtImg(Mat mat) throws IOException {
		//Encoding the image
		MatOfByte matOfByte = new MatOfByte();
		Imgcodecs.imencode(".jpg", mat, matOfByte);
		//Storing the encoded Mat in a byte array
		byte[] byteArray = matOfByte.toArray();
		//Preparing the Buffered Image
		InputStream in = new ByteArrayInputStream(byteArray);
		Image img = ImageIO.read(in);
		return img;
	}
}
