package raft;

import java.util.List;
import java.util.ArrayList;

/**
 * ログクラス
 */
public class Log {

  protected List<LogEntry> log;

  public Log() {
    log = new ArrayList<LogEntry>();
  }

}
