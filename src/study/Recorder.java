package study;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import core.DetectRepairSmell;
import core.ExtractCellArray;
import core.StructDefine;
import file.SheetReader;

public class Recorder {
	@SuppressWarnings("static-access")
	public static void main(String[] args) throws Exception {
		Recorder recorder = new Recorder();
		recorder.combineBooks("C:\\Users\\Richie\\Desktop\\books");
	}
	
	ArrayList<OneRecord> records;
	
	public Recorder() {
		records = new ArrayList<>();
	}
	
	private OneRecord handleOneFile(File file) throws Exception {
		String folder = file.getParent();
		String filename = file.getName();
		System.err.println(filename);
		
		OneRecord oneRecord = new OneRecord(folder, filename);
		
		FileInputStream input = new FileInputStream(file);

		Workbook workbook = null;
		if(filename.endsWith(".xlsx") || filename.endsWith(".XLSX")) {
			try{
				workbook = new XSSFWorkbook(input);	
			} catch (Exception e) {
				return oneRecord;
			}
		}
		else if(filename.endsWith(".xls") || filename.endsWith(".XLS")) {
			try{
				workbook = new HSSFWorkbook(input);
			} catch (Exception e) {
				return oneRecord;
			}
		}
		if(workbook == null) return oneRecord;
		int sheetNum = workbook.getNumberOfSheets();
		for(int i = 0 ; i < sheetNum ; i++) {
			SheetReader sheetReader = new SheetReader(workbook.getSheetAt(i));
			if(!sheetReader.getValid())
				return oneRecord;
		}
		
		oneRecord.set_canOpenbyPOI(true);
		oneRecord.set_numOfTables(sheetNum);
		
		int numOfFormulaSheets = 0;
		int numOfCellArrays = 0;
		for(int i = 0 ; i < sheetNum ; i++) {
			SheetReader sheetReader = new SheetReader(workbook.getSheetAt(i));
			if(sheetReader.getHasFormula()) {
				oneRecord.withFormula = true;
				numOfFormulaSheets++;
				ExtractCellArray extractCellArray = new ExtractCellArray(sheetReader);
				extractCellArray.identifySnippets();
				extractCellArray.extractCellArrays();
				ArrayList<StructDefine.Region> cellArrayList = extractCellArray.getCellArrays();
				numOfCellArrays += cellArrayList.size();
				
				for(StructDefine.Region cellArray : cellArrayList) {
					DetectRepairSmell detectRepairSmell = new DetectRepairSmell(sheetReader, cellArray);
					if(detectRepairSmell.hasSmell()) {
						oneRecord.add_cellArraysWithSmell(new OneRecord.CellArray(i, workbook.getSheetName(i), cellArray));
					}
				}
			}
		}
		oneRecord.set_numOfFormulaTables(numOfFormulaSheets);
		oneRecord.set_numOfCellArrays(numOfCellArrays);
		
		return oneRecord;
	}

	public void handleFiles(String filePath) throws Exception {
		File filepath = new File(filePath); 
		String[] filelist = filepath.list();
		for (int i = 0; i < filelist.length; i++) {
			if(filelist[i].endsWith(".xls") || filelist[i].endsWith(".xlsx") || filelist[i].endsWith(".XLS") || filelist[i].endsWith(".XLSX")) {
				File readfile = new File(filePath + "\\" + filelist[i]); 
				records.add(handleOneFile(readfile));
			}
		}
	}

