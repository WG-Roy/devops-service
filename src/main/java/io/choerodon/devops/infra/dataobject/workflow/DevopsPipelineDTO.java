package io.choerodon.devops.infra.dataobject.workflow;

import java.util.List;


/**
 * Created by Sheep on 2019/4/2.
 */
public class DevopsPipelineDTO {

    private Long pipelineRecordId;
    private List<DevopsPipelineStageDTO> stages;

    public Long getPipelineRecordId() {
        return pipelineRecordId;
    }

    public void setPipelineRecordId(Long pipelineRecordId) {
        this.pipelineRecordId = pipelineRecordId;
    }

    public List<DevopsPipelineStageDTO> getStages() {
        return stages;
    }

    public void setStages(List<DevopsPipelineStageDTO> stages) {
        this.stages = stages;
    }
}