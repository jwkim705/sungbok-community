package com.sungbok.community.common.vo;

import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@XmlRootElement(name = "ServiceResult")
public class ServiceResultVO {

  private MsgBodyVO msgBody;


}
