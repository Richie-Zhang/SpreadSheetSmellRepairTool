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
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import core.AutoDetectSmell;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;

public class AutoDetectUI extends JFrame {
	private static final long serialVersionUID = 1L;
	private JTextField textField_filePath;
	private JList<String> list_allFiles, list_smellFiles;
	private JProgressBar progressBar;
	private JLabel label;
	private JButton btn_detect;
	
	private int count, MaxCount = 0;
	private ArrayList<File> files;

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
					AutoDetectUI frame = new AutoDetectUI();
					frame.setVisible(true);
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public AutoDetectUI() {
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
				JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int openFile = chooser.showOpenDialog(null);
				
				if (openFile == JFileChooser.APPROVE_OPTION) {
					String filePath = chooser.getSelectedFile().getAbsolutePath();
					textField_filePath.setText(filePath);
					loadAllFiles(filePath);
					DefaultListModel<String> listModel = new DefaultListModel<String>();
					list_smellFiles.setModel(listModel);
					progressBar.setValue(0);
					count = 0; MaxCount = files.size();
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
		
		JLabel lblFilesWithSmell = new JLabel("Files with Smell");
		panel_smellFiles.add(lblFilesWithSmell, BorderLayout.NORTH);
		
		JScrollPane scrollPane_1 = new JScrollPane();
		scrollPane_1.setPreferredSize(new Dimension(200, HEIGHT));
		panel_smellFiles.add(scrollPane_1, BorderLayout.CENTER);
		
		list_smellFiles = new JList<String>();
		scrollPane_1.setViewportView(list_smellFiles);
		
		JPanel panel_process = new JPanel();
		contentPane.add(panel_process, BorderLayout.SOUTH);
		
		progressBar = new JProgressBar();
		progressBar.setMinimum(0);
		progressBar.setMaximum(100);
		panel_process.add(progressBar);
		
		label = new JLabel("");
		panel_process.add(label);
		
		Thread barThread = new BarThread();  
		barThread.start();
	}

	private void loadAllFiles(String filePath) {
		File filepath = new File(filePath); 
		String[] filelist = filepath.list();
		files = new ArrayList<>();
		for (int i = 0; i < filelist.length; i++) {
			if(filelist[i].endsWith(".xls") || filelist[i].endsWith(".xlsx")) {
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

	class BarThread extends Thread {  
		private int DELAY = 100;  
		public BarThread() {
			super();
	    }  
		public void run() {  
		    while(progressBar.getValue() < progressBar.getMaximum()) {  
		    	try {
		    		if(MaxCount == 0) continue;
		    		else progressBar.setValue(100*count/MaxCount);
		    		Thread.sleep(DELAY); 
		        } 
		    	catch (InterruptedException ignoredException) {  
		        }  
		    }  
		}  
	}  

	class DetectThread extends Thread {
		public DetectThread() {
			super();
		}
		
		public void run() {
			DefaultListModel<String> listModel = new DefaultListModel<String>();
			for(File file : files) {
				try {
					if(AutoDetectSmell.autoDetectFile(file)) {
						listModel.addElement(file.getName());
						list_smellFiles.setModel(listModel);
					}
					count++;
					label.setText(count+"/"+MaxCount);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			JOptionPane.showMessageDialog(null, "All Files have been auto-detected successfully !!");
		}
	}
}
