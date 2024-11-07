package raft;

import raft.messages.Message;
import raft.messages.VoteMessage;
import raft.messages.HeartbeatMessage;
import raft.messages.RequestVoteMessage;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * ノードを表すクラス
 */
public class Node extends Thread {

  // shared memory of node list between nodes
  protected List<Node> nodeList;

  public long id;
  public NodeStatus status;

  // timeout management
  public double startTime;
  public double timeout;

  // message queue
  public BlockingQueue<Message> queue = new LinkedBlockingQueue<Message>();

  public long[] workingNodes;
  public int currentTerm = 0; // incrémenté à chaque fois que le timeout expire
                              // lorsque follower
  public long votedFor = -1;
  public int numOfVotes = 0;

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
      System.out.println(toString() + " upgraded to CANDIDATE");
    } else if (status == NodeStatus.CANDIDATE) {
      changeStatus(NodeStatus.LEADER);
      System.out.println(toString() + " upgraded to LEADER");
    }
  }

  /**
   * 状態をダウングレードするメソッド
   * リーダーは候補者に，候補者はフォロワーに降格する．
   */
  private void downgrade() {
    if (status == NodeStatus.LEADER) {
      changeStatus(NodeStatus.CANDIDATE);
      System.out.println(toString() + " downgraded to CANDIDATE");
    } else if (status == NodeStatus.CANDIDATE) {
      changeStatus(NodeStatus.FOLLOWER);
      System.out.println(toString() + " downgraded to FOLLOWER");
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
    System.out.println(toString() + " started");

    newTimeout();

    while (true) {
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
   * メッセージ取得メソッド
   */
  private void readMessages() {
    Message msg;
    // メッセージがなくなるまで読み取る
    while ((msg = queue.poll()) != null) {
      if (msg.getClass() == HeartbeatMessage.class) {
        HeartbeatMessage heartbeatMsg = (HeartbeatMessage) msg;

        if (status != NodeStatus.LEADER) {
          // リーダー以外がリーダーのメッセージを受け取った場合
          newTimeout();
          System.out.println(toString() + " received: " + heartbeatMsg);
        } else if (heartbeatMsg.leaderId != id) {
          // 他のリーダーからメッセージを受け取った場合
          downgrade();
          System.out.println(String.format(
              "%d - I'm the leader and I received a heartbeat from %d: downgrading...",
              id, heartbeatMsg.leaderId));
        } else {
          // リーダーが自身のメッセージを受け取った場合
          newTimeout();
          System.out.println(toString() + " received my: " + heartbeatMsg);
        }
      } else if (msg.getClass() == VoteMessage.class) {
        VoteMessage voteMsg = (VoteMessage) msg;

        System.out.println(toString() + " received: " + voteMsg);

        if (voteMsg.voteGranted) {
          numOfVotes++;
        }
      } else if (msg.getClass() == RequestVoteMessage.class) {
        RequestVoteMessage requestVoteMsg = (RequestVoteMessage) msg;

        System.out.println(toString() + " received: " + requestVoteMsg);

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
    }
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
      System.out.println(toString() + " got majority (" + numOfVotes + " votes)");
      numOfVotes = 0;
      newTimeout();
    }

    if (timeoutExpired()) {
      System.out.println(toString() + " timeout expired to become leader");
      downgrade();
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

}
