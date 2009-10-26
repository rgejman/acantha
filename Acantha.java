import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.awt.Font;

import javax.swing.Timer;
import java.awt.event.*;


public class Acantha {

	final static String version = "1.0.5";
	final static String latestVersionURL = "http://www.domain.com/acantha/latest_version.html";
	final static String latestVersionMessageURL = "http://www.domain.com/acantha/latest_version_message.html";
	final static String programName = "acantha";
	final static int updateInterval = 60 * 3 * 1000; // 60s * 15m * 1000ms

	JFrame mainFrame = new JFrame();
	JFrame updateFrame = new JFrame();

	Process process;
	runTor runTor = new runTor();
	Properties p = System.getProperties();
	String os = p.getProperty("os.name").toLowerCase();
	String arch = p.getProperty("os.arch").toLowerCase();
	String os_v = p.getProperty("os.version").toLowerCase();
	JTextArea area = new JTextArea(7, 60); //rows, columns
	JTextArea updateArea;
	JButton toggle = new JButton("Start");

	boolean on = false;

	public Acantha() {

		System.out.println(os + " " + os_v + " " + arch);
		try {
			copyFiles();
		} catch(Exception e) {
			e.printStackTrace();
		}
		setupMainWindow();
		
		new checkForUpdate().start();
		
		Timer timer = new Timer(updateInterval, new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				new checkForUpdate().start();
			}
		});
		timer.start();
	}

	private void setupMainWindow() {
		toggle.addActionListener(new ALButton());

		area.setEditable(false);
		area.setFocusable(true);
		JPanel panel = new JPanel();
		JScrollPane spane = new JScrollPane(area);
		spane.setAutoscrolls(true);
		area.setAutoscrolls(true);
		panel.add(toggle);
		panel.add(spane);
		mainFrame.add(panel);
		mainFrame.setSize(800,300);
		mainFrame.setTitle("Acantha Onion Routing");
		mainFrame.setVisible(true);
	}

	private void setupUpdateWindow(String content) {

		updateArea = new JTextArea(7,50);
		updateArea.setEditable(false);
		updateArea.setFocusable(true);
		updateArea.setWrapStyleWord(true);
		updateArea.setLineWrap(true);
		JPanel panel = new JPanel();
		panel.add(updateArea);
		updateFrame.add(panel);
		updateFrame.setSize(800,300);
		updateFrame.setTitle("You must update " + programName);

		updateArea.setText(content);

		updateFrame.setVisible(true);
	}

	public void copyFiles() throws Exception {

		File f = new File(System.getProperty("user.home") + File.separator +  "tor");
		f.mkdirs();
		InputStream is = null;
		OutputStream out = null;
		if(os.contains("windows")) {
			File exe = new File(f.getCanonicalPath()  + File.separator + "tor.exe");
			if(exe.exists()) {
				exe.delete();
				exe.createNewFile();
			}
			is = getClass().getResourceAsStream("windows/tor.exe");
			out = new FileOutputStream(exe);
		}
		else if(os.contains("mac")) {
			is = getClass().getResourceAsStream("mac/tor");
			System.out.println(f.getCanonicalPath()  + File.separator + "tor");
			out = new FileOutputStream(f.getCanonicalPath()  + File.separator + "tor");
		}


		byte[] buf = new byte[1024];
		int len;
		while ((len = is.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		is.close();
		out.close();

		if(os.contains("mac")) {
			Runtime.getRuntime().exec("chmod +x " + System.getProperty("user.home") + "/tor/tor");
		}


		is = getClass().getResourceAsStream("torrc");
		File output_torrc = new File(f + File.separator + "torrc");
		if(output_torrc.exists()) {
			output_torrc.delete();
			output_torrc.createNewFile();
		}
		out = new FileOutputStream(output_torrc);

		buf = new byte[1024];
		while ((len = is.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		is.close();
		out.close();
	}

	protected void finalize() throws Throwable {
		try {
			endTor();
			int val = process.exitValue();

		} catch(Exception e) {
			endTor();
		}
		finally {
			super.finalize();
		}
	}


	public static void main(String[] args) {
		Acantha acantha = new Acantha();
	}

	public void endTor() {
		if(on) {
			process.destroy();
			on = false;
			toggle.setText("Start");
		}
	}

	class runTor extends Thread {
		public void run() {
			try {
				ShutdownHook shutdownHook = new ShutdownHook();
				Runtime.getRuntime().addShutdownHook(shutdownHook);
				if(os.contains("mac")) {
					runMac();
				}
				else if(os.contains("windows")) {
					runWindows();
				}
				on = true;
				toggle.setText("Stop");

				InputStream is = process.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line;
				while (on && (line = br.readLine()) != null) {
					area.append(line + "\n");
					area.setCaretPosition(area.getText().length());
					System.out.println(line);
					Thread.sleep(200);
				}

			} catch(Exception e) {
				e.printStackTrace();
			}
		}

		public void runWindows() throws IOException {
			String start = System.getProperty("user.home") + File.separatorChar + "tor" + File.separatorChar;
			String[] args = { start + "tor.exe", "-f", start + "torrc" };
			ProcessBuilder pb = new ProcessBuilder(args);
			pb.directory(new File(start));
			process = pb.start();
		}

		public void runMac() throws IOException {
			String start = System.getProperty("user.home") + File.separatorChar + "tor" + File.separatorChar;
			String[] args = { start + "tor", "-f", start + "torrc" };
			ProcessBuilder pb = new ProcessBuilder(args);
			pb.directory(new File(start));
			process = pb.start();
		}
	}

	class checkForUpdate extends Thread {
		private String getPage(String url) throws Exception {
			URL server = new URL(url);
			HttpURLConnection connection = (HttpURLConnection) server.openConnection();
			connection.connect();
			InputStream in = connection.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String input = "";
			String line;
			while((line = reader.readLine()) != null) {
				input += "\n" + line;
			}
			input = input.trim();
			return input;
		}
		public void run() {
			try {
				System.out.println("Checking for updates...");
				String latestVersion = getPage(latestVersionURL);
				if(version.equals(latestVersion)) {
					System.out.println("No updates found. Version reported by server: " + latestVersion);
					return;
				}
				else {
					System.out.println("New Version available: " + latestVersion);
				}
				String updateMessage = getPage(latestVersionMessageURL);
				endTor();
				mainFrame.setVisible(false); //Turn the Tor Frame off
				setupUpdateWindow(updateMessage);

			} catch(Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

	class ShutdownHook extends Thread {
		public void run() {
			endTor();
		}
	}

	class ALButton implements ActionListener {

		public void actionPerformed(ActionEvent arg0) {
			if(on) { // i.e., Turn Tor Off
				endTor();
			}
			else { // i.e., Turn Tor On
				area.setText("");
				runTor = new runTor();
				runTor.start();
			}
		}
	}
}