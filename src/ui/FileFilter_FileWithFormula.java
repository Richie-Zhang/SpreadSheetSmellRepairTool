package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Locale;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import file.ReadWriteFile;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;

import org.apache.commons.io.FileUtils;

public class FileFilter_FileWithFormula extends JFrame {
	private static final long serialVersionUID = 1L;

	private JTextField textField_filePath;
	private JList<String> list_allFiles, list_smellFiles;
	private JButton btn_detect;

	private ArrayList<File> files;

	private String lastPath = System.getProperty("user.dir");

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		Locale.setDefault(Locale.ENGLISH);
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					FileFilter_FileWithFormula frame = new FileFilter_FileWithFormula();
					frame.setVisible(true);

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public FileFilter_FileWithFormula() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(450, 420); 
		setLocationRelativeTo(null);
		JPanel contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);

		JPanel panel_open = new JPanel();
		contentPane.add(panel_open, BorderLayout.NORTH);
		panel_open.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));

		JButton btn_open = new JButton("Open");
		btn_open.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser chooser = new JFileChooser(lastPath);
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int openFile = chooser.showOpenDialog(null);

				if (openFile == JFileChooser.APPROVE_OPTION) {
					String filePath = chooser.getSelectedFile().getAbsolutePath();
					lastPath = filePath;
					textField_filePath.setText(filePath);
					loadAllFiles(filePath);
					DefaultListModel<String> listModel = new DefaultListModel<String>();
					list_smellFiles.setModel(listModel);
					btn_detect.setEnabled(true);
				}
			}
		});
		panel_open.add(btn_open);

		textField_filePath = new JTextField();
		panel_open.add(textField_filePath);
		textField_filePath.setColumns(20);

		btn_detect = new JButton("Start");
		btn_detect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				btn_detect.setEnabled(false);
				DetectThread detectThread = new DetectThread();
				detectThread.start();
			}
		});
		panel_open.add(btn_detect);
		btn_detect.setEnabled(false);

		JPanel panel_allFiles = new JPanel();
		contentPane.add(panel_allFiles, BorderLayout.WEST);
		panel_allFiles.setLayout(new BorderLayout(0, 0));

		JLabel lblAllFiles = new JLabel("All Files");
		panel_allFiles.add(lblAllFiles, BorderLayout.NORTH);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setPreferredSize(new Dimension(200, HEIGHT));
		panel_allFiles.add(scrollPane, BorderLayout.CENTER);

		list_allFiles = new JList<String>();
		scrollPane.setViewportView(list_allFiles);

		JPanel panel_smellFiles = new JPanel();
		contentPane.add(panel_smellFiles, BorderLayout.EAST);
		panel_smellFiles.setLayout(new BorderLayout(0, 0));

		JLabel lblFilesWithSmell = new JLabel("Valid Files");
		panel_smellFiles.add(lblFilesWithSmell, BorderLayout.NORTH);

		JScrollPane scrollPane_1 = new JScrollPane();
		scrollPane_1.setPreferredSize(new Dimension(200, HEIGHT));
		panel_smellFiles.add(scrollPane_1, BorderLayout.CENTER);

		list_smellFiles = new JList<String>();
		scrollPane_1.setViewportView(list_smellFiles);
	}

	private void loadAllFiles(String filePath) {
		File filepath = new File(filePath); 
		String[] filelist = filepath.list();
		files = new ArrayList<>();
		for (int i = 0; i < filelist.length; i++) {
			if(filelist[i].endsWith(".xls") || filelist[i].endsWith(".xlsx") || filelist[i].endsWith(".XLS") || filelist[i].endsWith(".XLSX")) {
				File readfile = new File(filePath + "\\" + filelist[i]); 
				files.add(readfile);
			}
		}

		DefaultListModel<String> listModel = new DefaultListModel<String>();
		for(File file : files) {
			listModel.addElement(file.getName());
		}
		list_allFiles.setModel(listModel);
	}


	class DetectThread extends Thread {
		public DetectThread() {
			super();
		}

		public void run() {
			DefaultListModel<String> listModel = new DefaultListModel<String>();
			String filePath = null;
			if(files.size() >= 1) {
				filePath = files.get(0).getParent();
				File filedir = new File(filePath + "\\files with formula");
				filedir.mkdir();
			}
			
			for(File file : files) {
				try {
					System.err.println(file.getName());
					if(ReadWriteFile.hasFormula(file)) {
						
						listModel.addElement(file.getName());
						list_smellFiles.setModel(listModel);
						
						String des = filePath + "\\files with formula\\" + file.getName();
						String src = file.getAbsolutePath();
						
						File desFile =new File(des);
						if(!desFile.exists())
							desFile.createNewFile(); 
						
						FileUtils.copyFile(new File(src), desFile);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			JOptionPane.showMessageDialog(null, "All Files have been handled successfully !!");
		}
	}
}
