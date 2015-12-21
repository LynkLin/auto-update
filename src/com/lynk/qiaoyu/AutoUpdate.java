package com.lynk.qiaoyu;

import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPDataTransferListener;
import it.sauronsoftware.ftp4j.FTPFile;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import java.awt.Toolkit;

import javax.swing.JLabel;

import java.awt.Font;

import javax.swing.SwingConstants;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import java.util.Properties;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JDialog;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import javax.swing.UIManager;

public class AutoUpdate extends JFrame {
	private static final long serialVersionUID = 1L;
	
	private long totalSize = Long.MAX_VALUE;
	
	private String localPropertiesPath;//1
	private String localVerName;//2
	private String serverVer;//3
	private String ftpName;//4
	private int ftpPort;//5
	private String ftpUser;//6
	private String ftpPwd;//7
	private String ftpPath;//8
	private String ftpUpdateTxtName;//9
	private String ftpUpdateJarName;//10
	
	private String localUpdateTxtPath;//11
	private String localUpdateJarPath;//12
//	private String localExeName;//13
	
	private long receivedSize = 0;
	
	private JPanel contentPane;
	private JTextArea uiUpdateMessage;
	private JProgressBar uiProcess;

	public static void main(final String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					JFrame.setDefaultLookAndFeelDecorated(true);
					JDialog.setDefaultLookAndFeelDecorated(true);
					UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
					Font font = new Font("微软雅黑", Font.PLAIN, 12);
					UIManager.put("OptionPane.messageFont", font);
					AutoUpdate frame = new AutoUpdate(args);
					frame.setLocationRelativeTo(null);
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public AutoUpdate(String[] args) {
		localPropertiesPath = args[0];
		localVerName = args[1];
		serverVer = args[2];
		ftpName = args[3];
		ftpPort = Integer.parseInt(args[4]);
		ftpUser = args[5];
		ftpPwd = args[6];
		ftpPath = args[7];
		ftpUpdateTxtName = args[8];
		ftpUpdateJarName = args[9];
		localUpdateTxtPath = args[10];
		localUpdateJarPath = args[11];
//		localExeName = args[12];
		
		initComponents();
		updateFiles();
	}
	
	private String getProperty() {
		Properties prop = new Properties();
		InputStream is = null;
		try {
			is = new FileInputStream(System.getProperty("user.dir") + File.separator + localPropertiesPath);
			prop.load(is);
			return prop.getProperty(localVerName, "");
		} catch (Exception e) {
			return "";
		} finally {
			try {
				if (is != null) {
					is.close();
				}
			} catch (IOException e) {
			}
		}
	}
	
	private void saveProperty() throws Exception {
		Properties prop = new Properties();
		InputStream is = null;
		OutputStream os = null;
		try {
			is = new FileInputStream(System.getProperty("user.dir") + File.separator + localPropertiesPath);
			prop.load(is);
			is.close();
			os = new FileOutputStream(System.getProperty("user.dir") + File.separator + localPropertiesPath);
			prop.setProperty(localVerName, serverVer);
			prop.store(os, null);
		} catch (Exception e) {
			throw new Exception("保存参数错误, 请联系管理员!" + e.getMessage());
		} finally {
			if(os != null) {
				os.close();
			}
		}
	}
	
	/**
	 * 下载更新
	 */
	private void updateFiles() {
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		Update update = new Update();
		update.addPropertyChangeListener(new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if("progress".equals(evt.getPropertyName())) {
					uiProcess.setValue((int) evt.getNewValue());
				}
			}
		});
		update.execute();
	}
	
	class Update extends SwingWorker<String, String> {
		
		@Override
		protected String doInBackground() throws Exception {
			//删除更新日志
			File txtFile = new File(System.getProperty("user.dir") + File.separator + localUpdateTxtPath);
			if(txtFile.exists()) {
				txtFile.delete();
			}
			
			/**
			 * 备份原文件
			 */
			File jarFile = new File(System.getProperty("user.dir") + File.separator + localUpdateJarPath);
			File backJarFile = new File(System.getProperty("user.dir") + File.separator + localUpdateJarPath + "_bak");
			if(backJarFile.exists()) {
				backJarFile.delete();
			}
			if(jarFile.exists()) {
				jarFile.renameTo(backJarFile);
			}
			
			FTPClient client = new FTPClient();
			try {
				client.connect(ftpName, ftpPort);
				client.login(ftpUser, ftpPwd);
				if(ftpPath != null && !ftpPath.isEmpty()) {
					client.changeDirectory(ftpPath);
				}
				client.setType(FTPClient.TYPE_BINARY);
				if(ftpUpdateTxtName != null && !ftpUpdateTxtName.isEmpty()) {
					//下载更新日志
					client.download(ftpUpdateTxtName, txtFile);
					
					//显示日志
					InputStream is = null;
					InputStreamReader in = null;
					BufferedReader reader = null;
					try {
						is = new FileInputStream(txtFile);
						in = new InputStreamReader(is, "GBK");
						reader = new BufferedReader(in);
						String lineText = null;
						while ((lineText = reader.readLine()) != null) {
							publish(lineText);
						}
					} catch (Exception e) {
						throw e;
					} finally {
						try {
							if(reader != null) {
								reader.close();
							}
							if(in != null) {
								in.close();
							}
							if(is != null) {
								is.close();
							}
						} catch (Exception e) {
							throw e;
						}
					}
				}
				
				//获取文件总大小
				FTPFile[] files = client.list();
				for(FTPFile file: files) {
					if(file.getName().equals(ftpUpdateJarName)) {
						totalSize = file.getSize();
						break;
					}
				}
				//下载程序文件
				client.download(ftpUpdateJarName, jarFile, new FTPDataTransferListener() {
					
					@Override
					public void transferred(int length) {
						receivedSize += length;
						setProgress((int) (receivedSize * 100 / totalSize));
					}
					@Override
					public void started() {
					}
					@Override
					public void failed() {
					}
					@Override
					public void completed() {
						setProgress(100);
					}
					@Override
					public void aborted() {
					}
				});
				if(backJarFile.exists()) {
					backJarFile.delete();
				}
			} catch (Exception e) {
				if(jarFile.exists()) {
					jarFile.delete();
				}
				if(backJarFile.exists()) {
					backJarFile.renameTo(jarFile);
				}
				throw e;
			} finally {
				client.disconnect(true);
			}
			return null;
		}
		

		@Override
		protected void process(List<String> chunks) {
			for(String chunk: chunks) {
				uiUpdateMessage.append(chunk);
				uiUpdateMessage.append("\n");
			}
		}


		@Override
		protected void done() {
			try {
				get();
				saveProperty();
				JOptionPane.showMessageDialog(AutoUpdate.this, "升级成功!", "自动更新", JOptionPane.INFORMATION_MESSAGE);
				Runtime.getRuntime().exec("java_lib\\bin\\javaw -jar \"" + System.getProperty("user.dir") + "/" + localUpdateJarPath + "\"");
				System.exit(0);
			} catch (Exception e) {
				JOptionPane.showMessageDialog(AutoUpdate.this, e.getMessage(), "自动更新", JOptionPane.ERROR_MESSAGE);
				System.exit(0);
			} finally {
				setDefaultCloseOperation(EXIT_ON_CLOSE);
			}
		}
	}
	
	private void initComponents() {
		setAlwaysOnTop(true);
		setTitle("自动更新");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setIconImage(Toolkit.getDefaultToolkit().getImage(AutoUpdate.class.getResource("/resources/images/icon.png")));
		setBounds(100, 100, 654, 541);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		
		JPanel panel = new JPanel();
		panel.setLayout(null);
		
		JLabel label = new JLabel("本地版本:");
		label.setBounds(10, 10, 105, 25);
		panel.add(label);
		label.setHorizontalAlignment(SwingConstants.RIGHT);
		label.setFont(new Font("方正姚体", Font.PLAIN, 17));
		
		JLabel uiLocalVer = new JLabel(getProperty());
		uiLocalVer.setBounds(125, 10, 60, 25);
		panel.add(uiLocalVer);
		uiLocalVer.setForeground(Color.BLUE);
		uiLocalVer.setHorizontalAlignment(SwingConstants.LEFT);
		uiLocalVer.setFont(new Font("方正姚体", Font.PLAIN, 17));
		
		JLabel label_1 = new JLabel("服务器版本:");
		label_1.setBounds(346, 10, 105, 25);
		panel.add(label_1);
		label_1.setHorizontalAlignment(SwingConstants.RIGHT);
		label_1.setFont(new Font("方正姚体", Font.PLAIN, 17));
		
		JLabel uiServerVer = new JLabel(serverVer);
		uiServerVer.setBounds(461, 10, 60, 25);
		panel.add(uiServerVer);
		uiServerVer.setHorizontalAlignment(SwingConstants.LEFT);
		uiServerVer.setForeground(Color.RED);
		uiServerVer.setFont(new Font("方正姚体", Font.PLAIN, 17));
		
		JScrollPane scrollPane = new JScrollPane();
		
		uiProcess = new JProgressBar();
		uiProcess.setMaximum(100);
		uiProcess.setFont(new Font("微软雅黑", Font.PLAIN, 12));
		GroupLayout gl_contentPane = new GroupLayout(contentPane);
		gl_contentPane.setHorizontalGroup(
			gl_contentPane.createParallelGroup(Alignment.TRAILING)
				.addComponent(panel, GroupLayout.DEFAULT_SIZE, 636, Short.MAX_VALUE)
				.addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 636, Short.MAX_VALUE)
				.addComponent(uiProcess, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 636, Short.MAX_VALUE)
		);
		gl_contentPane.setVerticalGroup(
			gl_contentPane.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPane.createSequentialGroup()
					.addComponent(panel, GroupLayout.PREFERRED_SIZE, 48, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 412, Short.MAX_VALUE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(uiProcess, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE))
		);
		
		uiUpdateMessage = new JTextArea();
		uiUpdateMessage.setEditable(false);
		uiUpdateMessage.setFont(new Font("微软雅黑", Font.PLAIN, 14));
		scrollPane.setViewportView(uiUpdateMessage);
		contentPane.setLayout(gl_contentPane);
	}
}
