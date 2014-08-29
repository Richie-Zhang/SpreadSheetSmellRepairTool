package core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor.BLUE;
import org.apache.poi.hssf.util.HSSFColor.RED;
import org.apache.poi.hssf.util.HSSFColor.YELLOW;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import core.StructDefine.Position;
import core.StructDefine.Region;
import core.StructDefine.RepairAdvise;
import core.StructDefine.SmellAndRepair;
import file.MyCellStyle;
import file.SheetReader;

public class AutoDetectSmell {
	public static boolean autoDetectFile(File file) throws Exception {
		boolean hasSmell = false;
		FileInputStream input = new FileInputStream(file);
		Workbook workbook = null;
		String desFileName = null;
		if(file.getName().endsWith(".xlsx") || file.getName().endsWith(".XLSX")) {
			workbook = new XSSFWorkbook(input);
			desFileName = file.getAbsolutePath().substring(0, file.getAbsolutePath().length()-5) + " (annotated).xlsx";
		}
		else if(file.getName().endsWith(".xls") || file.getName().endsWith(".XLS")) {
			workbook = new HSSFWorkbook(input);
			desFileName = file.getAbsolutePath().substring(0, file.getAbsolutePath().length()-4) + " (annotated).xls";
		}
		int sheetNum = workbook.getNumberOfSheets();
		
		for(int i = 0 ; i < sheetNum ; i++) {
			Sheet sheet = workbook.getSheetAt(i);
			SheetReader sheetReader = new SheetReader(sheet);
			ExtractCellArray extractCellArray = new ExtractCellArray(sheetReader);
			extractCellArray.identifySnippets();
			extractCellArray.extractCellArrays();
			ArrayList<Region> cellArrayList = extractCellArray.getCellArrays();
			
			MyCellStyle myCellStyle = new MyCellStyle(workbook, sheet);
			
			for(Region cellArray : cellArrayList) {
				DetectRepairSmell detectRepairSmell = new DetectRepairSmell(sheetReader, cellArray);
				if(detectRepairSmell.hasSmell()) {
					hasSmell = true;
					boolean recovery = detectRepairSmell.recovery();
					if(!recovery) detectRepairSmell.synthesize();
					try {
						detectRepairSmell.repairSmell();
					} catch (Exception e1) {
						e1.printStackTrace();
					}
					
					if(detectRepairSmell.getState() != 1) {
						SmellAndRepair smellAndRepair = detectRepairSmell.getSmellAndRepair();
						for(int j = 0 ; j < smellAndRepair.GetPositions().size() ; j++) {
							Position position = smellAndRepair.GetPositions().get(j);
							RepairAdvise repairAdvise = smellAndRepair.GetRepairAdvises().get(j);

							char c = (char) ('A' + position.GetColumn());
							String posString = c + "" + (position.GetRow() + 1);
							String formulaString = repairAdvise.GetFormula();
							String commentText = null;
							if(repairAdvise.GetType() == 1) {
								myCellStyle.setCellColor(position, RED.index);
								commentText = "Error: Change Cell "+posString+" to formula: "+formulaString+", value is:"+repairAdvise.GetValue()+"\n";
							}
							else if(repairAdvise.GetType() == 2) {
								myCellStyle.setCellColor(position, YELLOW.index);
								commentText = "Smell: Change Cell "+posString+" to formula: "+formulaString+", value is:"+repairAdvise.GetValue()+"\n";
							}
							myCellStyle.setCellComment(position, commentText);
						}
					}
					else {
						int startRow = cellArray.GetTopLeft().GetRow(), endRow = cellArray.GetBottomRight().GetRow();
				    	int startColumn = cellArray.GetTopLeft().GetColumn(), endColumn = cellArray.GetBottomRight().GetColumn();
				    	for(int m = startRow ; m <= endRow ; m++)
				    		for(int n = startColumn ; n <= endColumn ; n++) {
				    			myCellStyle.setCellColor(new StructDefine.Position(m, n), BLUE.index);
				    		}
					}
				}
			}	
		}
		if(hasSmell) {
			File desFile = new File(desFileName);
			if(!desFile.exists()) desFile.createNewFile();
			FileOutputStream out = new FileOutputStream(desFile);
			workbook.write(out);
			out.close();
		}
		return hasSmell;
	}
	
