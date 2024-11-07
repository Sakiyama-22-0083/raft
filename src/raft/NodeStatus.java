package raft;

/**
 * ノードの状態
 */
public enum NodeStatus {

  FOLLOWER(1),
  CANDIDATE(2),
  LEADER(3);

  private NodeStatus(int status) {
  }

}
