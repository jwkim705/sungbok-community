package com.sungbok.community.common.vo;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.Serial;
import java.io.Serializable;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonInclude(value = Include.NON_NULL)
public abstract class AbstractObject implements Serializable {
  @Serial
  private static final long serialVersionUID = -1633867412493063160L;

  public String toString() {
    MappingJackson2HttpMessageConverter mapper = new MappingJackson2HttpMessageConverter();
    try {
      return mapper.getObjectMapper().writeValueAsString(this);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
