package com.AVMisc;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.Mixer.Info;

import de.ralleytn.simple.audio.Audio;
import de.ralleytn.simple.audio.AudioEvent;
import de.ralleytn.simple.audio.AudioException;
import de.ralleytn.simple.audio.FileFormat;
import de.ralleytn.simple.audio.Recorder;
import de.ralleytn.simple.audio.StreamedAudio;

public class AudioUtils {
	private long AUDIO_RECORD_TIME = 0;
	private AtomicBoolean recordingAudio = new AtomicBoolean();
	private AtomicBoolean playingAudio = new AtomicBoolean();
	private AtomicBoolean stopStreaming = new AtomicBoolean();
	private AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
	private TargetDataLine line = null;
	private Thread recordingAudioThread;
	private Thread playingAudioThread;
	private String recordingAudioFileName;
	private String playingAudioFileName;
	private String streamingAudioIP;
	private Audio audio;
	private Recorder recorder;
	private int streamingAudioPort;
	private int recordingAudioTime;
	private Scanner sc;
	

	public AudioUtils(Scanner sc) {
		this.sc = sc;
		recordingAudio.set(false);
		playingAudio.set(false);
		stopStreaming.set(false);
	}
	
	/***
	 * List Devices
	 */
	public void listAudioDevices() {
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
	public void recordAudio(boolean streaming) {
		// Check if audio is already being recorded
		if (!recordingAudio.get()) {
			if(streaming) {
				try {
					// Get the IP address and port
					System.out.print("Enter the IP address of the server: ");
					streamingAudioIP = sc.nextLine();
					System.out.print("Enter the port number: ");
					streamingAudioPort = Integer.parseInt(sc.nextLine());
				} catch (NumberFormatException e) {
					System.out.println(e.getMessage());
					return;
				}
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
								recorder = new Recorder(FileFormat.AU, audioFormat);
								recorder.start(dos);
								recordingAudio.set(true);
								System.out.println("Streaming audio for 3 minutes...");
								Thread.sleep(180000);
							} 
							catch ( IOException | InterruptedException | AudioException e) {
								System.out.println(e.getMessage());
							}
							finally {
								recorder.stop();
								socket.close();
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
						recordingAudioFileName = String.format("Audio_%s.au", CommonUtils.DTF.format(LocalDateTime.now()));
						
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
	public void stopRecordingAudio() {
		if(recordingAudio.get()) {
			recordingAudioThread.interrupt();
			if(line != null) {
				line.stop();
				line.close();
			}
		}
	}
	
	/***
	 * Play Audio
	 */
	public void playAudio() {
		if(!playingAudio.get()) {
			System.out.print("Enter the path of the audio file to play on the local machine: ");
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
								playingAudio.set(false);
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
	public void stopPlayingAudio() {
		if(playingAudio.get()) {
			audio.stop();
			audio.close();
			audio = null;
			playingAudio.set(false);
		}
	}
}
