package io.choerodon.devops.api.vo;

import java.util.List;

import io.swagger.annotations.ApiModelProperty;

import io.choerodon.devops.api.vo.polaris.PolarisStorageControllerResultVO;

/**
 * 带有Polaris扫描结果的实例数据
 *
 * @author zmf
 * @since 2/18/20
 */
public class InstanceWithPolarisResultVO {
    @ApiModelProperty("环境id")
    private Long envId;
    @ApiModelProperty("实例id")
    private Long instanceId;
    @ApiModelProperty("实例code")
    private String instanceCode;
    @ApiModelProperty("应用服务名称")
    private String appServiceName;
    @ApiModelProperty("应用服务id")
    private Long appServiceId;
    @ApiModelProperty("应用服务code")
    private String appServiceCode;
    @ApiModelProperty("实例包含的配置文件")
    private List<PolarisStorageControllerResultVO> items;

    public Long getEnvId() {
        return envId;
    }

    public void setEnvId(Long envId) {
        this.envId = envId;
    }

    public Long getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(Long instanceId) {
        this.instanceId = instanceId;
    }

    public String getInstanceCode() {
        return instanceCode;
    }

    public void setInstanceCode(String instanceCode) {
        this.instanceCode = instanceCode;
    }

    public String getAppServiceName() {
        return appServiceName;
    }

    public void setAppServiceName(String appServiceName) {
        this.appServiceName = appServiceName;
    }

    public Long getAppServiceId() {
        return appServiceId;
    }

    public void setAppServiceId(Long appServiceId) {
        this.appServiceId = appServiceId;
    }

    public String getAppServiceCode() {
        return appServiceCode;
    }

    public void setAppServiceCode(String appServiceCode) {
        this.appServiceCode = appServiceCode;
    }

    public List<PolarisStorageControllerResultVO> getItems() {
        return items;
    }

    public void setItems(List<PolarisStorageControllerResultVO> items) {
        this.items = items;
    }
}
