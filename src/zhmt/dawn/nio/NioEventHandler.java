package zhmt.dawn.nio;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

public interface NioEventHandler {
	public SelectableChannel getChannel();

	public void setSelectionKey(SelectionKey key);

	public SelectionKey getSelectionKey();
	
	public void onNioEvent();
}
