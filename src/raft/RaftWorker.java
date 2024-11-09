package raft;

import raft.messages.Message;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;

/**
 * Raftの管理クラス
 */
public class RaftWorker implements Runnable {

  public List<Node> nodes;
  public BlockingQueue<Message> queues;
  private String logFilePath;

  public RaftWorker(String logFilePath) {
    this.logFilePath = logFilePath;
    nodes = new ArrayList<Node>();
  }

  /**
   * 新規ノードを作成し，起動するメソッド
   */
  public void createNewNode() {
    Node node = new Node(nodes.size() + 1);
    node.setNodeList(nodes);
    node.setLogFile(logFilePath);
    node.start();

    nodes.add(node);
  }

  /**
   * バックグラウンド処理メソッド
   * メッセージの送信やノードの更新を行う．
   */
  public void run() {
  }

  // TODO: 全てのノードでノード停止メソッドを呼び出すメソッドを作成する．

}
