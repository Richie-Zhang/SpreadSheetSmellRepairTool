package test;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFComment;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFPatriarch;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFClientAnchor;
import java.io.IOException;
import java.io.FileOutputStream;
public class PoiWriter {
public static void main(String[] args) throws IOException {
  //创建工作簿对象
  HSSFWorkbook wb=new HSSFWorkbook();
  //创建工作表对象
  HSSFSheet sheet=wb.createSheet("我的工作表");
  //创建绘图对象
  HSSFPatriarch p=sheet.createDrawingPatriarch();
  //创建单元格对象,批注插入到4行,1列,B5单元格
  HSSFCell cell=sheet.createRow(4).createCell(1);
  //插入单元格内容
  cell.setCellValue(new HSSFRichTextString("批注"));
  //获取批注对象
  //(int dx1, int dy1, int dx2, int dy2, short col1, int row1, short col2, int row2)
  //前四个参数是坐标点,后四个参数是编辑和显示批注时的大小.
  HSSFComment comment=p.createComment(new HSSFClientAnchor(0,0,0,0,(short)3,3,(short)5,6));
  //输入批注信息
  comment.setString(new HSSFRichTextString("插件批注成功!插件批注成功!"));
  //添加作者,选中B5单元格,看状态栏
  comment.setAuthor("toad");
  //将批注添加到单元格对象中
  cell.setCellComment(comment);
  //创建输出流
  FileOutputStream out=new FileOutputStream("writerPostil.xls");
  
  wb.write(out);
  //关闭流对象
  out.close();
}
}