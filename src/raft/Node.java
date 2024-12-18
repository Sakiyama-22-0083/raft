package raft;

import raft.messages.Message;
import raft.messages.VoteMessage;
import raft.messages.HeartbeatMessage;
import raft.messages.RequestVoteMessage;

import java.util.List;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedWriter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * ノードを表すクラス
 */
public class Node extends Thread {
  public long id;
  public NodeStatus status;
  protected List<Node> nodeList;// 他ノードのリスト
  public double startTime;// タイムアウトの開始時間
  public double timeout;// タイムアウト時間
  // 受け取ったメッセージのキュー
  public BlockingQueue<Message> queue = new LinkedBlockingQueue<Message>();
  public long[] workingNodes;
  public int currentTerm = 0;// 現在の任期
  public long votedFor = -1;// 投票したノードのID(すでに投票していれば-1)
  public int numOfVotes = 0;// 自分に投票したノードの数
  public String logFilePath;// ログのファイルパス
  private boolean stopped = false;// ノードを停止するためのフラグ

  /**
   * デフォルトのコンストラクタ
   * 
   * @param id
   */
  public Node(long id) {
    this(id, NodeStatus.FOLLOWER);
  }

  /**
   * 状態を指定するコンストラクタ
   * 
   * @param id
   * @param status
   */
  public Node(long id, NodeStatus status) {
    this.id = id;
    this.status = status;
  }

  /**
   * ノードリストのセッターメソッド
   * 
   * @param nodeList
   */
  public void setNodeList(List<Node> nodeList) {
    this.nodeList = nodeList;
  }

  public void setLogFile(String logFilePath) {
    this.logFilePath = logFilePath;
  }

  /**
   * 状態を変更するメソッド
   * 
   * @param status
   */
  public void changeStatus(NodeStatus status) {
    this.status = status;
  }

  /**
   * 状態をアップグレードするメソッド
   * フォロワーは候補者に，候補者はリーダーに昇格する．
   */
  private void upgrade() {
    if (status == NodeStatus.FOLLOWER) {
      changeStatus(NodeStatus.CANDIDATE);
      // ログを残す
      String logData = toString() + " upgraded to CANDIDATE";
      writeData(logFilePath, logData);
      System.out.println(logData);
    } else if (status == NodeStatus.CANDIDATE) {
      changeStatus(NodeStatus.LEADER);
      // ログを残す
      String logData = toString() + " upgraded to LEADER";
      writeData(logFilePath, logData);
      System.out.println(logData);
    }
  }

  /**
   * 状態をダウングレードするメソッド
   * リーダーは候補者に，候補者はフォロワーに降格する．
   */
  private void downgrade() {
    if (status == NodeStatus.LEADER) {
      changeStatus(NodeStatus.CANDIDATE);
      // ログを残す
      String logData = toString() + " downgraded to CANDIDATE";
      writeData(logFilePath, logData);
      System.out.println(logData);
    } else if (status == NodeStatus.CANDIDATE) {
      changeStatus(NodeStatus.FOLLOWER);
      // ログを残す
      String logData = toString() + " downgraded to FOLLOWER";
      writeData(logFilePath, logData);
      System.out.println(logData);
    }
  }

  /**
   * ランダムなタイムアウト時間を生成するメソッド
   * 
   * @return
   */
  private double randomTimeout() {
    if (status == NodeStatus.LEADER) {
      return 2000 + (Math.random() * (3000 - 2000));
    }
    return 4000 + (Math.random() * (6000 - 4000));
  }

  /**
   * タイムアウト時間をリセットするメソッド
   */
  private void newTimeout() {
    startTime = RaftUtils.currentTimestamp();
    timeout = randomTimeout();
  }

  /**
   * タイムアウト判定メソッド
   * 
   * @return
   */
  private boolean timeoutExpired() {
    if (timeout == 0) {
      return false;
    }
    return RaftUtils.currentTimestamp() > startTime + timeout;
  }

  /**
   * ノード起動メソッド
   */
  @Override
  public void run() {
    // ログを残す
    String logData = toString() + " started";
    writeData(logFilePath, logData);
    System.out.println(logData);

    newTimeout();

    while (!stopped) {
      readMessages();

      if (status == NodeStatus.FOLLOWER) {
        followerBehaviour();
      } else if (status == NodeStatus.CANDIDATE) {
        candidateBehaviour();
      } else if (status == NodeStatus.LEADER) {
        leaderBehaviour();
      }
    }
  }

  /**
   * ノードを停止するメソッド
   */
  public void shutdown() {
    stopped = true;
  }

  /**
   * メッセージ取得メソッド
   */
  private void readMessages() {
    Message msg;
    // メッセージがなくなるまで読み取る
    while ((msg = queue.poll()) != null) {
      if (msg.getClass() == HeartbeatMessage.class) {
        readHeartBeatMessages(msg);
      } else if (msg.getClass() == VoteMessage.class) {
        readVoteMessages(msg);
      } else if (msg.getClass() == RequestVoteMessage.class) {
        readRequestsVoteMessages(msg);
      }
    }
  }

