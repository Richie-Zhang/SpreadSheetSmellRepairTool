package test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.hssf.util.HSSFColor.RED;
import org.apache.poi.hssf.util.HSSFColor.YELLOW;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import core.StructDefine.Position;
import core.StructDefine.Region;
import file.MyCellStyle;

public class TestCellWriter {
	public static void main(String[] args) throws IOException {
		File f = new File("F:\\test.xlsx");
		FileInputStream in = new FileInputStream(f);
		Workbook wbs = new XSSFWorkbook(in);
		Sheet sheet = wbs.getSheetAt(0);
		
		MyCellStyle myCellStyle = new MyCellStyle(wbs, sheet);
		myCellStyle.setCellBorder(new Region(new Position(1, 4), new Position(4, 8)), CellStyle.BORDER_DOUBLE);
		myCellStyle.setCellColor(new Region(new Position(1, 4), new Position(4, 8)), YELLOW.index);
		myCellStyle.setCellColor(new Region(new Position(1, 4), new Position(2, 5)), RED.index);
		//myCellStyle.setCellCommemt(new Position(2, 2), "promptContent");
	
		FileOutputStream out = new FileOutputStream(f);
		wbs.write(out);
		out.close();
	}
}
