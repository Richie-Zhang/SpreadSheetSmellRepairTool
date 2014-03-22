package file;

import org.apache.poi.ss.usermodel.*;
import core.StructDefine.Position;
import core.StructDefine.Region;;

public class MyCellStyle {
	private Workbook wb;
	private Sheet sheet;
	
	public MyCellStyle(Workbook workbook, Sheet sheet) {
		this.wb = workbook;
		this.sheet = sheet;
	}
	
	public void setCellBorder(Region snip, short border) {
		int tlRow = snip.GetTopLeft().GetRow(), tlColumn = snip.GetTopLeft().GetColumn();
		int brRow = snip.GetBottomRight().GetRow(), brColumn = snip.GetBottomRight().GetColumn();

		for(int i = tlRow ; i <= brRow ; i++) {
			Cell cell = sheet.getRow(i).getCell(tlColumn);
			CellStyle cellStyle = wb.createCellStyle();
			cellStyle.cloneStyleFrom(cell.getCellStyle());
			cellStyle.setBorderLeft(border);
			cell.setCellStyle(cellStyle);
			
			cell = sheet.getRow(i).getCell(brColumn);
			CellStyle cellStyle2 = wb.createCellStyle();
			cellStyle2.cloneStyleFrom(cell.getCellStyle());
			cellStyle2.setBorderRight(border);
			cell.setCellStyle(cellStyle2);
		}
		
		for(int i = tlColumn ; i <= brColumn ; i++) {
			Cell cell = sheet.getRow(tlRow).getCell(i);
			CellStyle cellStyle = wb.createCellStyle();
			cellStyle.cloneStyleFrom(cell.getCellStyle());
			cellStyle.setBorderTop(border);
			cell.setCellStyle(cellStyle);
			
			cell = sheet.getRow(brRow).getCell(i);
			CellStyle cellStyle2 = wb.createCellStyle();
			cellStyle2.cloneStyleFrom(cell.getCellStyle());
			cellStyle2.setBorderBottom(border);
			cell.setCellStyle(cellStyle2);
		}
	}

	public void setCellColor(Position pos, short color) {
		Cell cell = sheet.getRow(pos.GetRow()).getCell(pos.GetColumn());
		CellStyle cellStyle = wb.createCellStyle();
		cellStyle.cloneStyleFrom(cell.getCellStyle());
		cellStyle.setFillForegroundColor(color);
		cellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
		cell.setCellStyle(cellStyle);
	}
	
	public void setCellColor(Region snip, short color) {
		int tlRow = snip.GetTopLeft().GetRow(), tlColumn = snip.GetTopLeft().GetColumn();
		int brRow = snip.GetBottomRight().GetRow(), brColumn = snip.GetBottomRight().GetColumn();
		for(int i = tlRow ; i <= brRow ; i++)
			for(int j = tlColumn ; j <= brColumn ; j++) {
				Cell cell = sheet.getRow(i).getCell(j);
				CellStyle cellStyle = wb.createCellStyle();
				cellStyle.cloneStyleFrom(cell.getCellStyle());
				cellStyle.setFillForegroundColor(color);
				cellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
				cell.setCellStyle(cellStyle);
			}
	}

	public void setCellComment(Position pos, String info) {
		Drawing drawing = sheet.createDrawingPatriarch();
		CreationHelper factory = wb.getCreationHelper();
		ClientAnchor anchor = factory.createClientAnchor();
		Comment comment = drawing.createCellComment(anchor);
		RichTextString str = factory.createRichTextString(info);
		comment.setString(str);
		
		Cell cell = sheet.getRow(pos.GetRow()).getCell(pos.GetColumn());
		if(cell.getCellComment() != null);
			cell.removeCellComment();
		cell.setCellComment(comment);
	}
}
