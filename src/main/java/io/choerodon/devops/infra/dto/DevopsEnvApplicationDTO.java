package io.choerodon.devops.infra.dto;

import javax.persistence.Table;

@Table(name = "devops_env_application")
public class DevopsEnvApplicationDTO {
    private Long appServiceId;
    private Long envId;

    public DevopsEnvApplicationDTO() {
    }

    public DevopsEnvApplicationDTO(Long appServiceId, Long envId) {
        this.appServiceId = appServiceId;
        this.envId = envId;
    }

    public Long getAppServiceId() {
        return appServiceId;
    }

    public void setAppServiceId(Long appServiceId) {
        this.appServiceId = appServiceId;
    }

    public Long getEnvId() {
        return envId;
    }

    public void setEnvId(Long envId) {
        this.envId = envId;
    }
}