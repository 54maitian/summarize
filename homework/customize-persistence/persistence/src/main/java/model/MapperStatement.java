package model;

import cache.Cache;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MapperStatement {
  // sqlid
  private String id;
  // 参数类型
  private String parameterType;
  // 结果集类型
  private String resultType;
  // 具体sql
  private String sql;
  //sql类型
  private String sqlType;
  private Cache cache;
}
