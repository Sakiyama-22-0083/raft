package raft;

/**
 * ノードの状態を表す列挙型クラス
 */
public enum NodeStatus {

  FOLLOWER(1),
  CANDIDATE(2),
  LEADER(3);

  private NodeStatus(int status) {
  }

}
