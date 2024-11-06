package raft.messages;

/**
 * 投票リクエストメッセージクラス
 */
public class RequestVoteMessage extends Message {

  public long candidateId;// 候補者ID
  public int term;// 任期

  public RequestVoteMessage(long nodeId, int term) {
    this.candidateId = nodeId;
    this.term = term;
  }

  /**
   * 投票リクエストメッセージ作成メソッド
   * 
   * @param nodeId
   * @param term
   * @return
   */
  public static Message write(long nodeId, int term) {
    return new RequestVoteMessage(nodeId, term);
  }

  /**
   * 候補IDのゲッター
   * 
   * @return
   */
  public long getCandidateId() {
    return candidateId;
  }

  /**
   * 候補IDのセッター
   * 
   * @param candidateId
   */
  public void setCandidateId(long candidateId) {
    this.candidateId = candidateId;
  }

  /**
   * 任期のゲッター
   * 
   * @return
   */
  public double getTerm() {
    return term;
  }

  /**
   * 任期のセッター
   * 
   * @param term
   */
  public void setTerm(int term) {
    this.term = term;
  }

  /**
   * 保持する情報を文字列で返すメソッド
   */
  @Override
  public String toString() {
    return String.format("RequestVoteMessage[ candidate=%d, term=%d ]",
        candidateId, term);
  }

}
