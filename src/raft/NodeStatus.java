package raft;

/**
 * ノードの状態クラス
 */
public enum NodeStatus {

  FOLLOWER(1),
  CANDIDATE(2),
  LEADER(2);

  private NodeStatus(int status) {
  }

}
