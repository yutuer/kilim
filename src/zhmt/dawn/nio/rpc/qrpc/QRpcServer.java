package zhmt.dawn.nio.rpc.qrpc;

import kilim.Pausable;
import kilim.Scheduler;
import kilim.Task;
import zhmt.dawn.nio.TcpChannel;
import zhmt.dawn.nio.TcpServer;

public class QRpcServer {
	private String ip;
	private int port;

	public QRpcServer(String ip, int port) {
		this.ip = ip;
		this.port = port;
	}

	public void start(Scheduler sch) {
		new Task() {
			@Override
			public void execute() throws Pausable, Exception {
				TcpServer server = new TcpServer(ip, port) {
					@Override
					protected void onAccepted(TcpChannel ch) {
						new ConnTask(ch).start();
					}
				};
				server.start();
			}
		}.start();
	}

	static class ConnTask extends Task {
		private TcpChannel channel;

		public ConnTask(TcpChannel channel) {
			this.channel = channel;
		}

		@Override
		public void execute() throws Pausable, Exception {
			try {
				while (true) {
					loopOnce();
				}
			} finally {
				channel.close();
				this.channel = null;
			}
		}

		private void loopOnce() {

		}
		
		void recv_deserial(){}
		void process(){}
		void serial_send(){}

	}

	private void processConn(TcpChannel ch) {

	}

	public static void main(String[] args) {
		Scheduler sch = Scheduler.getDefaultScheduler();

		QRpcServer server = new QRpcServer("0.0.0.0", 10000);
		server.start(sch);
	}
}
