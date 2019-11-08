package dnsfilter;

/*
 PersonalDNSFilter 1.5
 Copyright (C) 2017 Ingo Zenz

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.

 Find the latest version at http://www.zenz-solutions.de/personaldnsfilter
 Contact:i.z@gmx.net
 */


import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.StringTokenizer;
import java.util.Vector;

import util.ExecutionEnvironment;
import util.GroupedLogger;
import util.Logger;
import util.LoggerInterface;

public class DNSFilterProxy implements Runnable {

	DatagramSocket receiver;
	boolean stopped = false;
	int port;

	public DNSFilterProxy() {
		this.port = 53;
	}

	public DNSFilterProxy(int port) {
		this.port = port;
	}

	private static void initDNS(DNSFilterManager dnsFilterMgr) {

		boolean detect = Boolean.parseBoolean(dnsFilterMgr.getConfig().getProperty("detectDNS", "true"));
		if (detect) {
			Logger.getLogger().logLine("DNS detection not supported for this device");
			Logger.getLogger().message("DNS detection not supported - Using fallback!");
		}
		Vector<DNSServer> dnsAdrs = new Vector<>();
		int timeout = Integer.parseInt(dnsFilterMgr.getConfig().getProperty("dnsRequestTimeout", "15000"));

		StringTokenizer fallbackDNS = new StringTokenizer(dnsFilterMgr.getConfig().getProperty("fallbackDNS", ""), ";");
		int cnt = fallbackDNS.countTokens();
		for (int i = 0; i < cnt; i++) {
			String dnsEntry = fallbackDNS.nextToken().trim();
			Logger.getLogger().logLine("DNS:" + dnsEntry);
			try {
				dnsAdrs.add(DNSServer.getInstance().createDNSServer(dnsEntry, timeout));
			} catch (IOException e) {
				Logger.getLogger().logException(e);
			}
		}
		DNSCommunicator.getInstance().setDNSServers(dnsAdrs.toArray(new DNSServer[dnsAdrs.size()]));
	}

	public static void main(String[] args) throws Exception {
		class StandaloneEnvironment extends ExecutionEnvironment  {
			
			boolean debug = false;
			boolean debugInit = false;

			
			@Override
			public boolean debug() {
				if (!debugInit) {
					debug = Boolean.parseBoolean(DNSFilterManager.getInstance().getConfig().getProperty("debug", "false"));
					debugInit=true;
				}
					
				return debug;
			}

			@Override
			public void onReload() {
				DNSFilterProxy.initDNS(DNSFilterManager.getInstance());
			}

			@Override
			public InputStream getAsset(String path) {
				return Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
			}

		}

		class StandaloneLogger  implements LoggerInterface {

			@Override
			public void logLine(String txt) {
				System.out.println(txt);
			}

			@Override
			public void logException(Exception e) {
				e.printStackTrace();
			}

			@Override
			public void log(String txt) {
				System.out.print(txt);

			}

			@Override
			public void message(String txt) {
				logLine(txt);
			}

			@Override
			public void closeLogger() {

			}

		}

		Logger.setLogger(new GroupedLogger(new LoggerInterface[] {new StandaloneLogger()}));
		ExecutionEnvironment.setEnvironment(new StandaloneEnvironment());
		
		DNSFilterManager filtermgr = DNSFilterManager.getInstance();
	
		filtermgr.init();
		initDNS(filtermgr);
		DNSFilterProxy runner = new DNSFilterProxy(53);
		runner.run();
	}

	@Override
	public void run() {
		try {
			receiver = new DatagramSocket(port);
		} catch (IOException eio) {
			Logger.getLogger().logLine("Exception:Cannot open DNS port " + port + "!" + eio.getMessage());
			return;
		}
		Logger.getLogger().logLine("DNSFilterProxy running on port " + port + "!");
		while (!stopped) {
			try {
				byte[] data = new byte[1024];
				DatagramPacket request = new DatagramPacket(data, 0, 1024);
				receiver.receive(request);
				new Thread(new DNSResolver(request, receiver)).start();
			} catch (IOException e) {
				if (!stopped)
					Logger.getLogger().logLine("Exception:" + e.getMessage());
			}
		}
		Logger.getLogger().logLine("DNSFilterProxy stopped!");
	}


	public synchronized void stop() {
		stopped = true;
		if (receiver == null)
			return;
		receiver.close();
	}


}