  /**
   * ハートビートメッセージを読み取るメソッド
   * 
   * @param msg
   */
  private void readHeartBeatMessages(Message msg) {
    HeartbeatMessage heartbeatMsg = (HeartbeatMessage) msg;

    if (status != NodeStatus.LEADER) {// リーダー以外がリーダーのメッセージを受け取った場合
      newTimeout();
      // ログを残す
      String logData = toString() + " received: " + heartbeatMsg;
      writeData(logFilePath, logData);
      System.out.println(logData);
    } else if (heartbeatMsg.leaderId != id) {// 他のリーダーからメッセージを受け取った場合
      downgrade();
      // ログを残す
      String logData = String.format("%d - I'm the leader and I received a heartbeat from %d: downgrading...", id,
          heartbeatMsg.leaderId);
      writeData(logFilePath, logData);
      System.out.println(logData);
    } else {// リーダーが自身のメッセージを受け取った場合
      newTimeout();
      // ログを残す
      String logData = toString() + " received my: " + heartbeatMsg;
      writeData(logFilePath, logData);
      System.out.println(logData);
    }
  }

  /**
   * 投票メッセージを読み取るメソッド
   * 
   * @param msg
   */
  private void readVoteMessages(Message msg) {
    VoteMessage voteMsg = (VoteMessage) msg;
    // ログを残す
    String logData = toString() + " received: " + voteMsg;
    writeData(logFilePath, logData);
    System.out.println(logData);

    if (voteMsg.voteGranted) {
      numOfVotes++;
    }
  }

  /**
   * 投票リクエストメッセージを読み取るメソッド
   * 
   * @param msg
   */
  private void readRequestsVoteMessages(Message msg) {
    RequestVoteMessage requestVoteMsg = (RequestVoteMessage) msg;
    // ログを残す
    String logData = toString() + " received: " + requestVoteMsg;
    writeData(logFilePath, logData);
    System.out.println(logData);

    if (requestVoteMsg.term < currentTerm) {
      // 過去の任期のリクエストには投票しない．
      sendMessageTo(requestVoteMsg.candidateId, VoteMessage.write(false, requestVoteMsg.term));
    } else if (votedFor == -1 || requestVoteMsg.candidateId == -1) {
      // まだ投票をしていなければ投票する．
      votedFor = requestVoteMsg.candidateId;
      sendMessageTo(requestVoteMsg.candidateId, VoteMessage.write(true, requestVoteMsg.term));
    } else {
      // すでに投票している場合，投票しない．
      sendMessageTo(requestVoteMsg.candidateId, VoteMessage.write(false, requestVoteMsg.term));
    }
    newTimeout();
  }

  /**
   * リーダーの行動メソッド
   * タイムアウトするとハートビートメッセージを送る
   */
  private void leaderBehaviour() {
    if (timeoutExpired()) {
      broadcastMessage(HeartbeatMessage.write(id, currentTerm));
    }
  }

  /**
   * フォロワーの行動メソッド
   * ハートビートメッセージを受けるとタイムアウト時間がリセットされるので
   * タイムアウトした時はリーダーとの通信ができていないことを表す．
   * その場合，任期をインクリメントし，候補者として他のノードに投票リクエストを送る．
   */
  private void followerBehaviour() {
    if (timeoutExpired()) {
      currentTerm++;
      numOfVotes = 0;
      upgrade();
      broadcastMessage(RequestVoteMessage.write(id, currentTerm));
      newTimeout();
    }
  }

  /**
   * 候補者の行動メソッド
   * 過半数の投票が得られれば，リーダに昇格する．
   * タイムアウトするとフォロワーに戻る．
   */
  private void candidateBehaviour() {
    if (numOfVotes >= (nodeList.size() / 2)) {
      upgrade();
      // ログを残す
      String logData = toString() + " got majority (" + numOfVotes + " votes)";
      writeData(logFilePath, logData);
      System.out.println(logData);

      numOfVotes = 0;
      newTimeout();
    }

    if (timeoutExpired()) {
      downgrade();
      // ログを残す
      String logData = toString() + " timeout expired to become leader";
      writeData(logFilePath, logData);
      System.out.println(logData);
    }
  }

  /**
   * 指定したノードにメッセージを追加するメソッド
   * 
   * @param nodeId
   * @param message
   */
  private void sendMessageTo(long nodeId, Message message) {
    synchronized (nodeList) {
      for (Node node : nodeList) {
        if (node.id == nodeId) {
          node.appendMessage(message);
        }
      }
    }
  }

  /**
   * 全てのノードにメッセージを追加するメソッド
   * 
   * @param message
   */
  private void broadcastMessage(Message message) {
    synchronized (nodeList) {
      for (Node node : nodeList) {
        node.appendMessage(message);
      }
    }
  }

  /**
   * メッセージを追加するメソッド
   * 
   * @param message
   */
  public void appendMessage(Message message) {
    queue.add(message);
  }

  /**
   * ノードのidを文字列で返すメソッド
   */
  public String toString() {
    return String.format("Node[ id=%d ]", id);
  }

  /**
   * データを書き込むメソッド
   * 第1引数のファイルに第2引数の内容を追記する．
   * 
   * @param logFilePath
   * @param data
   */
  private void writeData(String logFilePath, String data) {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFilePath, true))) {
      writer.write(data);
      writer.newLine();
    } catch (IOException e) {
      System.err.println("エラーが発生しました: " + e.getMessage());
    }
  }

}
