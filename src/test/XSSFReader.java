package test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class XSSFReader {
	public static void main(String[] args) {
		File f = new File("F:\\test.xlsx");

			FileInputStream in = null;;
			try {
				in = new FileInputStream(f);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Workbook wbs = null;
			try {
				wbs = new XSSFWorkbook(in);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			Sheet childSheet = wbs.getSheetAt(0);
			childSheet.setForceFormulaRecalculation(true);
			//System.out.println(childSheet.getPhysicalNumberOfRows());
			//System.out.println("����" + childSheet.getLastRowNum());
			for (int j = 0; j <= childSheet.getLastRowNum(); j++) {
				//System.out.println("line:" + j);
				Row row = childSheet.getRow(j);
				if(row == null) continue;
				//System.out.println(row.getPhysicalNumberOfCells());
				//System.out.println("����" + row.getLastCellNum());
				for (int k = 0; k < row.getLastCellNum(); k++) {
					//System.out.print("row : " + k);
					Cell cell = row.getCell(k);
					if (null != cell) {
						switch (cell.getCellType()) {
						case Cell.CELL_TYPE_NUMERIC: // ����
							//System.out.print(cell.getNumericCellValue() + "   ");
							break;
						case Cell.CELL_TYPE_STRING: // �ַ�
							//System.out.print(cell.getStringCellValue() + "   ");
							break;
						case Cell.CELL_TYPE_BOOLEAN: // Boolean
							//System.out.println(cell.getBooleanCellValue() + "   ");
							//System.out.print(cell.getNumericCellValue() + "   ");
							break;
						case Cell.CELL_TYPE_FORMULA: // ��ʽ
							System.out.print(cell.getCellFormula() + "   ");
							try{
								System.out.print(cell.getNumericCellValue() + "   ");
							} catch(IllegalStateException e){
								System.out.print(cell.getStringCellValue() + "   ");
							}
							System.out.println();
							break;
						case Cell.CELL_TYPE_BLANK: // ��ֵ
							//System.out.println("NULL");
							//System.out.print(cell.getNumericCellValue() + "   ");
							break;
						case Cell.CELL_TYPE_ERROR: // ����
							//System.out.println("ERROR");
							break;
						default:
							//System.out.print("δ֪����   ");
							break;
						}
					} 
					else {
						//System.out.print("Cell Empty");
					}
				}
				System.out.println();
				
			}
			
			FileOutputStream out = null;
			try {
				out = new FileOutputStream(f);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				wbs.write(out);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

	}
}
