package com.AVMisc;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;

import org.jcodec.api.awt.AWTSequenceEncoder;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamException;

public class VideoUtils {
	
	private AtomicBoolean recordingVideo = new AtomicBoolean();
	private Thread recordingVideoThread;
	private List<Webcam> webcams;
	private Scanner sc;
	
	public VideoUtils(Scanner sc) {
		this.sc = sc;
		webcams = Webcam.getWebcams();
		recordingVideo.set(false);
	}
	
	private Webcam chooseWebcam() {
		try {
			listVideoDevices();
			System.out.print("Enter the ID of the webcam to use: ");
			int ID = sc.nextInt();
			sc.nextLine();
			return webcams.get(ID);
		}
		catch (IndexOutOfBoundsException | InputMismatchException e) {
			throw new WebcamException(e.getMessage());
		}
	}
	
	public void listVideoDevices() {
		if(webcams.size() != 0) {
			System.out.println("\n------------------------\n     VIDEO DEVICES\n------------------------");
			for(int i = 0; i < webcams.size(); i++) {
				Webcam webcam = webcams.get(i);
				System.out.printf("\tID: %d, Name: %s\n", i, webcam.getName());
			}
			
		}
		else {
			System.out.println("\nNo video devices");
		}
	}
	
	/***
	 * Record Video
	 */
	public void recordVideo() {
		if(!recordingVideo.get()) {
			Webcam webcam;
			int seconds;
			try {
				webcam = chooseWebcam();
				System.out.print("Enter the number of seconds to record (About 500 KB/S): ");
				seconds = sc.nextInt();
				sc.nextLine();
			}
			catch (WebcamException | InputMismatchException e) {
				System.out.println(e.getMessage());
				return;
			}
			
			recordingVideoThread = new Thread() {
				public void run() {
					String fileName = String.format("Video_%s.mp4", CommonUtils.DTF.format(LocalDateTime.now()));
					AWTSequenceEncoder encoder = null;
					try {
						File file = new File(fileName);
						recordingVideo.set(true);
						webcam.open();
						int fps = 24;
						encoder = AWTSequenceEncoder.createSequenceEncoder(file, fps);
						int framesToEncode = seconds * fps;
						BufferedImage image = null;
						System.out.println("Recording video...");
						

						for (int i = 0; i < framesToEncode; i++) {
							image = webcam.getImage();
							encoder.encodeImage(image);
						}

					}
					catch (InputMismatchException | IndexOutOfBoundsException | IOException e) {
						System.out.println(e.getMessage());
					}
					finally {
						recordingVideo.set(false);
						webcam.close();
						try {
							encoder.finish();
						}
						catch (IOException e) {
							System.out.println(e.getMessage());
						}
						System.out.printf("Video saved as %s\n", fileName);
					}
				}
			};
			recordingVideoThread.start();
		}
	}

	
	
	/***
	 * Stop Recording Video
	 */
	public void stopRecordingVideo() {
		if(recordingVideo.get()) {
			recordingVideoThread.interrupt();
		}
	}
	
	/***
	 * Take a Snapshot
	 */
	public void takeSnapshot() {
		if(webcams.size() > 0) {
			try {
				Webcam webcam = chooseWebcam();
				webcam.open();
				BufferedImage image = webcam.getImage();
				String fileName = String.format("Snapshot_%s.png", CommonUtils.DTF.format(LocalDateTime.now()));
				ImageIO.write(image, "PNG", new File(fileName));
				webcam.close();
				System.out.printf("Snapshot saved as '%s'\n", fileName);
			}
			catch (WebcamException | NullPointerException | IOException e) {
				System.out.println(e.getMessage());
			}
		}
	}

}
