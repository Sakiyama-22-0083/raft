package raft.messages;

/**
 * ハートビートメッセージクラス
 */
public class HeartbeatMessage extends Message {

  public long leaderId;// リーダーのID
  public int term;// 任期

  public HeartbeatMessage(long leaderId, int term) {
    this.leaderId = leaderId;
    this.term = term;
  }

  /**
   * メッセージ作成メソッド
   * 
   * @param leaderId
   * @param term
   * @return
   */
  public static Message write(long leaderId, int term) {
    return new HeartbeatMessage(leaderId, term);
  }

  /**
   * 保持する情報を文字列で返すメソッド
   */
  @Override
  public String toString() {
    return String.format("HeartbeatMessage[ leader=%d, term=%d ]", leaderId, term);
  }

}
