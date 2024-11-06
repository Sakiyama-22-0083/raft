package raft;

public class LogEntry {

  /**
   * ログを項目ごとに保持するクラス
   */
  protected String command;
  protected int term;

  /**
   * ログ内容を受け取り保持するコンストラクタ
   *
   * @param command
   * @param period
   */
  public LogEntry(String command, int period) {
    this.command = command;
    this.term = period;
  }

}
