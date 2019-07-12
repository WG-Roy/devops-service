package io.choerodon.devops.infra.convertor;


import io.choerodon.core.convertor.ConvertorI;
import io.choerodon.devops.api.vo.DevopsConfigMapDTO;
import io.choerodon.devops.api.vo.iam.entity.DevopsConfigMapE;
import io.choerodon.devops.infra.dto.DevopsConfigMapDO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

<<<<<<< HEAD
<<<<<<< HEAD:src/main/java/io/choerodon/devops/infra/convertor/DevopsConfigMapConvertor.java
=======
import io.choerodon.core.convertor.ConvertorI;
import io.choerodon.devops.api.vo.DevopsConfigMapDTO;
import io.choerodon.devops.api.vo.iam.entity.DevopsConfigMapE;
import io.choerodon.devops.infra.dataobject.DevopsConfigMapDO;
>>>>>>> [IMP] 修改AppControler重构:src/main/java/io/choerodon/devops/domain/application/convertor/DevopsConfigMapConvertor.java
=======
>>>>>>> [REF] refactor DevopsBranchRepository

@Component
public class DevopsConfigMapConvertor implements ConvertorI<DevopsConfigMapE, DevopsConfigMapDO, DevopsConfigMapDTO> {
    @Override
    public DevopsConfigMapE doToEntity(DevopsConfigMapDO devopsConfigMapDO) {
        DevopsConfigMapE devopsConfigMapE = new DevopsConfigMapE();
        BeanUtils.copyProperties(devopsConfigMapDO, devopsConfigMapE);
        if (devopsConfigMapDO.getEnvId() != null) {
            devopsConfigMapE.initDevopsEnvironmentE(devopsConfigMapDO.getEnvId());
        }
        if (devopsConfigMapDO.getCommandId() != null) {
            devopsConfigMapE.initDevopsEnvCommandE(devopsConfigMapDO.getCommandId());
        }
        return devopsConfigMapE;
    }

    @Override
    public DevopsConfigMapDO entityToDo(DevopsConfigMapE devopsConfigMapE) {
        DevopsConfigMapDO devopsConfigMapDO = new DevopsConfigMapDO();
        BeanUtils.copyProperties(devopsConfigMapE, devopsConfigMapDO);
        if (devopsConfigMapE.getDevopsEnvCommandE() != null) {
            devopsConfigMapDO.setCommandId(devopsConfigMapE.getDevopsEnvCommandE().getId());
        }
        if (devopsConfigMapE.getDevopsEnvironmentE() != null) {
            devopsConfigMapDO.setEnvId(devopsConfigMapE.getDevopsEnvironmentE().getId());
        }
        return devopsConfigMapDO;
    }

    @Override
    public DevopsConfigMapE dtoToEntity(DevopsConfigMapDTO devopsConfigMapDTO) {
        DevopsConfigMapE devopsConfigMapE = new DevopsConfigMapE();
        BeanUtils.copyProperties(devopsConfigMapDTO, devopsConfigMapE);
        if (devopsConfigMapDTO.getEnvId() != null) {
            devopsConfigMapE.initDevopsEnvironmentE(devopsConfigMapDTO.getEnvId());
        }
        return devopsConfigMapE;
    }


}
