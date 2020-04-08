package io.choerodon.devops.app.service;

import java.util.List;

import io.choerodon.devops.api.vo.DevopsCiPipelineVO;
import io.choerodon.devops.infra.dto.DevopsCiPipelineDTO;

/**
 * 〈功能简述〉
 * 〈ci流水线servcie〉
 *
 * @author wanghao
 * @Date 2020/4/2 17:59
 */
public interface DevopsCiPipelineService {
    /**
     * 创建流水线
     * @param projectId
     * @param devopsCiPipelineVO
     * @return
     */
    DevopsCiPipelineDTO create(Long projectId, DevopsCiPipelineVO devopsCiPipelineVO);

    /**
     * 更新流水线
     * @param projectId
     * @param ciPipelineId
     * @param devopsCiPipelineVO
     * @return
     */
    DevopsCiPipelineDTO update(Long projectId, Long ciPipelineId, DevopsCiPipelineVO devopsCiPipelineVO);

    DevopsCiPipelineVO query(Long projectId, Long ciPipelineId);

    /**
     * 根据应用服务id查询流水线
     * @param id
     * @return
     */
    DevopsCiPipelineDTO queryByAppSvcId(Long id);

    List<DevopsCiPipelineVO> listByProjectIdAndAppName(Long projectId, String name);
}