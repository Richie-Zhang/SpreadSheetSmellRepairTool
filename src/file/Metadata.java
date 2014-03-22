package file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import core.StructDefine;

public class Metadata {
	private static String getMetadataFilename(String filename) {
		return filename.substring(0, filename.lastIndexOf(".")) + ".meta";
	}
	
	public static ArrayList<StructDefine.Region> loadMetadataSnippet(String filename, int currentSheetIndex) throws IOException {
		String metaFilename = getMetadataFilename(filename);
		File metaFile = new File(metaFilename);
		if(!metaFile.exists()) return null;
		
		ArrayList<StructDefine.Region> snippets = new ArrayList<>();
		BufferedReader bw = new BufferedReader(new FileReader(metaFile));
		String line = null;
		while((line = bw.readLine()) != null){
			if(!line.trim().startsWith("[Snippet]." + currentSheetIndex)) continue;
			String[] temp = line.split("\\.");
			StructDefine.Region snip = new StructDefine.Region(new StructDefine.Position(Integer.parseInt(temp[2]), Integer.parseInt(temp[3])),
					new StructDefine.Position(Integer.parseInt(temp[4]), Integer.parseInt(temp[5])));
			snippets.add(snip);
		}
		bw.close();
		return snippets;
	}
	
	public static ArrayList<StructDefine.Region> loadMetadataCellArray(String filename, int currentSheetIndex) throws IOException {
		String metaFilename = getMetadataFilename(filename);
		File metaFile = new File(metaFilename);
		if(!metaFile.exists()) return null;
		
		ArrayList<StructDefine.Region> cellArrays = new ArrayList<>();
		BufferedReader bw = new BufferedReader(new FileReader(metaFile));
		String line = null;
		while((line = bw.readLine()) != null){
			if(!line.trim().startsWith("[CellArray]." + currentSheetIndex)) continue;
			String[] temp = line.split("\\.");
			StructDefine.Region cellArray = new StructDefine.Region(new StructDefine.Position(Integer.parseInt(temp[2]), Integer.parseInt(temp[3])),
					new StructDefine.Position(Integer.parseInt(temp[4]), Integer.parseInt(temp[5])));
			cellArrays.add(cellArray);
		}
		bw.close();
		return cellArrays;
	}
	
	public static boolean saveMetadataSnippet(String filename, int currentSheetIndex, ArrayList<StructDefine.Region> snippets) throws IOException {
		String metaFilename = getMetadataFilename(filename);
		File metaFile = new File(metaFilename);
		if(!metaFile.exists()) metaFile.createNewFile();
		else{
			if(!removeMetadataSnippet(metaFilename, currentSheetIndex)) return false;
		}
		
		FileWriter fileWriter = new FileWriter(metaFilename, true);
		for(StructDefine.Region snip : snippets) {
			fileWriter.write("[Snippet]." + currentSheetIndex + "." + snip.GetTopLeft().GetRow() + "." + snip.GetTopLeft().GetColumn() + "." + snip.GetBottomRight().GetRow() + "." + snip.GetBottomRight().GetColumn() + "\n");
		}
		fileWriter.flush();
		fileWriter.close();
		return true;
	}
	
	public static boolean saveMetadataCellArray(String filename, int currentSheetIndex, ArrayList<StructDefine.Region> cellArrays) throws IOException {
		String metaFilename = getMetadataFilename(filename);
		File metaFile = new File(metaFilename);
		if(!metaFile.exists()) metaFile.createNewFile();
		else{
			if(!removeMetadataCellArray(metaFilename, currentSheetIndex)) return false;
		}
		
		FileWriter fileWriter = new FileWriter(metaFilename, true);
		for(StructDefine.Region cellArray : cellArrays) {
			fileWriter.write("[CellArray]." + currentSheetIndex + "." + cellArray.GetTopLeft().GetRow() + "." + cellArray.GetTopLeft().GetColumn() + "." + cellArray.GetBottomRight().GetRow() + "." + cellArray.GetBottomRight().GetColumn() + "\n");
		}
		fileWriter.flush();
		fileWriter.close();
		return true;
	}
	
	public static boolean removeMetadataSnippet(String filename, int currentSheetIndex) throws IOException {
		File inFile = new File(filename);
		File tempFile = new File(inFile.getAbsolutePath() + ".tmp");
		BufferedReader br = new BufferedReader(new FileReader(inFile));
		PrintWriter pw = new PrintWriter(new FileWriter(tempFile));
		String line = null;
		while ((line = br.readLine()) != null) {
			if (!line.trim().startsWith("[Snippet]." + currentSheetIndex)) {
				pw.println(line);	
			}
		}
		pw.flush();
		pw.close();
		br.close();
		if (!inFile.delete()) return false;
		if (!tempFile.renameTo(inFile)) return false;
		return true;
	}
	
	public static boolean removeMetadataCellArray(String filename, int currentSheetIndex) throws IOException {
		File inFile = new File(filename);
		File tempFile = new File(inFile.getAbsolutePath() + ".tmp");
		BufferedReader br = new BufferedReader(new FileReader(inFile));
		PrintWriter pw = new PrintWriter(new FileWriter(tempFile));
		String line = null;
		while ((line = br.readLine()) != null) {
			if (!line.trim().startsWith("[CellArray]." + currentSheetIndex)) {
				pw.println(line);	
			}
		}
		pw.flush();
		pw.close();
		br.close();
		if (!inFile.delete()) return false;
		if (!tempFile.renameTo(inFile)) return false;
		return true;
	}
}
