package file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ReadWriteFile {
	public static ArrayList<SheetReader> readExcelFile(String filename) throws Exception {
		ArrayList<SheetReader> sheetReaders = new ArrayList<>();
		
		File file = new File(filename);
		FileInputStream input = new FileInputStream(file);

		Workbook workbook = null;
		if(filename.endsWith(".xlsx")) {
			workbook = new XSSFWorkbook(input);	
		}
		else if(filename.endsWith(".xls")) {
			workbook = new HSSFWorkbook(input);
		}

		int sheetNum = workbook.getNumberOfSheets();
		
		for(int i = 0 ; i < sheetNum ; i++) {
			SheetReader sheetReader = new SheetReader(workbook.getSheetAt(i));
			if(sheetReader.getOverFlow())
				return null;
			sheetReaders.add(sheetReader);
		}
		
		return sheetReaders;
	}
	
	public static String writeExcelFile(String filename, ArrayList<SheetReader> sheetReaders) throws IOException {
		File srcFile = new File(filename);
		FileInputStream input = new FileInputStream(srcFile);
		FileOutputStream output = null;
		Workbook workbook = null;
		String desFileName = null;
		if(filename.endsWith(".xlsx")) {
			workbook = new XSSFWorkbook(input);
			desFileName = filename.substring(0, filename.length()-5) + "_handled.xlsx";
		}
		else if(filename.endsWith(".xls")) {
			workbook = new HSSFWorkbook(input);
			desFileName = filename.substring(0, filename.length()-4) + "_handled.xls";
		}
		input.close();
		File desFile = new File(desFileName);
		if(!desFile.exists()) desFile.createNewFile();
		else {
			int result = JOptionPane.showConfirmDialog(null, "File has existed, sure to replace it ?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if(result != JOptionPane.YES_OPTION) return null;
		}
		output = new FileOutputStream(desFile);
		
		for(int i = 0 ; i < workbook.getNumberOfSheets() ; i++) {
			Sheet sheet = workbook.getSheetAt(i);
			SheetReader sheetReader = sheetReaders.get(i);
			
			for(int row = 0 ; row < 500 ; row++) {
				for(int column = 0 ; column < 26 ; column++) {
					if(sheetReader.getCells()[row][column].getCellType() != Cell.CELL_TYPE_FORMULA)
						continue;
					Cell cell = sheet.getRow(row).getCell(column);
					cell.setCellValue(Double.parseDouble(sheetReader.getCells()[row][column].getValue()));
					cell.setCellFormula(sheetReader.getCells()[row][column].getFormula());
					cell.setCellType(Cell.CELL_TYPE_FORMULA);
				}
			}
		}
		
		workbook.write(output);
		output.close();
		return desFileName;
	}

}