	public static boolean autoDetect(File file) throws Exception {
		boolean hasSmell = false;
		FileInputStream input = new FileInputStream(file);
		Workbook workbook = null;
		//String desFileName = null;
		if(file.getName().endsWith(".xlsx") || file.getName().endsWith(".XLSX")) {
			workbook = new XSSFWorkbook(input);
			//desFileName = file.getAbsolutePath().substring(0, file.getAbsolutePath().length()-5) + " (annotated).xlsx";
		}
		else if(file.getName().endsWith(".xls") || file.getName().endsWith(".XLS")) {
			workbook = new HSSFWorkbook(input);
			//desFileName = file.getAbsolutePath().substring(0, file.getAbsolutePath().length()-4) + " (annotated).xls";
		}
		int sheetNum = workbook.getNumberOfSheets();
		
		for(int k = 0 ; k < sheetNum ; k++) {
			Sheet sheet = workbook.getSheetAt(k);
			SheetReader sheetReader = new SheetReader(sheet);
			ExtractCellArray extractCellArray = new ExtractCellArray(sheetReader);
			extractCellArray.identifySnippets();
			extractCellArray.extractCellArrays();
			ArrayList<Region> cellArrayList = extractCellArray.getCellArrays();
			
			MyCellStyle myCellStyle = new MyCellStyle(workbook, sheet);
			
			for(Region cellArray : cellArrayList) {
				DetectRepairSmell detectRepairSmell = new DetectRepairSmell(sheetReader, cellArray);
				if(detectRepairSmell.hasSmell()) {
					hasSmell = true;
					if(detectRepairSmell.recovery()) {
						Formula formula = detectRepairSmell.getRepairFormula();
						int startRow = cellArray.GetTopLeft().GetRow(), endRow = cellArray.GetBottomRight().GetRow();
				    	int startColumn = cellArray.GetTopLeft().GetColumn(), endColumn = cellArray.GetBottomRight().GetColumn();
				    	for(int i = startRow ; i <= endRow ; i++)
				    		for(int j = startColumn ; j <= endColumn ; j++) {
				    			if(sheetReader.getCells()[i][j].getCellType() != Cell.CELL_TYPE_FORMULA)
				    				myCellStyle.setCellColor(new StructDefine.Position(i, j), YELLOW.index);
				    			else {
				    				Formula form = new Formula(new StructDefine.Position(i,j), sheetReader.getCells()[i][j].getFormula());
				    				if(!form.getR1C1Formula().equals(formula.getR1C1Formula()))
				    					myCellStyle.setCellColor(new StructDefine.Position(i, j), YELLOW.index);
				    			}
				    			
				    		}
					}
					else {
						int startRow = cellArray.GetTopLeft().GetRow(), endRow = cellArray.GetBottomRight().GetRow();
				    	int startColumn = cellArray.GetTopLeft().GetColumn(), endColumn = cellArray.GetBottomRight().GetColumn();
				    	for(int i = startRow ; i <= endRow ; i++)
				    		for(int j = startColumn ; j <= endColumn ; j++) {
				    			myCellStyle.setCellColor(new StructDefine.Position(i, j), YELLOW.index);
				    		}
					}
				}
			}	
		}
		
		if(hasSmell) {
			String filePath = file.getParent();
			File filedir = new File(filePath + "\\AmCheck");
			if(!filedir .exists()  && !filedir .isDirectory())
				filedir.mkdir();
			String desFileName = filePath + "\\AmCheck\\" + file.getName();
			File desFile = new File(desFileName);
			if(!desFile.exists()) desFile.createNewFile();
			FileOutputStream out = new FileOutputStream(desFile);
			workbook.write(out);
			out.close();
		}
		return hasSmell;
	}
}
