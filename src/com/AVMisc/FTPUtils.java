package com.AVMisc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.InputMismatchException;
import java.util.Scanner;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

public class FTPUtils {
	private FTPClient ftp;
	private Scanner sc;
	
	public FTPUtils(Scanner sc) {
		this.sc = sc;
	}
	
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
		catch(IOException | InputMismatchException e) {
			System.out.println(e.getMessage());
			return false;
		}
	}
	
	/***
	 * Upload files
	 */
	public void upload() {
		try {
			if(!connectToFTPServer()) {
				System.out.println(ftp.getReplyString());
			}
			// Continue if the server connection attempt was successful
			else {
				ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
				// Upload a file
				System.out.print("Enter the path of the file on the local machine to upload: ");
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
	 * Download files from your server to the local machine
	 */
	public void download() {
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

}