	public void writeToExcel(String fileName) throws IOException {
		File desFile = new File(fileName);
		if(!desFile.exists()) desFile.createNewFile();
		FileOutputStream output = new FileOutputStream(desFile);
		
		Workbook workbook = new XSSFWorkbook();
		//CreationHelper createHelper = workbook.getCreationHelper();
		Sheet sheet = workbook.createSheet("new sheet");
		Row row = sheet.createRow(0);
		row.createCell(0).setCellValue("folder");
		row.createCell(1).setCellValue("file name");
		row.createCell(2).setCellValue("POI");
		row.createCell(3).setCellValue("worksheets");
		row.createCell(4).setCellValue("with formula");
		row.createCell(5).setCellValue("formula worksheets");
		row.createCell(6).setCellValue("cell arrays");
		row.createCell(7).setCellValue("smells");
		
		for(int i = 0 ; i < records.size() ; i++) {
			OneRecord oneRecord = records.get(i);
			row = sheet.createRow(i+1);
			row.createCell(0).setCellValue(getFolder(oneRecord.get_folder()));
			row.createCell(1).setCellValue(oneRecord.get_filename());
			row.createCell(2).setCellValue(oneRecord.get_canOpenbyPOI());
			row.createCell(3).setCellValue(oneRecord.get_numOfTables());
			row.createCell(4).setCellValue(oneRecord.get_withFormula());
			row.createCell(5).setCellValue(oneRecord.get_numOfFormulaTables());
			row.createCell(6).setCellValue(oneRecord.get_numOfCellArrays());
			row.createCell(7).setCellValue(oneRecord.get_numOfSmellCellArrays());
		}
		
		workbook.write(output);
		output.close();
	}
	
	private String getFolder(String folder) {
		if(folder.contains("\\cs101\\"))
			return "cs101";
		else if(folder.contains("\\database\\"))
			return "database";
		else if(folder.contains("\\filby\\"))
			return "filby";
		else if(folder.contains("\\financial\\"))
			return "financial";
		else if(folder.contains("\\forms3\\"))
			return "forms3";
		else if(folder.contains("\\grades\\"))
			return "grades";
		else if(folder.contains("\\homework\\"))
			return "homework";
		else if(folder.contains("\\inventory\\"))
			return "inventory";
		else if(folder.contains("\\jackson\\"))
			return "jackson";
		else if(folder.contains("\\modeling\\"))
			return "modeling";
		else if(folder.contains("\\personal\\"))
			return "personal";
		else return folder;
	}

	public static void combineBooks(String filePath) throws IOException {
		Workbook workbook = new XSSFWorkbook();
		//CreationHelper createHelper = workbook.getCreationHelper();
		Sheet sheet = workbook.createSheet("new sheet");
		Row row = sheet.createRow(0);
		row.createCell(0).setCellValue("folder");
		row.createCell(1).setCellValue("file name");
		row.createCell(2).setCellValue("POI");
		row.createCell(3).setCellValue("worksheets");
		row.createCell(4).setCellValue("with formula");
		row.createCell(5).setCellValue("formula worksheets");
		row.createCell(6).setCellValue("cell arrays");
		row.createCell(7).setCellValue("smells");
		int index = 1;
		
		File filepath = new File(filePath); 
		String[] filelist = filepath.list();
		for (int i = 0; i < filelist.length; i++) {
			if(filelist[i].endsWith(".xlsx")) {
				File readfile = new File(filePath + "\\" + filelist[i]);
				FileInputStream input = new FileInputStream(readfile);
				Workbook wb = new XSSFWorkbook(input);
				input.close();
				Sheet wbs = wb.getSheetAt(0);
				for(int j = 1 ; j <= wbs.getLastRowNum(); j++) {
					Row readRow = wbs.getRow(j);
					row = sheet.createRow(index++);
					row.createCell(0).setCellValue(readRow.getCell(0).getStringCellValue());
					row.createCell(1).setCellValue(readRow.getCell(1).getStringCellValue());
					row.createCell(2).setCellValue(readRow.getCell(2).getBooleanCellValue());
					row.createCell(3).setCellValue((int)(readRow.getCell(3).getNumericCellValue()));
					row.createCell(4).setCellValue(readRow.getCell(4).getBooleanCellValue());
					row.createCell(5).setCellValue((int)(readRow.getCell(5).getNumericCellValue()));
					row.createCell(6).setCellValue((int)(readRow.getCell(6).getNumericCellValue()));
					row.createCell(7).setCellValue((int)(readRow.getCell(7).getNumericCellValue()));
				}
			}
		}
		
		File desFile = new File(filePath + "\\book.xlsx");
		if(!desFile.exists()) desFile.createNewFile();
		FileOutputStream output = new FileOutputStream(desFile);
		workbook.write(output);
		output.close();
	}
}
