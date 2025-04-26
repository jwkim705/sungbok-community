package com.sungbok.community.common.vo;

import com.rsupport.shuttlecock.dto.OpenApiResponseDTO;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@XmlRootElement(name = "msgBody")
public class MsgBodyVO {

  private List<OpenApiResponseDTO> itemList;

}
