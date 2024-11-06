package raft.messages;

/**
 * 投票メッセージクラス
 */
public class VoteMessage extends Message {

  public boolean voteGranted;
  public int term;

  public VoteMessage(boolean voteGranted, int term) {
    this.voteGranted = voteGranted;
    this.term = term;
  }

  /**
   * 投票メッセージの作成メソッド
   * 
   * @param voteGranted
   * @param term
   * @return
   */
  public static Message write(boolean voteGranted, int term) {
    return new VoteMessage(voteGranted, term);
  }

  /**
   * 保持する情報を文字列で返すメソッド
   */
  @Override
  public String toString() {
    return String.format("VoteMessage[ voteGranted=%b, term=%d ]", voteGranted,
        term);
  }

}
