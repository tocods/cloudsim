package com.wfc.cloudsim.cloudsim.container.utils;

import au.com.bytecode.opencsv.CSVReader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 * Run.
 *

 */
public class CostumeCSVReader {
	private static List<String[]> fileData;

	public CostumeCSVReader(File inputFile) {
		// TODO Auto-generated method stub
		CSVReader reader = null;
		try 
		{
//			Log.printLine(inputFile);
			//Get the CSVReader instance with specifying the delimiter to be used
			reader = new CSVReader(new FileReader(inputFile));
			fileData= reader.readAll();
			
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally	{
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

	}
}



	public static List<String[]> getFileData() {
		return fileData;
	}



	public static void setFileData(List<String[]> fileData) {
		CostumeCSVReader.fileData = fileData;
	}
 
}

