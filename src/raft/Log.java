package raft;

import java.util.List;
import java.util.ArrayList;

public class Log {

  protected List<LogEntry> log;

  public Log() {
    log = new ArrayList<LogEntry>();
  }

}
