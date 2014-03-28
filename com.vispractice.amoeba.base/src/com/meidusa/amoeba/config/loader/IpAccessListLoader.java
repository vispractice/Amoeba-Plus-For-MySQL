package com.meidusa.amoeba.config.loader;

import java.util.List;

public interface IpAccessListLoader {
  public List<String> loadIPRule();
  public List<String> reLoadIPRule();
}
