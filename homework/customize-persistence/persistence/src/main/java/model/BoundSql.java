package model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import utils.ParameterMapping;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class BoundSql {
  //解析后sql
  private String sql;
  //对应sql参数
  private List<ParameterMapping> mappingList;
}
