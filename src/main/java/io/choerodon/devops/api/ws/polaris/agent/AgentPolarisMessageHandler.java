package io.choerodon.devops.api.ws.polaris.agent;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import io.choerodon.devops.api.vo.polaris.PolarisResponsePayloadVO;
import io.choerodon.devops.infra.util.TypeUtil;
import io.choerodon.websocket.receive.TextMessageHandler;

@Component
public class AgentPolarisMessageHandler implements TextMessageHandler<PolarisResponsePayloadVO> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgentPolarisMessageHandler.class);

    @Override
    public void handle(WebSocketSession webSocketSession, String type, String key, PolarisResponsePayloadVO message) {
        //设置集群id
        Long clusterId = TypeUtil.objToLong(key.split(":")[1]);
        // TODO by zmf 存数据库
        LOGGER.info("Polaris message received: {}", message);
        try {
            webSocketSession.close();
        } catch (IOException e) {
            LOGGER.warn("Exception occurred when close webSocketSession for cluster with id {} and for type polaris. The ex is: {}", e);
        }
    }

    @Override
    public String matchPath() {
        return "/agent/polaris";
    }

    @Override
    public String matchType() {
        return "polaris";
    }
}
